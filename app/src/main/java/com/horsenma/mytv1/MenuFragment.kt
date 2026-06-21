package com.horsenma.mytv1

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.Twotwo.TwotwoTV.databinding.MenuBinding
import com.horsenma.mytv1.models.TVListModel
import com.horsenma.mytv1.models.TVModel
import com.horsenma.mytv1.models.TVGroupModel
import com.Twotwo.TwotwoTV.TwotwoTVApplication
import com.Twotwo.TwotwoTV.R
import androidx.recyclerview.widget.RecyclerView

class MenuFragment : Fragment(), GroupAdapter.ItemListener, ListAdapter.ItemListener {
    private var _binding: MenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter
    private lateinit var listAdapter: ListAdapter

    private var groupWidth = 0
    private var listWidth = 0

    private var groupModel = TVGroupModel()
    private var currentGroupIndex = 0

    /**
     * 从 ChannelLoader 数据构建 groupModel
     */
    fun buildFromChannelLoader() {
        groupModel = TVGroupModel()
        val groups = ChannelLoader.getGroups()
        groups.forEachIndexed { index, groupName ->
            val tvListModel = TVListModel(groupName, index)
            val channels = ChannelLoader.getChannelsByGroup(groupName)
            channels.forEach { (channelIndex, tv) ->
                val tvModel = ChannelLoader.createTVModel(tv, channelIndex)
                tvListModel.addTVModel(tvModel)
            }
            groupModel.addTVListModel(tvListModel)
        }
        currentGroupIndex = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext()
        val application = context.applicationContext as TwotwoTVApplication
        _binding = MenuBinding.inflate(inflater, container, false)

        binding.root.isFocusable = true
        binding.root.isFocusableInTouchMode = true

        buildFromChannelLoader()

        groupAdapter = GroupAdapter(context, binding.group, groupModel)
        binding.group.adapter = groupAdapter
        binding.group.layoutManager = LinearLayoutManager(context)
        groupWidth = application.px2Px(binding.group.layoutParams.width)
        binding.group.layoutParams.width = if (SP.compactMenu) groupWidth * 2 / 3 else groupWidth
        groupAdapter.setItemListener(this)
        binding.group.isFocusable = true
        binding.group.isFocusableInTouchMode = true

        val tvListModel = groupModel.getTVListModel(currentGroupIndex)
            ?: groupModel.getTVListModel(0)
            ?: TVListModel("", 0)

        listAdapter = ListAdapter(context, binding.list, tvListModel)
        binding.list.adapter = listAdapter
        binding.list.layoutManager = LinearLayoutManager(context)
        listWidth = application.px2Px(binding.list.layoutParams.width)
        binding.list.layoutParams.width = if (SP.compactMenu) listWidth * 4 / 5 else listWidth
        listAdapter.focusable(false)
        listAdapter.setItemListener(this)
        binding.list.isFocusable = true
        binding.list.isFocusableInTouchMode = true

        binding.menu.setOnClickListener { hideSelf() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val scrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING || newState == RecyclerView.SCROLL_STATE_IDLE) {
                    (activity as? MainActivity)?.menuActive()
                }
            }
        }
        binding.group.addOnScrollListener(scrollListener)
        binding.list.addOnScrollListener(scrollListener)

        view.post {
            binding.list.isFocusable = true
            binding.list.isFocusableInTouchMode = true
            if (listAdapter.itemCount > 0) {
                listAdapter.focusable(true)
                groupAdapter.focusable(false)
                binding.list.requestFocus()
                listAdapter.toPosition(0)
            } else {
                binding.group.isFocusable = true
                binding.group.isFocusableInTouchMode = true
                groupAdapter.focusable(true)
                listAdapter.focusable(false)
                binding.group.requestFocus()
                groupAdapter.toPosition(0)
            }
        }
    }

    override fun onKey(keyCode: Int): Boolean {
        (activity as? MainActivity)?.menuActive()
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (binding.group.findFocus() != null) {
                    if (listAdapter.itemCount == 0) {
                        Toast.makeText(context, getString(R.string.no_channels), Toast.LENGTH_LONG).show()
                        return true
                    }
                    groupAdapter.focusable(false)
                    listAdapter.focusable(true)
                    binding.list.isFocusable = true
                    binding.list.isFocusableInTouchMode = true
                    binding.list.requestFocus()
                    listAdapter.toPosition(0)
                    return true
                }
                return false
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> return true
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                hideSelf()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                hideSelf()
                return true
            }
        }
        return false
    }

    fun update() {
        buildFromChannelLoader()
        groupAdapter.update(groupModel)
        val tvListModel = groupModel.getTVListModel(currentGroupIndex)
            ?: groupModel.getTVListModel(0)
        if (tvListModel != null) {
            (binding.list.adapter as ListAdapter).update(tvListModel)
        }
    }

    fun updateList(position: Int) {
        currentGroupIndex = position
        val tvListModel = groupModel.getTVListModel(position)
        if (tvListModel != null) {
            (binding.list.adapter as ListAdapter).update(tvListModel)
        }
    }

    private fun hideSelf() {
        requireActivity().supportFragmentManager.beginTransaction()
            .hide(this)
            .commit()
    }

    override fun onItemFocusChange(tvListModel: TVListModel, hasFocus: Boolean) {
        if (hasFocus) {
            (binding.list.adapter as ListAdapter).update(tvListModel)
            (activity as MainActivity).menuActive()
        }
    }

    override fun onItemClicked(position: Int) {
        listAdapter.clear()
        groupAdapter.focusable(true)
        listAdapter.focusable(false)
        groupAdapter.toPosition(position)
        currentGroupIndex = position
        (activity as? MainActivity)?.menuActive()
    }

    override fun onItemFocusChange(tvModel: TVModel, hasFocus: Boolean) {
        if (hasFocus) {
            (activity as MainActivity).menuActive()
        }
    }

    override fun onItemClicked(tvModel: TVModel) {
        // 通过 MainActivity.play() 播放频道
        (activity as? MainActivity)?.play(tvModel.tv.id)
        (activity as? MainActivity)?.menuActive()
        (activity as? MainActivity)?.hideMenuFragment()
    }

    override fun onKey(listAdapter: ListAdapter, keyCode: Int): Boolean {
        (activity as? MainActivity)?.menuActive()
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                binding.group.visibility = VISIBLE
                groupAdapter.focusable(true)
                listAdapter.focusable(false)
                listAdapter.clear()
                groupAdapter.toPosition(currentGroupIndex)
                return true
            }
        }
        return false
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            // 刷新频道数据
            buildFromChannelLoader()
            groupAdapter.update(groupModel)
            val tvListModel = groupModel.getTVListModel(currentGroupIndex)
                ?: groupModel.getTVListModel(0)
            if (tvListModel != null) {
                (binding.list.adapter as ListAdapter).update(tvListModel)
            }

            view?.post {
                binding.list.isFocusable = true
                binding.list.isFocusableInTouchMode = true
                if (listAdapter.itemCount > 0) {
                    listAdapter.focusable(true)
                    groupAdapter.focusable(false)
                    binding.list.requestFocus()
                    listAdapter.toPosition(0)
                } else {
                    binding.group.isFocusable = true
                    binding.group.isFocusableInTouchMode = true
                    groupAdapter.focusable(true)
                    listAdapter.focusable(false)
                    binding.group.requestFocus()
                    groupAdapter.toPosition(0)
                }
                (activity as MainActivity).menuActive()
            }
        } else {
            view?.post {
                groupAdapter.visiable = false
                listAdapter.visiable = false
            }
        }
    }

    fun updateSize() {
        view?.post {
            binding.group.layoutParams.width = if (SP.compactMenu) groupWidth * 4 / 5 else groupWidth
            binding.list.layoutParams.width = if (SP.compactMenu) listWidth * 4 / 5 else listWidth
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MenuFragment"
    }
}
