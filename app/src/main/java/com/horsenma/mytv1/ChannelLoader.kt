package com.horsenma.mytv1

import android.content.Context
import android.util.Log
import com.Twotwo.TwotwoTV.R
import com.Twotwo.TwotwoTV.SourceDecoder
import com.horsenma.mytv1.data.Global.gson
import com.horsenma.mytv1.data.Global.typeTvList
import com.horsenma.mytv1.data.TV
import com.horsenma.mytv1.models.TVModel
import java.io.File

/**
 * 独立的频道加载器，替代 TVList 处理 WebView 模式的频道加载。
 * 所有方法均为纯数据操作，不涉及 LiveData，可安全在任意线程调用。
 */
object ChannelLoader {
    private const val TAG = "ChannelLoader"
    private const val CACHE_FILE_NAME = "web_channels.txt"

    private var channels: List<TV> = emptyList()

    /**
     * 读取并解析频道数据（IO 线程安全，不涉及 LiveData）
     */
    fun load(context: Context): List<TV> {
        val appDir = context.filesDir
        val file = File(appDir, CACHE_FILE_NAME)
        val str = if (file.exists()) {
            Log.i(TAG, "read $file")
            file.readText()
        } else {
            Log.i(TAG, "read resource")
            context.resources.openRawResource(R.raw.web_channels).bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }

        if (str.isEmpty()) {
            Log.e(TAG, "Input string is empty")
            channels = emptyList()
            return channels
        }

        try {
            var string = str
            val isPlainText = str.trim().startsWith("#EXTM3U") ||
                    str.trim().startsWith("http://") ||
                    str.trim().startsWith("https://")
            val isHex = str.trim().matches(Regex("^[0-9a-fA-F]+$"))

            if (isHex) {
                string = SourceDecoder.decodeHexSource(str) ?: str
                Log.i(TAG, "Decoded HEX, length=${string.length}")
            } else if (!isPlainText) {
                try {
                    string = SourceDecoder.decodeHexSource(str) ?: str
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode: ${e.message}")
                }
            }

            if (string.getOrNull(0) != '[') {
                Log.e(TAG, "Invalid format, not starting with '['")
                channels = emptyList()
                return channels
            }

            channels = gson.fromJson(string, typeTvList) ?: emptyList()
            Log.i(TAG, "Loaded ${channels.size} channels")
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            channels = emptyList()
        }

        return channels
    }

    /**
     * 从远端 URL 加载频道数据（IO 线程安全）
     */
    fun loadFromUrl(context: Context, url: String): List<TV> {
        try {
            val client = okhttp3.OkHttpClient()
            val request = okhttp3.Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.let { body ->
                    val str = body.string()
                    // 缓存到文件
                    val file = File(context.filesDir, CACHE_FILE_NAME)
                    file.writeText(str)
                    // 重新加载
                    return load(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadFromUrl error: ${e.message}")
        }
        return emptyList()
    }

    fun getChannel(index: Int): TV? {
        return channels.getOrNull(index)
    }

    fun getChannelCount(): Int = channels.size

    /**
     * 创建 TVModel（必须在主线程调用，因为 TVModel 内部使用 LiveData）
     */
    fun createTVModel(tv: TV, index: Int): TVModel {
        tv.id = index
        return TVModel(tv)
    }

    /**
     * 获取所有频道的分组列表
     */
    fun getGroups(): List<String> {
        return channels.map { it.group }.distinct()
    }

    /**
     * 获取指定分组的频道
     */
    fun getChannelsByGroup(group: String): List<Pair<Int, TV>> {
        return channels.mapIndexedNotNull { index, tv ->
            if (tv.group == group) Pair(index, tv) else null
        }
    }
}
