package com.example.xiaomiwallet.data

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationService {
    private val pushPlusApi = PushPlusApiClient.instance

    suspend fun sendPushPlusNotification(
        title: String,
        content: String,
        pushPlusToken: String
    ): Boolean {
        if (pushPlusToken.isBlank()) {
            println("❌ Push Plus Token 未配置，跳过通知发送")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JsonObject().apply {
                    addProperty("token", pushPlusToken)
                    addProperty("title", title)
                    addProperty("content", content)
                    addProperty("template", "txt")
                }

                println("🔄 正在发送Push Plus通知...")
                println("📧 标题: $title")
                println("🔑 Token: ${pushPlusToken.take(8)}***${if (pushPlusToken.length > 12) pushPlusToken.takeLast(4) else "***"}")

                val response = pushPlusApi.sendNotification(requestBody)
                
                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.get("code")?.asInt == 200) {
                        println("✅ Push Plus通知发送成功")
                        true
                    } else {
                        val message = result?.get("msg")?.asString ?: "未知错误"
                        println("❌ Push Plus通知发送失败: $message")
                        false
                    }
                } else {
                    println("❌ Push Plus通知发送失败: HTTP ${response.code()}")
                    false
                }
            } catch (e: Exception) {
                println("❌ Push Plus通知发送异常: ${e.message}")
                false
            }
        }
    }

    suspend fun sendTaskCompletionNotification(
        pushPlusToken: String,
        successfulAccounts: Int,
        failedAccounts: Int,
        totalAccounts: Int
    ) {
        val title = "小米钱包任务执行完成"
        val content = buildString {
            appendLine("📊 执行结果统计：")
            appendLine("✅ 成功账号：$successfulAccounts")
            appendLine("❌ 失败账号：$failedAccounts")
            appendLine("📱 总账号数：$totalAccounts")
            appendLine("📅 执行时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        }

        sendPushPlusNotification(title, content, pushPlusToken)
    }

    suspend fun sendExchangeCompletionNotification(
        pushPlusToken: String,
        totalExchanges: Int,
        successfulExchanges: Int,
        failedExchanges: Int,
        exchangeResults: List<String>
    ) {
        val title = "小米钱包会员兑换完成"
        val content = buildString {
            appendLine("🎁 兑换结果统计：")
            appendLine("✅ 成功兑换：$successfulExchanges")
            appendLine("❌ 失败兑换：$failedExchanges")
            appendLine("📊 总兑换数：$totalExchanges")
            appendLine()
            appendLine("📋 详细结果：")
            exchangeResults.take(10).forEach { result ->
                appendLine("• $result")
            }
            if (exchangeResults.size > 10) {
                appendLine("... 还有${exchangeResults.size - 10}条结果")
            }
            appendLine()
            appendLine("📅 执行时间：${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}")
        }

        sendPushPlusNotification(title, content, pushPlusToken)
    }
}
