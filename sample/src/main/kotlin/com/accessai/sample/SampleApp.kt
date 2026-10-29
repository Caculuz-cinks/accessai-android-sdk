package com.accessai.sample

import android.app.Application
import com.accessai.android.AccessAI
import com.accessai.android.AccessAIConfig
import com.accessai.core.Announcement
import com.accessai.core.actions.ActionResult
import com.accessai.core.actions.ParamValue
import com.accessai.core.actions.Resolution
import com.accessai.core.actions.Sensitivity
import com.accessai.core.actions.Validation
import com.accessai.core.actions.action
import com.accessai.core.announce
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A minimal host app: everything a real integration needs, with an in-memory "bank".
 * The AccessPay demo app (separate repo) is the full version.
 */
class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AccessAI.init(
            this,
            AccessAIConfig(appId = "accessai-sample", serverUrl = BuildConfig.ACCESSAI_URL.ifBlank { null }),
        )
        registerActions()
    }

    private fun registerActions() {
        AccessAI.registerAction(action("get_balance") {
            name = "check your balance"
            description = "Read the user's available balance"
            examples("what is my balance", "how much money do I have")
            handler { ActionResult.Success(announce { text("Your available balance is "); naira(SampleBank.balance.value); text(".") }) }
        })

        AccessAI.registerAction(action("navigate_to_transfer") {
            name = "open transfers"
            sensitivity = Sensitivity.NAVIGATE
            examples("I want to send money", "open transfers")
            handler {
                SampleBank.screen.value = "transfer"
                ActionResult.Success(Announcement.of("Transfer money screen. Tell me who to send to and how much."))
            }
        })

        AccessAI.registerAction(action("get_transaction_history") {
            name = "read your recent transactions"
            examples("read my recent transactions", "transaction history")
            handler {
                val items = SampleBank.transactions.value
                ActionResult.Success(
                    Announcement.of("You have ${items.size} recent transactions."),
                    items = items.map { (desc, kobo) -> announce { text("$desc, "); naira(kobo); text(".") } },
                    emptyMessage = Announcement.of("You have no transactions yet."),
                )
            }
        })

        AccessAI.registerAction(action("transfer_money") {
            name = "send money"
            description = "Send simulated money to a saved beneficiary"
            sensitivity = Sensitivity.SENSITIVE
            confirmPhrase = "confirm transfer"
            text("recipient", "Beneficiary name as the user said it", prompt = "Who would you like to send money to?") { raw ->
                SampleBank.findBeneficiary((raw as ParamValue.Text).value)
            }
            amount("amount_minor", "Amount in kobo", prompt = "How much would you like to send?")
            examples("send 5000 naira to David", "transfer 2k to Amaka")
            validate { p ->
                if (p.number("amount_minor") > SampleBank.balance.value) Validation.Invalid("That's more than your available balance. What amount would you like to send?")
                else Validation.Valid
            }
            summary { p -> announce { text("send "); naira(p.number("amount_minor")); text(" to ${p.spokenLabel("recipient")}") } }
            handler { ctx ->
                // A real app sends ctx.confirmationToken and ctx.idempotencyKey to its backend, which verifies them.
                SampleBank.transfer(ctx.params.text("recipient"), ctx.params.number("amount_minor"), ctx.idempotencyKey)
                ActionResult.Success(announce {
                    text("Your simulated transfer was successful. Your remaining balance is "); naira(SampleBank.balance.value); text(".")
                })
            }
        })
    }
}

/** Simulated funds only. */
object SampleBank {
    data class Beneficiary(val id: String, val name: String, val last4: String)

    val balance = MutableStateFlow(8_425_000L)
    val screen = MutableStateFlow("home")
    val transactions = MutableStateFlow(listOf("Salary" to 25_000_000L, "Airtime purchase" to 100_000L))
    private val beneficiaries = listOf(
        Beneficiary("ben_001", "David Adeyemi", "2391"),
        Beneficiary("ben_002", "David Okafor", "7710"),
        Beneficiary("ben_003", "Amaka Obi", "5521"),
    )
    private val processed = mutableSetOf<String>()

    fun findBeneficiary(query: String): Resolution {
        val words = query.lowercase().split(' ').filter { it.isNotBlank() }
        val matches = beneficiaries.filter { b -> words.all { it in b.name.lowercase().split(' ') } }
        return when (matches.size) {
            0 -> Resolution.NotFound("I couldn't find $query in your saved beneficiaries.")
            1 -> matches[0].let { Resolution.Resolved(ParamValue.Text(it.id), "${it.name} (…${it.last4})", "${it.name}, account ending ${it.last4.toList().joinToString(" ")}") }
            else -> Resolution.Ambiguous(matches.map { Resolution.Candidate(ParamValue.Text(it.id), "${it.name} (…${it.last4})", it.name) })
        }
    }

    @Synchronized
    fun transfer(beneficiaryId: String, kobo: Long, idempotencyKey: String) {
        if (!processed.add(idempotencyKey)) return
        val name = beneficiaries.first { it.id == beneficiaryId }.name
        balance.value -= kobo
        transactions.value = listOf("Transfer to $name" to kobo) + transactions.value
    }
}
