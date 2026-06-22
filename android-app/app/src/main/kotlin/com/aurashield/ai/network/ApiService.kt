package com.aurashield.ai.network

import com.aurashield.ai.model.PredictRequest
import com.aurashield.ai.model.PredictResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Talks to the FastAPI simulator described in the AI Backend track:
 *   uvicorn main:app --reload   (served at API_BASE_URL, default http://10.0.2.2:8000/)
 *
 * Endpoint contract:
 *   POST /predict
 *   body: { "audio_base64": "<wav bytes, base64>" }
 *   200:  { "is_deepfake": true, "confidence": 0.92 }
 */
interface ApiService {
    @POST("predict")
    suspend fun predict(@Body request: PredictRequest): PredictResponse
}
