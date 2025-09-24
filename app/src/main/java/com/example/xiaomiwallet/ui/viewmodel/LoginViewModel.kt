package com.example.xiaomiwallet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.xiaomiwallet.data.AccountRepository
import com.example.xiaomiwallet.data.LoginApiClient
import com.example.xiaomiwallet.data.model.Account
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LoginMethod {
    QR, COOKIE
}

data class LoginUiState(
    val alias: String = "",
    val qrUrl: String? = null,
    val statusText: String = "请选择登录方式",
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val loginMethod: LoginMethod = LoginMethod.QR,
    val passToken: String = "",
    val userId: String = ""
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val accountRepository = AccountRepository(application)
    private val loginApiService = LoginApiClient.instance
    private val gson = Gson()
    private var pollingJob: Job? = null

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAliasChange(newAlias: String) {
        _uiState.update { it.copy(alias = newAlias) }
    }

    fun onPassTokenChange(newToken: String) {
        _uiState.update { it.copy(passToken = newToken) }
    }

    fun onUserIdChange(newId: String) {
        _uiState.update { it.copy(userId = newId) }
    }

    fun onLoginMethodChange(method: LoginMethod) {
        _uiState.update { it.copy(loginMethod = method, statusText = "请输入账号信息") }
    }

    fun saveAccountFromCookie() {
        if (_uiState.value.alias.isBlank() || _uiState.value.passToken.isBlank() || _uiState.value.userId.isBlank()) {
            _uiState.update { it.copy(statusText = "❌ 请填写所有必填项") }
            return
        }
        viewModelScope.launch {
            val newAccount = Account(
                us = _uiState.value.alias,
                userId = _uiState.value.userId,
                passToken = _uiState.value.passToken,
                securityToken = null, // Not needed for cookie login
                exchangeConfigs = emptyList()
            )
            accountRepository.addAccount(newAccount)
            _uiState.update { it.copy(statusText = "🎉 账号 '${_uiState.value.alias}' 已保存", loginSuccess = true) }
        }
    }

    fun generateQrCode() {
        if (_uiState.value.alias.isBlank()) {
            _uiState.update { it.copy(statusText = "❌ 请输入账号别名") }
            return
        }

        pollingJob?.cancel() // Cancel any previous polling
        _uiState.update { it.copy(isLoading = true, statusText = "正在生成二维码...") }

        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val params = mapOf(
                        "_group" to "DEFAULT",
                        "_qrsize" to "240",
                        "qs" to "?callback=https%3A%2F%2Faccount.xiaomi.com%2Fsts%3Fsign%3DZvAtJIzsDsFe60LdaPa76nNNP58%253D%26followup%3Dhttps%253A%252F%252Faccount.xiaomi.com%252Fpass%252Fauth%252Fsecurity%252Fhome%26sid%3Dpassport&sid=passport&_group=DEFAULT",
                        "bizDeviceType" to "",
                        "callback" to "https://account.xiaomi.com/sts?sign=ZvAtJIzsDsFe60LdaPa76nNNP58=&followup=https://account.xiaomi.com/pass/auth/security/home&sid=passport",
                        "_hasLogo" to "false",
                        "theme" to "",
                        "sid" to "passport",
                        "needTheme" to "false",
                        "showActiveX" to "false",
                        "serviceParam" to "{\"checkSafePhone\":false,\"checkSafeAddress\":false,\"lsrp_score\":0.0}",
                        "_locale" to "zh_CN",
                        "_sign" to "2&V1_passport&BUcblfwZ4tX84axhVUaw8t6yi2E=",
                        "_dc" to System.currentTimeMillis().toString()
                    )
                    loginApiService.getLoginQr(params)
                }

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    if (responseBody != null && "&&&START&&&" in responseBody) {
                        val jsonStr = responseBody.split("&&&START&&&")[1]
                        val data = gson.fromJson(jsonStr, JsonObject::class.java)
                        val qr = data.get("qr")?.asString
                        val lp = data.get("lp")?.asString

                        if (qr != null && lp != null) {
                            _uiState.update { it.copy(qrUrl = qr, statusText = "📱 请使用小米手机APP扫描二维码", isLoading = false) }
                            startPolling(lp)
                        } else {
                            _uiState.update { it.copy(statusText = "❌ 获取二维码失败 (数据解析错误)", isLoading = false) }
                        }
                    } else {
                        _uiState.update { it.copy(statusText = "❌ 获取二维码失败 (响应格式错误)", isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(statusText = "❌ 获取二维码失败 (网络错误)", isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusText = "❌ 获取二维码异常: ${e.message}", isLoading = false) }
            }
        }
    }

    private fun startPolling(lp: String) {
        pollingJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val response = withContext(Dispatchers.IO) { loginApiService.checkLoginStatus(lp) }
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string()
                        if (responseBody != null && "&&&START&&&" in responseBody) {
                            val jsonStr = responseBody.split("&&&START&&&")[1]
                            val data = gson.fromJson(jsonStr, JsonObject::class.java)
                            val code = data.get("code")?.asInt

                            when (code) {
                                0 -> { // Success
                                    val userId = data.get("userId")?.asString
                                    val ssecurity = data.get("ssecurity")?.asString
                                    val passToken = data.get("passToken")?.asString
                                    val newAccount = Account(
                                        us = _uiState.value.alias,
                                        userId = userId,
                                        passToken = passToken,
                                        securityToken = ssecurity,
                                        exchangeConfigs = emptyList()
                                    )
                                    accountRepository.addAccount(newAccount)
                                    _uiState.update { it.copy(statusText = "🎉 登录成功! 账号已保存", loginSuccess = true) }
                                    pollingJob?.cancel()
                                }
                                700 -> _uiState.update { it.copy(statusText = "ℹ️ 状态更新: 等待扫码") }
                                701 -> _uiState.update { it.copy(statusText = "ℹ️ 状态更新: 已扫码, 请在手机上确认") }
                                702 -> {
                                    _uiState.update { it.copy(statusText = "❌ 二维码已过期，请重新生成") }
                                    pollingJob?.cancel()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore timeouts, continue polling
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
