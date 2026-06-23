package com.aurashield.app.ui.console

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import com.aurashield.app.databinding.FragmentConsoleBinding
import com.aurashield.app.databinding.ItemProtectedAppBinding
import com.aurashield.app.repository.VoiceGuardRepository
import com.aurashield.app.R

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
