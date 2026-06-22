package com.aurashield.ai.model

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the exact JSON contract returned by Deepali's FastAPI simulator:
 *   POST /predict
 *   { "is_deepfake": true, "confidence": 0.92 }
 *
 * Keep this in lockstep with the backend response model so deserialization
 * never breaks when the real model is swapped in for the stub.
 */
data class PredictResponse(
    @SerializedName("is_deepfake") val isDeepfake: Boolean,
    @SerializedName("confidence") val confidence: Double
) {
    /** Convenience: confidence as a 0-100 integer for UI display. */
    val riskPercent: Int
        get() = (confidence * 100).toInt()
}

/** Request body sent to /predict — adjust field name to match the backend's exact param. */
data class PredictRequest(
    @SerializedName("audio_base64") val audioBase64: String
)
