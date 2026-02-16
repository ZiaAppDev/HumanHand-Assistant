package com.humanhand.offlineassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

import com.humanhand.offlineassistant.voice.CommandParser;

import java.util.List;

public class HumanHandAccessibilityService extends AccessibilityService {
    private static final String TAG = "HumanHandAS";
    private WindowManager windowManager;

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            String target = intent.getStringExtra("target");
            String direction = intent.getStringExtra("direction");
            handleCommand(action, target, direction);
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        IntentFilter filter = new IntentFilter("com.humanhand.ACTION_COMMAND");
        registerReceiver(commandReceiver, filter);
        Log.d(TAG, "Service Connected and Receiver Registered");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private void handleCommand(String action, String target, String direction) {
        if (action == null) return;

        CommandParser.ActionType type = CommandParser.ActionType.valueOf(action);
        switch (type) {
            case OPEN_APP:
                openApplication(target);
                break;
            case CLICK:
                findAndClick(target);
                break;
            case SCROLL:
                performScroll(direction);
                break;
            case GO_BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case RECENTS:
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
        }
    }

    private void openApplication(String packageName) {
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } else {
            Log.e(TAG, "Could not find package: " + packageName);
        }
    }

    private void findAndClick(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            AccessibilityNodeInfo clickableNode = node;
            while (clickableNode != null && !clickableNode.isClickable()) {
                clickableNode = clickableNode.getParent();
            }

            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Clicked on: " + text);
            } else {
                Rect rect = new Rect();
                node.getBoundsInScreen(rect);
                performTap(rect.centerX(), rect.centerY());
                Log.d(TAG, "Tapped coordinates for: " + text);
            }
            node.recycle();
            if (clickableNode != null && clickableNode != node) clickableNode.recycle();
        } else {
            Log.d(TAG, "Node not found for text: " + text);
        }
        rootNode.recycle();
    }

    private void showExecutionIndicator(int x, int y) {
        final FrameLayout indicator = new FrameLayout(this);
        indicator.setBackgroundColor(Color.argb(100, 255, 0, 0)); 
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                50, 50,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = x - 25;
        params.y = y - 25;

        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                windowManager.addView(indicator, params);
                new Handler().postDelayed(() -> {
                    try {
                        windowManager.removeView(indicator);
                    } catch (Exception ignored) {}
                }, 500);
            } catch (Exception ignored) {}
        });
    }

    private void performTap(int x, int y) {
        showExecutionIndicator(x, y);
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 50));
        dispatchGesture(builder.build(), null, null);
    }

    private void performScroll(String direction) {
        int displayHeight = getResources().getDisplayMetrics().heightPixels;
        int displayWidth = getResources().getDisplayMetrics().widthPixels;

        Path path = new Path();
        int startX = displayWidth / 2;
        int startY = displayHeight / 2;
        int endX = startX;
        int endY = startY;

        switch (direction.toLowerCase()) {
            case "up": startY = (int) (displayHeight * 0.2); endY = (int) (displayHeight * 0.8); break;
            case "down": startY = (int) (displayHeight * 0.8); endY = (int) (displayHeight * 0.2); break;
            case "left": startX = (int) (displayWidth * 0.8); endX = (int) (displayWidth * 0.2); break;
            case "right": startX = (int) (displayWidth * 0.2); endX = (int) (displayWidth * 0.8); break;
        }

        path.moveTo(startX, startY);
        path.lineTo(endX, endY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 500));
        dispatchGesture(builder.build(), null, null);
    }

    @Override
    public void onDestroy() {
        if (commandReceiver != null) unregisterReceiver(commandReceiver);
        super.onDestroy();
    }
}
