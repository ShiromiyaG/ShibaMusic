package com.shirou.shibamusic.viewmodel

import android.app.Application
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map

import com.shirou.shibamusic.model.DownloadStack
import com.shirou.shibamusic.repository.DownloadRepository
import com.shirou.shibamusic.subsonic.models.Child

import com.shirou.shibamusic.util.Preferences

import java.util.ArrayList

class DownloadViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "DownloadViewModel"
    }

    private val downloadRepository: DownloadRepository = DownloadRepository()

    private val _downloadedTrackSample = MutableLiveData<List<Child>?>(null)
    private val _viewStack = MutableLiveData<ArrayList<DownloadStack>?>(null)

    init {
        initViewStack(DownloadStack(Preferences.getDefaultDownloadViewType(), null))
    }

    val viewStack: LiveData<ArrayList<DownloadStack>> = _viewStack.map { it!! }

    fun getDownloadedTracks(owner: LifecycleOwner): LiveData<List<Child>> {
        downloadRepository.getLiveDownload().observe(owner) { downloads ->
            val children = downloads.map { it as Child }
            _downloadedTrackSample.postValue(children)
        }
        return _downloadedTrackSample.map { it!! }
    }

    fun initViewStack(level: DownloadStack) {
        val stack = ArrayList<DownloadStack>().apply {
            add(level)
        }
        _viewStack.value = stack
    }

    fun pushViewStack(level: DownloadStack) {
        // As initViewStack is called in the constructor, _viewStack.value is guaranteed to be non-null.
        val currentStack = _viewStack.value!!
        currentStack.add(level)
        _viewStack.value = currentStack // Reassign to notify observers of list change
    }

    fun popViewStack() {
        // As initViewStack is called in the constructor, _viewStack.value is guaranteed to be non-null.
        val currentStack = _viewStack.value!!
        if (currentStack.isNotEmpty()) {
            currentStack.removeAt(currentStack.size - 1)
            _viewStack.value = currentStack // Reassign to notify observers
        } else {
            Log.w(TAG, "viewStack is empty, cannot pop element.")
        }
    }
}
