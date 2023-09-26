package com.metris.xposed.castenabler;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class XHooks implements IXposedHookLoadPackage {
    private static final String APP_TAG = "[CASTENABLER] ";

    @SuppressWarnings("RedundantThrows")
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        //before -> https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r25/packages/SystemUI/src/com/android/systemui/volume/VolumeDialogControllerImpl.java#1152
        //after -> https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r27/packages/SystemUI/src/com/android/systemui/volume/VolumeDialogControllerImpl.java#1159
        String systemUIpkg = "com.android.systemui";

        if (lpparam.packageName.equals(systemUIpkg)) {
            XposedBridge.log(APP_TAG + "hooking systemui");

            try {
                Class<?> mediaSessionsCallbacks = XposedHelpers.findClass(
                        systemUIpkg + ".volume.VolumeDialogControllerImpl$MediaSessionsCallbacks", lpparam.classLoader);
                Constructor<?>[] constructorList = mediaSessionsCallbacks.getDeclaredConstructors();
                XposedBridge.log(APP_TAG + "found " + constructorList.length + " constructors");

                for (Constructor<?> currentConstructor : mediaSessionsCallbacks.getDeclaredConstructors()) {
                    Class<?>[] parameterClass = currentConstructor.getParameterTypes();
                    Object[] hookObject = new Object[parameterClass.length + 1];
                    if (parameterClass.length >= 0)
                        System.arraycopy(parameterClass, 0, hookObject, 0, parameterClass.length);

                    hookObject[hookObject.length - 1] = new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            try {
                                XposedHelpers.setBooleanField(param.thisObject, "mShowRemoteSessions", true);
                            } catch (NoSuchFieldError e) {
                                try {
                                    XposedHelpers.setBooleanField(param.thisObject, "mVolumeAdjustmentForRemoteGroupSessions", true);
                                } catch (NoSuchFieldError e1) {
                                    XposedBridge.log(APP_TAG + "unable to hook VolumeDialogControllerImpl (no mShowRemoteSessions or mVolumeAdjustmentForRemoteGroupSessions)!");
                                }
                            }
                        }
                    };

                    XposedBridge.log(APP_TAG + "built hook with " + Arrays.toString(hookObject));
                    XposedHelpers.findAndHookConstructor(mediaSessionsCallbacks, hookObject);
                }
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log(APP_TAG + "cannot find MediaSessionsCallbacks class in systemui!");
            } catch (NoSuchMethodError e) {
                XposedBridge.log(APP_TAG + "cannot find MediaSessionsCallbacks constructor in systemui!");
            }
        }

        //-----------------------------------
        //before -> https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r25/services/core/java/com/android/server/media/MediaSessionRecord.java#397
        //after -> https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-12.0.0_r27/services/core/java/com/android/server/media/MediaSessionRecord.java#465
        //but no need to hook canHandleVolumeKey(), isPlaybackTypeLocal is always called first

        if ("android".equals(lpparam.packageName)) {
            XposedBridge.log(APP_TAG + "hooking framework");

            try {
                XposedHelpers.findAndHookMethod("com.android.server.media.MediaSessionRecord", lpparam.classLoader,
                        "isPlaybackTypeLocal", new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.setResult(true);
                            }
                        });
            } catch (XposedHelpers.ClassNotFoundError e) {
                XposedBridge.log(APP_TAG + "cannot find MediaSessionRecord class in framework!");
            } catch (NoSuchMethodError e) {
                XposedBridge.log(APP_TAG + "cannot find method isPlaybackTypeLocal in framework!");
            }
        }
    }
}
