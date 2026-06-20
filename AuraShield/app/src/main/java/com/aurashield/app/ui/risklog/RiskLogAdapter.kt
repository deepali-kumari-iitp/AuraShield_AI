package com.aurashield.app.ui.risklog

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aurashield.app.R
import com.aurashield.app.databinding.ItemRiskLogBinding
import com.aurashield.app.model.RiskLevel
import com.aurashield.app.model.VoiceRiskEvent

class RiskLogAdapter : ListAdapter<VoiceRiskEvent, RiskLogAdapter.ViewHolder>(DIFF) {

    private var expandedId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiskLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == expandedId)
    }

    inner class ViewHolder(private val binding: ItemRiskLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: VoiceRiskEvent, expanded: Boolean) {
            val context = binding.root.context
            binding.phoneNumber.text = event.phoneNumber
            binding.timestamp.text = event.timestampLabel
            binding.riskPill.text = "${event.riskPercent}% RISK"
            binding.classificationText.text = event.classification
            binding.forensicDetailsText.text = event.forensicDetails

            val (pillBg, pillColor) = when (event.riskLevel) {
                RiskLevel.HIGH_RISK -> R.drawable.bg_pill_red_outline to R.color.signal_red
                RiskLevel.SUSPICIOUS -> R.drawable.bg_pill_amber_outline to R.color.signal_amber
                RiskLevel.SAFE -> R.drawable.bg_pill_safe_outline to R.color.aura_mint
            }
            binding.riskPill.setBackgroundResource(pillBg)
            binding.riskPill.setTextColor(context.getColor(pillColor))

            binding.expandPanel.visibility = if (expanded) android.view.View.VISIBLE else android.view.View.GONE
            if (expanded) {
                binding.spectrogramView.setSeed(event.id.hashCode().toLong())
            }

            binding.root.setOnClickListener {
                expandedId = if (expanded) null else event.id
                notifyDataSetChanged()
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<VoiceRiskEvent>() {
            override fun areItemsTheSame(oldItem: VoiceRiskEvent, newItem: VoiceRiskEvent) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: VoiceRiskEvent, newItem: VoiceRiskEvent) =
                oldItem == newItem
        }
    }
}
