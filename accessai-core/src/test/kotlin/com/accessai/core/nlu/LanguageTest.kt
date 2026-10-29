package com.accessai.core.nlu

import com.accessai.core.actions.ParamValue
import com.accessai.core.format.Naira
import com.accessai.core.protocol.Canonical
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LanguageTest {

    @Test
    fun `amounts in many forms`() {
        val cases = mapOf(
            "send 5000 to David" to 500_000L,
            "5k" to 500_000L,
            "₦5,000" to 500_000L,
            "N5000" to 500_000L,
            "1.5k" to 150_000L,
            "5,000.50" to 500_050L,
            "five thousand naira" to 500_000L,
            "a thousand naira" to 100_000L,
            "two thousand five hundred" to 250_000L,
            "one hundred and fifty thousand" to 15_000_000L,
            "5 thousand" to 500_000L,
            "2m" to 200_000_000L,
        )
        for ((text, kobo) in cases) assertEquals(kobo, AmountParser.parseKobo(text), text)
    }

    @Test
    fun `no amount or two amounts gives null`() {
        assertNull(AmountParser.parseKobo("send money to David"))
        assertNull(AmountParser.parseKobo("send 5000 or 6000"))
        assertNull(AmountParser.parseKobo("0"))
    }

    @Test
    fun `naira is spoken and displayed clearly`() {
        assertEquals("five thousand naira", Naira.spoken(500_000))
        assertEquals("eighty-four thousand two hundred and fifty naira", Naira.spoken(8_425_000))
        assertEquals("one naira and fifty kobo", Naira.spoken(150))
        assertEquals("one million naira", Naira.spoken(100_000_000))
        assertEquals("₦84,250", Naira.display(8_425_000))
        assertEquals("₦1,000.05", Naira.display(100_005))
    }

    @Test
    fun `confirmation grammar is exact`() {
        val phrase = "confirm transfer"
        for (ok in listOf("confirm", "confirm transfer", "Confirm transfer.", "please confirm transfer", "yes confirm transfer", "I confirm")) {
            assertTrue(LocalGrammar.isConfirm(LocalGrammar.normalize(ok), phrase), ok)
        }
        for (no in listOf("yes", "ok", "don't confirm", "do not confirm transfer", "confirm transfer to david", "cancel", "confirmed", "")) {
            assertFalse(LocalGrammar.isConfirm(LocalGrammar.normalize(no), phrase), no)
        }
    }

    @Test
    fun `controls are whole-utterance only`() {
        assertEquals(Control.CANCEL, LocalGrammar.control(LocalGrammar.normalize("Cancel!")))
        assertEquals(Control.REPEAT, LocalGrammar.control(LocalGrammar.normalize("say that again please")))
        assertNull(LocalGrammar.control(LocalGrammar.normalize("cancel my card")))
    }

    @Test
    fun `candidate picking`() {
        val labels = listOf("David Adeyemi (…2391)", "David Okafor (…7710)")
        assertEquals(0, LocalGrammar.pickCandidate("adeyemi", labels))
        assertEquals(1, LocalGrammar.pickCandidate("david okafor", labels))
        assertEquals(1, LocalGrammar.pickCandidate("the second one", labels))
        assertNull(LocalGrammar.pickCandidate("david", labels))
        assertNull(LocalGrammar.pickCandidate("tunde", labels))
    }

    @Test
    fun `canonical json matches the python reference`() {
        val params = mapOf("recipient" to ParamValue.Text("ben_001"), "amount_minor" to ParamValue.Number(500_000))
        assertEquals("""{"amount_minor":500000,"recipient":"ben_001"}""", Canonical.json(params))
        // python: hashlib.sha256(json.dumps(p, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()).hexdigest()
        assertEquals("b729a280eabf6c63b6a6b889bfa1b26d4dad23286f2e3a36b98028e35879cd47", Canonical.sha256(params))
    }
}
