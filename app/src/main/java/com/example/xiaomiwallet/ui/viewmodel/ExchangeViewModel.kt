package com.example.xiaomiwallet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xiaomiwallet.data.AccountRepository
import com.example.xiaomiwallet.data.TaskRepository
import com.example.xiaomiwallet.data.NotificationService
import com.example.xiaomiwallet.data.ResultManager
import com.example.xiaomiwallet.data.SettingsRepository
import com.example.xiaomiwallet.data.MembershipExchangeService
import com.example.xiaomiwallet.data.model.Account
import com.example.xiaomiwallet.data.model.ExchangeConfig
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ExchangeUiState(
    val accounts: List<Account> = emptyList(),
    val statusText: String = "准备就绪"
)

class ExchangeViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository = AccountRepository(application)
    private val taskRepository = TaskRepository()
    private val notificationService = NotificationService()
    private val settingsRepository = SettingsRepository(application)
    private val membershipExchangeService = MembershipExchangeService(taskRepository)

    private val _uiState = MutableStateFlow(ExchangeUiState())
    val uiState: StateFlow<ExchangeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.accountsFlow.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun addExchangeConfig(us: String, config: ExchangeConfig) {
        viewModelScope.launch {
            val currentAccounts = uiState.value.accounts
            val accountToUpdate = currentAccounts.find { it.us == us }
            if (accountToUpdate != null) {
                // Avoid duplicate config for the same type
                val newConfigs = accountToUpdate.exchangeConfigs.filterNot { it.type == config.type } + config
                val updatedAccount = accountToUpdate.copy(exchangeConfigs = newConfigs)
                
                val updatedAccountsList = currentAccounts.map {
                    if (it.us == us) updatedAccount else it
                }
                accountRepository.saveAccounts(updatedAccountsList)
            }
        }
    }

    fun deleteExchangeConfig(us: String, configType: String) {
        viewModelScope.launch {
            val currentAccounts = uiState.value.accounts
            val accountToUpdate = currentAccounts.find { it.us == us }
            if (accountToUpdate != null) {
                val newConfigs = accountToUpdate.exchangeConfigs.filterNot { it.type == configType }
                val updatedAccount = accountToUpdate.copy(exchangeConfigs = newConfigs)

                val updatedAccountsList = currentAccounts.map {
                    if (it.us == us) updatedAccount else it
                }
                accountRepository.saveAccounts(updatedAccountsList)
            }
        }
    }

    fun runExchange() {
        viewModelScope.launch {
            // 检查授权状态
            val licenseKey = settingsRepository.licenseKeyFlow.first()
            if (licenseKey.isBlank()) {
                _uiState.update { it.copy(statusText = "❌ 请先输入并验证授权码") }
                return@launch
            }
            
            val startTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val logs = mutableListOf<String>()
            logs.add("✅ 会员兑换任务开始")
            logs.add("执行时间: $startTime")
            
            _uiState.update { it.copy(statusText = "🚀 开始执行兑换...") }
            logs.add("🚀 开始执行兑换...")
            val accounts = uiState.value.accounts
            if (accounts.isEmpty()) {
                logs.add("❌ 没有找到账号")
                _uiState.update { it.copy(statusText = "❌ 没有找到账号") }
                recordExchangeResult(startTime, logs, false, "没有找到账号")
                return@launch
            }

            logs.add("找到 ${accounts.size} 个账号")
            val allExchangeResults = mutableListOf<String>()
            var totalExchanges = 0
            var successfulExchanges = 0
            var failedExchanges = 0

            for (account in accounts) {
                if (account.exchangeConfigs.isEmpty()) {
                    logs.add("⚠️ ${account.us}: 未配置兑换项目，跳过")
                    continue
                }

                logs.add("\n--- 处理账号: ${account.us} ---")
                _uiState.update { it.copy(statusText = "正在处理账号: ${account.us}") }
                
                logs.add("1. 获取会话Cookie...")
                val cookies = taskRepository.getSessionCookies(account)
                if (cookies == null) {
                    logs.add("❌ 获取会话失败")
                    _uiState.update { it.copy(statusText = "❌ ${account.us}: 获取会话失败") }
                    continue
                }
                logs.add("✅ 会话Cookie获取成功")

                logs.add("2. 执行兑换操作...")
                // 使用增强的兑换服务
                val exchangeResults = membershipExchangeService.performExchangeForAccount(account, cookies)
                
                // 统计结果
                totalExchanges += exchangeResults.size
                logs.add("共执行 ${exchangeResults.size} 项兑换")
                
                for (result in exchangeResults) {
                    if (result.success) {
                        successfulExchanges++
                        val successMsg = "✅ ${result.membershipType}: ${result.message}"
                        allExchangeResults.add(successMsg)
                        logs.add(successMsg)
                    } else {
                        failedExchanges++
                        val failMsg = "❌ ${result.membershipType}: ${result.message}"
                        allExchangeResults.add(failMsg)
                        logs.add(failMsg)
                    }
                    
                    // 更新UI状态显示最新结果
                    _uiState.update { it.copy(statusText = "${if (result.success) "✅" else "❌"} ${account.us}: ${result.message}") }
                    delay(1500) // 让用户看到每个结果
                }
            }
            
            val finalStatus = "✅ 兑换完毕: 成功 $successfulExchanges, 失败 $failedExchanges"
            logs.add("\n$finalStatus")
            _uiState.update { it.copy(statusText = finalStatus) }
            
            // 记录兑换结果到日志系统
            recordExchangeResult(startTime, logs, successfulExchanges > 0, "", allExchangeResults)
            
            // 发送兑换完成通知
            try {
                val pushPlusToken = settingsRepository.pushPlusTokenFlow.first()
                
                notificationService.sendExchangeCompletionNotification(
                    pushPlusToken = pushPlusToken,
                    totalExchanges = totalExchanges,
                    successfulExchanges = successfulExchanges,
                    failedExchanges = failedExchanges,
                    exchangeResults = allExchangeResults
                )
            } catch (e: Exception) {
                println("❌ 发送兑换完成通知失败: ${e.message}")
            }
        }
    }

    private fun recordExchangeResult(
        startTime: String,
        logs: List<String>,
        success: Boolean,
        errorMessage: String = "",
        exchangeResults: List<String> = emptyList()
    ) {
        val endTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        ResultManager.addTaskResult(
            accountName = "会员兑换",
            success = success,
            errorMessage = errorMessage,
            logs = logs,
            exchangeResults = exchangeResults,
            startTime = startTime,
            endTime = endTime
        )
    }
}