
package com.axi.assistant

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.*
import android.graphics.Color
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var tts: TextToSpeech

    private var selectedLocale = Locale("ta", "IN")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = Color.rgb(15, 15, 25)
        window.navigationBarColor = Color.BLACK

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(32, 32, 32, 32)
        layout.setBackgroundColor(Color.rgb(15, 15, 25))

        val title = TextView(this)
        title.text = "AXI AI ASSISTANT 🚀"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER

        statusText = TextView(this)
        statusText.text = "Vanakkam! I am AXI."
        statusText.textSize = 18f
        statusText.setTextColor(Color.LTGRAY)
        statusText.gravity = Gravity.CENTER

        resultText = TextView(this)
        resultText.text = "Tamil + English Voice Assistant"
        resultText.textSize = 20f
        resultText.setTextColor(Color.WHITE)
        resultText.gravity = Gravity.CENTER

        val tamilButton = Button(this)
        tamilButton.text = "தமிழ்"

        val englishButton = Button(this)
        englishButton.text = "English"

        val micButton = Button(this)
        micButton.text = "🎤 Speak to AXI"

        val speakButton = Button(this)
        speakButton.text = "🔊 AXI Speak"

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(tamilButton)
        layout.addView(englishButton)
        layout.addView(micButton)
        layout.addView(speakButton)

        setContentView(layout)

        tts = TextToSpeech(this) { result ->
            if (result == TextToSpeech.SUCCESS) {
                tts.language = selectedLocale
            }
        }

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        statusText.text = "Listening..."
                    }

                    override fun onBeginningOfSpeech() {
                        statusText.text = "Speak now..."
                    }

                    override fun onEndOfSpeech() {
                        statusText.text = "Processing..."
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val spokenText = matches?.firstOrNull() ?: ""

                        resultText.text = spokenText
                        statusText.text = "AXI heard you!"

                        if (selectedLocale.language == "ta") {
                            speak("நான் கேட்டேன். வணக்கம்!")
                        } else {
                            speak("I heard you. Hello!")
                        }
                    }

                    override fun onError(error: Int) {
                        statusText.text = "Try speaking again."
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }
            )
        } else {
            statusText.text = "Speech recognition not available"
        }

        tamilButton.setOnClickListener {
            selectedLocale = Locale("ta", "IN")
            tts.language = selectedLocale
            statusText.text = "Tamil selected"
            speak("வணக்கம்")
        }

        englishButton.setOnClickListener {
            selectedLocale = Locale("en", "IN")
            tts.language = selectedLocale
            statusText.text = "English selected"
            speak("Hello, I am AXI")
        }

        micButton.setOnClickListener {
            startListening()
        }

        speakButton.setOnClickListener {
            if (selectedLocale.language == "ta") {
                speak("வணக்கம்! நான் AXI.")
            } else {
                speak("Hello! I am AXI, your assistant.")
            }
        }
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
                100
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            selectedLocale.toLanguageTag()
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PARTIAL_RESULTS,
            true
        )

        speechRecognizer.startListening(intent)
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

        if (requestCode == 100 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            statusText.text = "Microphone permission required"
        }
    }

    private fun speak(message: String) {
        tts.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "AXI_SPEECH"
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}
