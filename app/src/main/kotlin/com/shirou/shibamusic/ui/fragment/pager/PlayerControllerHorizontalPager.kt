package com.shirou.shibamusic.ui.fragment.pager

import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.shirou.shibamusic.ui.fragment.PlayerCoverFragment
import com.shirou.shibamusic.ui.fragment.PlayerLyricsFragment
import kotlin.OptIn

@OptIn(UnstableApi::class)
class PlayerControllerHorizontalPager(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PlayerCoverFragment()
            1 -> PlayerLyricsFragment()
            else -> PlayerCoverFragment()
        }
    }

    override fun getItemCount(): Int = 2

    companion object {
        private const val TAG = "PlayerControllerHorizontalPager"
    }
}
