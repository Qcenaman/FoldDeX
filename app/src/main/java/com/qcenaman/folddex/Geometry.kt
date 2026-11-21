package com.qcenaman.folddex

import kotlin.math.max
import kotlin.math.roundToInt

/** Size of the DeX virtual display for a given on-screen view size, render scale and DeX size (dpi). */
data class Geometry(val width: Int, val height: Int, val dpi: Int, val scaleX: Float, val scaleY: Float) {

    companion object {
        const val MIN_SCALE = 25
        const val MAX_SCALE = 100
        const val MIN_DPI = 120
        const val MAX_DPI = 640

        /**
         * scalePct: share of the real resolution DeX renders at (the GPU stretches it to fill the view).
         * sizeDpi: DeX density at full resolution; it is scaled too, so DeX looks the same size at any scale.
         */
        fun compute(viewWidth: Int, viewHeight: Int, scalePct: Int, sizeDpi: Int): Geometry {
            require(viewWidth > 0 && viewHeight > 0) { "view size must be positive" }
            val s = scalePct.coerceIn(MIN_SCALE, MAX_SCALE) / 100f
            val w = even((viewWidth * s).roundToInt())
            val h = even((viewHeight * s).roundToInt())
            val dpi = max(60, (sizeDpi.coerceIn(MIN_DPI, MAX_DPI) * s).roundToInt())
            return Geometry(w, h, dpi, w.toFloat() / viewWidth, h.toFloat() / viewHeight)
        }

        private fun even(v: Int): Int = max(2, v - (v and 1))
    }
}
