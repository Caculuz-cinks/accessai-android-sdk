package com.accessai.android

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import com.accessai.core.Announcement
import com.accessai.core.engine.SpeechOutput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Speaks announcements. When a screen reader such as TalkBack is running and [deferToScreenReader]
 * is true, announcements go to the screen reader instead, so the user never hears two voices at once.
 */
internal class AndroidSpeechOutput(
    context: Context,
    private val locale: Locale,
    private val deferToScreenReader: Boolean,
    private val speechRate: Float,
) : SpeechOutput {

    private val appContext = context.applicationContext
    private val accessibility = appContext.getSystemService(AccessibilityManager::class.java)
    private val queued = mutableListOf<Announcement>()
    private var ready = false
    private val active = mutableSetOf<String>()

    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking.asStateFlow()

    private val tts: TextToSpeech = TextToSpeech(appContext) { status ->
        synchronized(queued) {
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                configureVoice()
                queued.forEach { speakWithTts(it) }
            }
            queued.clear()
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = update { active += utteranceId }
            override fun onDone(utteranceId: String) = update { active -= utteranceId }
            override fun onStop(utteranceId: String, interrupted: Boolean) = update { active -= utteranceId }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = update { active -= utteranceId }
            override fun onError(utteranceId: String, errorCode: Int) = update { active -= utteranceId }
        })
    }

    val screenReaderActive: Boolean
        get() = accessibility?.isEnabled == true && accessibility.isTouchExplorationEnabled

    override fun speak(announcement: Announcement) {
        if (deferToScreenReader && screenReaderActive) {
            announceToScreenReader(announcement.spoken)
            return
        }
        synchronized(queued) {
            if (ready) speakWithTts(announcement) else queued += announcement
        }
    }

    override fun stop() {
        synchronized(queued) { queued.clear() }
        if (ready) tts.stop()
        update { active.clear() }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }

    private fun speakWithTts(announcement: Announcement) {
        tts.speak(announcement.spoken, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
    }

    private fun configureVoice() {
        val candidates = listOf(locale, Locale("en", "NG"), Locale.UK, Locale.US)
        val chosen = candidates.firstOrNull { tts.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
        chosen?.let { tts.language = it }
        tts.setSpeechRate(speechRate)
    }

    @Suppress("DEPRECATION")
    private fun announceToScreenReader(text: String) {
        val am = accessibility ?: return
        val event = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AccessibilityEvent(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        } else {
            AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        }
        event.packageName = appContext.packageName
        event.className = javaClass.name
        event.text.add(text)
        am.sendAccessibilityEvent(event)
    }

    private inline fun update(block: () -> Unit) {
        synchronized(active) {
            block()
            _speaking.value = active.isNotEmpty()
        }
    }
}
