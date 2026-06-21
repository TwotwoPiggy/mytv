package com.Twotwo.TwotwoTV

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.FrameLayout
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.Twotwo.TwotwoTV.databinding.PlayerBinding
import com.Twotwo.TwotwoTV.models.TVModel
import androidx.media3.ui.PlayerView
import com.Twotwo.TwotwoTV.data.StableSource
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.Twotwo.TwotwoTV.data.TV
import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Rational
import androidx.core.view.isVisible
import com.Twotwo.TwotwoTV.data.PlayerType
import com.horsenma.mytv1.WebFragmentCallback
import android.view.Gravity


class PlayerFragment : Fragment(), ExoPlayerCallback {
    private lateinit var viewModel: MainViewModel
    fun setViewModel(viewModel: MainViewModel) {
        this.viewModel = viewModel
    }
    private var _binding: PlayerBinding? = null
    private val binding get() = _binding!!
    internal var tvModel: TVModel? = null
    private val aspectRatio = 16f / 9f
    internal var isInPictureInPictureMode = false
    private val handler = Handler(Looper.myLooper()!!)
    private val delayHideVolume = 2 * 1000L
    private var isSourceButtonVisible = false

    // ExoPlayerEngine — delegates all ExoPlayer operations
    private val exoPlayerEngine = ExoPlayerEngine()

    init {
        exoPlayerEngine.setCallback(this)
    }

