package com.humanhand.offlineassistant.ui;

import android.app.ActivityManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.humanhand.offlineassistant.R;
import java.io.File;
import java.io.IOException;

public class MaintenanceDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        Button btnClearCache = findViewById(R.id.btn_clear_cache);
        Button btnOptimizeRam = findViewById(R.id.btn_optimize_ram);
        Button btnStartService = findViewById(R.id.btn_start_service);
        Button btnAccessibility = findViewById(R.id.btn_accessibility_settings);
        TextView tvModelStatus = findViewById(R.id.tv_model_status);
        Button btnSamsungOptimize = findViewById(R.id.btn_samsung_optimize);
        Button btnSamsungBattery = findViewById(R.id.btn_samsung_battery);

        btnClearCache.setOnClickListener(v -> clearAppCache());
        btnOptimizeRam.setOnClickListener(v -> optimizeRam());
        btnStartService.setOnClickListener(v -> startVoiceService());
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());
        btnSamsungOptimize.setOnClickListener(v -> openSamsungDeviceCare());
        btnSamsungBattery.setOnClickListener(v -> openSamsungBatterySettings());

        checkModelStatus(tvModelStatus);
    }

    private void openSamsungDeviceCare() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.IndexActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = getPackageManager().getLaunchIntentForPackage("com.samsung.android.lool");
                startActivity(intent);
            } catch (Exception e2) {
                Toast.makeText(this, "Samsung Device Care not found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openSamsungBatterySettings() {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Battery settings not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Enable 'HumanHand Offline Control'", Toast.LENGTH_LONG).show();
    }

    private void checkModelStatus(TextView tv) {
        try {
            String[] assets = getAssets().list("vosk-model-small-en-us-0.15");
            if (assets != null && assets.length > 0) {
                tv.setText("Model Status: Ready (Offline)");
                tv.setTextColor(0xFF4CAF50); // Green
            } else {
                tv.setText("Model Status: MISSING! (Add model to assets/model-en-us)");
                tv.setTextColor(0xFFF44336); // Red
            }
        } catch (IOException e) {
            tv.setText("Model Status: Error checking model");
        }
    }

    private void startVoiceService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivity(intent);
            Toast.makeText(this, "Please allow 'Display over other apps' for HumanHand", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(this, com.humanhand.offlineassistant.service.ForegroundVoiceService.class);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Assistant Started", Toast.LENGTH_SHORT).show();
    }

    private void clearAppCache() {
        try {
            File dir = getCacheDir();
            deleteDir(dir);
            Toast.makeText(this, "Cache Cleared", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) return false;
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    private void optimizeRam() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am != null) {
            am.killBackgroundProcesses(getPackageName());
            System.gc();
            Toast.makeText(this, "RAM Optimized", Toast.LENGTH_SHORT).show();
        }
    }
}
