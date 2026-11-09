package com.simplecalculatordeign.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.simplecalculatordeign.data.repository.AppSettings

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

/** Key-press feedback honouring the app's own Sound and Vibration settings. */
class Feedback(private val view: View, private val sound: Boolean, private val vibration: Boolean) {
    private val audio = view.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun click() {
        if (vibration) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        if (sound) audio?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.6f)
    }

    fun heavy() {
        if (vibration) view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        if (sound) audio?.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.6f)
    }
}

@Composable
fun rememberFeedback(): Feedback {
    val view = LocalView.current
    val settings = LocalAppSettings.current
    return remember(view, settings.soundEnabled, settings.vibrationEnabled) {
        Feedback(view, settings.soundEnabled, settings.vibrationEnabled)
    }
}

/** Copies full-precision text to the clipboard (Android 13+ shows its own confirmation). */
fun Context.copyToClipboard(label: String, text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun rememberCopier(): (String, String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { label: String, text: String -> context.copyToClipboard(label, text) } }
}
