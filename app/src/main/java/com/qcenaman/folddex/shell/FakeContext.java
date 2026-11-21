/*
 * Adapted from scrcpy (https://github.com/Genymobile/scrcpy), Apache License 2.0.
 * A context that presents itself as the shell package, so system services accept our calls.
 */
package com.qcenaman.folddex.shell;

import android.content.AttributionSource;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Process;

public final class FakeContext extends ContextWrapper {

    public static final String PACKAGE_NAME = "com.android.shell";

    private static FakeContext instance;

    public static synchronized FakeContext get() {
        if (instance == null) {
            instance = new FakeContext();
        }
        return instance;
    }

    private FakeContext() {
        super(Workarounds.getSystemContext());
    }

    @Override
    public String getPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public String getOpPackageName() {
        return PACKAGE_NAME;
    }

    @Override
    public AttributionSource getAttributionSource() {
        return new AttributionSource.Builder(Process.SHELL_UID).setPackageName(PACKAGE_NAME).build();
    }

    @SuppressWarnings("unused")
    public int getDeviceId() {
        return 0;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public Context createPackageContext(String packageName, int flags) {
        return this;
    }
}
