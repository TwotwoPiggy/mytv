package com.horsenma.mytv1

import android.content.Context
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import android.content.Intent
import androidx.annotation.RequiresApi
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.horsenma.mytv1.models.TVModel
import kotlin.math.abs
import com.Twotwo.TwotwoTV.R
import com.Twotwo.TwotwoTV.showToast
import kotlinx.coroutines.*


class MainActivity : FragmentActivity() {

    internal var webFragment = com.horsenma.mytv1.WebFragment()
    private val errorFragment = com.horsenma.mytv1.ErrorFragment()
    private val loadingFragment = com.horsenma.mytv1.LoadingFragment()
    private var infoFragment = com.horsenma.mytv1.InfoFragment()
    private var channelFragment = com.horsenma.mytv1.ChannelFragment()
    private var timeFragment = com.horsenma.mytv1.TimeFragment()
    private var menuFragment = com.horsenma.mytv1.MenuFragment()
    private var settingFragment = com.horsenma.mytv1.SettingFragment()

    private val handler = Handler(Looper.myLooper()!!)
    private val delayHideMenu = 10 * 1000L
    private val delayHideSetting = 1 * 60 * 1000L
    lateinit var gestureDetector: GestureDetector
    private var server: SimpleServer? = null
    private var isSafeToPerformFragmentTransactions = false

    private var currentIndex = 0
    private var currentTvModel: TVModel? = null

    private var menuPressCount = 0
    private var lastMenuPressTime = 0L
    private val MENU_PRESS_INTERVAL = 300L
    private val MENU_TAP_INTERVAL = 800L
    private val REQUIRED_MENU_PRESSES = 4
    private var lastSwitchTime = 0L
    private val DEBOUNCE_INTERVAL = 2000L
    private var lastBackPressTime = 0L
    private val BACK_PRESS_INTERVAL = 2000L

    private val handleEnterRunnable = Runnable {
        if (menuPressCount == 1) {
            showFragment(menuFragment)
            menuActive()
        }
        menuPressCount = 0
    }

    private val handleRightRunnable = Runnable {
        menuPressCount = 0
    }

