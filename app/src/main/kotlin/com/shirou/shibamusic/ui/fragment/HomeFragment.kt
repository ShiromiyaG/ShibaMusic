package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.FragmentHomeBinding
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.fragment.pager.HomePager
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

@UnstableApi
class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var activity: MainActivity? = null

    private var materialToolbar: MaterialToolbar? = null
    private var appBarLayout: AppBarLayout? = null
    private var tabLayout: TabLayout? = null

    override fun onCreateView(
        @NonNull inflater: LayoutInflater,
        @Nullable container: ViewGroup?,
        @Nullable savedInstanceState: Bundle?
    ): View {
        activity = requireActivity() as? MainActivity
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAppBar()
        initHomePager()
    }

    override fun onStart() {
        super.onStart()

        activity?.setBottomNavigationBarVisibility(true)
        activity?.setBottomSheetVisibility(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initAppBar() {
        val root = binding.root
        appBarLayout = root.findViewById(R.id.toolbar_fragment)
        materialToolbar = root.findViewById(R.id.toolbar)

        activity?.setSupportActionBar(materialToolbar)
        
        // The Java code uses Objects.requireNonNull(materialToolbar.getOverflowIcon()).setTint(...)
        // This implies an expectation that both materialToolbar and its overflowIcon are non-null,
        // and would throw NullPointerException otherwise. The '!!' operator provides equivalent semantic.
        materialToolbar!!.overflowIcon!!.setTint(requireContext().getColor(R.color.titleTextColor))

        tabLayout = TabLayout(requireContext()).apply {
            tabGravity = TabLayout.GRAVITY_FILL
            tabMode = TabLayout.MODE_FIXED
        }

        appBarLayout?.addView(tabLayout)
    }

    private fun initHomePager() {
        val pager = HomePager(this)

        pager.addFragment(HomeTabMusicFragment(), getString(R.string.home_section_music), R.drawable.ic_home)

        if (Preferences.isPodcastSectionVisible())
            pager.addFragment(HomeTabPodcastFragment(), getString(R.string.home_section_podcast), R.drawable.ic_graphic_eq)

        if (Preferences.isRadioSectionVisible())
            pager.addFragment(HomeTabRadioFragment(), getString(R.string.home_section_radio), R.drawable.ic_play_for_work)

        binding.homeViewPager.apply {
            adapter = pager
            offscreenPageLimit = 3
            isUserInputEnabled = false
        }

        // tabLayout is initialized in initAppBar, which is called before initHomePager.
        // Thus, it is expected to be non-null at this point.
        TabLayoutMediator(tabLayout!!, binding.homeViewPager) { tab, position ->
            tab.text = pager.getPageTitle(position)
            // tab.setIcon(pager.getPageIcon(position)); // Original commented out
        }.attach()

    tabLayout?.visibility = if (Preferences.isPodcastSectionVisible() || Preferences.isRadioSectionVisible()) View.VISIBLE else View.GONE
    }
}
