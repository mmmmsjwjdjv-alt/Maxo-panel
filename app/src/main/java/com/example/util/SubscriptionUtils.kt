package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SubscriptionDetails(
    val userKey: String,
    val subscriptionLabel: String,
    val currentDate: String,
    val currentTime: String,
    val daysRemaining: String,
    val hoursRemaining: String,
    val minutesRemaining: String,
    val isPermanent: Boolean,
    val isActive: Boolean,
    val expiryDateFormatted: String
)

object SubscriptionUtils {

    fun calculateSubscription(userKey: String, expiryDateStr: String): SubscriptionDetails {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val now = Date()

        val currentDate = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(now)
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(now)

        val expiryDate = try {
            if (expiryDateStr.isNotBlank()) sdf.parse(expiryDateStr) else null
        } catch (e: Exception) {
            null
        }

        if (expiryDate == null) {
            return SubscriptionDetails(
                userKey = userKey.ifBlank { "UNKNOWN" },
                subscriptionLabel = "INACTIVE",
                currentDate = currentDate,
                currentTime = currentTime,
                daysRemaining = "0",
                hoursRemaining = "0",
                minutesRemaining = "0",
                isPermanent = false,
                isActive = false,
                expiryDateFormatted = expiryDateStr.ifBlank { "N/A" }
            )
        }

        val diffMillis = expiryDate.time - now.time

        if (diffMillis <= 0) {
            return SubscriptionDetails(
                userKey = userKey.ifBlank { "UNKNOWN" },
                subscriptionLabel = "EXPIRED",
                currentDate = currentDate,
                currentTime = currentTime,
                daysRemaining = "0",
                hoursRemaining = "0",
                minutesRemaining = "0",
                isPermanent = false,
                isActive = false,
                expiryDateFormatted = expiryDateStr
            )
        }

        val totalDays = diffMillis / (1000L * 60 * 60 * 24)
        val hoursRemainingValue = (diffMillis / (1000L * 60 * 60)) % 24
        val minutesRemainingValue = (diffMillis / (1000L * 60)) % 60

        val isPermanent = totalDays > 200

        return if (isPermanent) {
            SubscriptionDetails(
                userKey = userKey,
                subscriptionLabel = "PERMANENT",
                currentDate = currentDate,
                currentTime = currentTime,
                daysRemaining = "PERMANENT",
                hoursRemaining = "PERMANENT",
                minutesRemaining = "PERMANENT",
                isPermanent = true,
                isActive = true,
                expiryDateFormatted = expiryDateStr
            )
        } else {
            SubscriptionDetails(
                userKey = userKey,
                subscriptionLabel = "$totalDays DAYS REMAINING",
                currentDate = currentDate,
                currentTime = currentTime,
                daysRemaining = "$totalDays",
                hoursRemaining = "$hoursRemainingValue",
                minutesRemaining = "$minutesRemainingValue",
                isPermanent = false,
                isActive = true,
                expiryDateFormatted = expiryDateStr
            )
        }
    }
}
