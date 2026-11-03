package io.github.strider_ape.sparrow

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast

private const val POWERAMP_PACKAGE = "com.maxmpz.audioplayer"

/** Launcher screen: shows whether Poweramp is installed and applies Sparrow or opens its settings. */
class SkinInfoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_info)

        findViewById<View>(R.id.apply_button).setOnClickListener { applySkin() }
        findViewById<View>(R.id.settings_button).setOnClickListener { openSkinSettings() }
    }

    override fun onResume() {
        super.onResume()
        val installed = isPowerampInstalled()
        findViewById<TextView>(R.id.status).setText(
            if (installed) R.string.status_ready else R.string.status_no_poweramp
        )
        findViewById<View>(R.id.apply_button).isEnabled = installed
        findViewById<View>(R.id.settings_button).isEnabled = installed
    }

    private fun isPowerampInstalled(): Boolean = try {
        packageManager.getPackageInfo(POWERAMP_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Starts Poweramp with Sparrow applied. Also forces a skin reload, which is handy while developing. */
    private fun applySkin() {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(POWERAMP_PACKAGE, "$POWERAMP_PACKAGE.StartupActivity")
            .putExtra("theme_pak", packageName)
            .putExtra("theme_id", R.style.Sparrow)
        startPoweramp(intent)
        finish()
    }

    /** Opens Poweramp's theme settings scrolled to Sparrow's options. */
    private fun openSkinSettings() {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(POWERAMP_PACKAGE, "$POWERAMP_PACKAGE.SettingsActivity")
            .putExtra("open", "theme")
            .putExtra("theme_pak", packageName)
            .putExtra("theme_id", R.style.Sparrow)
        // Poweramp requires startActivityForResult for open=theme
        @Suppress("DEPRECATION")
        startActivityForResult(intent, 1)
    }

    private fun startPoweramp(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.status_no_poweramp, Toast.LENGTH_LONG).show()
        }
    }
}
