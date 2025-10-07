package com.shirou.shibamusic.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.shirou.shibamusic.repository.SharingRepository
import com.shirou.shibamusic.subsonic.models.Share

class ShareBottomSheetViewModel(application: Application) : AndroidViewModel(application) {
    private val sharingRepository: SharingRepository = SharingRepository()

    var share: Share? = null

    fun updateShare(description: String, expires: Long) {
        val shareId = share?.id ?: return
        sharingRepository.updateShare(shareId, description, expires)
    }

    fun deleteShare() {
        val shareId = share?.id ?: return
        sharingRepository.deleteShare(shareId)
    }
}
