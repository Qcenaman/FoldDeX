package com.qcenaman.folddex

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("folddex", Context.MODE_PRIVATE)

    var scale: Int
        get() = sp.getInt("scale", 50)
        set(v) = sp.edit().putInt("scale", v.coerceIn(Geometry.MIN_SCALE, Geometry.MAX_SCALE)).apply()

    var dpi: Int
        get() = sp.getInt("dpi", 400)
        set(v) = sp.edit().putInt("dpi", v.coerceIn(Geometry.MIN_DPI, Geometry.MAX_DPI)).apply()
}
