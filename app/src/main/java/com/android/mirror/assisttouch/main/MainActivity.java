package com.android.mirror.assisttouch.main;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.service.AssistiveTouchService;
import com.android.mirror.assisttouch.utils.SystemsUtils;
import com.android.mirror.assisttouch.utils.TouchActionManager;

import java.util.List;

public class MainActivity extends Activity {

    private static final String PREF_SHORTCUT_CREATED = "first_launch_shortcut_created";

    private Spinner spinnerSingleTap;
    private Spinner spinnerLongPress;
    private Spinner spinnerDoubleTap;

    private TextView tvOverlayStatus;
    private Button btnOverlayPermission;

    private TextView tvAccessibilityStatus;
    private Button btnAccessibilitySettings;
    private Button btnCreateShortcut;

    private Switch switchOngoingNotification;
    private TextView tvBatteryStatus;
    private Button btnBatteryOptimization;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupSpinners();
        setupOngoingNotificationSwitch();
        setupClickListeners();
        checkAutoCreateShortcutOnFirstLaunch();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatusIndicators();
    }

    private void initViews() {
        spinnerSingleTap = findViewById(R.id.spinnerSingleTap);
        spinnerLongPress = findViewById(R.id.spinnerLongPress);
        spinnerDoubleTap = findViewById(R.id.spinnerDoubleTap);

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        btnOverlayPermission = findViewById(R.id.btnOverlayPermission);

        tvAccessibilityStatus = findViewById(R.id.tvAccessibilityStatus);
        btnAccessibilitySettings = findViewById(R.id.btnAccessibilitySettings);
        btnCreateShortcut = findViewById(R.id.btnCreateShortcut);

        switchOngoingNotification = findViewById(R.id.switchOngoingNotification);
        tvBatteryStatus = findViewById(R.id.tvBatteryStatus);
        btnBatteryOptimization = findViewById(R.id.btnBatteryOptimization);
    }

    private void checkAutoCreateShortcutOnFirstLaunch() {
        SharedPreferences sp = getSharedPreferences("assistive_touch_prefs", MODE_PRIVATE);
        boolean shortcutCreated = sp.getBoolean(PREF_SHORTCUT_CREATED, false);
        if (!shortcutCreated) {
            sp.edit().putBoolean(PREF_SHORTCUT_CREATED, true).apply();
            SystemsUtils.createHomeScreenShortcut(this);
        }
    }

    private void setupOngoingNotificationSwitch() {
        switchOngoingNotification.setChecked(TouchActionManager.isOngoingNotificationEnabled(this));
        switchOngoingNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            TouchActionManager.setOngoingNotificationEnabled(MainActivity.this, isChecked);
            Intent intent = new Intent(MainActivity.this, AssistiveTouchService.class);
            intent.setAction(AssistiveTouchService.ACTION_UPDATE_NOTIFICATION);
            startService(intent);
        });
    }

    private void setupSpinners() {
        List<TouchActionManager.TouchAction> actions = TouchActionManager.getAvailableActions(this);
        ArrayAdapter<TouchActionManager.TouchAction> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                actions
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerSingleTap.setAdapter(adapter);
        spinnerLongPress.setAdapter(adapter);
        spinnerDoubleTap.setAdapter(adapter);

        // Set default / saved selections
        int savedSingleTap = TouchActionManager.getAction(
                this,
                TouchActionManager.KEY_SINGLE_TAP,
                AccessibilityService.GLOBAL_ACTION_RECENTS
        );
        int savedLongPress = TouchActionManager.getAction(
                this,
                TouchActionManager.KEY_LONG_PRESS,
                AccessibilityService.GLOBAL_ACTION_BACK
        );
        int savedDoubleTap = TouchActionManager.getAction(
                this,
                TouchActionManager.KEY_DOUBLE_TAP,
                AccessibilityService.GLOBAL_ACTION_HOME
        );

        setSpinnerSelectionByActionId(spinnerSingleTap, actions, savedSingleTap);
        setSpinnerSelectionByActionId(spinnerLongPress, actions, savedLongPress);
        setSpinnerSelectionByActionId(spinnerDoubleTap, actions, savedDoubleTap);

        // Setup item selection listeners
        spinnerSingleTap.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TouchActionManager.TouchAction action = actions.get(position);
                TouchActionManager.setAction(MainActivity.this, TouchActionManager.KEY_SINGLE_TAP, action.getActionId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerLongPress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TouchActionManager.TouchAction action = actions.get(position);
                TouchActionManager.setAction(MainActivity.this, TouchActionManager.KEY_LONG_PRESS, action.getActionId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerDoubleTap.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TouchActionManager.TouchAction action = actions.get(position);
                TouchActionManager.setAction(MainActivity.this, TouchActionManager.KEY_DOUBLE_TAP, action.getActionId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setSpinnerSelectionByActionId(Spinner spinner, List<TouchActionManager.TouchAction> actions, int actionId) {
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i).getActionId() == actionId) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupClickListeners() {
        btnOverlayPermission.setOnClickListener(v -> openOverlaySettings());
        btnAccessibilitySettings.setOnClickListener(v -> openAccessibilitySettings(MainActivity.this));
        btnCreateShortcut.setOnClickListener(v -> requestCreateShortcut());
        btnBatteryOptimization.setOnClickListener(v -> requestIgnoreBatteryOptimization());
    }

    private void requestCreateShortcut() {
        SystemsUtils.createHomeScreenShortcut(this);
        Toast.makeText(this, R.string.toast_shortcut_requested, Toast.LENGTH_SHORT).show();
    }

    private void updateStatusIndicators() {
        // Overlay permission
        boolean canDrawOverlays = Settings.canDrawOverlays(this);
        if (canDrawOverlays) {
            tvOverlayStatus.setText(R.string.overlay_status_enabled);
            tvOverlayStatus.setTextColor(0xFF2E7D32); // Green
            btnOverlayPermission.setEnabled(false);
            btnOverlayPermission.setText(R.string.btn_overlay_enabled);
        } else {
            tvOverlayStatus.setText(R.string.overlay_status_disabled);
            tvOverlayStatus.setTextColor(0xFFC62828); // Red
            btnOverlayPermission.setEnabled(true);
            btnOverlayPermission.setText(R.string.btn_overlay_grant);
        }

        // Accessibility service
        boolean isAccessibilityEnabled = SystemsUtils.isAccessibilityServiceEnabled(
                getApplicationContext(),
                AssistiveTouchService.class
        );
        if (isAccessibilityEnabled) {
            tvAccessibilityStatus.setText(R.string.accessibility_status_enabled);
            tvAccessibilityStatus.setTextColor(0xFF2E7D32); // Green
            btnAccessibilitySettings.setText(R.string.btn_accessibility_settings);
        } else {
            tvAccessibilityStatus.setText(R.string.accessibility_status_disabled);
            tvAccessibilityStatus.setTextColor(0xFFC62828); // Red
            btnAccessibilitySettings.setText(R.string.btn_accessibility_settings);
        }

        // Battery optimization
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            tvBatteryStatus.setText(R.string.battery_status_enabled);
            tvBatteryStatus.setTextColor(0xFF2E7D32); // Green
            btnBatteryOptimization.setEnabled(false);
            btnBatteryOptimization.setText(R.string.btn_battery_enabled);
        } else {
            tvBatteryStatus.setText(R.string.battery_status_disabled);
            tvBatteryStatus.setTextColor(0xFFE65100); // Orange
            btnBatteryOptimization.setEnabled(true);
            btnBatteryOptimization.setText(R.string.btn_battery_grant);
        }
    }

    private void openOverlaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_overlay_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAccessibilitySettings(Context context) {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.toast_accessibility_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void requestIgnoreBatteryOptimization() {
        try {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(this, R.string.toast_battery_error, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
