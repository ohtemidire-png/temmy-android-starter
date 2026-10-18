package com.temmy.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var reply: TextView
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private val requestAudio = 41
    private val bg = Color.rgb(9, 14, 28)
    private val cyan = Color.rgb(58, 220, 255)
    private val white = Color.rgb(245, 249, 255)
    private val muted = Color.rgb(164, 181, 204)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        tts = TextToSpeech(this, this)
        buildScreen()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }

        val brand = TextView(this).apply {
            text = "TEMMY"
            textSize = 30f
            setTextColor(cyan)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        layout.addView(brand, matchWrap())

        val tagline = TextView(this).apply {
            text = "YOUR PERSONAL AI ASSISTANT"
            textSize = 11f
            letterSpacing = 0.18f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        layout.addView(tagline, params(top = 6, bottom = 26))

        val orb = TextView(this).apply {
            text = "✦"
            textSize = 62f
            gravity = Gravity.CENTER
            setTextColor(cyan)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(Color.rgb(18, 36, 61))
                setStroke(dp(2), cyan)
            }
        }
        layout.addView(orb, LinearLayout.LayoutParams(dp(150), dp(150)).apply {
            gravity = Gravity.CENTER
            bottomMargin = dp(20)
        })

        status = TextView(this).apply {
            text = "Ready when you are"
            textSize = 17f
            setTextColor(white)
            gravity = Gravity.CENTER
        }
        layout.addView(status, params(bottom = 18))

        val mic = Button(this).apply {
            text = "🎙  TALK TO TEMMY"
            textSize = 15f
            isAllCaps = false
            setTextColor(bg)
            backgroundTintList = android.content.res.ColorStateList.valueOf(cyan)
            setOnClickListener { startListening() }
        }
        layout.addView(mic, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply {
            bottomMargin = dp(22)
        })

        val heardTitle = sectionTitle("YOU SAID")
        layout.addView(heardTitle, params(bottom = 6))
        transcript = bodyText("Your voice request will appear here.")
        layout.addView(transcript, params(bottom = 18))

        layout.addView(sectionTitle("TEMMY REPLIES"), params(bottom = 6))
        reply = bodyText("Hi! I'm TEMMY. Tap the microphone and try saying “Hello TEMMY” or “What can you do?”")
        layout.addView(reply, params(bottom = 18))

        val quick = Button(this).apply {
            text = "Try a sample command"
            isAllCaps = false
            setTextColor(white)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(35, 51, 76))
            setOnClickListener {
                transcript.text = "What can you do?"
                respond("I can listen to your voice, speak replies, tell you the time, and open Android settings. Online AI conversations will be connected in the next milestone.")
            }
        }
        layout.addView(quick, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)))

        val note = TextView(this).apply {
            text = "MILESTONE 1 • Voice prototype • Online AI not connected yet"
            textSize = 11f
            setTextColor(muted)
            gravity = Gravity.CENTER
        }
        layout.addView(note, params(top = 20))

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this phone right now.", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), requestAudio)
            return
        }
        try {
            if (recognizer == null) {
                recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
                    sr.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening… speak now" }
                        override fun onBeginningOfSpeech() { status.text = "I'm listening" }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() { status.text = "Thinking…" }
                        override fun onError(error: Int) {
                            status.text = "Ready when you are"
                            Toast.makeText(this@MainActivity, "Didn't catch that. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                        override fun onResults(results: Bundle?) {
                            val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                            status.text = "Ready when you are"
                            if (!words.isNullOrBlank()) {
                                transcript.text = words
                                handleCommand(words)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to TEMMY")
            }
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't start the microphone. Check permissions and try again.", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleCommand(words: String) {
        val q = words.lowercase(Locale.getDefault())
        when {
            "hello" in q || "hi temmy" in q ->
                respond("Hello! I'm TEMMY, your personal assistant. What would you like to do?")
            "what can you do" in q || "help" == q ->
                respond("I can hear voice requests, speak replies, tell you the time, and open Android settings. My online AI brain is the next step.")
            "time" in q -> {
                val time = java.text.SimpleDateFormat("h:mm a", Locale.getDefault()).format(java.util.Date())
                respond("The time is $time.")
            }
            "open settings" in q || "phone settings" in q -> {
                respond("Opening Android settings.")
                startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
            }
            "your name" in q || "who are you" in q ->
                respond("My name is TEMMY. I'm your personal assistant prototype.")
            else -> respond("I heard you say: $words. I don't have my online AI connection yet, so I can't answer that fully. We'll connect it in Milestone 2.")
        }
    }

    private fun respond(message: String) {
        reply.text = message
        tts?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "temmy_reply")
    }

    override fun onInit(statusCode: Int) {
        if (statusCode == TextToSpeech.SUCCESS) tts?.language = Locale.UK
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestAudio && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else if (requestCode == requestAudio) {
            Toast.makeText(this, "TEMMY needs microphone permission for voice input.", Toast.LENGTH_LONG).show()
        }
    }

    private fun sectionTitle(value: String) = TextView(this).apply {
        text = value
        textSize = 11f
        letterSpacing = 0.14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(cyan)
        gravity = Gravity.START
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value
        textSize = 15f
        setTextColor(white)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = android.graphics.drawable.GradientDrawable().apply {
            setColor(Color.rgb(19, 28, 46))
            cornerRadius = dp(14).toFloat()
            setStroke(dp(1), Color.rgb(39, 57, 82))
        }
    }

    private fun matchWrap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun params(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(top); bottomMargin = dp(bottom)
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        recognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
