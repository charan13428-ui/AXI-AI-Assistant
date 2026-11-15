
package com.axi.assistant

import android.app.*
import android.content.Intent
import android.os.*
import android.speech.*
import android.speech.tts.TextToSpeech
import java.util.Locale

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

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        if (intent?.action == ACTION_STOP) {
            stopSession()
            return START_NOT_STICKY
        }

        language = intent?.getStringExtra(EXTRA_LANGUAGE) ?: language

        createChannel()
        startForeground(NOTIFICATION_ID, makeNotification())

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            stopSession()
            return START_NOT_STICKY
        }

        stopping = false

        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(listener)
        }

        if (tts == null) {
            tts = TextToSpeech(this) { result ->
                if (result == TextToSpeech.SUCCESS) {
                    val r = tts?.setLanguage(
                        Locale.forLanguageTag(language)
                    )
                    ttsReady =
                        r != TextToSpeech.LANG_MISSING_DATA &&
                        r != TextToSpeech.LANG_NOT_SUPPORTED
                }
            }
        }

        startListening()
        return START_STICKY
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(results: Bundle?) {}
        override fun onEvent(type: Int, params: Bundle?) {}

        override fun onError(error: Int) {
            if (!stopping) {
                handler.postDelayed({ startListening() }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            if (stopping) return

            val words = results
                ?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )
                ?.firstOrNull()
                ?.lowercase(Locale.ROOT)
                .orEmpty()

            if (words.isBlank()) {
                handler.postDelayed({ startListening() }, 700)
            } else {
                handleCommand(words)
            }
        }
    }

    private fun startListening() {
        if (stopping || recognizer == null) return

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                language
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }

        try {
            recognizer?.startListening(intent)
        } catch (_: Exception) {
            handler.postDelayed({ startListening() }, 1500)
        }
    }

    private fun handleCommand(words: String) {
        val tamil = language.startsWith("ta")

        when {
            words.contains("stop listening") ||
            words.contains("stop axi") ||
            words.contains("கேட்பதை நிறுத்து") ||
            words.contains("நிறுத்து") -> {
                speak(
                    if (tamil) "சரி. நிறுத்துகிறேன்."
                    else "Okay. Stopping now."
                )
                handler.postDelayed({ stopSession() }, 1200)
                return
            }

            words.contains("open settings") ||
            words.contains("settings") ||
            words.contains("அமைப்புகள்") -> {
                speak(
                    if (tamil) "அமைப்புகளை திறக்கிறேன்."
                    else "Opening Settings."
                )
                handler.postDelayed({
                    try {
                        startActivity(
                            Intent(android.provider.Settings.ACTION_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    } catch (_: Exception) {}
                }, 700)
                return
            }

            words.contains("open youtube") ||
            words.contains("யூடியூப்") -> {
                openApp("com.google.android.youtube", tamil)
                return
            }

            words.contains("open whatsapp") ||
            words.contains("வாட்ஸ்அப்") -> {
                openApp("com.whatsapp", tamil)
                return
            }

            words.contains("open chrome") ||
            words.contains("குரோம்") -> {
                openApp("com.android.chrome", tamil)
                return
            }

            words.contains("hello") ||
            words.contains("hi") ||
            words.contains("வணக்கம்") -> {
                speak(
                    if (tamil) "வணக்கம்! நான் AXI."
                    else "Hello! I am AXI."
                )
                return
            }

            else -> {
                speak(
                    if (tamil)
                        "உங்கள் கட்டளையை கேட்டேன். இந்த வசதி இன்னும் உருவாக்கப்படவில்லை."
                    else
                        "I heard you. That command is not supported yet."
                )
            }
        }
    }

    private fun openApp(packageName: String, tamil: Boolean) {
        speak(
            if (tamil) "செயலியை திறக்கிறேன்."
            else "Opening the app."
        )

        handler.postDelayed({
            try {
                val launch = packageManager
                    .getLaunchIntentForPackage(packageName)

                if (launch != null) {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(launch)
                } else {
                    speak(
                        if (tamil) "இந்த செயலி நிறுவப்படவில்லை."
                        else "That app is not installed."
                    )
                }
            } catch (_: Exception) {
                speak(
                    if (tamil) "செயலியை திறக்க முடியவில்லை."
                    else "I couldn't open that app."
                )
            }
        }, 700)
    }

    private fun speak(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "AXI_BACKGROUND"
            )
            handler.postDelayed({ startListening() }, 2500)
        } else {
            handler.postDelayed({ startListening() }, 1000)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL,
                "AXI background voice",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun makeNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AxiVoiceService::class.java)
                .setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("AXI voice is active")
            .setContentText("AXI is listening. Use Stop to end.")
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopIntent
            )
            .build()
    }

    private fun stopSession() {
        stopping = true
        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.destroy()
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}

        recognizer = null
        tts = null
        ttsReady = false

        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        stopping = true
        handler.removeCallbacksAndMessages(null)

        try {
            recognizer?.destroy()
            tts?.shutdown()
        } catch (_: Exception) {}

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
