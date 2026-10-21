package com.android.mirror.assisttouch.service;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.utils.SystemsUtils;
import com.android.mirror.assisttouch.utils.TouchActionManager;
import com.android.mirror.assisttouch.utils.SystemsUtils;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class AssistiveTouchService extends AccessibilityService {

    public static final String ACTION_UPDATE_NOTIFICATION = "com.android.mirror.assisttouch.ACTION_UPDATE_NOTIFICATION";

    private boolean isMoving;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mStatusBarHeight;

    private int lastAssistiveTouchViewX;
    private int lastAssistiveTouchViewY;

    private View mAssistiveTouchView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;

    private Timer mTimer;

    private LayoutInflater mInflater;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        calculateForMyPhone();
        createAssistiveTouchView();
        updateNotificationState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateNotificationState();
        return START_STICKY;
    }

    private void updateNotificationState() {
        boolean enabled = TouchActionManager.isOngoingNotificationEnabled(this);
        if (enabled) {
            startForegroundServiceNotification();
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        }
    }

    private void startForegroundServiceNotification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String channelId = "assistive_touch_channel";
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                    channelId,
                    "AssistiveTouch 懸浮球服務",
                    android.app.NotificationManager.IMPORTANCE_LOW
            );
            android.app.NotificationManager manager = getSystemService(android.app.NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

            android.app.Notification notification = new android.app.Notification.Builder(this, channelId)
                    .setContentTitle("AssistiveTouch 懸浮球運行中")
                    .setContentText("保持懸浮球後台穩定運行")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build();

            try {
                if (android.os.Build.VERSION.SDK_INT >= 34) {
                    startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                } else {
                    startForeground(1001, notification);
                }
            } catch (Exception e) {
                try {
                    startForeground(1001, notification);
                } catch (Exception ignored) {}
            }
        }
    }

    private void executeAction(int action) {
        if (action > 0) {
            performGlobalAction(action);
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    private void init() {
        mTimer = new Timer();
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mInflater = LayoutInflater.from(this);
        mAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_layout, null);
        mAssistiveTouchView.setAlpha(1.0f);
    }

    private void calculateForMyPhone() {
        DisplayMetrics displayMetrics = SystemsUtils.getScreenSize(this);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void createAssistiveTouchView() {
        mParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.x = mScreenWidth;
        mParams.y = 520;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowManager.addView(mAssistiveTouchView, mParams);
        mAssistiveTouchView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(AssistiveTouchService.this.getBaseContext(), new GestureDetector.SimpleOnGestureListener() {
                
                // 長壓
                @Override
                public void onLongPress(MotionEvent e) {
                    if (isMoving) return;
                    int action = TouchActionManager.getAction(getApplicationContext(),
                            TouchActionManager.KEY_LONG_PRESS,
                            AccessibilityService.GLOBAL_ACTION_BACK);
                    executeAction(action);
                    super.onLongPress(e);
                }
                
                // 單擊
                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) { 
                    if (isMoving) return false;
                    int action = TouchActionManager.getAction(getApplicationContext(),
                            TouchActionManager.KEY_SINGLE_TAP,
                            AccessibilityService.GLOBAL_ACTION_RECENTS);
                    executeAction(action);
                    return true;
                }
                
                // 雙擊
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (isMoving) return false;
                    int action = TouchActionManager.getAction(getApplicationContext(),
                            TouchActionManager.KEY_DOUBLE_TAP,
                            AccessibilityService.GLOBAL_ACTION_HOME);
                    executeAction(action);
                    return true;
                }
            });

            private float initX = 0f;
            private float initY = 0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);

                float rawX = event.getRawX();
                float rawY = event.getRawY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMoving = false;
                        initX = rawX;
                        initY = rawY;
                        break;
                    case MotionEvent.ACTION_UP:
                        AssistiveTouchService.this.setAssistiveTouchViewAlign();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Context context = AssistiveTouchService.this;
                        float threshold = SystemsUtils.convertDpToPixel(15, context);
                        if (Math.abs(rawX - initX) > threshold || Math.abs(rawY - initY) > threshold) {
                            isMoving = true;
                            mParams.x = (int) (rawX - mAssistiveTouchView.getMeasuredWidth() / 2);
                            mParams.y = (int) (rawY - mAssistiveTouchView.getMeasuredHeight() / 2 - mStatusBarHeight);
                            mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
                        }
                }
                if (isMoving)
                    return true;
                else
                    return false;
            }
        });
    }


    private ValueAnimator myAssistiveTouchAnimator(final int fromx, final int tox, int fromy, final int toy, final boolean flag) {
        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("X", fromx, tox);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofInt("Y", fromy, toy);
        ValueAnimator v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2);
        v1.setDuration(100L);
        v1.setInterpolator(new DecelerateInterpolator());
        v1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer x = (Integer) animation.getAnimatedValue("X");
                Integer y = (Integer) animation.getAnimatedValue("Y");
                mParams.x = x;
                mParams.y = y;
                mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
            }
        });
        return v1;
    }

    private void setAssistiveTouchViewAlign() {
        calculateForMyPhone();
        int mAssistiveTouchViewWidth = mAssistiveTouchView.getMeasuredWidth();
        int mAssistiveTouchViewHeight = mAssistiveTouchView.getMeasuredHeight();
        int top = mParams.y + mAssistiveTouchViewWidth / 2;
        int left = mParams.x + mAssistiveTouchViewHeight / 2;
        int right = mScreenWidth - mParams.x - mAssistiveTouchViewWidth / 2;
        int bottom = mScreenHeight - mParams.y - mAssistiveTouchViewHeight / 2;
        int lor = Math.min(left, right);
        int tob = Math.min(top, bottom);
        int min = Math.min(lor, tob);
        lastAssistiveTouchViewX = mParams.x;
        lastAssistiveTouchViewY = mParams.y;
        if (min == top) mParams.y = 0;
        if (min == left) mParams.x = 0;
        if (min == right) mParams.x = mScreenWidth - mAssistiveTouchViewWidth;
        if (min == bottom) mParams.y = mScreenHeight - mAssistiveTouchViewHeight;
        myAssistiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        calculateForMyPhone();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            alignToRightEdge();
        } else {
            setAssistiveTouchViewAlign();
        }
    }

    private void alignToRightEdge() {
        int mAssistiveTouchViewWidth = mAssistiveTouchView.getMeasuredWidth();
        int mAssistiveTouchViewHeight = mAssistiveTouchView.getMeasuredHeight();
        lastAssistiveTouchViewX = mParams.x;
        lastAssistiveTouchViewY = mParams.y;
        mParams.x = mScreenWidth - mAssistiveTouchViewWidth;
        if (mParams.y + mAssistiveTouchViewHeight > mScreenHeight) {
            mParams.y = Math.max(0, mScreenHeight - mAssistiveTouchViewHeight);
        }
        myAssistiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        mWindowManager.removeView(mAssistiveTouchView);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("info.plateaukao.action.DISABLE_SERVICE".equals(intent.getAction())) {
                disableSelf();
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter filter = new IntentFilter("info.plateaukao.action.DISABLE_SERVICE");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }
}
