package com.axi.assistant

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.provider.Settings
import java.util.Locale
import java.text.DateFormat
import java.util.Date

class AxiVoiceService : Service() {
    companion object {
        const val ACTION_STOP = "com.axi.assistant.STOP"
        const val EXTRA_LANGUAGE = "axi_language"
        private const val CHANNEL = "axi_voice"
        private const val NOTIFICATION_ID = 4101
    }
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var language = "en-IN"
    private var stopping = false
    private var listening = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopSession(); return START_NOT_STICKY }
        language = intent?.getStringExtra(EXTRA_LANGUAGE) ?: language
        createChannel()
        startForeground(NOTIFICATION_ID, makeNotification())
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { stopSession(); return START_NOT_STICKY }
        stopping = false
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(listener)
        }
        if (tts == null) {
            tts = TextToSpeech(this) { result ->
                if (result == TextToSpeech.SUCCESS) {
                    val code = tts?.setLanguage(Locale.forLanguageTag(language))
                    ttsReady = code != TextToSpeech.LANG_MISSING_DATA && code != TextToSpeech.LANG_NOT_SUPPORTED
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(id: String?) {}
                        override fun onDone(id: String?) { handler.post { scheduleListening(700) } }
                        override fun onError(id: String?) { handler.post { scheduleListening(700) } }
                    })
                }
            }
        }
        scheduleListening(500)
        return START_STICKY
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { listening = true }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { listening = false }
        override fun onPartialResults(results: Bundle?) {}
        override fun onEvent(type: Int, params: Bundle?) {}
        override fun onError(error: Int) { listening = false; if (!stopping) scheduleListening(1200) }
        override fun onResults(results: Bundle?) {
            listening = false
            val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
            if (words.isNotEmpty()) handleCommand(words) else scheduleListening(700)
        }
    }

    private fun scheduleListening(delay: Long) {
        if (stopping) return
        handler.removeCallbacks(restartRunnable)
        handler.postDelayed(restartRunnable, delay)
    }
    private val restartRunnable = Runnable { startListening() }

    private fun startListening() {
        if (stopping || listening || recognizer == null) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) { scheduleListening(1600) }
    }

    private fun handleCommand(raw: String) {
        val w = raw.lowercase(Locale.ROOT)
        val tamil = language.startsWith("ta")
        if (w.contains("stop listening") || w.contains("stop axi") || w.contains("நிறுத்து")) {
            speak(if (tamil) "சரி. நிறுத்துகிறேன்." else "Okay, stopping AXI.")
            handler.postDelayed({ stopSession() }, 1400); return
        }
        if (w.contains("settings") || w.contains("அமைப்புகள்")) {
            speak(if (tamil) "அமைப்புகளை திறக்கிறேன்." else "Opening Settings.")
            handler.postDelayed({ launchIntent(Intent(Settings.ACTION_SETTINGS)) }, 600); return
        }
        if (w.contains("time") || w.contains("நேரம்") || w.contains("மணி")) {
            speak((if (tamil) "இப்போது நேரம் " else "The time is ") + DateFormat.getTimeInstance(DateFormat.SHORT, Locale.forLanguageTag(language)).format(Date())); return
        }
        if (w.contains("date") || w.contains("today") || w.contains("தேதி") || w.contains("இன்று")) {
            speak((if (tamil) "இன்றைய தேதி " else "Today's date is ") + DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag(language)).format(Date())); return
        }
        if (w.contains("hello") || w.contains("hi") || w.contains("வணக்கம்")) {
            speak(if (tamil) "வணக்கம்! நான் AXI. எப்படி உதவலாம்?" else "Hello! I'm AXI. How can I help?"); return
        }
        if (w.contains("who are you") || w.contains("your name") || w.contains("நீ யார்")) {
            speak(if (tamil) "நான் AXI, உங்கள் குரல் உதவியாளர்." else "I'm AXI, your voice assistant."); return
        }
        if (w.contains("help") || w.contains("what can you do")) {
            speak(if (tamil) "செயலிகளை திறக்கவும், நேரம், தேதி, அமைப்புகள் மற்றும் இணைய தேடல் கட்டளைகளை முயற்சிக்கவும்." else "Try opening apps, asking for the time or date, opening Settings, or searching the web."); return
        }
        if (w.contains("search") || w.contains("google") || w.contains("தேடு")) {
            val query = raw.replace(Regex("(?i)search( for)?|google|தேடு"), "").trim()
            if (query.isNotBlank()) {
                speak(if (tamil) "இணையத்தில் தேடுகிறேன்." else "Searching the web.")
                val i = Intent(Intent.ACTION_WEB_SEARCH).putExtra(android.app.SearchManager.QUERY, query).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                handler.postDelayed({ launchIntent(i) }, 500); return
            }
        }
        // Resolve installed launchable apps by their visible app names.
        val cleaned = w.replace(Regex("^(open|launch|start)\\s+"), "").trim()
        if (cleaned.isNotBlank()) {
            val launcherQuery = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val matches = packageManager.queryIntentActivities(launcherQuery, 0)
            val app = matches.firstOrNull { info ->
                val label = info.loadLabel(packageManager)?.toString()?.lowercase(Locale.ROOT).orEmpty()
                label == cleaned || label.contains(cleaned)
            }
            if (app != null) {
                val launch = packageManager.getLaunchIntentForPackage(app.activityInfo.packageName)
                if (launch != null) {
                    val label = app.loadLabel(packageManager).toString()
                    speak(if (tamil) "$label செயலியை திறக்கிறேன்." else "Opening $label.")
                    handler.postDelayed({ launchIntent(launch) }, 500)
                    return
                }
            }
        }
        speak(if (tamil) "கட்டளையை கேட்டேன். இந்த அம்சத்திற்கு இணைய AI இணைப்பு தேவை." else "I heard you. General AI answers require an AI connection, which isn't configured yet.")
    }

    private fun launchIntent(i: Intent) {
        try { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) } catch (_: Exception) { scheduleListening(900) }
    }
    private fun speak(message: String) {
        if (ttsReady) tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "AXI_REPLY")
        else scheduleListening(900)
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "AXI background voice", NotificationManager.IMPORTANCE_LOW))
        }
    }
    private fun makeNotification(): Notification {
        val stopIntent = PendingIntent.getService(this, 2, Intent(this, AxiVoiceService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL) else @Suppress("DEPRECATION") Notification.Builder(this)
        return b.setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("AXI voice is active").setContentText("Listening. Tap Stop to end.").setOngoing(true).addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent).build()
    }
    private fun stopSession() {
        stopping = true; handler.removeCallbacksAndMessages(null)
        try { recognizer?.cancel(); recognizer?.destroy(); tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
        recognizer = null; tts = null; ttsReady = false; listening = false
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else @Suppress("DEPRECATION") stopForeground(true)
        stopSelf()
    }
    override fun onDestroy() { stopping = true; handler.removeCallbacksAndMessages(null); try { recognizer?.destroy(); tts?.shutdown() } catch (_: Exception) {}; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}