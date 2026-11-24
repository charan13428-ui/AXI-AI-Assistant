
package com.axi.assistant

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import android.graphics.Color
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private var selectedLocale = Locale("ta", "IN")
    private var ttsReady = false

    companion object {
        private const val REQUEST_MIC = 100
        private const val REQUEST_BACKGROUND_MIC = 101
        private const val REQUEST_NOTIFICATIONS = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 25)
        window.navigationBarColor = Color.BLACK

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(32, 32, 32, 32)
        layout.setBackgroundColor(Color.rgb(15, 15, 25))

        val title = TextView(this).apply {
            text = "JARVIS AI ASSISTANT"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            text = "Vanakkam! I am JARVIS."
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        resultText = TextView(this).apply {
            text = "Tamil + English Voice Assistant"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val tamilButton = Button(this).apply {
            text = "தமிழ்"
        }

        val englishButton = Button(this).apply {
            text = "English"
        }

        val micButton = Button(this).apply {
            text = "🎤 Speak to JARVIS"
        }

        val speakButton = Button(this).apply {
            text = "🔊 JARVIS Speak"
        }

        val backgroundStartButton = Button(this).apply {
            text = "🎙️ Start Background Voice"
        }

        val backgroundStopButton = Button(this).apply {
            text = "⏹ Stop Background Voice"
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(tamilButton)
        layout.addView(englishButton)
        layout.addView(micButton)
        layout.addView(speakButton)
        layout.addView(backgroundStartButton)
        layout.addView(backgroundStopButton)

        val scrollView = ScrollView(this)
        scrollView.addView(layout)
        setContentView(scrollView)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                val languageResult = tts.setLanguage(selectedLocale)

                ttsReady =
                    languageResult != TextToSpeech.LANG_MISSING_DATA &&
                    languageResult != TextToSpeech.LANG_NOT_SUPPORTED

                if (!ttsReady) {
                    statusText.text =
                        "Install the selected voice language in Android Text-to-Speech settings."
                }
            } else {
                statusText.text = "Text-to-speech is unavailable."
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        statusText.text =
                            if (selectedLocale.language == "ta")
                                "கேட்கிறது..."
                            else "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        statusText.text =
                            if (selectedLocale.language == "ta")
                                "பேசுங்கள்..."
                            else "Speak now..."
                    }

                    override fun onEndOfSpeech() {
                        statusText.text =
                            if (selectedLocale.language == "ta")
                                "புரிந்துகொள்கிறேன்..."
                            else "Processing..."
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val spokenText =
                            matches?.firstOrNull()?.trim().orEmpty()

                        resultText.text = spokenText

                        if (spokenText.isBlank()) {
                            statusText.text =
                                if (selectedLocale.language == "ta")
                                    "மீண்டும் முயற்சிக்கவும்."
                                else "I couldn't hear that. Please try again."
                        } else {
                            statusText.text =
                                if (selectedLocale.language == "ta")
                                    "JARVIS பதிலளிக்கிறது..."
                                else "JARVIS is responding..."

                            handleCommand(spokenText)
                        }
                    }

                    override fun onError(error: Int) {
                        statusText.text =
                            if (selectedLocale.language == "ta")
                                "குரலை புரிந்துகொள்ள முடியவில்லை. மீண்டும் முயற்சிக்கவும்."
                            else "Couldn't recognise that. Please try again."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            )
        } else {
            statusText.text =
                "Speech recognition not available on this device."
        }

        tamilButton.setOnClickListener {
            selectedLocale = Locale("ta", "IN")
            updateTtsLanguage()
            statusText.text = "தமிழ் தேர்ந்தெடுக்கப்பட்டது"
            speak("வணக்கம்! நான் JARVIS.")
        }

        englishButton.setOnClickListener {
            selectedLocale = Locale("en", "IN")
            updateTtsLanguage()
            statusText.text = "English selected"
            speak("Hello! I am JARVIS.")
        }

        micButton.setOnClickListener {
            startListening()
        }

        speakButton.setOnClickListener {
            if (selectedLocale.language == "ta") {
                speak("வணக்கம்! நான் JARVIS. உங்களுக்கு எப்படி உதவலாம்?")
            } else {
                speak("Hello! I am JARVIS. How can I help you?")
            }
        }

        backgroundStartButton.setOnClickListener {
            startBackgroundVoice()
        }

        backgroundStopButton.setOnClickListener {
            stopBackgroundVoice()
        }
    }

    private fun updateTtsLanguage() {
        if (::tts.isInitialized && ttsReady) {
            val result = tts.setLanguage(selectedLocale)

            ttsReady =
                result != TextToSpeech.LANG_MISSING_DATA &&
                result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun startBackgroundVoice() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_BACKGROUND_MIC
            )
            return
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_NOTIFICATIONS
            )
            return
        }

        launchBackgroundService()
    }

    private fun launchBackgroundService() {
        val serviceIntent = Intent(
            this,
            AxiVoiceService::class.java
        ).apply {
            putExtra(
                AxiVoiceService.EXTRA_LANGUAGE,
                selectedLocale.toLanguageTag()
            )
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            statusText.text =
                if (selectedLocale.language == "ta")
                    "JARVIS பின்னணியில் தொடங்கப்பட்டது."
                else "JARVIS background voice started."

        } catch (_: Exception) {
            statusText.text =
                "Couldn't start background voice. Please try again."
        }
    }

    private fun stopBackgroundVoice() {
        try {
            stopService(
                Intent(this, AxiVoiceService::class.java)
            )

            statusText.text =
                if (selectedLocale.language == "ta")
                    "JARVIS பின்னணி குரல் நிறுத்தப்பட்டது."
                else "JARVIS background voice stopped."

        } catch (_: Exception) {
            statusText.text = "Couldn't stop background voice."
        }
    }

    private fun handleCommand(originalText: String) {
        val text = originalText.lowercase(Locale.ROOT)
        val isTamil = selectedLocale.language == "ta"

        val response: String

        when {
            text.contains("open settings") ||
            text.contains("settings") ||
            text.contains("அமைப்புகள்") ||
            text.contains("செட்டிங்ஸ்") -> {

                response =
                    if (isTamil)
                        "தொலைபேசி அமைப்புகளைத் திறக்கிறேன்."
                    else "Opening your phone settings."

                speak(response)

                try {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                } catch (_: Exception) {
                    statusText.text =
                        if (isTamil)
                            "அமைப்புகளைத் திறக்க முடியவில்லை."
                        else "Couldn't open settings."
                }
                return
            }

            text.contains("time") ||
            text.contains("நேரம்") ||
            text.contains("மணி") -> {

                val time = DateFormat.getTimeInstance(
                    DateFormat.SHORT,
                    selectedLocale
                ).format(Date())

                response =
                    if (isTamil) "இப்போது நேரம் $time."
                    else "The time is $time."
            }

            text.contains("date") ||
            text.contains("today") ||
            text.contains("தேதி") ||
            text.contains("இன்று") -> {

                val date = DateFormat.getDateInstance(
                    DateFormat.LONG,
                    selectedLocale
                ).format(Date())

                response =
                    if (isTamil) "இன்றைய தேதி $date."
                    else "Today's date is $date."
            }

            text.contains("hello") ||
            text.contains("hi") ||
            text.contains("வணக்கம்") ||
            text.contains("ஹலோ") ||
            text.contains("vanakkam") -> {

                response =
                    if (isTamil)
                        "வணக்கம்! இன்று நான் உங்களுக்கு எப்படி உதவலாம்?"
                    else "Hello! How can I help you today?"
            }

            text.contains("your name") ||
            text.contains("who are you") ||
            text.contains("உன் பெயர்") ||
            text.contains("நீ யார்") -> {

                response =
                    if (isTamil)
                        "என் பெயர் JARVIS. நான் உங்கள் தமிழ் மற்றும் ஆங்கில குரல் உதவியாளர்."
                    else
                        "My name is JARVIS. I am your Tamil and English voice assistant."
            }

            text.contains("what can you do") ||
            text.contains("help") ||
            text.contains("என்ன செய்ய") ||
            text.contains("உதவி") -> {

                response =
                    if (isTamil)
                        "நான் வணக்கம் சொல்லலாம், நேரம் மற்றும் தேதியைச் சொல்லலாம், தொலைபேசி அமைப்புகளைத் திறக்கலாம். இன்னும் பல அம்சங்கள் உருவாக்கப்பட்டுக் கொண்டிருக்கின்றன."
                    else
                        "I can respond to greetings, tell you the time and date, and open phone settings. More features are still being developed."
            }

            text.contains("thank you") ||
            text.contains("thanks") ||
            text.contains("நன்றி") -> {

                response =
                    if (isTamil)
                        "மகிழ்ச்சி! வேறு ஏதாவது உதவி வேண்டுமா?"
                    else "You're welcome! What else can I help with?"
            }

            else -> {
                response =
                    if (isTamil)
                        "இந்தக் கேள்விக்கான பதில் இன்னும் எனக்குத் தெரியாது. தற்போது வணக்கம், நேரம், தேதி மற்றும் அமைப்புகள் போன்ற கட்டளைகளை முயற்சிக்கலாம்."
                    else
                        "I don't know the answer to that yet. For now, try asking the time or date, saying hello, or asking me to open settings."
            }
        }

        statusText.text =
            if (isTamil) "JARVIS பதிலளித்தது"
            else "JARVIS replied"

        speak(response)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Speech recognition unavailable"
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_MIC
            )
            return
        }

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                selectedLocale.toLanguageTag()
            )

            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                selectedLocale.toLanguageTag()
            )

            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )
        }

        try {
            speechRecognizer.startListening(intent)
        } catch (_: Exception) {
            statusText.text =
                if (selectedLocale.language == "ta")
                    "குரல் சேவையைத் தொடங்க முடியவில்லை."
                else "Couldn't start voice recognition."
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        val granted =
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED

        when (requestCode) {
            REQUEST_MIC -> {
                if (granted) {
                    startListening()
                } else {
                    statusText.text = "Microphone permission is required."
                }
            }

            REQUEST_BACKGROUND_MIC -> {
                if (granted) {
                    startBackgroundVoice()
                } else {
                    statusText.text =
                        "Microphone permission is required for background voice."
                }
            }

            REQUEST_NOTIFICATIONS -> {
                if (granted) {
                    launchBackgroundService()
                } else {
                    statusText.text =
                        "Notification permission is needed to start JARVIS background voice."
                }
            }
        }
    }

    private fun speak(message: String) {
        if (::tts.isInitialized && ttsReady) {
            tts.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_SPEECH"
            )
        } else {
            statusText.text =
                if (selectedLocale.language == "ta")
                    "குரல் வசதி தயாராகவில்லை. TTS மொழியைச் சரிபார்க்கவும்."
                else "Voice output isn't ready. Check your Text-to-Speech language."
        }
    }

    override fun onDestroy() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}
