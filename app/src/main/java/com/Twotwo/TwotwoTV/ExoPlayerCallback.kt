package com.Twotwo.TwotwoTV

/**
 * Callback interface for ExoPlayerEngine -> PlayerFragment communication.
 * Modeled on the existing WebFragmentCallback pattern.
 */
interface ExoPlayerCallback {
    /** Called when playback starts successfully. */
    fun onPlaybackStarted()

    /** Called when playback stops. */
    fun onPlaybackStopped()

    /** Called on player error. */
    fun onPlaybackError(error: String)

    /** Called when video dimensions change. */
    fun onVideoSizeChanged(width: Int, height: Int)

    /** Called on ExoPlayer state transitions (BUFFERING, READY, IDLE, ENDED). */
    fun onPlaybackStateChanged(state: Int)
}
