package com.shirou.shibamusic.ui.fragment

import android.content.ComponentName
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import androidx.recyclerview.widget.LinearLayoutManager
import com.shirou.shibamusic.databinding.FragmentHomeTabRadioBinding
import com.shirou.shibamusic.interfaces.ClickCallback
import com.shirou.shibamusic.interfaces.RadioCallback
import com.shirou.shibamusic.subsonic.models.InternetRadioStation
import com.shirou.shibamusic.service.MediaManager
import com.shirou.shibamusic.service.MediaService
import com.shirou.shibamusic.ui.activity.MainActivity
import com.shirou.shibamusic.ui.adapter.InternetRadioStationAdapter
import com.shirou.shibamusic.ui.dialog.RadioEditorDialog
import com.shirou.shibamusic.util.Constants
import com.shirou.shibamusic.util.Preferences
import com.shirou.shibamusic.viewmodel.RadioViewModel
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class HomeTabRadioFragment : Fragment(), ClickCallback, RadioCallback {
    companion object {
        private const val TAG = "HomeTabRadioFragment"
    }

    private var bind: FragmentHomeTabRadioBinding? = null
    private lateinit var activity: MainActivity
    private var radioViewModel: RadioViewModel? = null

    private lateinit var internetRadioStationAdapter: InternetRadioStationAdapter

    private lateinit var mediaBrowserListenableFuture: ListenableFuture<MediaBrowser>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        activity = requireActivity() as MainActivity

        bind = FragmentHomeTabRadioBinding.inflate(inflater, container, false)
        radioViewModel = ViewModelProvider(requireActivity()).get(RadioViewModel::class.java)

        return bind!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()
        initRadioStationView()
    }

    override fun onStart() {
        super.onStart()
        initializeMediaBrowser()
    }

    override fun onStop() {
        releaseMediaBrowser()
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bind = null
    }

    private fun init() {
        bind!!.internetRadioStationPreTextView.setOnClickListener {
            val dialog = RadioEditorDialog(this)
            dialog.show(activity.supportFragmentManager, null)
        }

        bind!!.internetRadioStationTitleTextView.setOnLongClickListener {
            radioViewModel?.getInternetRadioStations(viewLifecycleOwner)
            true
        }

        bind!!.hideSectionButton.setOnClickListener {
            Preferences.setRadioSectionHidden()
        }
    }

    private fun initRadioStationView() {
        bind!!.internetRadioStationRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }

        internetRadioStationAdapter = InternetRadioStationAdapter(this)
        bind!!.internetRadioStationRecyclerView.adapter = internetRadioStationAdapter
        radioViewModel?.getInternetRadioStations(viewLifecycleOwner)?.observe(viewLifecycleOwner) { internetRadioStations ->
            bind?.homeRadioStationSector?.visibility = if (internetRadioStations.isNotEmpty()) View.VISIBLE else View.GONE
            bind?.emptyRadioStationLayout?.visibility = if (internetRadioStations.isEmpty()) View.VISIBLE else View.GONE

            internetRadioStationAdapter.setItems(internetRadioStations)
        }
    }

    private fun initializeMediaBrowser() {
        mediaBrowserListenableFuture = MediaBrowser.Builder(
            requireContext(),
            SessionToken(
                requireContext(),
                ComponentName(requireContext(), MediaService::class.java)
            )
        ).buildAsync()
    }

    private fun releaseMediaBrowser() {
        MediaBrowser.releaseFuture(mediaBrowserListenableFuture)
    }

    override fun onInternetRadioStationClick(bundle: Bundle) {
        val station = BundleCompat.getParcelable(bundle, Constants.INTERNET_RADIO_STATION_OBJECT, InternetRadioStation::class.java)
            ?: return
        MediaManager.startRadio(mediaBrowserListenableFuture, station)
        activity.setBottomSheetInPeek(true)
    }

    override fun onInternetRadioStationLongClick(bundle: Bundle) {
        val dialog = RadioEditorDialog(object : RadioCallback {
            override fun onDismiss() {
                radioViewModel?.refreshInternetRadioStations(viewLifecycleOwner)
            }
        })
        dialog.arguments = bundle
        dialog.show(activity.supportFragmentManager, null)
    }

    override fun onDismiss() {
        Handler(Looper.getMainLooper()).postDelayed({
            radioViewModel?.refreshInternetRadioStations(viewLifecycleOwner)
        }, 1000L)
    }
}
