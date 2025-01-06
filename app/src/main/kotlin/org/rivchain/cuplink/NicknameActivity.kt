package org.rivchain.cuplink

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.InputFilter
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rivchain.cuplink.util.Utils
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.coroutines.suspendCoroutine

class NicknameActivity : AppCompatActivity() {

    private lateinit var nickname: String
    private lateinit var backgroundView: ImageView
    private lateinit var splashText: TextView
    private lateinit var parent: LinearLayout
    private lateinit var nextButton: MaterialButton
    private lateinit var nicknameEditText: EditText

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nickname)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        nickname = generateRandomNickname()
        // Bind views
        backgroundView = findViewById(R.id.background_view)
        parent = findViewById(R.id.container)
        splashText = findViewById(R.id.splashText)
        nicknameEditText = findViewById(R.id.edit)
        nicknameEditText.filters = arrayOf(getEditTextFilter())
        nextButton = findViewById(R.id.next)
        nextButton.setOnClickListener {
            enterNickname()
        }
        // Start animations
        startAnimations()
    }

    private fun enterNickname(){
        if (nicknameEditText.text.toString()==""){
            DatabaseCache.database.settings.username = nickname
            DatabaseCache.save()
            finish()
        } else if (nicknameEditText.text != null && Utils.isValidName(nicknameEditText.text.toString())) {
            DatabaseCache.database.settings.username = nicknameEditText.text.toString()
            DatabaseCache.save()
            finish()
        } else {
            nicknameEditText.error = getString(R.string.invalid_name)
        }
    }

    private fun getEditTextFilter(): InputFilter {
        return object : InputFilter {
            override fun filter(
                source: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                var keepOriginal = true
                val sb = StringBuilder(end - start)
                for (i in start until end) {
                    val c = source[i]
                    if (isCharAllowed(c)) // put your condition here
                        sb.append(c) else keepOriginal = false
                }
                return if (keepOriginal) null else {
                    if (source is Spanned) {
                        val sp = SpannableString(sb)
                        TextUtils.copySpansFrom(source as Spanned, start, sb.length, null, sp, 0)
                        sp
                    } else {
                        sb
                    }
                }
            }

            private fun isCharAllowed(c: Char): Boolean {
                val ps: Pattern = Pattern.compile("^[A-Za-z0-9-]{1,23}$")
                val ms: Matcher = ps.matcher(c.toString())
                return ms.matches()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onBackPressed() {
        enterNickname()
    }

    private fun generateRandomNickname(): String {
        // Load the string arrays from resources
        val adjectives = this.resources.getStringArray(R.array.adjectives)
        val animals = this.resources.getStringArray(R.array.animals)

        // Pick a random adjective and a random animal
        val randomAdjective = adjectives.random()
        val randomAnimal = animals.random()

        // Return the concatenated result
        return "$randomAdjective$randomAnimal"
    }

    private fun calculateMaxYTranslation(context: Context, percentage: Float): Float {
        // Get display metrics
        val displayMetrics = DisplayMetrics()
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Get screen height
        val screenHeight = displayMetrics.heightPixels

        // Calculate max Y translation as a percentage of screen height
        return screenHeight * (percentage / 100)
    }

    // Coroutine-based animation sequence
    private fun startAnimations() {
        CoroutineScope(Dispatchers.Main).launch {
            startLogoMoveAnimation()
            startLogoScaleAnimation()
            delay(1000)
            startTextIntroAnimation()
            delay(1500)
            startNicknameAnimation()
            delay(500)
            showControls()
        }
    }

    // Step 1: Move up the parent view
    private suspend fun startLogoMoveAnimation() {
        suspendCoroutine<Unit> { continuation ->
            parent.animate()
                .translationYBy(-1 * calculateMaxYTranslation(this, 10.0f))
                .setDuration(1500)
                .withEndAction {
                    continuation.resumeWith(Result.success(Unit))
                }
                .start()
        }
    }

    // Step 2: Scale down the background view
    private suspend fun startLogoScaleAnimation() {
        suspendCoroutine<Unit> { continuation ->
            backgroundView.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(1500)
                .withEndAction {
                    continuation.resumeWith(Result.success(Unit))
                }
                .start()
        }
    }

    // Step 3: Animate intro text
    private suspend fun startTextIntroAnimation() {
        suspendCoroutine<Unit> { continuation ->
            backgroundView.animate()
                .setDuration(5000)
                .withStartAction {
                    splashText.text = ""
                    animateText(
                        splashText,
                        "Hi, I'm CupLink. Your new dapp"
                    ) {
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
                .start()
        }
    }

    // Step 4: Animate nickname text
    private suspend fun startNicknameAnimation() {
        suspendCoroutine<Unit> { continuation ->
            backgroundView.animate()
                .setDuration(5000)
                .withStartAction {
                    splashText.text = ""
                    animateText(
                        splashText,
                        "You can insert your name or use your generated RiV Mesh Space nickname: $nickname"
                    ) {
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
                .start()
        }
    }

    // Step 5: Show controls
    private suspend fun showControls() {
        nicknameEditText.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(1000)
                .start()
        }
        nextButton.apply {
            visibility = View.VISIBLE
            alpha = 0f
            animate()
                .alpha(1f)
                .setDuration(1000)
                .start()
        }
    }

    private fun animateText(
        textView: TextView,
        fullText: String,
        delay: Long = 100,
        onComplete: () -> Unit = {}
    ) {
        textView.text = "" // Clear previous text
        val spannableBuilder = SpannableStringBuilder()

        // Launch a coroutine on the Main thread
        CoroutineScope(Dispatchers.Main).launch {
            for (char in fullText) {
                spannableBuilder.append(char) // Append character
                textView.text = spannableBuilder // Update the TextView
                delay(delay) // Pause for the specified duration
            }
            onComplete() // Invoke the callback after completing the text animation
        }
    }
}
