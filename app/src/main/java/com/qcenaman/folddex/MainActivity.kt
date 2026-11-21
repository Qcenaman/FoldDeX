package com.qcenaman.folddex

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.qcenaman.folddex.shell.DexService
import rikka.shizuku.Shizuku

/** Full-screen surface that the DeX virtual display draws into; forwards touch and keys to it. */
class MainActivity : Activity(), SurfaceHolder.Callback {

    companion object {
        const val ACTION_STOP = "com.qcenaman.folddex.STOP"
        private const val REQ_SHIZUKU = 7

        @Volatile
        var running: Boolean = false
            private set
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var status: TextView
    private lateinit var prefs: Prefs

    @Volatile private var service: IDexService? = null
    @Volatile private var geometry: Geometry? = null
    @Volatile private var surfaceReady = false
    @Volatile private var displayId = -1
    private var bound = false
    private val matrix = Matrix()

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(packageName, DexService::class.java.name))
            .daemon(false)
            .processNameSuffix("dex")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = IDexService.Stub.asInterface(binder)
            attach()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            displayId = -1
            show("DeX service stopped")
        }
    }

    private val binderListener = Shizuku.OnBinderReceivedListener { runOnUiThread { connect() } }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU) runOnUiThread {
            if (result == PackageManager.PERMISSION_GRANTED) connect() else show("FoldDeX needs Shizuku permission")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == ACTION_STOP) {
            finishAndRemoveTask()
            return
        }
        running = true
        prefs = Prefs(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        window.setDecorFitsSystemWindows(false)

        surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        surfaceView.setOnTouchListener { _, ev -> forward(ev); true }
        surfaceView.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ -> layoutChanged(v.width, v.height) }
        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            text = "Starting DeX…"
        }
        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(surfaceView, FrameLayout.LayoutParams(-1, -1))
            addView(status, FrameLayout.LayoutParams(-1, -1))
        })
        hideBars()

        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        if (!Shizuku.pingBinder()) show("Start Shizuku, then open FoldDeX again")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == ACTION_STOP) stopDex()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideBars()
    }

    private fun hideBars() {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.systemBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun connect() {
        if (bound || isFinishing) return
        try {
            if (!Shizuku.pingBinder()) {
                show("Start Shizuku, then open FoldDeX again"); return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                show("Allow FoldDeX in Shizuku")
                Shizuku.requestPermission(REQ_SHIZUKU)
                return
            }
            Shizuku.bindUserService(serviceArgs, connection)
            bound = true
        } catch (t: Throwable) {
            show("Shizuku error: ${t.message}")
        }
    }

    private fun layoutChanged(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val g = Geometry.compute(w, h, prefs.scale, prefs.dpi)
        if (g == geometry) return
        geometry = g
        matrix.setScale(g.scaleX, g.scaleY)
        surfaceView.holder.setFixedSize(g.width, g.height)
        if (displayId >= 0) background { service?.resize(g.width, g.height, g.dpi) }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceReady = true
        attach()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        val s = service ?: return
        try {
            s.setSurface(null)
        } catch (_: Throwable) {
        }
    }

    private fun attach() = background {
        synchronized(this) {
            val s = service ?: return@background
            val g = geometry ?: return@background
            if (!surfaceReady) return@background
            val surface = surfaceView.holder.surface
            try {
                if (displayId < 0) {
                    val id = s.create(surface, g.width, g.height, g.dpi)
                    if (id < 0) {
                        show("Could not start DeX: ${s.lastError()}")
                        return@background
                    }
                    displayId = id
                    hideStatus()
                } else {
                    s.setSurface(surface)
                    s.resize(g.width, g.height, g.dpi)
                }
            } catch (t: Throwable) {
                show("Could not start DeX: ${t.message}")
            }
        }
    }

    private fun forward(ev: MotionEvent) {
        val s = service ?: return
        if (displayId < 0) return
        val e = MotionEvent.obtain(ev)
        e.transform(matrix)
        try {
            s.injectMotion(e)
        } catch (_: Throwable) {
        }
        e.recycle()
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        forward(event)
        return true
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        when (event.keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
            KeyEvent.KEYCODE_POWER -> return super.dispatchKeyEvent(event)
        }
        val s = service
        if (s == null || displayId < 0) return super.dispatchKeyEvent(event)
        try {
            s.injectKey(KeyEvent(event))
        } catch (_: Throwable) {
        }
        return true
    }

    fun stopDex() {
        background {
            try {
                service?.release()
            } catch (_: Throwable) {
            }
        }
        displayId = -1
        if (bound) {
            try {
                Shizuku.unbindUserService(serviceArgs, connection, true)
            } catch (_: Throwable) {
            }
            bound = false
        }
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        if (::prefs.isInitialized) {
            running = false
            Shizuku.removeBinderReceivedListener(binderListener)
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            if (bound) {
                try {
                    service?.release()
                    Shizuku.unbindUserService(serviceArgs, connection, true)
                } catch (_: Throwable) {
                }
                bound = false
            }
        }
        super.onDestroy()
    }

    private fun show(msg: String) = runOnUiThread {
        status.text = msg
        status.visibility = TextView.VISIBLE
    }

    private fun hideStatus() = runOnUiThread { status.visibility = TextView.GONE }

    private inline fun background(crossinline block: () -> Unit) {
        Thread { block() }.start()
    }
}
