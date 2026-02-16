package com.humanhand.offlineassistant.ui;

import android.app.ActivityManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.humanhand.offlineassistant.R;
import java.io.File;

public class MaintenanceDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);

        Button btnClearCache = findViewById(R.id.btn_clear_cache);
        Button btnOptimizeRam = findViewById(R.id.btn_optimize_ram);

        btnClearCache.setOnClickListener(v -> clearAppCache());
        btnOptimizeRam.setOnClickListener(v -> optimizeRam());
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
