package com.aurashield.ai.ui.risklog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aurashield.ai.databinding.FragmentRiskLogBinding
import com.aurashield.ai.repository.VoiceGuardRepository

import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RiskLogFragment : Fragment() {

    private var _binding: FragmentRiskLogBinding? = null
    private val binding get() = _binding!!
    private val adapter = RiskLogAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiskLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val layoutManager = LinearLayoutManager(requireContext()).apply {
            reverseLayout = false
            stackFromEnd = false
        }
        binding.riskLogRecyclerView.layoutManager = layoutManager
        binding.riskLogRecyclerView.adapter = adapter

        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                if (positionStart == 0) {
                    binding.riskLogRecyclerView.scrollToPosition(0)
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            snapshotFlow { com.aurashield.ai.data.HistoryRegistry.records.toList() }
                .collectLatest { recordsList ->
                    val events = recordsList.map { record ->
                        com.aurashield.ai.model.VoiceRiskEvent(
                            id = record.id,
                            phoneNumber = record.phoneNumber,
                            timestampLabel = record.timestampString,
                            riskPercent = record.riskPercentage.toInt(),
                            classification = if (record.isAiClone) "AI Voice Clone" else if (record.riskPercentage >= 30f) "Pitch Variance" else "Safe Contact",
                            forensicDetails = record.details,
                            isDeepfake = record.isAiClone
                        )
                    }
                    adapter.submitList(events)
                }
        }

        loadEvents()

        binding.swipeRefresh.setOnRefreshListener {
            com.aurashield.ai.data.HistoryRegistry.loadCallLogRecords(requireContext())
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun loadEvents() {
        com.aurashield.ai.data.HistoryRegistry.loadCallLogRecords(requireContext())
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
