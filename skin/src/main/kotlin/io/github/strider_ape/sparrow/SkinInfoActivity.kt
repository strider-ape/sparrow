package io.github.strider_ape.sparrow

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val POWERAMP_PACKAGE = "com.maxmpz.audioplayer"
private const val EXTRA_APPLY = "apply"
private const val EXTRA_LIGHT = "light"
private const val EXTRA_STYLE = "style"

/** A Sparrow look: one skin entry in xml/skins.xml, with swatch colors that hint at its background. */
private class Look(val style: Int, val name: Int, val description: Int, vararg val swatch: Int)

/** Every look, in the order shown. Each is also a separate skin in Poweramp's skin list. */
private val LOOKS = listOf(
    Look(R.style.Sparrow, R.string.look_color, R.string.look_color_description, 0xFF8A2A22.toInt(), 0xFF2A0C0A.toInt()),
    Look(R.style.SparrowGradient, R.string.look_gradient, R.string.look_gradient_description,
        0xFFC86A62.toInt(), 0xFF7E241D.toInt(), 0xFF120404.toInt()),
    Look(R.style.SparrowAurora, R.string.look_aurora, R.string.look_aurora_description,
        0xFF6E2A3A.toInt(), 0xFF3A1730.toInt(), 0xFF7C5CFF.toInt()),
    Look(R.style.SparrowBlur, R.string.look_blur, R.string.look_blur_description,
        0xFF7C8AA6.toInt(), 0xFFA6727F.toInt(), 0xFF2E3242.toInt()),
    Look(R.style.SparrowVivid, R.string.look_vivid, R.string.look_vivid_description, 0xFFD93A2E.toInt(), 0xFF5C120E.toInt()),
    Look(R.style.SparrowMuted, R.string.look_muted, R.string.look_muted_description, 0xFF8C726A.toInt(), 0xFF2D2422.toInt()),
    Look(R.style.SparrowOnyx, R.string.look_onyx, R.string.look_onyx_description, 0xFF000000.toInt(), 0xFF000000.toInt()),
    Look(R.style.SparrowLight, R.string.look_light, R.string.look_light_description, 0xFFF6E9C8.toInt(), 0xFFE2D6B8.toInt()),
)

/** Launcher screen: applies one of Sparrow's looks in Poweramp, or opens Sparrow's skin options. */
class SkinInfoActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skin_info)

        val looks = findViewById<LinearLayout>(R.id.looks)
        LOOKS.forEach { looks.addView(lookCard(it)) }
        findViewById<View>(R.id.settings_button).setOnClickListener { openSkinSettings() }

        // Used by scripts/run-on-phone.sh: applying from here reloads the skin even while Poweramp is open
        if (savedInstanceState == null && intent.getBooleanExtra(EXTRA_APPLY, false) && isPowerampInstalled()) {
            val styleName = intent.getStringExtra(EXTRA_STYLE)
            val styleId = styleName?.let { resources.getIdentifier(it, "style", packageName) } ?: 0
            when {
                styleId != 0 -> applyStyle(styleId)
                else -> applySkin(intent.getBooleanExtra(EXTRA_LIGHT, false))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val installed = isPowerampInstalled()
        findViewById<TextView>(R.id.status).setText(
            if (installed) R.string.status_ready else R.string.status_no_poweramp
        )
        findViewById<View>(R.id.settings_button).isEnabled = installed
        val looks = findViewById<LinearLayout>(R.id.looks)
        for (i in 0 until looks.childCount) looks.getChildAt(i).isEnabled = installed
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

    /** One row of the gallery: a swatch, the look's name and description. Tapping it applies the look. */
    private fun lookCard(look: Look): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(16), dp(12))
            val shape = GradientDrawable().apply {
                cornerRadius = dp(20).toFloat()
                setColor(getColor(R.color.info_card))
            }
            background = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), shape, null)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = dp(10) }
            isClickable = true
            setOnClickListener { applyStyle(look.style) }
        }

        val swatch = View(this).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, look.swatch).apply {
                cornerRadius = dp(14).toFloat()
                setStroke(dp(1), 0x26FFFFFF)
            }
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }
        card.addView(swatch)

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                .apply { leftMargin = dp(16) }
        }
        val inter = resources.getFont(R.font.inter)
        texts.addView(TextView(this).apply {
            setText(look.name)
            setTextColor(getColor(R.color.info_text))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = Typeface.create(inter, Typeface.BOLD)
        })
        texts.addView(TextView(this).apply {
            setText(look.description)
            setTextColor(getColor(R.color.info_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            typeface = inter
        })
        card.addView(texts)
        card.contentDescription = getString(look.name)
        return card
    }

    private fun isPowerampInstalled(): Boolean = try {
        packageManager.getPackageInfo(POWERAMP_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun applySkin(light: Boolean = false) =
        applyStyle(if (light) R.style.SparrowLight else R.style.Sparrow)

    /** Starts Poweramp with the given Sparrow skin entry (one of the styles in xml/skins.xml). */
    private fun applyStyle(styleId: Int) {
        val intent = Intent(Intent.ACTION_MAIN)
            .setClassName(POWERAMP_PACKAGE, "$POWERAMP_PACKAGE.StartupActivity")
            .putExtra("theme_pak", packageName)
            .putExtra("theme_id", styleId)
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
