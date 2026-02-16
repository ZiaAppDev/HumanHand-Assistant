package com.humanhand.offlineassistant.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.humanhand.offlineassistant.voice.CommandParser;

import java.util.List;

public class HumanHandAccessibilityService extends AccessibilityService {
    private static final String TAG = "HumanHandAS";

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
        IntentFilter filter = new IntentFilter("com.humanhand.ACTION_COMMAND");
        registerReceiver(commandReceiver, filter);
        Log.d(TAG, "Service Connected and Receiver Registered");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We can track screen changes here if needed for dynamic updates
    }

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
        }
    }

    private void openApplication(String packageName) {
        // Simplification: In a real app, mapping 'WhatsApp' to 'com.whatsapp' is needed
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(packageName);
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
        } else {
            // Try searching by label if package name fails
            Log.e(TAG, "Could not find package: " + packageName);
        }
    }

    private void findAndClick(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            performTap(rect.centerX(), rect.centerY());
            node.recycle();
        } else {
            Log.d(TAG, "Node not found for text: " + text);
        }
        rootNode.recycle();
    }

    private void performTap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        dispatchGesture(builder.build(), null, null);
    }

    private void performScroll(String direction) {
        // Basic scroll implementation using GestureDescription
        Log.d(TAG, "Scrolling " + direction);
        // Implementation for swipe gestures based on direction
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(commandReceiver);
        super.onDestroy();
    }
}
