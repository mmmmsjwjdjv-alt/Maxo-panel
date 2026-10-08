package com.example.data.model

data class UserRecord(
    val key: String,
    val deviceId: String,
    val expiryDate: String,
    val allowOffline: Boolean
)
