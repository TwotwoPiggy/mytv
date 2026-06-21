package com.Twotwo.TwotwoTV

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.view.Gravity
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.DISCONTINUITY_REASON_AUTO_TRANSITION
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.ui.PlayerView
import android.os.Build
import com.Twotwo.TwotwoTV.data.PlayerType
import com.Twotwo.TwotwoTV.data.SourceType
import com.Twotwo.TwotwoTV.data.StableSource
import com.Twotwo.TwotwoTV.data.TV
import com.Twotwo.TwotwoTV.models.TVModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Encapsulated ExoPlayer lifecycle management.
 * Handles creation, playback, source switching, monitoring, and release.
 * Communicates with PlayerFragment via [ExoPlayerCallback].
 */
@OptIn(UnstableApi::class)
class ExoPlayerEngine {

    companion object {
        private const val TAG = "ExoPlayerEngine"
    }

    // Player state
    private var player: ExoPlayer? = null
    private var callback: ExoPlayerCallback? = null
    private var tvModel: TVModel? = null
    private var context: Context? = null
    private var playerView: PlayerView? = null

    // Handler for periodic monitoring
    private val handler = Handler(Looper.myLooper()!!)
    private val engineScope = CoroutineScope(Dispatchers.Main)

    // Playback monitoring fields
    private var isStable = false
    private var playbackStartTime = 0L
    private var lastStopTime = 0L
    private var bufferingCount = 0
    private var lastSwitchTime = 0L
    private var bufferingStartTime = 0L
    private val bufferingTimestamps = mutableListOf<Long>()
    private var lastBufferingTime = 0L
    private var lastSwitchSourceTime = 0L
    private var lastPauseTime = 0L

    // Constants
    private val checkPlaybackInterval = 15_000L
    private val stablePlaybackDuration = 30_000L
    private val stablePlaybackThreshold = 10_000L
    private val bufferingThreshold = 5
    private val bufferingDurationThreshold = 8_000L
    private val switchCooldown = 15_000L
    private val switchSourceDebounce = 2_000L
    private val retryCooldown = 30_000L
    private val stopDurationThreshold = 5_000L

    /**
     * Create and configure ExoPlayer with renderers factory and codec selector.
     */
    fun create(context: Context, playerView: PlayerView) {
        this.context = context
        this.playerView = playerView

        val renderersFactory = DefaultRenderersFactory(context)
        val playerMediaCodecSelector = PlayerMediaCodecSelector()
        renderersFactory.setMediaCodecSelector(playerMediaCodecSelector)
        renderersFactory.setExtensionRendererMode(
            if (SP.softDecode) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
        )

        player = ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
        player?.repeatMode = REPEAT_MODE_ALL
        player?.playWhenReady = true

        setupPlayerListener()

        playerView.player = player
        Log.d(TAG, "ExoPlayer created")
    }

    /**
     * Register callback for playback state notifications.
     */
    fun setCallback(callback: ExoPlayerCallback) {
        this.callback = callback
    }

    /**
     * Start playing a TVModel.
     */
    fun play(model: TVModel) {
        this.tvModel = model
        playbackStartTime = System.currentTimeMillis()

        player?.run {
            val videoUrl = model.getVideoUrl() ?: run {
                Log.w(TAG, "No valid URL for ${model.tv.title}")
                model.setErrInfo("播放错误")
                return
            }
            val mediaItem = model.getMediaItem() ?: run {
                Log.w(TAG, "No valid mediaItem for ${model.tv.title}")
                model.setErrInfo("播放错误")
                return
            }
            val mediaSource = model.getMediaSource()
            try {
                if (mediaSource != null) {
                    setMediaSource(mediaSource)
                } else {
                    setMediaItem(mediaItem)
                }
                prepare()
                playWhenReady = true
                Log.d(TAG, "IPTV playback started for ${model.tv.title}")
            } catch (e: Exception) {
                Log.e(TAG, "IPTV playback failed for ${model.tv.title}: ${e.message}")
                model.setErrInfo("播放错误")
            }
        } ?: Log.w(TAG, "Player is null, cannot play ${model.tv.title}")

        startPlaybackMonitor()
    }

