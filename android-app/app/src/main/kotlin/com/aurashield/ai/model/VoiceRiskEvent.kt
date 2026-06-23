package com.aurashield.ai.model

enum class RiskLevel { SAFE, SUSPICIOUS, HIGH_RISK }

/**
 * Domain model that backs the Risk Log + System Lock screens.
 * Built directly from [PredictResponse] once the live backend is wired in
 * (see VoiceGuardRepository.toRiskEvent()).
 */
data class VoiceRiskEvent(
    val id: String,
    val phoneNumber: String,
    val timestampLabel: String,
    val riskPercent: Int,
    val classification: String,
    val forensicDetails: String,
    val isDeepfake: Boolean
) {
    val riskLevel: RiskLevel
        get() = when {
            riskPercent >= 70 -> RiskLevel.HIGH_RISK
            riskPercent >= 30 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }
}

data class ProtectedApp(
    val name: String,
    var isEnabled: Boolean
)

data class InferenceStats(
    val latencyMs: Int,
    val bufferWindowMs: Int,
    val modelFootprintLabel: String
)