    @OptIn(UnstableApi::class)
    fun enterPictureInPictureMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "Picture-in-Picture mode not supported on API ${Build.VERSION.SDK_INT}")
            return
        }
        if (!isTouchScreenDevice()) {
            Log.d(TAG, "Picture-in-Picture mode skipped: Not a touchscreen device")
            return
        }
        // 获取视频的实际宽高比
        val aspectRatio = if (tvModel?.tv?.playerType == PlayerType.WEBVIEW) {
            Rational(16, 9)
        } else {
            val videoSize = binding.playerView.player?.videoSize
            if (videoSize != null && videoSize.width > 0 && videoSize.height > 0) {
                val ratio = videoSize.width.toFloat() / videoSize.height
                when {
                    ratio > 2.39f -> Rational(239, 100)
                    ratio < 1 / 2.39f -> Rational(100, 239)
                    else -> Rational(videoSize.width, videoSize.height)
                }
            } else {
                Rational(16, 9)
            }
        }

        val params = PictureInPictureParams.Builder()
            .setAspectRatio(aspectRatio)
            .build()
        try {
            requireActivity().enterPictureInPictureMode(params)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Failed to enter Picture-in-Picture mode: ${e.message}")
            return
        }
        if (_binding != null) {
            if (tvModel?.tv?.playerType == PlayerType.WEBVIEW) {
                binding.webView.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                binding.webView.visibility = View.VISIBLE
                binding.playerView.visibility = View.GONE
                binding.webView.requestLayout()
                binding.webView.requestFocus()

                childFragmentManager.findFragmentById(R.id.web_view)?.let { fragment ->
                    if (fragment is com.horsenma.mytv1.WebFragment) {
                        fragment.injectScalingCssForPiP()
                    }
                }
            } else {
                binding.playerView.resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                binding.playerView.useController = false
                binding.playerView.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = Gravity.CENTER
                }
                binding.playerView.requestLayout()
                binding.playerView.requestFocus()
                exoPlayerEngine.ensurePlaying()
            }
            setSourceButtonVisibility(false)
            binding.icon.visibility = View.GONE
            binding.volume.visibility = View.GONE
            binding.playerView.clearFocus()
        }
        isInPictureInPictureMode = true
        Log.d(TAG, "Entered Picture-in-Picture mode with aspectRatio=$aspectRatio, playerType=${tvModel?.tv?.playerType}")
    }

    @OptIn(UnstableApi::class)
    fun exitPictureInPictureMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.d(TAG, "Picture-in-Picture mode not supported on API ${Build.VERSION.SDK_INT}")
            return
        }
        if (_binding != null) {
            isInPictureInPictureMode = false
            setSourceButtonVisibility(isTouchScreenDevice() && SP.showSourceButton)
            onFullScreenModeChanged()
            Log.d(TAG, "Exiting Picture-in-Picture mode, btn_source visible=${binding.btnSource.isVisible}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            exoPlayerEngine.create(requireContext(), binding.playerView)
            binding.playerView.isFocusable = true
            binding.playerView.isFocusableInTouchMode = true
            binding.playerView.requestFocus()
            Log.d(TAG, "PlayerView focus requested: isFocusable=${binding.playerView.isFocusable}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize PlayerFragment view: ${e.message}", e)
        }
        (activity as MainActivity).ready()

        val btnSource = view.findViewById<Button>(R.id.btn_source)
        // 初始化 btn_source 可见性
        setSourceButtonVisibility(isTouchScreenDevice() && SP.showSourceButton)
        Log.d(TAG, "btn_source initialized: visibility=${btnSource.isVisible}, isTouchScreen=${isTouchScreenDevice()}, showSourceButton=${SP.showSourceButton}")

        // 设置 btn_source 的双击手势监听
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (btnSource.isEnabled && btnSource.isVisible) {
                    (activity as? MainActivity)?.sourceUp()
                    Log.d(TAG, "btn_source double tapped, triggering sourceUp")
                    return true
                }
                return false
            }
            override fun onLongPress(e: MotionEvent) {
                if (btnSource.isEnabled && btnSource.isVisible) {
                    val mainActivity = activity as? MainActivity
                    mainActivity?.showFragment(mainActivity.sourceSelectFragment)
                    Log.d(TAG, "btn_source long pressed, showing SourceSelectFragment")
                }
            }
        })

        // 确保 btn_source 优先接收触摸事件
        btnSource.setOnTouchListener { _, event ->
            if (btnSource.isEnabled && btnSource.isVisible) {
                gestureDetector.onTouchEvent(event)
                true // 消耗事件，防止 PlayerView 拦截
            } else {
                false // 不可见或禁用时透传事件
            }
        }

        // 防止 PlayerView 拦截 btn_source 的事件
        binding.playerView.setOnTouchListener { _, event ->
            val buttonRect = android.graphics.Rect()
            btnSource.getGlobalVisibleRect(buttonRect)
            if (btnSource.isVisible && buttonRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                btnSource.dispatchTouchEvent(event)
                true
            } else {
                // 传递给 MainActivity 的 gestureDetector
                (activity as? MainActivity)?.gestureDetector?.onTouchEvent(event) ?: false
                true // 始终消耗事件
            }
        }
    }

    // 控制 btn_source 可见性
    @OptIn(UnstableApi::class)
    fun setSourceButtonVisibility(visible: Boolean) {
        val btnSource = binding.root.findViewById<Button>(R.id.btn_source) ?: return
        val shouldShow = if (!visible) {
            false // 画中画模式下始终隐藏
        } else {
            // 所有设备都显示换源按钮（触屏用点击，遥控器用长按）
            SP.showSourceButton
        }
        btnSource.visibility = if (shouldShow) View.VISIBLE else View.GONE
        btnSource.isFocusable = shouldShow
        btnSource.isEnabled = shouldShow
        btnSource.isFocusableInTouchMode = shouldShow // 确保触摸交互
        isSourceButtonVisible = shouldShow
        Log.d(TAG, "setSourceButtonVisibility: visible=$visible, shouldShow=$shouldShow, showSourceButton=${SP.showSourceButton}, btnSource.focusable=${btnSource.isFocusable}")
    }

    // -- ExoPlayerCallback implementation --

    override fun onPlaybackStarted() {
        tvModel?.let { model ->
            model.confirmSourceType()
            model.confirmVideoIndex()
            model.setErrInfo("")
            model.retryTimes = 0
        }
        Log.d(TAG, "ExoPlayer playback started")
    }

    override fun onPlaybackStopped() {
        Log.d(TAG, "ExoPlayer playback stopped")
    }

    override fun onPlaybackError(error: String) {
        tvModel?.let { model ->
            model.setErrInfo(error)
        }
        Log.e(TAG, "ExoPlayer playback error: $error")
    }

    override fun onVideoSizeChanged(width: Int, height: Int) {
        if (!isInPictureInPictureMode) {
            updatePlayerViewLayout()
        }
        Log.d(TAG, "Video size changed: ${width}x${height}")
    }

    override fun onPlaybackStateChanged(state: Int) {
        // Buffering detection and source switching delegated to ExoPlayerEngine
    }

    // -- UI layout methods --

    @OptIn(UnstableApi::class)
    private fun updatePlayerViewLayout() {
        val playerView = binding.playerView
        val app = TwotwoTVApplication.getInstance()
        val isFullScreen = SP.fullScreenMode

        playerView.resizeMode = if (isFullScreen) {
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL
        } else {
            androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        val layoutParams = FrameLayout.LayoutParams(
            if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else app.videoWidthPx(),
            if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else app.videoHeightPx()
        ).apply {
            gravity = Gravity.CENTER // 确保居中
        }
        playerView.layoutParams = layoutParams

        playerView.requestLayout()
        playerView.post {
            Log.d(TAG, "Updated PlayerView layout: fullScreen=$isFullScreen, width=${layoutParams.width}, height=${layoutParams.height}, gravity=${layoutParams.gravity}")
        }
    }

    @OptIn(UnstableApi::class)
    fun onFullScreenModeChanged() {
        if (!isAdded || isInPictureInPictureMode || _binding == null) {
            Log.d(TAG, "onFullScreenModeChanged skipped: isAdded=$isAdded, isInPiP=$isInPictureInPictureMode, binding=${_binding}")
            return
        }
        val app = TwotwoTVApplication.getInstance()
        val isFullScreen = SP.fullScreenMode
        if (tvModel?.tv?.playerType == PlayerType.WEBVIEW) {
            binding.webView.layoutParams = FrameLayout.LayoutParams(
                if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else app.videoWidthPx(),
                if (isFullScreen) ViewGroup.LayoutParams.MATCH_PARENT else app.videoHeightPx()
            ).apply {
                gravity = Gravity.CENTER
            }
            binding.webView.visibility = View.VISIBLE
            binding.playerView.visibility = View.GONE
            binding.webView.bringToFront() // 确保 WebView 在顶层
            binding.webView.requestLayout()
            binding.webView.post {
                Log.d(TAG, "web_view updated for fullScreenMode: fullScreen=$isFullScreen, width=${binding.webView.width}, height=${binding.webView.height}")
            }
            // 通知 WebFragment 更新布局
            childFragmentManager.findFragmentById(R.id.web_view)?.let { fragment ->
                if (fragment is com.horsenma.mytv1.WebFragment) {
                    fragment.updateWebViewLayout()
                }
            }
            binding.webView.requestFocus()
            binding.webView.isFocusable = true
            binding.webView.isFocusableInTouchMode = true
        } else {
            updatePlayerViewLayout()
            binding.playerView.visibility = View.VISIBLE
            binding.webView.visibility = View.GONE
            binding.playerView.requestFocus()
            binding.playerView.isFocusable = true
            binding.playerView.isFocusableInTouchMode = true
        }
        // 强制刷新整个布局
        binding.root.requestLayout()
        binding.root.requestFocus()
        // 验证窗口尺寸
        val displayMetrics = resources.displayMetrics
        Log.d(TAG, "onFullScreenModeChanged: fullScreen=$isFullScreen, videoWidthPx=${app.videoWidthPx()}, videoHeightPx=${app.videoHeightPx()}, screenWidth=${displayMetrics.widthPixels}, screenHeight=${displayMetrics.heightPixels}")
    }

    private fun selectRandomStableSource(): StableSource? {
        val stableSources = SP.getStableSources()
        return if (stableSources.isNotEmpty()) {
            stableSources.sortedByDescending { it.timestamp }.firstOrNull()
        } else {
            null
        }
    }

    private fun saveStableSource(tvModel: TVModel) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentUrl = tvModel.getVideoUrl() ?: run {
                    Log.w(TAG, "Failed to save stable source: ${tvModel.tv.title}, no valid URL")
                    return@launch
                }
                val tv = tvModel.tv
                Log.d(TAG, "Preparing to save stable source: ${tv.title}, videoIndex=${tvModel.videoIndexValue}, url=$currentUrl")
                val newSource = StableSource(
                    id = tv.id,
                    name = tv.name,
                    title = tv.title,
                    description = tv.description,
                    logo = tv.logo,
                    image = tv.image,
                    uris = listOf(currentUrl),
                    videoIndex = tvModel.videoIndexValue,
                    headers = tv.headers,
                    group = tv.group,
                    sourceType = tvModel.getSourceTypeCurrent().name,
                    number = tv.number,
                    child = tv.child,
                    timestamp = System.currentTimeMillis(),
                    playerType = tv.playerType,
                    block = tv.block,
                    script = tv.script,
                    selector = tv.selector,
                    started = tv.started,
                    finished = tv.finished
                )
                val currentSources = SP.getStableSources()
                // 检查 id、playerType、uris 和 videoIndex 是否完全相同
                val existingSource = currentSources.firstOrNull { it.id == newSource.id }
                if (existingSource != null &&
                    existingSource.playerType == newSource.playerType &&
                    existingSource.uris == newSource.uris &&
                    existingSource.videoIndex == newSource.videoIndex
                ) {
                    Log.d(TAG, "Skipping save stable source: ${newSource.title}, identical to existing (playerType=${newSource.playerType}, url=$currentUrl, videoIndex=${newSource.videoIndex})")
                    return@launch
                }
                // 保存新源，覆盖同 id 的旧源
                val updatedSources = (currentSources.filter { it.id != newSource.id } + newSource)
                    .sortedByDescending { it.timestamp }.take(200)
                SP.setStableSources(updatedSources)
                Log.d(TAG, "Saved stable source: ${newSource.title}, playerType=${newSource.playerType}, url=$currentUrl, videoIndex=${newSource.videoIndex}, uris=${newSource.uris}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save stable source: ${tvModel.tv.title}, error=${e.message}", e)
            }
        }
    }

    /**
     * Recreate the ExoPlayer engine with current settings (e.g., after soft decode toggle).
     */
    fun recreatePlayer() {
        exoPlayerEngine.release()
        if (_binding != null) {
            exoPlayerEngine.create(requireContext(), binding.playerView)
            tvModel?.let { exoPlayerEngine.play(it) }
        }
    }

    /**
     * Release the ExoPlayer engine (called from Activity before process kill or fragment recreation).
     */
    fun releasePlayer() {
        exoPlayerEngine.release()
    }

    /**
     * Check if the ExoPlayer engine has an active player.
     */
    fun isPlayerActive(): Boolean {
        return exoPlayerEngine.isPlaying()
    }

    @OptIn(UnstableApi::class)
    fun getCurrentResolution(): String? {
        return exoPlayerEngine.getCurrentResolution()
    }

    fun ensurePlaying() {
        exoPlayerEngine.ensurePlaying()
    }

    @OptIn(UnstableApi::class)
    fun switchSource(tvModel: TVModel) {
        exoPlayerEngine.switchSource(tvModel)
    }

    @OptIn(UnstableApi::class)
    fun play(tvModel: TVModel) {
        this.tvModel = tvModel
        val stableSource = SP.getStableSources().firstOrNull { it.id == tvModel.tv.id }
        if (stableSource != null) {
            tvModel.tv = tvModel.tv.copy(
                playerType = stableSource.playerType,
                videoIndex = stableSource.videoIndex
            )
            tvModel.setVideoIndex(stableSource.videoIndex)
            Log.d(TAG, "Applied stable source: ${tvModel.tv.title}, playerType=${tvModel.tv.playerType}, url=${tvModel.getVideoUrl()}, videoIndex=${tvModel.videoIndexValue}")
        } else {
            Log.d(TAG, "No stable source found for ${tvModel.tv.title}, using default uris=${tvModel.tv.uris}, videoIndex=${tvModel.videoIndexValue}")
        }
        Log.d(TAG, "Playing tvModel: ${tvModel.tv.title}, playerType: ${tvModel.tv.playerType}, uris: ${tvModel.tv.uris.size}")

        if (tvModel.tv.playerType == PlayerType.WEBVIEW) {
            // Release ExoPlayer engine before switching to WebView
            exoPlayerEngine.release()
            binding.playerView.player = null
            binding.playerView.visibility = View.GONE
            binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
            binding.webView.visibility = View.VISIBLE
            val app = TwotwoTVApplication.getInstance()
            binding.webView.layoutParams = FrameLayout.LayoutParams(
                app.videoWidthPx(),
                app.videoHeightPx()
            ).apply {
                gravity = Gravity.CENTER
            }
            binding.webView.requestLayout()
            binding.webView.post {
                Log.d(TAG, "web_view actual size: width=${binding.webView.width}, height=${binding.webView.height}")
            }
            binding.webView.bringToFront()
            (activity as? MainActivity)?.updateFullScreenMode(SP.fullScreenMode)
            try {
                val webFragment = com.horsenma.mytv1.WebFragment()
                if (isAdded && !isDetached && !childFragmentManager.isStateSaved) {
                    childFragmentManager.beginTransaction()
                        .replace(R.id.web_view, webFragment)
                        .commitNow()
                    Log.d(TAG, "WebFragment loaded for ${tvModel.tv.title}")
                } else {
                    Log.w(TAG, "Skipped WebFragment loading: isAdded=$isAdded, isDetached=$isDetached, isStateSaved=${childFragmentManager.isStateSaved}")
                    tvModel.setErrInfo(R.string.play_error.getString())
                    binding.webView.visibility = View.GONE
                    binding.playerView.visibility = View.VISIBLE
                    binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                    binding.playerView.bringToFront()
                    binding.playerView.requestFocus()
                    binding.playerView.requestLayout()
                    exoPlayerEngine.create(requireContext(), binding.playerView)
                    return
                }

                webFragment.setCallback(object : WebFragmentCallback {
                    override fun onPlaybackStarted() {
                        Log.d(TAG, "WebView playback started for ${tvModel.tv.title}")
                    }
                    override fun onPlaybackStopped() {
                        Log.d(TAG, "WebView playback stopped for ${tvModel.tv.title}")
                    }
                    override fun onPlaybackError(error: String) {
                        Log.e(TAG, "WebView playback error for ${tvModel.tv.title}: $error")
                    }
                })

                webFragment.viewLifecycleOwnerLiveData.observe(viewLifecycleOwner) { owner ->
                    if (owner != null) {
                        webFragment.play(
                            com.horsenma.mytv1.models.TVModel(
                                com.horsenma.mytv1.data.TV(
                                    id = tvModel.tv.id,
                                    title = tvModel.tv.title,
                                    name = tvModel.tv.name,
                                    uris = tvModel.tv.uris,
                                    group = tvModel.tv.group,
                                    logo = tvModel.tv.logo,
                                    block = tvModel.tv.block ?: emptyList(),
                                    script = tvModel.tv.script,
                                    selector = tvModel.tv.selector,
                                    started = tvModel.tv.started,
                                    finished = tvModel.tv.finished,
                                    index = tvModel.videoIndexValue
                                )
                            )
                        )
                        Log.d(TAG, "WebFragment playing: ${tvModel.tv.title}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load WebFragment: ${e.message}")
                tvModel.setErrInfo(R.string.play_error.getString())
                binding.webView.visibility = View.GONE
                binding.playerView.visibility = View.VISIBLE
                binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                binding.playerView.bringToFront()
                binding.playerView.requestFocus()
                binding.playerView.requestLayout()
                exoPlayerEngine.create(requireContext(), binding.playerView)
            }
            binding.webView.requestFocus()
            binding.playerView.isFocusable = false
            binding.playerView.setOnTouchListener(null)
        } else {
            // IPTV 播放逻辑。 移除 WebFragment
            if (isAdded && !isDetached && childFragmentManager.isStateSaved.not()) {
                childFragmentManager.findFragmentById(R.id.web_view)?.let { webFragment ->
                    if (webFragment is com.horsenma.mytv1.WebFragment) {
                        try {
                            childFragmentManager.beginTransaction()
                                .remove(webFragment)
                                .commit()
                            Log.d(TAG, "Removed WebFragment for ${tvModel.tv.title}")
                        } catch (e: IllegalStateException) {
                            Log.e(TAG, "Failed to remove WebFragment: ${e.message}", e)
                        }
                    }
                }
            } else {
                Log.w(TAG, "Skipped WebFragment removal: isAdded=$isAdded, isDetached=$isDetached, isStateSaved=${childFragmentManager.isStateSaved}")
            }
            binding.playerView.visibility = View.VISIBLE
            binding.webView.visibility = View.GONE
            binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            binding.playerView.bringToFront()
            updatePlayerViewLayout()
            binding.playerView.requestFocus()
            binding.playerView.requestLayout()

            Log.d(TAG, "Playing IPTV: ${tvModel.tv.title}, uris: ${tvModel.tv.uris.size}")

            // Create engine if needed and play
            exoPlayerEngine.create(requireContext(), binding.playerView)
            exoPlayerEngine.play(tvModel)

            binding.playerView.requestFocus()
            binding.webView.isFocusable = false
        }
    }

    @OptIn(UnstableApi::class)
    fun updateSource() {
        tvModel?.let { model ->
            exoPlayerEngine.stop()
            play(model)
        }
    }

    private fun isTouchScreenDevice(): Boolean {
        val context = context ?: return false
        val packageManager = context.packageManager
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? android.app.UiModeManager
        val isTv = uiModeManager?.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val hasTouchScreen = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
        return hasTouchScreen && !isTv
    }

    fun showVolume(visibility: Int) {
        binding.icon.visibility = visibility
        binding.volume.visibility = visibility
        hideVolume()
    }

    fun setVolumeMax(volume: Int) {
        binding.volume.max = volume
    }

    fun setVolume(progress: Int, volume: Boolean = false) {
        val context = requireContext()
        binding.volume.progress = progress
        binding.icon.setImageDrawable(
            ContextCompat.getDrawable(
                context,
                if (volume) {
                    if (progress > 0) R.drawable.volume_up_24px else R.drawable.volume_off_24px
                } else {
                    R.drawable.light_mode_24px
                }
            )
        )
    }

    fun hideVolume() {
        handler.removeCallbacks(hideVolumeRunnable)
        handler.postDelayed(hideVolumeRunnable, delayHideVolume)
    }

    fun hideVolumeNow() {
        handler.removeCallbacks(hideVolumeRunnable)
        handler.postDelayed(hideVolumeRunnable, 0)
    }

    private val hideVolumeRunnable = Runnable {
        binding.icon.visibility = View.GONE
        binding.volume.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        exoPlayerEngine.ensurePlaying()
    }

    override fun onPause() {
        super.onPause()
        if (tvModel?.tv?.playerType == PlayerType.WEBVIEW) {
            Log.d(TAG, "Skipping pause for WEBVIEW")
            return
        }
        if (!SP.enableScreenOffAudio && exoPlayerEngine.isPlaying()) {
            exoPlayerEngine.stop()
            Log.d(TAG, "Paused player due to SP.enableScreenOffAudio=false")
        }
    }

    // 添加广播接收器
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                if (!SP.enableScreenOffAudio && exoPlayerEngine.isPlaying()) {
                    exoPlayerEngine.stop()
                    Log.d(TAG, "Paused player on SCREEN_OFF in ${if (isInPictureInPictureMode) "PiP" else "Full-Screen"} mode")
                }
            } else if (intent.action == Intent.ACTION_SCREEN_ON) {
                if (!SP.enableScreenOffAudio) {
                    exoPlayerEngine.ensurePlaying()
                    Log.d(TAG, "Resumed player on SCREEN_ON in ${if (isInPictureInPictureMode) "PiP" else "Full-Screen"} mode")
                }
            }
        }
    }

    // 在 onCreate 中注册
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        try {
            requireActivity().registerReceiver(screenReceiver, filter)
            Log.d(TAG, "Screen broadcast receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register screen broadcast receiver: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayerEngine.release()
        requireActivity().unregisterReceiver(screenReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        exoPlayerEngine.release()
        _binding = null
    }

    companion object {
        private const val TAG = "PlayerFragment"
    }
}
