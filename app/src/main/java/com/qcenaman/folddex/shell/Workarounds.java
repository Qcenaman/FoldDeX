/*
 * Adapted from scrcpy (https://github.com/Genymobile/scrcpy), Apache License 2.0.
 * Makes a bare shell-uid process look enough like an app for DisplayManager/InputManager.
 */
package com.qcenaman.folddex.shell;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressLint("PrivateApi,BlockedPrivateApi,SoonBlockedPrivateApi,DiscouragedPrivateApi")
public final class Workarounds {

    private static Class<?> activityThreadClass;
    private static Object activityThread;
    private static boolean applied;

    private Workarounds() {
    }

    public static synchronized void apply() throws Exception {
        if (applied) {
            return;
        }
        activityThreadClass = Class.forName("android.app.ActivityThread");
        Method current = activityThreadClass.getDeclaredMethod("currentActivityThread");
        current.setAccessible(true);
        activityThread = current.invoke(null);
        if (activityThread == null) {
            Constructor<?> ctor = activityThreadClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            activityThread = ctor.newInstance();
            Field f = activityThreadClass.getDeclaredField("sCurrentActivityThread");
            f.setAccessible(true);
            f.set(null, activityThread);
            Field sys = activityThreadClass.getDeclaredField("mSystemThread");
            sys.setAccessible(true);
            sys.setBoolean(activityThread, true);
        }
        fillConfigurationController();
        fillAppInfo();
        fillAppContext();
        applied = true;
    }

    private static void fillConfigurationController() {
        try {
            Field field = activityThreadClass.getDeclaredField("mConfigurationController");
            field.setAccessible(true);
            if (field.get(activityThread) != null) {
                return;
            }
            Class<?> ccClass = Class.forName("android.app.ConfigurationController");
            Class<?> atiClass = Class.forName("android.app.ActivityThreadInternal");
            Constructor<?> ctor = ccClass.getDeclaredConstructor(atiClass);
            ctor.setAccessible(true);
            field.set(activityThread, ctor.newInstance(activityThread));
        } catch (Throwable ignored) {
            // workaround only
        }
    }

    private static void fillAppInfo() {
        try {
            Field bound = activityThreadClass.getDeclaredField("mBoundApplication");
            bound.setAccessible(true);
            if (bound.get(activityThread) != null) {
                return;
            }
            Class<?> abdClass = Class.forName("android.app.ActivityThread$AppBindData");
            Constructor<?> ctor = abdClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object abd = ctor.newInstance();
            ApplicationInfo info = new ApplicationInfo();
            info.packageName = FakeContext.PACKAGE_NAME;
            Field appInfo = abdClass.getDeclaredField("appInfo");
            appInfo.setAccessible(true);
            appInfo.set(abd, info);
            bound.set(activityThread, abd);
        } catch (Throwable ignored) {
            // workaround only
        }
    }

    private static void fillAppContext() {
        try {
            Field field = activityThreadClass.getDeclaredField("mInitialApplication");
            field.setAccessible(true);
            if (field.get(activityThread) != null) {
                return;
            }
            Application app = Instrumentation.newApplication(Application.class, FakeContext.get());
            field.set(activityThread, app);
        } catch (Throwable ignored) {
            // workaround only
        }
    }

    static Context getSystemContext() {
        try {
            Method m = activityThreadClass.getDeclaredMethod("getSystemContext");
            m.setAccessible(true);
            return (Context) m.invoke(activityThread);
        } catch (Throwable t) {
            return null;
        }
    }
}
