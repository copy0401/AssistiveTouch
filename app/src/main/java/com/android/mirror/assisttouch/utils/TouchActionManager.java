package com.android.mirror.assisttouch.utils;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.android.mirror.assisttouch.R;

import java.util.ArrayList;
import java.util.List;

public class TouchActionManager {

    private static final String PREF_NAME = "assistive_touch_prefs";
    public static final String KEY_SINGLE_TAP = "action_single_tap";
    public static final String KEY_LONG_PRESS = "action_long_press";
    public static final String KEY_DOUBLE_TAP = "action_double_tap";
    public static final String KEY_ONGOING_NOTIFICATION = "ongoing_notification";

    public static class TouchAction {
        private final String name;
        private final int actionId;

        public TouchAction(String name, int actionId) {
            this.name = name;
            this.actionId = actionId;
        }

        public String getName() {
            return name;
        }

        public int getActionId() {
            return actionId;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static List<TouchAction> getAvailableActions(Context context) {
        List<TouchAction> list = new ArrayList<>();
        list.add(new TouchAction(context.getString(R.string.action_recents), AccessibilityService.GLOBAL_ACTION_RECENTS));
        list.add(new TouchAction(context.getString(R.string.action_back), AccessibilityService.GLOBAL_ACTION_BACK));
        list.add(new TouchAction(context.getString(R.string.action_home), AccessibilityService.GLOBAL_ACTION_HOME));
        list.add(new TouchAction(context.getString(R.string.action_notifications), AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));
        list.add(new TouchAction(context.getString(R.string.action_quick_settings), AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS));
        list.add(new TouchAction(context.getString(R.string.action_power_dialog), AccessibilityService.GLOBAL_ACTION_POWER_DIALOG));
        list.add(new TouchAction(context.getString(R.string.action_toggle_split_screen), AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            list.add(new TouchAction(context.getString(R.string.action_take_screenshot), AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT));
            list.add(new TouchAction(context.getString(R.string.action_lock_screen), AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN));
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(new TouchAction(context.getString(R.string.action_dismiss_notification_shade), 15));
        }

        list.add(new TouchAction(context.getString(R.string.action_none), 0));
        return list;
    }

    public static int getAction(Context context, String key, int defaultAction) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getInt(key, defaultAction);
    }

    public static void setAction(Context context, String key, int actionId) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(key, actionId).apply();
    }

    public static boolean isOngoingNotificationEnabled(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return sp.getBoolean(KEY_ONGOING_NOTIFICATION, true);
    }

    public static void setOngoingNotificationEnabled(Context context, boolean enabled) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putBoolean(KEY_ONGOING_NOTIFICATION, enabled).apply();
    }
}
