package com.aurashield.app.repository

import com.aurashield.app.model.InferenceStats
import com.aurashield.app.model.PredictRequest
import com.aurashield.app.model.PredictResponse
import com.aurashield.app.model.ProtectedApp
import com.aurashield.app.model.VoiceRiskEvent
import com.aurashield.app.network.RetrofitClient
import kotlinx.coroutines.delay

/**
 * Single source of truth for both the demo/mock data driving the UI right now
 * and the real network call into Deepali's FastAPI backend. Swap
 * USE_MOCK_DATA to false once /predict is live and reachable.
 */
object VoiceGuardRepository {

    private const val USE_MOCK_DATA = true

    suspend fun analyzeAudio(audioBase64: String): PredictResponse {
        return if (USE_MOCK_DATA) {
            delay(900) // simulate on-device inference latency
            PredictResponse(isDeepfake = true, confidence = 0.92)
        } else {
            RetrofitClient.apiService.predict(PredictRequest(audioBase64))
        }
    }

    fun toRiskEvent(
        response: PredictResponse,
        phoneNumber: String,
        timestampLabel: String
    ): VoiceRiskEvent {
        return VoiceRiskEvent(
            id = System.currentTimeMillis().toString(),
            phoneNumber = phoneNumber,
            timestampLabel = timestampLabel,
            riskPercent = response.riskPercent,
            classification = if (response.isDeepfake) "AI Voice Clone" else "Human Voice",
            forensicDetails = if (response.isDeepfake) {
                "Deepfake anomaly identified. Formant shifts match cloned model pattern. Pitch variance threshold exceeded: +12%."
            } else {
                "No cloning anomalies detected. Spectral and formant patterns consistent with natural speech."
            },
            isDeepfake = response.isDeepfake
        )
    }

    fun mockRiskLog(): List<VoiceRiskEvent> = listOf(
        VoiceRiskEvent(
            id = "1",
            phoneNumber = "+1 (555) 019-2834",
            timestampLabel = "10 mins ago",
            riskPercent = 92,
            classification = "AI Voice Clone",
            forensicDetails = "Deepfake anomaly identified. Formant shifts match cloned model pattern. Pitch variance threshold exceeded: +12%.",
            isDeepfake = true
        ),
        VoiceRiskEvent(
            id = "2",
            phoneNumber = "+1 (555) 048-1920",
            timestampLabel = "1 hour ago",
            riskPercent = 45,
            classification = "Inconclusive",
            forensicDetails = "Mixed spectral signature. Background noise reduced confidence. Recommend manual review.",
            isDeepfake = false
        ),
        VoiceRiskEvent(
            id = "3",
            phoneNumber = "+1 (555) 012-7384",
            timestampLabel = "3 hours ago",
            riskPercent = 2,
            classification = "Human Voice",
            forensicDetails = "No cloning anomalies detected. Spectral and formant patterns consistent with natural speech.",
            isDeepfake = false
        )
    )

    fun mockProtectedApps(): List<ProtectedApp> = listOf(
        ProtectedApp("Google Pay (GPay)", true),
        ProtectedApp("PhonePe", true),
        ProtectedApp("Paytm", false)
    )

    fun mockInferenceStats(): InferenceStats = InferenceStats(
        latencyMs = 14,
        bufferWindowMs = 500,
        modelFootprintLabel = "~15 MB (Quantized TFLite)"
    )
}
