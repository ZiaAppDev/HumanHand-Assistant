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

        btnClearCache.setOnClickListener(v -> clearAppCache());
        btnOptimizeRam.setOnClickListener(v -> optimizeRam());
        btnStartService.setOnClickListener(v -> startVoiceService());
        btnAccessibility.setOnClickListener(v -> openAccessibilitySettings());

        checkModelStatus(tvModelStatus);
    }

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "Enable 'HumanHand Offline Control'", Toast.LENGTH_LONG).show();
    }

    private void checkModelStatus(TextView tv) {
        try {
            String[] assets = getAssets().list("model-en-us");
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
