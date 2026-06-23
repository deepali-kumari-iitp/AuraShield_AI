package com.aurashield.app.ui.systemlock

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aurashield.app.databinding.LayoutEmergencyLockBinding
import java.util.Locale

class SystemLockFragment : Fragment() {

    private var _binding: LayoutEmergencyLockBinding? = null
    private val binding get() = _binding!!
    private var countDownTimer: CountDownTimer? = null

    companion object {
        private const val LOCKOUT_MILLIS = 15 * 60 * 1000L // 15:00
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = LayoutEmergencyLockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startCountdown()
        binding.biometricButton.setOnClickListener { showBiometricPrompt() }
    }

    private fun startCountdown() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(LOCKOUT_MILLIS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                binding.countdownTimer.text =
                    String.format(Locale.US, "%02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                binding.countdownTimer.text = "00:00"
            }
        }.start()
    }

    private fun showBiometricPrompt() {
        val activity = activity ?: return
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                activity,
                "Biometric override unavailable on this device.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    countDownTimer?.cancel()
                    Toast.makeText(activity, "Identity verified. Transactions unlocked.", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(activity, "Verification failed: $errString", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Verify it's really you")
            .setSubtitle("Confirm your identity to lift the AuraShield lockout")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
