package com.humanhand.offlineassistant.voice;

import android.content.Context;
import android.util.Log;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;
import org.vosk.android.RecognitionListener;

import java.io.IOException;

public class VoiceRecognitionManager {
    private static final String TAG = "VoiceRecManager";
    private Model model;
    private SpeechService speechService;
    private final Context context;

    public VoiceRecognitionManager(Context context) {
        this.context = context;
        initModel();
    }

    private void initModel() {
        StorageService.unpack(context, "vosk-model-small-en-us-0.15", "model",
                (model) -> {
                    this.model = model;
                    Log.d(TAG, "Model loaded");
                },
                (exception) -> Log.e(TAG, "Failed to unpack model", exception));
    }

    public void startListening(RecognitionListener listener) {
        if (model == null) {
            Log.e(TAG, "Model not loaded yet");
            return;
        }

        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(listener);
            Log.d(TAG, "Started listening");
        } catch (IOException e) {
            Log.e(TAG, "Failed to start listening", e);
        }
    }

    public void stopListening() {
        if (speechService != null) {
            speechService.stop();
            speechService = null;
            Log.d(TAG, "Stopped listening");
        }
    }
    
    public void destroy() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
    }
}
