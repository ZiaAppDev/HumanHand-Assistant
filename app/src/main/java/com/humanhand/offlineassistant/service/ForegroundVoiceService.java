package com.humanhand.offlineassistant.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.humanhand.offlineassistant.R;
import com.humanhand.offlineassistant.voice.CommandParser;
import com.humanhand.offlineassistant.voice.VoiceRecognitionManager;

import org.json.JSONObject;
import org.vosk.android.RecognitionListener;

import java.util.Locale;

public class ForegroundVoiceService extends Service implements RecognitionListener, TextToSpeech.OnInitListener {
    private static final String TAG = "ForegroundVoiceService";
    private static final String CHANNEL_ID = "VoiceAssistantChannel";
    private VoiceRecognitionManager voiceManager;
    private TextToSpeech tts;
    private CommandParser.Command pendingCommand;

    private boolean isListening = false;

    private final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (isListening) {
                voiceManager.stopListening();
                isListening = false;
                speak("Stopped listening.");
            } else {
                voiceManager.startListening(ForegroundVoiceService.this);
                isListening = true;
                speak("How can I help?");
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        voiceManager = new VoiceRecognitionManager(this);
        tts = new TextToSpeech(this, this);
        registerReceiver(toggleReceiver, new android.content.IntentFilter("com.humanhand.TOGGLE_LISTENING"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification("Ready..."));
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Voice Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String text) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Human-Hand Assistant")
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG, "Partial: " + hypothesis);
    }

    @Override
    public void onResult(String hypothesis) {
        Log.d(TAG, "Result: " + hypothesis);
        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.optString("text", "");
            
            if (text.isEmpty()) return;

            if (text.contains("confirm")) {
                executePendingCommand();
            } else if (text.contains("cancel")) {
                pendingCommand = null;
                speak("Action cancelled.");
            } else {
                CommandParser.Command cmd = CommandParser.parse(text);
                if (cmd.action != CommandParser.ActionType.UNKNOWN) {
                    requestConfirmation(cmd);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse result JSON", e);
        }
    }

    private void requestConfirmation(CommandParser.Command cmd) {
        this.pendingCommand = cmd;
        String actionText = "";
        switch (cmd.action) {
            case OPEN_APP: actionText = "open " + cmd.target; break;
            case CLICK: actionText = "click " + cmd.target; break;
            case SCROLL: actionText = "scroll " + cmd.direction; break;
            case GO_BACK: actionText = "go back"; break;
            case HOME: actionText = "go home"; break;
            case RECENTS: actionText = "show recent apps"; break;
        }
        speak("I am about to " + actionText + ". Should I proceed? Say Confirm or Cancel.");
    }

    private void executePendingCommand() {
        if (pendingCommand != null) {
            // Send broadcast to Accessibility Service
            Intent intent = new Intent("com.humanhand.ACTION_COMMAND");
            intent.putExtra("action", pendingCommand.action.name());
            if (pendingCommand.target != null) intent.putExtra("target", pendingCommand.target);
            if (pendingCommand.direction != null) intent.putExtra("direction", pendingCommand.direction);
            sendBroadcast(intent);
            
            speak("Executing " + pendingCommand.action.name().replace("_", " ").toLowerCase());
            pendingCommand = null;
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "confirmation");
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    @Override
    public void onFinalResult(String hypothesis) {
        onResult(hypothesis);
    }

    @Override
    public void onError(Exception exception) {
        Log.e(TAG, "Recognition error", exception);
    }

    @Override
    public void onTimeout() {}

    @Override
    public void onDestroy() {
        if (voiceManager != null) voiceManager.destroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
