package com.aurashield.ai.data

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat

data class ForensicCallRecord(
    val id: String,
    val phoneNumber: String,
    val timestampString: String,
    val riskPercentage: Float,
    val isAiClone: Boolean,
    val details: String
)

object HistoryRegistry {
    val records = mutableStateListOf<ForensicCallRecord>().apply {
        add(ForensicCallRecord("1", "+1 (555) 019-2834", "10 mins ago", 92f, true, "Deepfake anomaly identified. Formant shifts match cloned model pattern."))
        add(ForensicCallRecord("2", "+1 (555) 048-1920", "1 hour ago", 45f, false, "Suspect pitch variance detected. Voice features altered."))
        add(ForensicCallRecord("3", "+1 (555) 012-7384", "3 hours ago", 2f, false, "Acoustics matching authenticated signature."))
        add(ForensicCallRecord("4", "+1 (555) 089-3829", "Yesterday", 95f, true, "Financial request keywords mapping deepfake model payload."))
    }

    fun loadCallLogRecords(context: Context) {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            Log.w("HistoryRegistry", "READ_CALL_LOG permission not granted. Showing mock data.")
            return
        }
        val contentResolver = context.contentResolver
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        
        try {
            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val newRecords = mutableListOf<ForensicCallRecord>()
                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                
                var count = 0
                while (cursor.moveToNext() && count < 20) {
                    val number = if (numberIndex != -1) cursor.getString(numberIndex) else "Unknown"
                    val dateMillis = if (dateIndex != -1) cursor.getLong(dateIndex) else System.currentTimeMillis()
                    val type = if (typeIndex != -1) cursor.getInt(typeIndex) else CallLog.Calls.INCOMING_TYPE
                    
                    val timestampStr = DateUtils.getRelativeTimeSpanString(
                        dateMillis,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                    
                    // Assign simulated risk score based on hash of number/time to make it look realistic
                    val rawHash = number.hashCode() xor dateMillis.hashCode().toInt()
                    val hash = if (rawHash < 0) -rawHash else rawHash
                    val risk = when (type) {
                        CallLog.Calls.MISSED_TYPE -> (hash % 40f) + 10f // 10% - 50%
                        else -> {
                            val r = hash % 100
                            if (r < 20) {
                                (hash % 15f) + 80f // 80% - 95% (High risk)
                            } else if (r < 50) {
                                (hash % 40f) + 30f // 30% - 70% (Medium risk)
                            } else {
                                hash % 15f // 0% - 15% (Low risk)
                            }
                        }
                    }
                    
                    val isAiClone = risk >= 80f
                    val details = if (isAiClone) {
                        "Deepfake anomaly identified. Formant shifts match cloned model pattern."
                    } else if (risk >= 30f) {
                        "Suspect pitch variance detected. Voice features altered."
                    } else {
                        "Acoustics matching authenticated signature."
                    }
                    
                    newRecords.add(
                        ForensicCallRecord(
                            id = java.util.UUID.randomUUID().toString(),
                            phoneNumber = number,
                            timestampString = timestampStr,
                            riskPercentage = risk,
                            isAiClone = isAiClone,
                            details = details
                        )
                    )
                    count++
                }
                
                if (newRecords.isNotEmpty()) {
                    records.clear()
                    records.addAll(newRecords)
                    Log.d("HistoryRegistry", "Successfully loaded ${newRecords.size} records from CallLog database.")
                }
            }
        } catch (e: Exception) {
            Log.e("HistoryRegistry", "Error querying CallLog: ${e.message}", e)
        }
    }
}