    /**
     * Switch to a different source URL with HLS support.
     */
    fun switchSource(model: TVModel) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwitchSourceTime < switchSourceDebounce) {
            Log.d(TAG, "Debounced switchSource for ${model.tv.title}")
            return
        }
        lastSwitchSourceTime = currentTime
        playbackStartTime = currentTime

        // Show source info toast
        val totalSources = model.tv.uris.filter { it.isNotBlank() }.size
        val sourceIndex = model.videoIndexValue + 1
        context?.let { ctx ->
            val toast = Toast.makeText(ctx, "线路 $sourceIndex / $totalSources", Toast.LENGTH_LONG)
            val textView = toast.view?.findViewById<TextView>(android.R.id.message)
            textView?.textSize = 30f
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()
            handler.postDelayed({ toast.cancel() }, 5000)
        }

        stopPlaybackMonitor()
        this.tvModel = model

        val videoUrl = model.getVideoUrl() ?: run {
            Log.w(TAG, "No valid URL for ${model.tv.title}")
            model.setErrInfo("播放错误")
            return
        }
        Log.d(TAG, "Switching source: ${model.tv.title}, url: $videoUrl, videoIndex=${model.videoIndexValue}")

        player?.run {
            val mediaItem = model.getMediaItem() ?: run {
                Log.w(TAG, "No valid mediaItem for ${model.tv.title}")
                model.setErrInfo("播放错误")
                return
            }
            stop()
            clearMediaItems()
            val mediaSource = model.getMediaSource()
            try {
                val hlsMediaSource = if (mediaSource != null && videoUrl.endsWith(".m3u8")) {
                    HlsMediaSource.Factory(DefaultHttpDataSource.Factory())
                        .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(3))
                        .createMediaSource(mediaItem)
                } else {
                    mediaSource
                }
                if (hlsMediaSource != null) {
                    setMediaSource(hlsMediaSource)
                } else {
                    setMediaItem(mediaItem)
                }
                prepare()
                playWhenReady = true
                Log.d(TAG, "Switched to source: ${model.tv.title}, videoIndex=${model.videoIndexValue}, url=$videoUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch source for ${model.tv.title}: ${e.message}", e)
                model.setErrInfo("播放错误")
                if (model.tv.uris.size > model.videoIndexValue + 1) {
                    model.setVideoIndex(model.videoIndexValue + 1)
                    model.confirmVideoIndex()
                    switchSource(model)
                    Log.d(TAG, "Retrying with next source: index=${model.videoIndexValue}, url=${model.getVideoUrl()}")
                }
            }
        } ?: Log.w(TAG, "Player is null, cannot switch source for ${model.tv.title}")

        startPlaybackMonitor()
    }

    /**
     * Stop current playback without destroying the engine.
     */
    fun stop() {
        player?.stop()
        stopPlaybackMonitor()
        Log.d(TAG, "Playback stopped")
    }

    /**
     * Full cleanup: release player, clear handler callbacks, null out all references.
     */
    fun release() {
        stopPlaybackMonitor()
        handler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
        callback = null
        tvModel = null
        context = null
        playerView = null
        Log.d(TAG, "ExoPlayerEngine released")
    }

    /**
     * Resume if paused.
     */
    fun ensurePlaying() {
        player?.run {
            if (!isPlaying && tvModel != null) {
                prepare()
                playWhenReady = true
            }
        }
    }

    /**
     * Get current video resolution.
     */
    fun getCurrentResolution(): String? {
        return if (tvModel?.tv?.playerType == PlayerType.IPTV) {
            player?.videoSize?.let { videoSize ->
                if (videoSize.width > 0 && videoSize.height > 0) "${videoSize.width}x${videoSize.height}" else null
            }
        } else {
            null
        }
    }

    /**
     * Check if player is currently playing.
     */
    fun isPlaying(): Boolean {
        return player?.isPlaying == true
    }

    /**
     * Get current tvModel.
     */
    fun getCurrentTvModel(): TVModel? {
        return tvModel
    }

    // -- Internal methods --

    private fun setupPlayerListener() {
        player?.addListener(object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                callback?.onVideoSizeChanged(videoSize.width, videoSize.height)
                Log.d(TAG, "Video size changed: ${videoSize.width}x${videoSize.height}")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val model = tvModel ?: return
                if (isPlaying) {
                    model.confirmSourceType()
                    model.confirmVideoIndex()
                    model.setErrInfo("")
                    model.retryTimes = 0
                    bufferingCount = 0
                    bufferingStartTime = 0L
                    bufferingTimestamps.clear()
                    lastBufferingTime = 0L
                    playbackStartTime = System.currentTimeMillis()
                    callback?.onPlaybackStarted()
                    lastStopTime = 0L
                    Log.d(TAG, "${model.tv.title} is playing")
                } else {
                    isStable = false
                    playbackStartTime = 0L
                    lastStopTime = System.currentTimeMillis()
                    callback?.onPlaybackStopped()
                    Log.i(TAG, "${model.tv.title} playback stopped")
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                callback?.onPlaybackStateChanged(state)
                if (!SP.autoSwitchSource) return
                val model = tvModel ?: return
                if (player == null) return

                val currentTime = System.currentTimeMillis()

                // Stable period check
                if (currentTime - playbackStartTime < stablePlaybackThreshold) {
                    if (state == Player.STATE_READY) {
                        playbackStartTime = currentTime
                    }
                    return
                }

                // Buffering detection
                if (state == Player.STATE_BUFFERING) {
                    if (currentTime - lastBufferingTime < 500L) return
                    if (bufferingStartTime == 0L) {
                        bufferingStartTime = currentTime
                    }
                    lastBufferingTime = currentTime
                    bufferingTimestamps.add(currentTime)
                    bufferingCount = bufferingTimestamps.count { it >= currentTime - 10_000L }
                    val bufferingDuration = currentTime - bufferingStartTime
                    bufferingTimestamps.removeAll { it < currentTime - 10_000L }

                    if ((bufferingCount >= bufferingThreshold && currentTime - lastSwitchTime >= switchCooldown) ||
                        (bufferingDuration >= bufferingDurationThreshold && currentTime - lastSwitchTime >= switchCooldown)
                    ) {
                        if (model.retryTimes < model.retryMaxTimes && player!!.currentPosition > 0) {
                            Log.i(TAG, "Non-smooth playback: bufferingCount=$bufferingCount, duration=$bufferingDuration")
                            // Signal to PlayerFragment via callback to trigger source switch
                            callback?.onPlaybackError("buffering_timeout")
                            lastSwitchTime = currentTime
                            playbackStartTime = currentTime
                            bufferingCount = 0
                            bufferingStartTime = 0L
                            bufferingTimestamps.clear()
                            lastBufferingTime = 0L
                        }
                    }
                } else if (state == Player.STATE_READY) {
                    if (currentTime - lastBufferingTime >= 2_000L) {
                        bufferingStartTime = 0L
                        bufferingCount = 0
                        bufferingTimestamps.clear()
                        lastBufferingTime = 0L
                    }
                } else if (state == Player.STATE_ENDED) {
                    bufferingStartTime = 0L
                    bufferingCount = 0
                    bufferingTimestamps.clear()
                    lastBufferingTime = 0L
                    playbackStartTime = 0L
                    lastStopTime = currentTime
                    Log.w(TAG, "${model.tv.title} playback ended, retrying immediately")
                    handler.postDelayed({
                        switchSource(model)
                        lastSwitchTime = System.currentTimeMillis()
                        lastStopTime = 0L
                    }, 500L)
                }
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Auto-transition handled by PlayerFragment via callback
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "Player error: ${error.errorCode}, message=${error.message}")
                val model = tvModel ?: return

                if (model.tv.playerType == PlayerType.WEBVIEW) {
                    if (error.errorCode !in listOf(
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
                        )
                    ) {
                        Log.d(TAG, "Ignored non-network ExoPlayer error for WEBVIEW: ${model.tv.title}")
                        return
                    }
                }

                lastStopTime = System.currentTimeMillis()
                callback?.onPlaybackError(error.message ?: "Unknown error")

                if (System.currentTimeMillis() - lastSwitchTime >= retryCooldown) {
                    Log.w(TAG, "${model.tv.title} error, retrying immediately")
                    lastSwitchTime = System.currentTimeMillis()
                    lastStopTime = 0L
                }
            }
        })
    }

    private fun startPlaybackMonitor() {
        stopPlaybackMonitor()
        handler.postDelayed(checkPlaybackRunnable, checkPlaybackInterval)
        handler.postDelayed(stableSourceCheckRunnable, stablePlaybackDuration)
    }

    private fun stopPlaybackMonitor() {
        handler.removeCallbacks(checkPlaybackRunnable)
        handler.removeCallbacks(stableSourceCheckRunnable)
    }

    private val checkPlaybackRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val model = tvModel
            if (model == null || player == null) {
                handler.postDelayed(this, checkPlaybackInterval)
                return
            }

            val isPlaying = player?.isPlaying == true &&
                player?.playbackState == Player.STATE_READY &&
                player?.playWhenReady == true

            val stopDuration = if (lastStopTime > 0) currentTime - lastStopTime else 0L
            val cooldownRemaining = if (currentTime - lastSwitchTime < retryCooldown) {
                retryCooldown - (currentTime - lastSwitchTime)
            } else 0L

            Log.d(TAG, "Playback check: isPlaying=$isPlaying, lastStopTime=$lastStopTime, " +
                "stopDuration=$stopDuration, cooldownRemaining=$cooldownRemaining")

            if (!isPlaying && lastStopTime > 0 && stopDuration >= stopDurationThreshold && cooldownRemaining == 0L) {
                Log.w(TAG, "${model.tv.title} stopped for ${stopDurationThreshold / 1000}s, retrying")
                switchSource(model)
                lastSwitchTime = currentTime
                lastStopTime = 0L
            } else if (isPlaying && stopDuration == 0L && cooldownRemaining == 0L) {
                if (currentTime - playbackStartTime >= stablePlaybackDuration &&
                    bufferingCount == 0 && model.retryTimes == 0
                ) {
                    isStable = true
                    Log.d(TAG, "Stable source detected: ${model.tv.title}")
                }
            }
            handler.postDelayed(this, checkPlaybackInterval)
        }
    }

    private val stableSourceCheckRunnable = Runnable {
        val model = tvModel
        if (player?.isPlaying == true && model != null &&
            System.currentTimeMillis() - playbackStartTime >= stablePlaybackDuration &&
            bufferingCount == 0 && model.retryTimes == 0
        ) {
            isStable = true
            Log.d(TAG, "Stable source saved via stableSourceCheckRunnable: ${model.tv.title}")
        }
    }

    /**
     * Custom MediaCodecSelector for API 23 compatibility and soft decode preference.
     */
    class PlayerMediaCodecSelector : MediaCodecSelector {
        override fun getDecoderInfos(
            mimeType: String,
            requiresSecureDecoder: Boolean,
            requiresTunnelingDecoder: Boolean
        ): MutableList<MediaCodecInfo> {
            val infos = MediaCodecUtil.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
            // API 23: prefer software decoders for compatibility
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                val softwareCodecs = infos.filter { !it.hardwareAccelerated }
                if (softwareCodecs.isNotEmpty()) {
                    Log.d(TAG, "API 23 detected, using software codecs for $mimeType")
                    return softwareCodecs.toMutableList()
                }
            }
            if (SP.softDecode) {
                val softwareCodecs = infos.filter { !it.hardwareAccelerated }
                if (softwareCodecs.isNotEmpty()) {
                    return softwareCodecs.toMutableList()
                }
            } else if (mimeType.startsWith("audio/")) {
                val softwareCodecs = infos.filter { !it.hardwareAccelerated }
                if (softwareCodecs.isNotEmpty()) {
                    return softwareCodecs.toMutableList()
                }
            }
            if (mimeType == MimeTypes.VIDEO_H265 && !requiresSecureDecoder && !requiresTunnelingDecoder) {
                if (infos.isNotEmpty()) {
                    val infosNew = infos.find { it.name == "c2.android.hevc.decoder" }
                        ?.let { mutableListOf(it) }
                    if (infosNew != null) {
                        return infosNew
                    }
                }
            }
            return infos
        }
    }
}
