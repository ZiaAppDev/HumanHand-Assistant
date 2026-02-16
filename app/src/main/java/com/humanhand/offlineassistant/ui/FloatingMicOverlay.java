package com.humanhand.offlineassistant.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.humanhand.offlineassistant.R;

public class FloatingMicOverlay {
    private final Context context;
    private final WindowManager windowManager;
    private View overlayView;
    private ImageView micButton;

    public FloatingMicOverlay(Context context) {
        this.context = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void show() {
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;

        overlayView = LayoutInflater.from(context).inflate(R.layout.overlay_mic, null);
        
        micButton = overlayView.findViewById(R.id.mic_button);
        micButton.setOnClickListener(v -> {
            Intent intent = new Intent("com.humanhand.TOGGLE_LISTENING");
            context.sendBroadcast(intent);
        });

        windowManager.addView(overlayView, params);
    }

    public void startBlinking() {
        if (micButton != null) {
            Animation blink = new AlphaAnimation(1, 0);
            blink.setDuration(500);
            blink.setInterpolator(new LinearInterpolator());
            blink.setRepeatCount(Animation.INFINITE);
            blink.setRepeatMode(Animation.REVERSE);
            micButton.startAnimation(blink);
        }
    }

    public void stopBlinking() {
        if (micButton != null) {
            micButton.clearAnimation();
        }
    }

    public void hide() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
            overlayView = null;
        }
    }
}
