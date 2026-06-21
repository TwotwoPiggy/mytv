package com.Twotwo.TwotwoTV

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.Twotwo.TwotwoTV.databinding.FragmentSourceSelectBinding

/**
 * 源选择侧边栏 - 类似频道列表的TV风格
 * 显示在屏幕右侧，支持遥控器上下导航和确认选择
 */
class SourceSelectFragment : Fragment() {

    private var _binding: FragmentSourceSelectBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val hideDelay = 8000L // 8秒后自动隐藏
    private var hideRunnable: Runnable? = null

    private lateinit var sourceAdapter: SimpleSourceAdapter
    private var sources: List<SourceItem> = emptyList()
    private var currentSourceIndex: Int = 0

    // 回调：当用户选择了一个源
    var onSourceSelected: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSourceSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置 RecyclerView
        binding.sourceList.layoutManager = LinearLayoutManager(context)
        sourceAdapter = SimpleSourceAdapter { position ->
            // 点击选择源
            selectSource(position)
        }
        binding.sourceList.adapter = sourceAdapter

        // 设置按键监听
        view.setOnKeyListener { _, keyCode, event ->
            if (event?.action == KeyEvent.ACTION_DOWN) {
                handleKeyPress(keyCode)
            } else false
        }

        // 点击外部区域关闭
        binding.sourceSelectRoot.setOnClickListener {
            hideSelf()
        }
    }

    /**
     * 显示源列表
     */
    fun show(channelName: String, sourceUrls: List<String>, currentIndex: Int) {
        currentSourceIndex = currentIndex

        // 构建源列表
        sources = sourceUrls.mapIndexed { index, url ->
            SourceItem(
                index = index,
                url = url,
                displayName = "线路 ${index + 1}",
                isCurrent = index == currentIndex
            )
        }

        // 更新UI
        binding.channelName.text = channelName
        binding.sourceInfo.text = "当前: 线路 ${currentIndex + 1}/${sources.size}"

        // 更新适配器
        sourceAdapter.submitList(sources, currentIndex)

        // 显示并聚焦
        view?.visibility = View.VISIBLE
        view?.requestFocus()

        // 滚动到当前源位置
        if (currentIndex >= 0 && currentIndex < sources.size) {
            binding.sourceList.scrollToPosition(currentIndex)
        }

        // 启动自动隐藏计时器
        startAutoHideTimer()

        Log.d(TAG, "Showing source list: channel=$channelName, sources=${sources.size}, current=$currentIndex")
    }

    /**
     * 处理按键事件
     */
    private fun handleKeyPress(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                hideSelf()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                resetAutoHideTimer()
                val newPos = (sourceAdapter.currentFocusedPosition - 1).coerceAtLeast(0)
                sourceAdapter.setFocusPosition(newPos)
                binding.sourceList.smoothScrollToPosition(newPos)
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                resetAutoHideTimer()
                val newPos = (sourceAdapter.currentFocusedPosition + 1).coerceAtMost(sources.size - 1)
                sourceAdapter.setFocusPosition(newPos)
                binding.sourceList.smoothScrollToPosition(newPos)
                true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                resetAutoHideTimer()
                selectSource(sourceAdapter.currentFocusedPosition)
                true
            }
            else -> false
        }
    }

    /**
     * 选择源
     */
    private fun selectSource(position: Int) {
        if (position in sources.indices) {
            Log.d(TAG, "Source selected: position=$position, url=${sources[position].url}")
            onSourceSelected?.invoke(position)
            hideSelf()
        }
    }

    /**
     * 隐藏自身
     */
    fun hideSelf() {
        handler.removeCallbacksAndMessages(null)
        view?.visibility = View.GONE
        _binding?.sourceList?.adapter = null
    }

    /**
     * 启动自动隐藏计时器
     */
    private fun startAutoHideTimer() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        hideRunnable = Runnable { hideSelf() }
        handler.postDelayed(hideRunnable!!, hideDelay)
    }

    /**
     * 重置自动隐藏计时器
     */
    private fun resetAutoHideTimer() {
        hideRunnable?.let { handler.removeCallbacks(it) }
        startAutoHideTimer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }

    /**
     * 源数据类
     */
    data class SourceItem(
        val index: Int,
        val url: String,
        val displayName: String,
        val isCurrent: Boolean
    )

    /**
     * 简单的源列表适配器
     */
    inner class SimpleSourceAdapter(
        private val onItemClick: (Int) -> Unit
    ) : RecyclerView.Adapter<SimpleSourceAdapter.ViewHolder>() {

        private var items: List<SourceItem> = emptyList()
        private var focusedPosition: Int = 0
        private var currentSourceIndex: Int = 0

        val currentFocusedPosition: Int get() = focusedPosition

        fun submitList(newItems: List<SourceItem>, currentIdx: Int) {
            items = newItems
            currentSourceIndex = currentIdx
            focusedPosition = currentIdx.coerceIn(0, newItems.size - 1)
            notifyDataSetChanged()
        }

        fun setFocusPosition(position: Int) {
            val oldPos = focusedPosition
            focusedPosition = position.coerceIn(0, items.size - 1)
            if (oldPos != focusedPosition) {
                notifyItemChanged(oldPos)
                notifyItemChanged(focusedPosition)
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_source_simple, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item, position == focusedPosition, position == currentSourceIndex)

            // 设置点击事件
            holder.itemView.setOnClickListener {
                onItemClick(position)
            }

            // 设置焦点变化监听
            holder.itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    focusedPosition = position
                    notifyDataSetChanged()
                }
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val indexText: TextView = itemView.findViewById(R.id.source_index)
            private val resolutionText: TextView = itemView.findViewById(R.id.source_resolution)
            private val pingText: TextView = itemView.findViewById(R.id.source_ping)
            private val currentMarker: TextView = itemView.findViewById(R.id.current_marker)

            fun bind(item: SourceItem, isFocused: Boolean, isCurrent: Boolean) {
                indexText.text = "${item.index + 1}"
                resolutionText.text = item.displayName
                pingText.text = item.url.takeLast(30) // 显示URL末尾作为标识

                // 当前源标记
                currentMarker.visibility = if (isCurrent) View.VISIBLE else View.GONE

                // 焦点样式
                if (isFocused) {
                    itemView.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.focus))
                    resolutionText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                } else {
                    itemView.setBackgroundColor(0x00000000)
                    resolutionText.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.white))
                }
            }
        }
    }

    companion object {
        private const val TAG = "SourceSelectFragment"
    }
}
