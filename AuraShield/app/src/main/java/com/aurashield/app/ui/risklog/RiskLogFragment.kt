package com.aurashield.app.ui.risklog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.aurashield.app.databinding.FragmentRiskLogBinding
import com.aurashield.app.repository.VoiceGuardRepository

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

        binding.riskLogRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.riskLogRecyclerView.adapter = adapter
        loadEvents()

        binding.swipeRefresh.setOnRefreshListener { loadEvents() }
    }

    private fun loadEvents() {
        adapter.submitList(VoiceGuardRepository.mockRiskLog())
        binding.swipeRefresh.isRefreshing = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
