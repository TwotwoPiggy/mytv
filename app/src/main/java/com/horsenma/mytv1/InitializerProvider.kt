package com.horsenma.mytv1

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import android.util.Log

internal class InitializerProvider : ContentProvider() {

    // Happens before Application#onCreate. Only init SP here.
    // WebView 模式使用 ChannelLoader 在 Activity 中异步加载频道，不再需要提前初始化 TVList。
    override fun onCreate(): Boolean {
        Log.i("InitializerProvider", "Initializing SP")
        SP.init(context!!)
        Log.i("InitializerProvider", "Initialization complete")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ) = unsupported()

    override fun getType(uri: Uri) = unsupported()

    override fun insert(uri: Uri, values: ContentValues?) = unsupported()

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) =
        unsupported()

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = unsupported()

    private fun unsupported(errorMessage: String? = null): Nothing =
        throw UnsupportedOperationException(errorMessage)
}