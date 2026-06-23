package com.aurashield.ai.ui.livemonitor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aurashield.ai.databinding.FragmentLiveMonitorBinding
import com.aurashield.ai.repository.VoiceGuardRepository

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

        binding.callSimulationSwitch.isChecked = com.aurashield.ai.service.BackgroundMonitorService.isSimulatedCallActive
        binding.callSimulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            val intent = android.content.Intent(requireContext(), com.aurashield.ai.service.BackgroundMonitorService::class.java).apply {
                action = com.aurashield.ai.service.BackgroundMonitorService.ACTION_SIMULATE_CALL
                putExtra(com.aurashield.ai.service.BackgroundMonitorService.EXTRA_CALL_ACTIVE, isChecked)
            }
            requireContext().startService(intent)
            android.widget.Toast.makeText(
                requireContext(),
                if (isChecked) "Call Stream Simulation Active" else "Call Stream Simulation Stopped",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.waveformView.start()
        binding.callSimulationSwitch.isChecked = com.aurashield.ai.service.BackgroundMonitorService.isSimulatedCallActive
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
