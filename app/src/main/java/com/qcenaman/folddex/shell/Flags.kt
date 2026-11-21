package com.qcenaman.folddex.shell

/** Virtual display flags: the same set scrcpy uses for --new-display, which DeX accepts as a desktop. */
object Flags {
    const val PUBLIC = 1
    const val PRESENTATION = 1 shl 1
    const val OWN_CONTENT_ONLY = 1 shl 3
    const val SUPPORTS_TOUCH = 1 shl 6
    const val ROTATES_WITH_CONTENT = 1 shl 7
    const val DESTROY_CONTENT_ON_REMOVAL = 1 shl 8
    const val SHOULD_SHOW_SYSTEM_DECORATIONS = 1 shl 9
    const val TRUSTED = 1 shl 10
    const val OWN_DISPLAY_GROUP = 1 shl 11
    const val ALWAYS_UNLOCKED = 1 shl 12
    const val TOUCH_FEEDBACK_DISABLED = 1 shl 13
    const val OWN_FOCUS = 1 shl 14
    const val DEVICE_DISPLAY_GROUP = 1 shl 15

    fun forSdk(sdk: Int): Int {
        var f = PUBLIC or PRESENTATION or OWN_CONTENT_ONLY or SUPPORTS_TOUCH or ROTATES_WITH_CONTENT or
            DESTROY_CONTENT_ON_REMOVAL or SHOULD_SHOW_SYSTEM_DECORATIONS
        if (sdk >= 33) f = f or TRUSTED or OWN_DISPLAY_GROUP or ALWAYS_UNLOCKED or TOUCH_FEEDBACK_DISABLED
        if (sdk >= 34) f = f or OWN_FOCUS or DEVICE_DISPLAY_GROUP
        return f
    }
}
