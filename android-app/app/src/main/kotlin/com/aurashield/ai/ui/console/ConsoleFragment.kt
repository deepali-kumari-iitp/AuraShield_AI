package com.aurashield.ai.ui.console

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.aurashield.ai.databinding.FragmentConsoleBinding
import com.aurashield.ai.databinding.ItemProtectedAppBinding
import com.aurashield.ai.repository.VoiceGuardRepository
import com.aurashield.ai.R

class ConsoleFragment : Fragment() {

    private var _binding: FragmentConsoleBinding? = null
    private val binding get() = _binding!!
    private var pulseAnimator: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConsoleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.protectionRing.setProgress(92)
        bindRegistry()
        startShieldPulse()
        bindEngineControls()
    }

    private fun bindEngineControls() {
        binding.engineSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isServiceRunning()) {
                    val intent = android.content.Intent(requireContext(), com.aurashield.ai.service.BackgroundMonitorService::class.java)
                    androidx.core.content.ContextCompat.startForegroundService(requireContext(), intent)
                    android.widget.Toast.makeText(requireContext(), "Call Monitoring Engine Active", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                if (isServiceRunning()) {
                    val intent = android.content.Intent(requireContext(), com.aurashield.ai.service.BackgroundMonitorService::class.java).apply {
                        action = com.aurashield.ai.service.BackgroundMonitorService.ACTION_STOP_SERVICE
                    }
                    requireContext().startService(intent)
                    android.widget.Toast.makeText(requireContext(), "Monitoring Engine Stopped", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.overlaySwitch.setOnCheckedChangeListener { _, isChecked ->
            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                android.provider.Settings.canDrawOverlays(requireContext())
            } else {
                true
            }
            if (isChecked && !hasPermission) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${requireContext().packageName}")
                    )
                    startActivity(intent)
                }
            } else if (!isChecked && hasPermission) {
                android.widget.Toast.makeText(requireContext(), "Revoke overlay permission in system settings to disable", android.widget.Toast.LENGTH_SHORT).show()
                binding.overlaySwitch.isChecked = true
            }
        }

        binding.simulationSwitch.setOnCheckedChangeListener { _, isChecked ->
            com.aurashield.ai.service.BackgroundMonitorService.isSimulatedAttackActive = isChecked
            android.widget.Toast.makeText(
                requireContext(),
                if (isChecked) "Voice Attack Simulation Active" else "Simulation Disabled",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshEngineControls()
    }

    private fun refreshEngineControls() {
        binding.engineSwitch.isChecked = isServiceRunning()
        binding.overlaySwitch.isChecked = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            android.provider.Settings.canDrawOverlays(requireContext())
        } else {
            true
        }
        binding.simulationSwitch.isChecked = com.aurashield.ai.service.BackgroundMonitorService.isSimulatedAttackActive
    }

    @Suppress("DEPRECATION")
    private fun isServiceRunning(): Boolean {
        val manager = requireContext().getSystemService(android.content.Context.ACTIVITY_SERVICE) as? android.app.ActivityManager ?: return false
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (com.aurashield.ai.service.BackgroundMonitorService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    private fun bindRegistry() {
        binding.registryList.removeAllViews()
        val apps = VoiceGuardRepository.mockProtectedApps()
        apps.forEachIndexed { index, app ->
            val row = ItemProtectedAppBinding.inflate(
                layoutInflater, binding.registryList, false
            )
            row.appName.text = app.name
            row.appSwitch.isChecked = app.isEnabled
            row.appLockIcon.setColorFilter(
                resources.getColor(
                    if (app.isEnabled) R.color.aura_mint else R.color.text_tertiary, null
                )
            )
            row.appSwitch.setOnCheckedChangeListener { _, isChecked ->
                row.appLockIcon.setColorFilter(
                    resources.getColor(
                        if (isChecked) R.color.aura_mint else R.color.text_tertiary, null
                    )
                )
            }
            binding.registryList.addView(row.root)

            if (index != apps.lastIndex) {
                val divider = View(requireContext())
                divider.setBackgroundColor(resources.getColor(R.color.bg_stroke, null))
                val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                divider.layoutParams = params
                binding.registryList.addView(divider)
            }
        }
    }

    /** Soft breathing glow: the two outer rings scale + fade in a staggered loop. */
    private fun startShieldPulse() {
        val outer = pulse(binding.glowRingOuter, 1900, 0)
        val inner = pulse(binding.glowRingInner, 1900, 300)
        pulseAnimator = AnimatorSet().apply {
            playTogether(outer, inner)
            start()
        }
    }

    private fun pulse(target: View, duration: Long, startDelay: Long): AnimatorSet {
        val scaleX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, 1.12f, 1f)
        val scaleY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, 1.12f, 1f)
        val alpha = ObjectAnimator.ofFloat(target, View.ALPHA, target.alpha, target.alpha * 0.4f, target.alpha)
        return AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            this.duration = duration
            this.startDelay = startDelay
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isAdded) start()
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pulseAnimator?.cancel()
        _binding = null
    }
}
