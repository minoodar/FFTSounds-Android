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

public class UnityAudioBridge {

    private static final String TAG = "UnityAudioBridge";
    private static UnityAudioBridge instance;

    private Context context;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private final VisualizerHelper visualizerHelper = new VisualizerHelper();

    private UnityAudioBridge() {}

    public static synchronized UnityAudioBridge getInstance(Context context) {
        if (instance == null) {
            instance = new UnityAudioBridge();
            instance.init(context);
        }
        return instance;
    }

    /**
     * NEW: A dummy static method.
     * Its only purpose is to be called from Unity's C# to force ProGuard/R8
     * to recognize this class as "in use" and prevent it from being stripped.
     * @return A simple confirmation string.
     */
    public static String getDummyString() {
        return "UnityAudioBridge is alive!";
    }

    private void init(Context context) {
        this.context = context.getApplicationContext();
        Log.d(TAG, "UnityAudioBridge initializing...");
        connectToService();
    }

    private void connectToService() {
        if (context == null) {
            Log.e(TAG, "Context is null. Cannot connect to service.");
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

    public void play(String commaSeparatedFilePaths) {
        if (context == null || commaSeparatedFilePaths == null || commaSeparatedFilePaths.isEmpty()) {
            Log.e(TAG, "Cannot play. Input string is null or empty.");
            return;
        }

        String[] filePaths = commaSeparatedFilePaths.split(",");
        ArrayList<Uri> uris = new ArrayList<>();
        ArrayList<String> displayNames = new ArrayList<>();
        for (String path : filePaths) {
            if (path.trim().isEmpty()) continue;
            File file = new File(path.trim());
            if (file.exists()) {
                uris.add(Uri.fromFile(file));
                displayNames.add(file.getName());
            } else {
                Log.w(TAG, "File does not exist: " + path);
            }
        }

        if (uris.isEmpty()) {
            Log.e(TAG, "No valid files found to play.");
            return;
        }

        Intent intent = new Intent(context, PlaybackService.class);
        intent.putParcelableArrayListExtra("URIS", uris);
        intent.putStringArrayListExtra("DISPLAY_NAMES", displayNames);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    public void pause() { if (mediaController != null) mediaController.pause(); }
    public void resume() { if (mediaController != null) mediaController.play(); }
    public void seekToNext() { if (mediaController != null) mediaController.seekToNextMediaItem(); }
    public void seekToPrevious() { if (mediaController != null) mediaController.seekToPreviousMediaItem(); }
    public void setShuffleMode(boolean enabled) { if (mediaController != null) mediaController.setShuffleModeEnabled(enabled); }
    public void setRepeatMode(int mode) { if (mediaController != null) mediaController.setRepeatMode(mode); }

    public String getFrequencyDataJson() {
        FrequencyBands bands = VisualizerHelper.frequencyBands.getValue();
        JSONObject json = new JSONObject();
        try {
            json.put("bass", bands.getBass());
            json.put("mid", bands.getMid());
            json.put("treble", bands.getTreble());
        } catch (JSONException e) { return "{}"; }
        return json.toString();
    }

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

    public boolean isPlaying() { return mediaController != null && mediaController.isPlaying(); }

    public void release() {
        Log.d(TAG, "Releasing UnityAudioBridge resources.");
        if (visualizerHelper != null) visualizerHelper.stop();
        if (controllerFuture != null) MediaController.releaseFuture(controllerFuture);
        mediaController = null;
        context = null;
        instance = null;
    }
}
