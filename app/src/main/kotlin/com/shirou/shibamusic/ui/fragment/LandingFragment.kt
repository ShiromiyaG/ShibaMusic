package com.shirou.shibamusic.ui.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.shirou.shibamusic.R

class LandingFragment : Fragment() {

    companion object {
        private const val TAG = "LandingFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "LandingFragment onCreateView")
        return inflater.inflate(R.layout.fragment_landing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "LandingFragment onViewCreated")

        val progressBar: ProgressBar? = view.findViewById(R.id.landing_progress_bar)
        val textView: TextView? = view.findViewById(R.id.landing_text)

        // Garantir que o fragmento seja visível mesmo se houver problemas de navegação
        progressBar?.visibility = View.VISIBLE

        // Mostrar nome do app após um pequeno delay para debugging
        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                textView?.visibility = View.VISIBLE
                Log.d(TAG, "LandingFragment text made visible")
            }
        }, 1000)
    }
}
