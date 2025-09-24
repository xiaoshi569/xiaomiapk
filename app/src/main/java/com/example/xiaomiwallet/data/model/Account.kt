package com.example.xiaomiwallet.data.model

data class ExchangeConfig(
    val type: String, // e.g., "腾讯视频"
    val phone: String
)

data class Account(
    val us: String, // 别名
    val userId: String?,
    val passToken: String?,
    val securityToken: String?,
    val exchangeConfigs: List<ExchangeConfig> = emptyList()
)
