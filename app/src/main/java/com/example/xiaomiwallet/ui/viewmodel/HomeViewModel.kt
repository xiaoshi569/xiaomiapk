package com.example.xiaomiwallet.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.provider.Settings
import com.example.xiaomiwallet.data.*
import com.example.xiaomiwallet.data.model.Account
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class HomeUiState(
    val licenseKey: String = "",
    val pushPlusToken: String = "",
    val autoRunEnabled: Boolean = false,
    val accountCount: Int = 0,
    val loggedInCount: Int = 0,
    val licenseVerified: Boolean = false,
    val statusText: String = "欢迎使用",
    val isCountingDown: Boolean = false,
    val countdownSeconds: Int = 0,
    val showCancelButton: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val accountRepository = AccountRepository(application)
    private val taskRepository = TaskRepository()
    private val authApiService = AuthApiClient.instance
    private val notificationService = NotificationService()
    
    private var countdownJob: kotlinx.coroutines.Job? = null

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Collect account changes to update counts
        viewModelScope.launch {
            accountRepository.accountsFlow.collect { accounts ->
                _uiState.update {
                    it.copy(
                        accountCount = accounts.size,
                        loggedInCount = accounts.count { acc -> acc.userId != null }
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.licenseKeyFlow.collect { licenseKey ->
                _uiState.update { it.copy(licenseKey = licenseKey) }
                
                // 自动验证授权（当授权码加载完成后）
                if (licenseKey.isNotBlank()) {
                    verifyLicense()
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.pushPlusTokenFlow.collect { pushPlusToken ->
                _uiState.update { it.copy(pushPlusToken = pushPlusToken) }
            }
        }
        viewModelScope.launch {
            settingsRepository.autoRunEnabledFlow.collect { autoRunEnabled ->
                _uiState.update { it.copy(autoRunEnabled = autoRunEnabled) }
            }
        }
        
        // 启动时自动运行逻辑：当设置加载完成、授权验证成功且启用自动运行时，自动开始任务
        var hasAutoRunTriggered = false
        viewModelScope.launch {
            // 组合授权状态和自动运行设置
            combine(
                _uiState.map { it.licenseVerified },
                _uiState.map { it.autoRunEnabled }
            ) { licenseVerified, autoRunEnabled ->
                licenseVerified && autoRunEnabled
            }.collect { shouldAutoRun ->
                if (shouldAutoRun && !hasAutoRunTriggered) {
                    hasAutoRunTriggered = true
                    // 延迟3秒后自动开始运行，给用户时间看到界面
                    kotlinx.coroutines.delay(3000)
                    _uiState.update { it.copy(statusText = "🤖 检测到自动运行已启用，准备执行任务...") }
                    kotlinx.coroutines.delay(1000)
                    startAutoRunCountdown()
                }
            }
        }
    }

    private fun getDeviceId(): String {
        val androidId = Settings.Secure.getString(
            getApplication<Application>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
        // Hash the ANDROID_ID to get a consistent 16-character hex string, similar to the python version
        return MessageDigest.getInstance("MD5").digest(androidId.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .substring(0, 16)
    }

    fun updateLicenseKey(key: String) {
        viewModelScope.launch {
            settingsRepository.saveLicenseKey(key)
        }
    }

    fun updatePushPlusToken(token: String) {
        viewModelScope.launch {
            settingsRepository.savePushPlusToken(token)
        }
    }

    fun toggleAutoRun() {
        viewModelScope.launch {
            val newValue = !_uiState.value.autoRunEnabled
            settingsRepository.saveAutoRunEnabled(newValue)
        }
    }

    fun verifyLicense() {
        val currentKey = _uiState.value.licenseKey
        if (currentKey.isBlank()) {
            _uiState.update { it.copy(licenseVerified = false, statusText = "❌ 请先输入授权码") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(statusText = "🔍 验证授权中...") }
            
            try {
                val deviceId = getDeviceId()
                
                // 完全按照gui.py的方式请求
                val requestBody = com.google.gson.JsonObject().apply {
                    addProperty("key", currentKey)  // gui.py使用"key"而不是"licenseKey"
                    addProperty("device_id", deviceId)  // gui.py使用"device_id"而不是"deviceId"
                }
                
                println("🔑 设备ID: $deviceId")
                println("🔐 正在验证授权码...")
                
                val response = authApiService.verifyLicense(requestBody)
                
                if (response.isSuccessful) {
                    val result = response.body()
                    val isValid = result?.get("valid")?.asBoolean ?: false
                    
                    if (isValid) {
                        val message = result?.get("message")?.asString ?: "验证通过"
                        println("✅ 授权验证成功: $message")
                        _uiState.update { 
                            it.copy(
                                licenseVerified = true, 
                                statusText = "✅ 授权验证成功: $message"
                            ) 
                        }
                    } else {
                        val message = result?.get("message")?.asString ?: "验证失败"
                        println("❌ 授权验证失败: $message")
                        _uiState.update { 
                            it.copy(
                                licenseVerified = false, 
                                statusText = "❌ 授权验证失败: $message"
                            ) 
                        }
                    }
                } else {
                    val errorMsg = "授权服务器连接失败 (HTTP ${response.code()})"
                    println("❌ $errorMsg")
                    _uiState.update { 
                        it.copy(
                            licenseVerified = false, 
                            statusText = "❌ $errorMsg"
                        ) 
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        licenseVerified = false, 
                        statusText = "❌ 授权验证异常: ${e.message}"
                    ) 
                }
            }
        }
    }

    fun runAllTasks() {
        if (!_uiState.value.licenseVerified) {
            _uiState.update { it.copy(statusText = "❌ 请先验证授权") }
            return
        }

        viewModelScope.launch {
            val accounts = accountRepository.accountsFlow.first()
            if (accounts.isEmpty()) {
                _uiState.update { it.copy(statusText = "❌ 没有找到账号") }
                return@launch
            }

            _uiState.update { it.copy(statusText = "🚀 开始执行所有账号的任务...") }
            var successCount = 0
            var failCount = 0

            for ((index, account) in accounts.withIndex()) {
                // 如果不是第一个账号，添加账号间延迟避免冲突
                if (index > 0) {
                    _uiState.update { it.copy(statusText = "⏳ 等待 5 秒后执行下一个账号...") }
                    delay(5000) // 账号间延迟5秒
                }
                
                _uiState.update { it.copy(statusText = "正在执行: ${account.us} (${index + 1}/${accounts.size})") }
                
                try {
                    val success = executeTasksForAccount(account)
                    if (success) successCount++ else failCount++
                    
                } catch (e: Exception) {
                    failCount++
                    _uiState.update { it.copy(statusText = "❌ 账号 ${account.us} 执行异常: ${e.message}") }
                    delay(2000) // Show error for a while
                }
            }
            
            val finalStatusText = "✅ 执行完毕: 成功 $successCount, 失败 $failCount"
            _uiState.update { it.copy(statusText = finalStatusText) }
            
            // Send notification if configured
            try {
                val pushPlusToken = settingsRepository.pushPlusTokenFlow.first()
                if (pushPlusToken.isNotBlank()) {
                    notificationService.sendTaskCompletionNotification(
                        pushPlusToken = pushPlusToken,
                        successfulAccounts = successCount,
                        failedAccounts = failCount,
                        totalAccounts = accounts.size
                    )
                }
            } catch (e: Exception) {
                println("❌ 发送通知失败: ${e.message}")
            }
        }
    }

    private suspend fun executeTasksForAccount(account: Account): Boolean {
        val startTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val logs = mutableListOf<String>()
        var success = false
        var errorMessage = ""
        
        try {
            logs.add("✅ 账号 '${account.us}' 任务执行开始")
            logs.add("用户ID: ${account.userId}")
            logs.add("执行时间: $startTime")
            logs.add("开始执行任务...")
            
            _uiState.update { it.copy(statusText = "获取会话Cookie...") }
            logs.add("1. 获取会话Cookie...")
            
            // 1. 获取会话Cookie
            val cookies = taskRepository.getSessionCookies(account)
            if (cookies == null) {
                errorMessage = "获取会话失败"
                logs.add("❌ $errorMessage")
                logs.add("   请检查passToken和userId是否正确")
                logs.add("   passToken: ${account.passToken?.take(20)}...")
                logs.add("   userId: ${account.userId}")
                _uiState.update { it.copy(statusText = "❌ ${account.us}: $errorMessage") }
                delay(1500)
                return false
            } else {
                logs.add("✅ 会话Cookie获取成功")
            }
            
            // 2. 查询用户信息和今日记录
            _uiState.update { it.copy(statusText = "查询用户信息和奖励记录...") }
            logs.add("2. 查询用户信息和奖励记录...")
            val userInfo = taskRepository.queryUserInfoAndRecords(cookies)
            if (userInfo == null) {
                errorMessage = "获取用户信息失败"
                logs.add("❌ $errorMessage")
                _uiState.update { it.copy(statusText = "❌ ${account.us}: $errorMessage") }
                delay(1500)
                return false
            }
            
            val (totalDays, todayRecords) = userInfo
            logs.add("✅ 当前可兑换视频天数: $totalDays")
            _uiState.update { it.copy(statusText = "当前可兑换视频天数: $totalDays") }
            delay(1000)
            
            // 3. 检查今日记录数量
            if (todayRecords.isNotEmpty()) {
                logs.add("📅 检测到今日已有 ${todayRecords.size} 条奖励记录")
                todayRecords.forEach { record ->
                    val createTime = record["createTime"] as? String ?: "未知时间"
                    val value = record["value"] as? Int ?: 0
                    val days = value / 100.0
                    logs.add("| ⏰ $createTime")
                    logs.add("| 🎁 领到视频会员，+${String.format("%.2f", days)}天")
                }
            } else {
                logs.add("📅 今日暂无奖励记录，准备执行任务")
            }
            
            val todayCompleted = todayRecords.size >= 3
            
            if (todayCompleted) {
                _uiState.update { it.copy(statusText = "✅ ${account.us}: 今天已有 ${todayRecords.size} 条奖励记录（≥3条），跳过任务执行") }
                delay(2000)
                return true
            } else {
                _uiState.update { it.copy(statusText = "📅 ${account.us}: 今日有 ${todayRecords.size} 条奖励记录（<3条），开始执行任务") }
                delay(1000)
                
                // 显示今日记录详情
                if (todayRecords.isNotEmpty()) {
                    for (record in todayRecords) {
                        val createTime = record["createTime"] as String
                        val value = record["value"] as Int
                        val days = value / 100.0
                        _uiState.update { it.copy(statusText = "   ⏰ $createTime | 🎁 +${String.format("%.2f", days)}天") }
                        delay(800)
                    }
                }
                
                // 按照gui.py重新构建的任务执行逻辑
                
                // 4. 先尝试完成新手任务
                logs.add("3. 尝试完成应用下载试用任务...")
                _uiState.update { it.copy(statusText = "3. 尝试完成应用下载试用任务...") }
                delay(1000)
                
                val newUserTaskId = taskRepository.completeNewUserTask(cookies)
                if (newUserTaskId != null) {
                    logs.add("✅ 完成应用下载试用成功，获得userTaskId: $newUserTaskId")
                    _uiState.update { it.copy(statusText = "✅ 完成应用下载试用成功，获得userTaskId: $newUserTaskId") }
                    delay(2000)
                    
                    // gui.py: time.sleep(2) 然后领取奖励
                    val awardReceived = taskRepository.receiveNewUserAward(cookies, newUserTaskId)
                    if (awardReceived) {
                        logs.add("✅ 领取应用下载试用奖励成功")
                        _uiState.update { it.copy(statusText = "✅ 领取应用下载试用奖励成功") }
                    } else {
                        logs.add("❌ 领取应用下载试用奖励失败")
                        _uiState.update { it.copy(statusText = "⚠️ 领取应用下载试用奖励失败") }
                    }
                    delay(2000)
                } else {
                    logs.add("⚠️ 应用下载试用任务已完成或不可用")
                    _uiState.update { it.copy(statusText = "⚠️ 应用下载试用任务已完成或不可用") }
                    delay(2000)
                }
                
                // 5. 执行两轮浏览任务 - 完全按照gui.py的逻辑
                var taskSuccess = true
                
                // gui.py: 如果今天已经完成任务，直接跳过浏览任务
                if (todayRecords.size >= 3) {
                    _uiState.update { it.copy(statusText = "✅ 今天已经完成所有任务，跳过浏览任务执行") }
                    delay(2000)
                } else {
                    // gui.py: for round_num in range(2):
                    for (roundNum in 0..1) {
                        logs.add("\n--- 开始第 ${roundNum + 1} 轮任务 ---")
                        _uiState.update { it.copy(statusText = "\n--- 开始第 ${roundNum + 1} 轮任务 ---") }
                        delay(1000)
                        
                        // gui.py: tasks = rnl.get_task_list() - 每轮都重新获取！
                        logs.add("正在获取任务列表...")
                        _uiState.update { it.copy(statusText = "正在获取任务列表...") }
                        val tasks = taskRepository.getTaskList(cookies)
                        if (tasks.isNullOrEmpty()) {
                            logs.add("⚠️ 未找到可执行的任务列表，可能今日任务已完成")
                            _uiState.update { it.copy(statusText = "⚠️ 未找到可执行的任务列表，可能今日任务已完成") }
                            delay(2000)
                            break
                        } else {
                            logs.add("✅ 获取到 ${tasks.size} 个浏览任务")
                            _uiState.update { it.copy(statusText = "✅ 获取到 ${tasks.size} 个浏览任务") }
                            delay(1000)
                        }
                        
                        // gui.py: task = tasks[0]
                        val task = tasks[0]
                        logs.add("选择第一个任务: ${task["taskName"]}")
                        _uiState.update { it.copy(statusText = "选择第一个任务: ${task["taskName"]}") }
                        delay(1000)
                        
                        // gui.py: rnl.t_id = task['generalActivityUrlInfo']['id']
                        val generalActivityUrlInfo = task["generalActivityUrlInfo"] as? Map<String, String>
                        val tId = generalActivityUrlInfo?.get("id") ?: ""
                        
                        logs.add("解析任务参数: taskId=${task["taskId"]}, tId=$tId")
                        _uiState.update { it.copy(statusText = "解析任务参数: taskId=${task["taskId"]}, tId=$tId") }
                        delay(1000)
                        
                        if (tId.isBlank()) {
                            logs.add("❌ 无法获取任务t_id，中断执行")
                            _uiState.update { it.copy(statusText = "❌ 无法获取任务t_id，中断执行") }
                            // gui.py: 只有在无法获取t_id时才设置success=false并break
                            taskSuccess = false
                            delay(2000)
                            break
                        }
                        
                        val taskId = task["taskId"] as String
                        val taskCode = task["taskCode"] as String
                        val browsClickUrlId = generalActivityUrlInfo?.get("browsClickUrlId") ?: ""
                        
                        // gui.py: result_obj["logs"].append("4. 执行浏览任务...")
                        logs.add("4. 执行浏览任务...")
                        _uiState.update { it.copy(statusText = "4. 执行浏览任务...") }
                        delay(1000)
                        
                        // gui.py: delay = random.randint(10, 15)
                        logs.add("等待随机延迟...")
                        _uiState.update { it.copy(statusText = "等待随机延迟...") }
                        val delaySeconds = kotlin.random.Random.nextInt(10, 16)
                        logs.add("等待 $delaySeconds 秒...")
                        _uiState.update { it.copy(statusText = "等待 $delaySeconds 秒...") }
                        delay(delaySeconds * 1000L)
                        
                        // gui.py: user_task_id = rnl.complete_task(task_id=task_id, t_id=rnl.t_id, brows_click_url_id=brows_click_url_id)
                        var userTaskId = taskRepository.completeTask(cookies, taskId, tId, browsClickUrlId)
                        
                        // gui.py: time.sleep(random.randint(2, 4))
                        delay(kotlin.random.Random.nextLong(2000, 5000))
                        
                        // gui.py: if not user_task_id: 重试逻辑
                        if (userTaskId.isNullOrBlank()) {
                            _uiState.update { it.copy(statusText = "⚠️ 任务完成接口返回为空，尝试从获取任务接口重试...") }
                            delay(kotlin.random.Random.nextLong(2000, 5000))
                            userTaskId = taskRepository.getTask(cookies, taskCode)
                        }
                        
                        if (!userTaskId.isNullOrBlank()) {
                            // gui.py: result_obj["logs"].append("5. 领取奖励...")
                            logs.add("5. 领取奖励...")
                            _uiState.update { it.copy(statusText = "5. 领取奖励...") }
                            delay(kotlin.random.Random.nextLong(2000, 5000))
                            
                            // gui.py: rnl.receive_award(user_task_id=user_task_id)
                            val awardResult = taskRepository.receiveAwardWithDetails(cookies, userTaskId)
                            if (awardResult.first) {
                                logs.add("✅ 奖励领取成功")
                                _uiState.update { it.copy(statusText = "✅ 奖励领取成功") }
                            } else {
                                val errorMsg = awardResult.second
                                logs.add("⚠️ 领取奖励时可能出现问题: $errorMsg")
                                _uiState.update { it.copy(statusText = "⚠️ 领取奖励时可能出现问题: $errorMsg") }
                            }
                        } else {
                            logs.add("❌ 未能获取user_task_id，无法领取本轮奖励")
                            _uiState.update { it.copy(statusText = "❌ 未能获取user_task_id，无法领取本轮奖励") }
                        }
                        
                        // gui.py: time.sleep(random.randint(2, 4))
                        delay(kotlin.random.Random.nextLong(2000, 5000))
                    }
                }
                
                if (taskSuccess) {
                    // gui.py: result_obj["logs"].append("\n6. 刷新最终数据...")
                    logs.add("\n6. 刷新最终数据...")
                    _uiState.update { it.copy(statusText = "\n6. 刷新最终数据...") }
                    delay(1000)
                    
                    // 重新查询用户信息和记录
                    val finalUserInfo = taskRepository.queryUserInfoAndRecords(cookies)
                    if (finalUserInfo != null) {
                        val (finalTotalDays, finalTodayRecords) = finalUserInfo
                        logs.add("✅ 任务执行完成！最终可兑换视频天数: $finalTotalDays")
                        _uiState.update { it.copy(statusText = "✅ 任务执行完成！最终可兑换视频天数: $finalTotalDays") }
                        delay(2000)
                        
                        // gui.py: 添加今日记录到日志
                        if (finalTodayRecords.isNotEmpty()) {
                            logs.add("\n📅 今日新增奖励记录:")
                            _uiState.update { it.copy(statusText = "\n📅 今日新增奖励记录:") }
                            delay(1000)
                            for (record in finalTodayRecords) {
                                val recordTime = record["createTime"] as String
                                val value = record["value"] as Int
                                val days = value / 100.0
                                logs.add("| ⏰ $recordTime")
                                logs.add("| 🎁 领到视频会员，+${String.format("%.2f", days)}天")
                                _uiState.update { it.copy(statusText = "| ⏰ $recordTime") }
                                delay(500)
                                _uiState.update { it.copy(statusText = "| 🎁 领到视频会员，+${String.format("%.2f", days)}天") }
                                delay(500)
                            }
                        } else {
                            logs.add("\n📅 今日暂无新增奖励记录")
                            _uiState.update { it.copy(statusText = "\n📅 今日暂无新增奖励记录") }
                            delay(1000)
                        }
                        
                        // gui.py: result_obj["logs"].append("\n7. 主页任务完成，如需兑换会员请使用专门的兑换功能")
                        logs.add("\n7. 主页任务完成，如需兑换会员请使用专门的兑换功能")
                        _uiState.update { it.copy(statusText = "\n7. 主页任务完成，如需兑换会员请使用专门的兑换功能") }
                        delay(2000)
                    }
                }
                
                success = true
                return success
            }
            
        } catch (e: Exception) {
            errorMessage = "任务执行异常: ${e.message}"
            logs.add("❌ $errorMessage")
            _uiState.update { it.copy(statusText = "❌ ${account.us}: $errorMessage") }
            e.printStackTrace()
            delay(2000)
            return false
        } finally {
            // 记录任务执行结果到日志系统
            val endTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            logs.add("\n任务执行结束时间: $endTime")
            
            ResultManager.addTaskResult(
                accountName = account.us,
                success = success,
                errorMessage = errorMessage,
                logs = logs,
                exchangeResults = emptyList(), // 主页任务不包含兑换结果
                startTime = startTime,
                endTime = endTime
            )
        }
    }

    fun startAutoRunCountdown() {
        if (!_uiState.value.licenseVerified) {
            _uiState.update { it.copy(statusText = "❌ 请先验证授权码才能执行任务") }
            return
        }

        if (_uiState.value.isCountingDown) {
            return // Already counting down
        }

        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isCountingDown = true, 
                    countdownSeconds = 10,
                    showCancelButton = true,
                    statusText = "⏰ 倒计时中..."
                ) 
            }

            for (i in 10 downTo 1) {
                _uiState.update { it.copy(countdownSeconds = i) }
                delay(1000)
            }

            _uiState.update { 
                it.copy(
                    isCountingDown = false, 
                    countdownSeconds = 0,
                    showCancelButton = false
                ) 
            }

            runAllTasks()
        }
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.update { 
            it.copy(
                isCountingDown = false, 
                countdownSeconds = 0,
                showCancelButton = false,
                statusText = "❌ 倒计时已取消"
            ) 
        }
    }

    fun cancelAutoRun() {
        cancelCountdown()
    }

    fun onLicenseKeyChange(key: String) {
        _uiState.update { it.copy(licenseKey = key) }
    }

    fun onPushPlusTokenChange(token: String) {
        _uiState.update { it.copy(pushPlusToken = token) }
    }

    fun onAutoRunChange(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.saveAutoRunEnabled(enabled)
        }
    }

    fun saveSettings() {
        viewModelScope.launch {
            settingsRepository.saveLicenseKey(_uiState.value.licenseKey)
            settingsRepository.savePushPlusToken(_uiState.value.pushPlusToken)
        }
    }

}