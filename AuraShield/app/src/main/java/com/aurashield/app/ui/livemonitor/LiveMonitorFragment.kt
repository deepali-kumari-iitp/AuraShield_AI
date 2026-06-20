package com.aurashield.app.ui.livemonitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aurashield.app.databinding.FragmentLiveMonitorBinding
import com.aurashield.app.repository.VoiceGuardRepository

class LiveMonitorFragment : Fragment() {

    private var _binding: FragmentLiveMonitorBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLiveMonitorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val stats = VoiceGuardRepository.mockInferenceStats()
        binding.latencyValue.text = "${stats.latencyMs} ms"
        binding.bufferValue.text = "${stats.bufferWindowMs} ms"
        binding.footprintValue.text = stats.modelFootprintLabel
    }

    override fun onResume() {
        super.onResume()
        binding.waveformView.start()
    }

    override fun onPause() {
        super.onPause()
        binding.waveformView.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
