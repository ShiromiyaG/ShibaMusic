package com.shirou.shibamusic.ui.fragment.pager

import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.adapter.FragmentStateAdapter

@OptIn(UnstableApi::class)
class HomePager(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments: MutableList<Fragment> = mutableListOf()
    private val titles: MutableList<String> = mutableListOf()
    private val icons: MutableList<Int> = mutableListOf()

    override fun createFragment(position: Int): Fragment = fragments[position]

    override fun getItemCount(): Int = fragments.size

    fun addFragment(fragment: Fragment, title: String, drawable: Int) {
        fragments.add(fragment)
        titles.add(title)
        icons.add(drawable)
    }

    fun getPageTitle(position: Int): String = titles[position]

    fun getPageIcon(position: Int): Int = icons[position]
}
