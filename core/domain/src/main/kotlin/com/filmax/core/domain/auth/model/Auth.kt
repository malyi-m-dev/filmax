package com.filmax.core.domain.auth.model

data class DeviceCode(
    val code: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int,
)

data class Token(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
)
