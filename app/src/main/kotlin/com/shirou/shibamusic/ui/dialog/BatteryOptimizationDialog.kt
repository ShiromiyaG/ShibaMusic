package com.shirou.shibamusic.ui.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.fragment.app.DialogFragment
import androidx.media3.common.util.UnstableApi
import com.shirou.shibamusic.R
import com.shirou.shibamusic.databinding.DialogBatteryOptimizationBinding
import com.shirou.shibamusic.util.Preferences
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.OptIn

@OptIn(UnstableApi::class)
class BatteryOptimizationDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bind = DialogBatteryOptimizationBinding.inflate(layoutInflater)

        return MaterialAlertDialogBuilder(requireContext())
            .setView(bind.root)
            .setTitle(R.string.activity_battery_optimizations_title)
            .setPositiveButton(R.string.battery_optimization_positive_button) { _, _ -> openPowerSettings() }
            .setNeutralButton(R.string.battery_optimization_neutral_button) { _, _ -> Preferences.dontAskForOptimization() }
            .setNegativeButton(R.string.battery_optimization_negative_button, null)
            .create()
    }

    private fun openPowerSettings() {
        val intent = Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
        }
        startActivity(intent)
    }
}
