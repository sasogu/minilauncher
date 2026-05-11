package es.sasogu.minilauncher

import android.app.Activity
import android.content.pm.LauncherApps
import android.os.Bundle
import android.widget.Toast

class ConfirmPinShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            val launcherApps = getSystemService(LauncherApps::class.java)
            val request = launcherApps?.getPinItemRequest(intent)
            if (request != null && request.isValid && request.accept()) {
                Toast.makeText(this, R.string.pinned_shortcut_added, Toast.LENGTH_SHORT).show()
            }
        }
        finish()
    }
}
