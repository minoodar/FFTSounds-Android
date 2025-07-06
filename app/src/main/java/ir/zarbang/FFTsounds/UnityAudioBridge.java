package ir.zarbang.FFTsounds.bridge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import ir.zarbang.FFTsounds.fft.FrequencyBands;
import ir.zarbang.FFTsounds.fft.VisualizerHelper;
import ir.zarbang.FFTsounds.service.PlaybackService;

/**
 * UnityAudioBridge serves as the single point of contact (Facade) for the Unity application.
 * It provides a clean, simple API to control the native Android audio engine.
 * This class is designed as a singleton to maintain a single connection to the MediaController.
 */
public class UnityAudioBridge {

    private static final String TAG = "UnityAudioBridge";
    private static UnityAudioBridge instance;

    private Context context;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private final VisualizerHelper visualizerHelper = new VisualizerHelper();

    private UnityAudioBridge() {}

    public static synchronized UnityAudioBridge getInstance() {
        if (instance == null) {
            instance = new UnityAudioBridge();
        }
        return instance;
    }

    public void init(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "UnityAudioBridge initializing...");
        connectToService();
    }

    private void connectToService() {
        if (context == null) {
            Log.e(TAG, "Context is null. Cannot connect to service. Did you call init()?");
            return;
        }
        SessionToken sessionToken = new SessionToken(context, new ComponentName(context, PlaybackService.class));
        controllerFuture = new MediaController.Builder(context, sessionToken).buildAsync();
        controllerFuture.addListener(() -> {
            try {
                mediaController = controllerFuture.get();
                mediaController.addListener(new Player.Listener() {
                    @Override
                    public void onIsPlayingChanged(boolean isPlaying) {
                        if (isPlaying) {
                            visualizerHelper.start();
                        } else {
                            visualizerHelper.stop();
                        }
                    }
                });
                Log.d(TAG, "MediaController connected successfully.");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to connect to MediaController", e);
            }
        }, ContextCompat.getMainExecutor(context));
    }

    public void play(String[] filePaths) {
        if (context == null || filePaths == null || filePaths.length == 0) return;

        ArrayList<Uri> uris = new ArrayList<>();
        ArrayList<String> displayNames = new ArrayList<>();
        for (String path : filePaths) {
            File file = new File(path);
            if (file.exists()) {
                uris.add(Uri.fromFile(file));
                displayNames.add(file.getName());
            }
        }

        if (uris.isEmpty()) return;

        Intent intent = new Intent(context, PlaybackService.class);
        intent.putParcelableArrayListExtra("URIS", uris);
        intent.putStringArrayListExtra("DISPLAY_NAMES", displayNames);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public void pause() {
        if (mediaController != null) mediaController.pause();
    }

    public void resume() {
        if (mediaController != null) mediaController.play();
    }

    public void seekToNext() {
        if (mediaController != null) mediaController.seekToNextMediaItem();
    }

    public void seekToPrevious() {
        if (mediaController != null) mediaController.seekToPreviousMediaItem();
    }

    public void setShuffleMode(boolean enabled) {
        if (mediaController != null) mediaController.setShuffleModeEnabled(enabled);
    }

    public void setRepeatMode(int mode) {
        if (mediaController != null) mediaController.setRepeatMode(mode);
    }

    /**
     * CORRECTED: Renamed to match the API specification for Unity.
     * @return A JSON string, e.g., {"bass": 0.75, "mid": 0.4, "treble": 0.2}.
     */
    public String getFrequencyDataJson() {
        FrequencyBands bands = VisualizerHelper.frequencyBands.getValue();
        JSONObject json = new JSONObject();
        try {
            json.put("bass", bands.getBass());
            json.put("mid", bands.getMid());
            json.put("treble", bands.getTreble());
        } catch (JSONException e) {
            return "{}";
        }
        return json.toString();
    }

    /**
     * CORRECTED: Renamed to match the API specification for Unity.
     * @return A JSON string, e.g., {"title": "MySong.mp3", "duration": 240000, "position": 120000, "isPlaying": true}.
     */
    public String getCurrentTrackInfoJson() {
        JSONObject json = new JSONObject();
        if (mediaController != null && mediaController.getCurrentMediaItem() != null) {
            MediaItem currentItem = mediaController.getCurrentMediaItem();
            try {
                if (currentItem.mediaMetadata.title != null) {
                    json.put("title", currentItem.mediaMetadata.title.toString());
                }
                json.put("duration", mediaController.getDuration());
                json.put("position", mediaController.getCurrentPosition());
                json.put("isPlaying", mediaController.isPlaying());
            } catch (JSONException e) { /* return empty json */ }
        }
        return json.toString();
    }

    public boolean isPlaying() {
        return mediaController != null && mediaController.isPlaying();
    }

    public void release() {
        Log.d(TAG, "Releasing UnityAudioBridge resources.");
        if (visualizerHelper != null) visualizerHelper.stop();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        mediaController = null;
        context = null;
        instance = null;
    }
}
