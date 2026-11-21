package com.qcenaman.folddex;

import android.view.Surface;
import android.view.MotionEvent;
import android.view.KeyEvent;

interface IDexService {
    // Shizuku calls this transaction to stop the service process.
    void destroy() = 16777114;

    // Creates the virtual display drawing into surface; returns its display id (or -1).
    int create(in Surface surface, int width, int height, int dpi) = 1;
    void setSurface(in Surface surface) = 2;
    void resize(int width, int height, int dpi) = 3;
    void release() = 4;
    int displayId() = 5;
    oneway void injectMotion(in MotionEvent event) = 6;
    oneway void injectKey(in KeyEvent event) = 7;
    String lastError() = 8;
}