    private val handleTapRunnable = Runnable {
        if (menuPressCount >= REQUIRED_MENU_PRESSES) {
            showSetting()
            menuPressCount = 0
        } else if (menuPressCount == 2) {
            showFragment(menuFragment)
            menuActive()
        }
        menuPressCount = 0
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SP.init(this)

        updateFullScreenMode(com.Twotwo.TwotwoTV.SP.fullScreenMode)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, webFragment, "WebFragment")
                .add(R.id.main_browse_fragment, errorFragment, "ErrorFragment")
                .add(R.id.main_browse_fragment, loadingFragment, "LoadingFragment")
                .add(R.id.main_browse_fragment, infoFragment, "InfoFragment")
                .add(R.id.main_browse_fragment, channelFragment, "ChannelFragment")
                .add(R.id.main_browse_fragment, menuFragment, "MenuFragment")
                .add(R.id.main_browse_fragment, settingFragment, "SettingFragment")
                .hide(menuFragment)
                .hide(settingFragment)
                .hide(errorFragment)
                .hide(loadingFragment)
                .show(webFragment)
                .commitNow()
            isSafeToPerformFragmentTransactions = true
        }

        gestureDetector = GestureDetector(this, GestureListener(this))

        // 延迟 500ms 确保 WebView 完全初始化，然后加载并播放
        handler.postDelayed({
            loadAndPlay()
        }, 500)

        server = SimpleServer(this)
    }

    /**
     * 加载频道数据并播放第一个频道
     */
    private fun loadAndPlay() {
        lifecycleScope.launch {
            try {
                val channels = withContext(Dispatchers.IO) {
                    ChannelLoader.load(this@MainActivity)
                }
                Log.d(TAG, "Loaded ${channels.size} channels")
                if (channels.isEmpty()) {
                    Log.w(TAG, "No channels available")
                    getString(R.string.no_available_channel).showToast(Toast.LENGTH_LONG)
                    return@launch
                }

                val tv = ChannelLoader.getChannel(0)!!
                val tvModel = ChannelLoader.createTVModel(tv, 0)
                currentIndex = 0
                currentTvModel = tvModel

                // 观察 errInfo 状态变化
                tvModel.errInfo.observe(this@MainActivity) { errInfo ->
                    if (errInfo == null) return@observe
                    Log.d(TAG, "errInfo changed: $errInfo")
                    if (errInfo == "" || errInfo == "web ok") {
                        hideFragment(loadingFragment)
                        hideErrorFragment()
                        showFragment(webFragment)
                        Log.i(TAG, "${tvModel.tv.title} 播放中")
                    } else {
                        hideFragment(loadingFragment)
                        hideFragment(webFragment)
                        showErrorFragment(errInfo)
                        Log.i(TAG, "${tvModel.tv.title} 错误: $errInfo")
                    }
                }

                // 显示加载中，然后播放
                showFragment(loadingFragment)
                webFragment.play(tvModel)
                infoFragment.show(tvModel)
                Log.d(TAG, "Playing first channel: ${tvModel.tv.title}")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load channels: ${e.message}", e)
                getString(R.string.no_available_channel).showToast(Toast.LENGTH_LONG)
            }
        }
    }

    fun ready(tag: String) {
        Log.i(TAG, "ready $tag")
    }

    fun updateMenuSize() {
        menuFragment.updateSize()
    }

    /**
     * 播放指定索引的频道
     */
    fun play(position: Int) {
        val count = ChannelLoader.getChannelCount()
        if (position < 0 || position >= count) {
            Toast.makeText(this, "频道不存在", Toast.LENGTH_LONG).show()
            return
        }
        val tv = ChannelLoader.getChannel(position) ?: return
        val tvModel = ChannelLoader.createTVModel(tv, position)
        currentIndex = position
        currentTvModel = tvModel

        // 观察 errInfo 状态变化
        tvModel.errInfo.observe(this) { errInfo ->
            if (errInfo == null) return@observe
            if (errInfo == "" || errInfo == "web ok") {
                hideFragment(loadingFragment)
                hideErrorFragment()
                showFragment(webFragment)
            } else {
                hideFragment(loadingFragment)
                hideFragment(webFragment)
                showErrorFragment(errInfo)
            }
        }

        hideErrorFragment()
        showFragment(loadingFragment)
        webFragment.play(tvModel)
        infoFragment.show(tvModel)
        if (SP.channelNum) {
            channelFragment.show(tvModel)
        }
        Log.i(TAG, "Playing channel $position: ${tvModel.tv.title}")
    }

    fun prev() {
        val count = ChannelLoader.getChannelCount()
        if (count == 0) return
        var position = currentIndex - 1
        if (position < 0) position = count - 1
        play(position)
    }

    fun next() {
        val count = ChannelLoader.getChannelCount()
        if (count == 0) return
        var position = currentIndex + 1
        if (position >= count) position = 0
        play(position)
    }

    fun onPlayEnd() {
        val tvModel = currentTvModel ?: return
        if (SP.repeatInfo) {
            infoFragment.show(tvModel)
            if (SP.channelNum) {
                channelFragment.show(tvModel)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun updateFullScreenMode(isFullScreen: Boolean) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        val params = window.attributes
        if (isFullScreen) {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
        }
        window.attributes = params
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.decorView.requestLayout()
        window.decorView.invalidate()
        if (isSafeToPerformFragmentTransactions && webFragment.isAdded) {
            handler.removeCallbacksAndMessages(null)
            handler.postDelayed({
                webFragment.updateWebViewLayout()
            }, 100)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
            return true
        }
        return super.onTouchEvent(event)
    }

    private inner class GestureListener(context: Context) :
        GestureDetector.SimpleOnGestureListener() {

        private var screenWidth = windowManager.defaultDisplay.width
        private var screenHeight = windowManager.defaultDisplay.height
        private val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        private var maxVolume = 0

        init {
            maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        }

        override fun onDown(e: MotionEvent): Boolean {
            webFragment.hideVolumeNow()
            settingActive()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            return handleTapCount(1)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val currentTime = System.currentTimeMillis()
            val timeSinceLastTap = currentTime - lastMenuPressTime
            if (timeSinceLastTap <= MENU_TAP_INTERVAL) {
                menuPressCount += 2
            } else {
                menuPressCount = 2
            }
            lastMenuPressTime = currentTime
            handler.removeCallbacks(handleTapRunnable)
            handler.postDelayed(handleTapRunnable, MENU_TAP_INTERVAL)
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val oldX = e1?.rawX ?: 0f
            val oldY = e1?.rawY ?: 0f
            val newX = e2.rawX
            val newY = e2.rawY
            if (oldX > screenWidth / 3 && oldX < screenWidth * 2 / 3 && abs(newX - oldX) < abs(newY - oldY)) {
                if (velocityY > 0) {
                    if ((!menuFragment.isAdded || menuFragment.isHidden) && (!settingFragment.isAdded || settingFragment.isHidden)) {
                        prev()
                    }
                }
                if (velocityY < 0) {
                    if ((!menuFragment.isAdded || menuFragment.isHidden) && (!settingFragment.isAdded || settingFragment.isHidden)) {
                        next()
                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        private var lastScrollTime: Long = 0
        private var decayFactor: Float = 1.0f

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val oldX = e1?.rawX ?: 0f
            val oldY = e1?.rawY ?: 0f

            if (oldX < screenWidth / 3) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastScrollTime
                lastScrollTime = currentTime
                decayFactor = 0.01f.coerceAtLeast(decayFactor - 0.03f * deltaTime)
                val delta = ((oldY - e2.rawY) * decayFactor * 0.2 / screenHeight).toFloat()
                adjustBrightness(delta)
                decayFactor = 1.0f
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            if (oldX > screenWidth * 2 / 3 && abs(distanceY) > abs(distanceX)) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = currentTime - lastScrollTime
                lastScrollTime = currentTime
                decayFactor = 0.01f.coerceAtLeast(decayFactor - 0.03f * deltaTime)
                val delta = ((oldY - e2.rawY) * maxVolume * decayFactor * 0.2 / screenHeight).toInt()
                adjustVolume(delta)
                decayFactor = 1.0f
                return super.onScroll(e1, e2, distanceX, distanceY)
            }

            return super.onScroll(e1, e2, distanceX, distanceY)
        }

        private fun adjustVolume(deltaVolume: Int) {
            val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            var newVolume = currentVolume + deltaVolume
            if (newVolume < 0) newVolume = 0
            else if (newVolume > maxVolume) newVolume = maxVolume
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
            webFragment.setVolumeMax(maxVolume * 100)
            webFragment.setVolume(newVolume * 100, true)
            webFragment.showVolume(View.VISIBLE)
        }

        private fun adjustBrightness(deltaBrightness: Float) {
            var brightness = window.attributes.screenBrightness
            brightness += deltaBrightness
            brightness = 0.1f.coerceAtLeast(0.9f.coerceAtMost(brightness))
            window.attributes = window.attributes.apply { screenBrightness = brightness }
            webFragment.setVolumeMax(100)
            webFragment.setVolume((brightness * 100).toInt())
            webFragment.showVolume(View.VISIBLE)
        }
    }

    fun showFragment(fragment: Fragment) {
        if (!isSafeToPerformFragmentTransactions) return
        if (!fragment.isAdded) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, fragment)
                .commitAllowingStateLoss()
            return
        }
        if (!fragment.isHidden) return
        supportFragmentManager.beginTransaction()
            .show(fragment)
            .commitAllowingStateLoss()
    }

    private fun hideFragment(fragment: Fragment) {
        if (!isSafeToPerformFragmentTransactions) return
        if (!fragment.isAdded || fragment.isHidden) return
        supportFragmentManager.beginTransaction()
            .hide(fragment)
            .commitAllowingStateLoss()
    }

    fun menuActive() {
        handler.removeCallbacks(hideMenu)
        handler.postDelayed(hideMenu, delayHideMenu)
    }

    private val hideMenu = Runnable {
        if (!isFinishing && !supportFragmentManager.isStateSaved) {
            if (!menuFragment.isHidden) {
                supportFragmentManager.beginTransaction().hide(menuFragment).commit()
            }
        }
    }

    fun settingActive() {
        handler.removeCallbacks(hideSetting)
        handler.postDelayed(hideSetting, delayHideSetting)
    }

    private val hideSetting = Runnable {
        if (!isFinishing && !isDestroyed && !supportFragmentManager.isStateSaved) {
            try {
                if (!settingFragment.isHidden) {
                    supportFragmentManager.beginTransaction().hide(settingFragment).commit()
                }
                addTimeFragment()
            } catch (e: IllegalStateException) {
                Log.e(TAG, "Failed to hide SettingFragment: ${e.message}", e)
            }
        }
    }

    fun addTimeFragment() {
        if (SP.time) showFragment(timeFragment) else hideFragment(timeFragment)
    }

    private fun showChannel(channel: String) {
        if (!menuFragment.isHidden) return
        if (settingFragment.isVisible) return
        channelFragment.show(channel)
    }

    private fun channelUp() {
        if (menuFragment.isHidden && settingFragment.isHidden) {
            if (SP.channelReversal) next() else prev()
        }
    }

    private fun channelDown() {
        if (menuFragment.isHidden && settingFragment.isHidden) {
            if (SP.channelReversal) prev() else next()
        }
    }

    private fun showSetting() {
        if (menuFragment.isAdded && !menuFragment.isHidden) return
        showFragment(settingFragment)
        settingActive()
    }

    fun hideMenuFragment() {
        supportFragmentManager.beginTransaction().hide(menuFragment).commit()
    }

    private fun hideSettingFragment() {
        supportFragmentManager.beginTransaction().hide(settingFragment).commit()
    }

    private fun showErrorFragment(msg: String) {
        errorFragment.show(msg)
        if (!errorFragment.isHidden) return
        supportFragmentManager.beginTransaction().show(errorFragment).commitNow()
    }

    private fun hideErrorFragment() {
        errorFragment.show("hide")
        if (errorFragment.isHidden) return
        supportFragmentManager.beginTransaction().hide(errorFragment).commitNow()
    }

    private fun handleTapCount(tapCount: Int): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastTap = currentTime - lastMenuPressTime
        if (timeSinceLastTap <= MENU_TAP_INTERVAL) {
            menuPressCount += tapCount
        } else {
            menuPressCount = tapCount
        }
        lastMenuPressTime = currentTime
        handler.removeCallbacks(handleTapRunnable)
        handler.postDelayed(handleTapRunnable, MENU_TAP_INTERVAL)
        return true
    }

    private fun handleSettingsKeyPress(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastMenuPressTime <= MENU_PRESS_INTERVAL) {
            menuPressCount++
            if (menuPressCount >= REQUIRED_MENU_PRESSES) {
                showSetting()
                menuPressCount = 0
                return true
            }
        } else {
            menuPressCount = 1
        }
        lastMenuPressTime = currentTime
        return true
    }

    fun onKey(keyCode: Int): Boolean {
        when (keyCode) {
            KEYCODE_ESCAPE, KEYCODE_BACK -> {
                if (menuFragment.isAdded && !menuFragment.isHidden) {
                    menuActive()
                    hideMenuFragment()
                    return true
                }
                if (settingFragment.isAdded && !settingFragment.isHidden) {
                    hideSettingFragment()
                    addTimeFragment()
                    settingActive()
                    return true
                }
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < BACK_PRESS_INTERVAL) {
                    finishAffinity()
                    return true
                }
                lastBackPressTime = currentTime
                R.string.press_back_exit.showToast()
                return true
            }
            KEYCODE_0, KEYCODE_1, KEYCODE_2, KEYCODE_3, KEYCODE_4,
            KEYCODE_5, KEYCODE_6, KEYCODE_7, KEYCODE_8, KEYCODE_9 -> {
                showChannel((keyCode - 7).toString())
                return true
            }
            KEYCODE_BOOKMARK, KEYCODE_UNKNOWN, KEYCODE_HELP,
            KEYCODE_SETTINGS, KEYCODE_MENU -> {
                settingActive()
                return handleSettingsKeyPress()
            }
            KEYCODE_DPAD_UP, KEYCODE_CHANNEL_UP -> {
                if (menuFragment.isAdded && !menuFragment.isHidden) {
                    menuActive()
                    return false
                }
                if (settingFragment.isAdded && !settingFragment.isHidden) {
                    settingActive()
                    return false
                }
                channelUp()
                return true
            }
            KEYCODE_DPAD_DOWN, KEYCODE_CHANNEL_DOWN -> {
                if (menuFragment.isAdded && !menuFragment.isHidden) {
                    menuActive()
                    return false
                }
                if (settingFragment.isAdded && !settingFragment.isHidden) {
                    settingActive()
                    return false
                }
                channelDown()
                return true
            }
            KEYCODE_ENTER, KEYCODE_DPAD_CENTER -> {
                val currentTime = System.currentTimeMillis()
                val timeSinceLastPress = currentTime - lastMenuPressTime
                if (timeSinceLastPress <= 400) {
                    menuPressCount++
                    if (menuPressCount >= 4) {
                        showSetting()
                        menuPressCount = 0
                        handler.removeCallbacks(handleEnterRunnable)
                        return true
                    }
                } else {
                    menuPressCount = 1
                }
                lastMenuPressTime = currentTime
                handler.removeCallbacks(handleEnterRunnable)
                handler.postDelayed(handleEnterRunnable, 600)
                return true
            }
            KEYCODE_DPAD_LEFT -> {
                if (menuFragment.isAdded && !menuFragment.isHidden ||
                    settingFragment.isAdded && !settingFragment.isHidden) {
                    settingActive()
                    menuActive()
                    return false
                }
                return false
            }
            KEYCODE_DPAD_RIGHT -> {
                if (menuFragment.isAdded && !menuFragment.isHidden ||
                    settingFragment.isAdded && !settingFragment.isHidden) {
                    settingActive()
                    menuActive()
                    return false
                }
                val currentTime = System.currentTimeMillis()
                val timeSinceLastPress = currentTime - lastMenuPressTime
                if (timeSinceLastPress <= 400) {
                    menuPressCount++
                    if (menuPressCount >= 4) {
                        showSetting()
                        menuPressCount = 0
                        handler.removeCallbacks(handleRightRunnable)
                        return true
                    }
                } else {
                    menuPressCount = 1
                }
                lastMenuPressTime = currentTime
                handler.removeCallbacks(handleRightRunnable)
                handler.postDelayed(handleRightRunnable, 600)
                return true
            }
        }
        return false
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (onKey(keyCode)) return true
        return false
    }

    override fun onResume() {
        super.onResume()
        isSafeToPerformFragmentTransactions = true
        addTimeFragment()
    }

    fun handleWebviewTypeSwitch(enable: Boolean) {
        if (enable) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSwitchTime < DEBOUNCE_INTERVAL) return
        lastSwitchTime = currentTime

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                if (webFragment.isAdded) {
                    supportFragmentManager.beginTransaction()
                        .remove(webFragment)
                        .commitNowAllowingStateLoss()
                    webFragment = WebFragment()
                }
                supportFragmentManager.fragments.forEach { fragment ->
                    if (fragment.isAdded && !fragment.isHidden) {
                        supportFragmentManager.beginTransaction()
                            .hide(fragment)
                            .commitNowAllowingStateLoss()
                    }
                }
                com.Twotwo.TwotwoTV.SP.enableWebviewType = false
                delay(500)
                val intent = Intent(this@MainActivity, com.Twotwo.TwotwoTV.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                startActivity(intent)
                finish()
            } catch (e: Exception) {
                Log.e(TAG, "Error switching to IPTV: ${e.message}", e)
                R.string.switch_iptv_failed.showToast()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        isSafeToPerformFragmentTransactions = false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        server?.stop()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
