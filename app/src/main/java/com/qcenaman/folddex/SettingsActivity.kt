package com.qcenaman.folddex

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView

/** Render scale (speed vs sharpness) and DeX size. Changes apply the next time DeX starts. */
class SettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad, pad, pad)
        }

        root.addView(TextView(this).apply { text = "Render resolution"; textSize = 18f })
        val scales = listOf(100 to "Full (sharpest, smallest taskbar)", 75 to "75%", 50 to "50%", 38 to "38% (taskbar matches desktop icons)")
        val group = RadioGroup(this)
        scales.forEach { (value, label) ->
            group.addView(RadioButton(this).apply {
                id = value
                text = label
                isChecked = prefs.scale == value
            })
        }
        group.setOnCheckedChangeListener { _, id -> prefs.scale = id }
        root.addView(group)

        val sizeLabel = TextView(this).apply { textSize = 18f; setPadding(0, pad, 0, 0) }
        fun label(dpi: Int) { sizeLabel.text = "DeX size: $dpi dpi (lower = more space)" }
        label(prefs.dpi)
        root.addView(sizeLabel)
        root.addView(SeekBar(this).apply {
            max = (Geometry.MAX_DPI - Geometry.MIN_DPI) / 20
            progress = (prefs.dpi - Geometry.MIN_DPI) / 20
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                    val dpi = Geometry.MIN_DPI + p * 20
                    prefs.dpi = dpi
                    label(dpi)
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        root.addView(TextView(this).apply {
            text = "Changes apply the next time DeX starts."
            setPadding(0, pad, 0, 0)
        })
        setContentView(root)
    }
}
