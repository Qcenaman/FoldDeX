package com.qcenaman.folddex.shell

import android.content.Context
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.os.Build
import android.view.InputEvent
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.Surface
import com.qcenaman.folddex.IDexService
import org.lsposed.hiddenapibypass.HiddenApiBypass
import kotlin.system.exitProcess

/**
 * Runs as a Shizuku user service in a shell-uid process. Owns the virtual display that
 * Samsung DeX starts on, and injects the activity's touch/key events into it.
 */
class DexService() : IDexService.Stub() {

    @Suppress("unused")
    constructor(@Suppress("UNUSED_PARAMETER") context: Context) : this()

    private var display: VirtualDisplay? = null
    private var error: String? = null
    private var inputManager: InputManager? = null
    private val injectMethod by lazy {
        InputManager::class.java.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
    }
    private val setDisplayIdMethod by lazy {
        InputEvent::class.java.getMethod("setDisplayId", Int::class.javaPrimitiveType)
    }

    private fun prepare() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) HiddenApiBypass.addHiddenApiExemptions("L")
        Workarounds.apply()
    }

    @Synchronized
    override fun create(surface: Surface?, width: Int, height: Int, dpi: Int): Int {
        return try {
            display?.let { return it.display.displayId }
            prepare()
            val ctor = DisplayManager::class.java.getDeclaredConstructor(Context::class.java)
            ctor.isAccessible = true
            val dm = ctor.newInstance(FakeContext.get())
            val vd = dm.createVirtualDisplay("FoldDeX", width, height, dpi, surface, Flags.forSdk(Build.VERSION.SDK_INT))
            display = vd
            error = null
            vd.display.displayId
        } catch (t: Throwable) {
            error = t.toString()
            -1
        }
    }

    @Synchronized
    override fun setSurface(surface: Surface?) {
        try {
            display?.surface = surface
        } catch (t: Throwable) {
            error = t.toString()
        }
    }

    @Synchronized
    override fun resize(width: Int, height: Int, dpi: Int) {
        try {
            display?.resize(width, height, dpi)
        } catch (t: Throwable) {
            error = t.toString()
        }
    }

    @Synchronized
    override fun release() {
        try {
            display?.release()
        } catch (_: Throwable) {
        }
        display = null
    }

    override fun displayId(): Int = display?.display?.displayId ?: -1

    override fun lastError(): String? = error

    override fun injectMotion(event: MotionEvent?) {
        event ?: return
        inject(event)
        event.recycle()
    }

    override fun injectKey(event: KeyEvent?) {
        event ?: return
        inject(event)
    }

    private fun inject(event: InputEvent) {
        val id = displayId()
        if (id < 0) return
        try {
            val im = inputManager ?: (FakeContext.get().getSystemService(Context.INPUT_SERVICE) as InputManager)
                .also { inputManager = it }
            setDisplayIdMethod.invoke(event, id)
            injectMethod.invoke(im, event, 0) // INJECT_INPUT_EVENT_MODE_ASYNC
        } catch (t: Throwable) {
            error = t.toString()
        }
    }

    override fun destroy() {
        release()
        exitProcess(0)
    }
}
