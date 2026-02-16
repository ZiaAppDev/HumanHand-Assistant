package com.humanhand.offlineassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.os.Build;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
            case TYPE:
                typeText(target);
                break;
            case CALL:
                makeCall(target);
                break;
            case TOGGLE_WIFI:
                toggleWifi();
                break;
            case TOGGLE_FLASHLIGHT:
                toggleFlashlight();
                break;
            case TOGGLE_SPEAKER:
                toggleSpeaker();
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

    private boolean findAndClick(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;

        boolean success = false;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            AccessibilityNodeInfo clickableNode = node;
            while (clickableNode != null && !clickableNode.isClickable()) {
                clickableNode = clickableNode.getParent();
            }

            if (clickableNode != null) {
                success = clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Clicked on: " + text);
            } else {
                Rect rect = new Rect();
                node.getBoundsInScreen(rect);
                performTap(rect.centerX(), rect.centerY());
                Log.d(TAG, "Tapped coordinates for: " + text);
                success = true;
            }
            node.recycle();
            if (clickableNode != null && clickableNode != node) clickableNode.recycle();
        } else {
            Log.d(TAG, "Node not found for text: " + text);
        }
        rootNode.recycle();
        return success;
    }

    private void typeText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focusedNode != null) {
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            Log.d(TAG, "Typed text into focused node: " + text);
            focusedNode.recycle();
        } else {
            Log.d(TAG, "No focused input node found to type into");
        }
        rootNode.recycle();
    }

    private void makeCall(String contactName) {
        // Step 1: Try to find and click the contact name (in WhatsApp or Dialer)
        if (!findAndClick(contactName)) {
            // Step 2: Fallback - Open Dialer for manual confirmation if name not found
            Log.d(TAG, "Contact not found on screen, opening dialer for " + contactName);
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void toggleWifi() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null) {
            boolean isEnabled = wm.isWifiEnabled();
            wm.setWifiEnabled(!isEnabled);
            Log.d(TAG, "WiFi toggled to " + !isEnabled);
        }
    }

    private void toggleFlashlight() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                String cameraId = cm.getCameraIdList()[0];
                cm.setTorchMode(cameraId, true); // For now just turn it ON, can be refined to toggle
                Log.d(TAG, "Flashlight turned ON");
            } catch (Exception e) {
                Log.e(TAG, "Flashlight error", e);
            }
        }
    }

    private void toggleSpeaker() {
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            boolean isOn = am.isSpeakerphoneOn();
            am.setSpeakerphoneOn(!isOn);
            Log.d(TAG, "Speaker toggled to " + !isOn);
        }
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
