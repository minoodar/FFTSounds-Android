package ir.zarbang.FFTsounds.service

import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import ir.zarbang.FFTsounds.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
    }

    private fun initializePlayer() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    private fun copyToCacheAndGetUri(uri: Uri, displayName: String): Uri? {
        val cacheFile = File(cacheDir, displayName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(cacheFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return cacheFile.toUri()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uris = intent?.getParcelableArrayListExtra<Uri>("URIS")
        val displayNames = intent?.getStringArrayListExtra("DISPLAY_NAMES")

        if (uris != null && displayNames != null && uris.isNotEmpty()) {
            serviceScope.launch {
                val mediaItems = uris.mapIndexedNotNull { index, uri ->
                    val displayName = displayNames.getOrNull(index) ?: "Unknown"
                    val cachedUri = copyToCacheAndGetUri(uri, displayName)
                    cachedUri?.let {
                        MediaItem.Builder()
                            .setUri(it)
                            .setMediaMetadata(
                                androidx.media3.common.MediaMetadata.Builder()
                                    .setTitle(displayName)
                                    .build()
                            )
                            .build()
                    }
                }

                withContext(Dispatchers.Main) {
                    if (mediaItems.isNotEmpty()) {
                        player.stop()
                        player.clearMediaItems()
                        player.addMediaItems(mediaItems)
                        player.prepare()
                        player.play()
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceJob.cancel()
        super.onDestroy()
    }
}
