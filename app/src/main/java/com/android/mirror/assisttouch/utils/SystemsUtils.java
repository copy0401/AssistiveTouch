package com.android.mirror.assisttouch.utils;

import android.accessibilityservice.AccessibilityService;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemsUtils {

    public static DisplayMetrics getScreenSize(Context context) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        if (mWindowManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                android.graphics.Rect bounds = mWindowManager.getCurrentWindowMetrics().getBounds();
                displayMetrics.widthPixels = bounds.width();
                displayMetrics.heightPixels = bounds.height();
                displayMetrics.densityDpi = context.getResources().getDisplayMetrics().densityDpi;
            } else {
                mWindowManager.getDefaultDisplay().getMetrics(displayMetrics);
            }
        }
        return displayMetrics;
    }

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static float convertDpToPixel(float dp, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return px;
    }

    public static float convertPixelsToDp(float px, Context context) {
        Resources resources = context.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }

    public static boolean isAccessibilityServiceEnabled(Context context, Class<? extends AccessibilityService> service) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString != null && !prefString.isEmpty()) {
            String[] enabledServices = prefString.split(":");
            for (String enabledService : enabledServices) {
                ComponentName enabledServiceName = ComponentName.unflattenFromString(enabledService);
                if (enabledServiceName != null && enabledServiceName.getClassName().equals(service.getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void createHomeScreenShortcut(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            android.content.pm.ShortcutManager shortcutManager = context.getSystemService(android.content.pm.ShortcutManager.class);
            if (shortcutManager != null && shortcutManager.isRequestPinShortcutSupported()) {
                Intent pinShortcutIntent = new Intent(context, com.android.mirror.assisttouch.main.MainActivity.class);
                pinShortcutIntent.setAction(Intent.ACTION_MAIN);

                android.content.pm.ShortcutInfo pinShortcutInfo = new android.content.pm.ShortcutInfo.Builder(context, "assistive_touch_pin_shortcut")
                        .setShortLabel(context.getString(com.android.mirror.assisttouch.R.string.app_name))
                        .setLongLabel(context.getString(com.android.mirror.assisttouch.R.string.app_title))
                        .setIcon(android.graphics.drawable.Icon.createWithResource(context, com.android.mirror.assisttouch.R.mipmap.ic_launcher))
                        .setIntent(pinShortcutIntent)
                        .build();

                shortcutManager.requestPinShortcut(pinShortcutInfo, null);
            }
        } else {
            Intent shortcutIntent = new Intent(context.getApplicationContext(), com.android.mirror.assisttouch.main.MainActivity.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(com.android.mirror.assisttouch.R.string.app_name));
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(context.getApplicationContext(), com.android.mirror.assisttouch.R.mipmap.ic_launcher));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            addIntent.putExtra("duplicate", false);
            context.sendBroadcast(addIntent);
        }
    }

}
