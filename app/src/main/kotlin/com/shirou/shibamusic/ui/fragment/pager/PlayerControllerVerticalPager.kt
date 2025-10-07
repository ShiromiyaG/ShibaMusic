package com.shirou.shibamusic.ui.fragment.pager

import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter

import com.shirou.shibamusic.ui.fragment.PlayerControllerFragment
import com.shirou.shibamusic.ui.fragment.PlayerQueueFragment
import kotlin.OptIn

@OptIn(UnstableApi::class)
class PlayerControllerVerticalPager(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val maps: MutableMap<Int, Fragment> = mutableMapOf()

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlayerControllerFragment().also { maps[position] = it }
            1 -> PlayerQueueFragment().also { maps[position] = it }
            else -> PlayerControllerFragment().also { maps[position] = it }
        }
    }

    override fun getItemCount(): Int = 2

    fun getRegisteredFragment(position: Int): Fragment? {
        return maps[position]
    }
}
