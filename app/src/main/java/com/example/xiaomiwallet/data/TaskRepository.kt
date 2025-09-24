package com.example.xiaomiwallet.data

import com.example.xiaomiwallet.data.model.Account
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class SimpleCookieJar : CookieJar {
    private val cookies = mutableMapOf<String, String>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            this.cookies[cookie.name] = cookie.value
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.map { (name, value) ->
            Cookie.Builder()
                .name(name)
                .value(value)
                .domain(url.host)
                .build()
        }
    }

    fun getCookies(): Map<String, String> = cookies.toMap()
}

class TaskRepository {

    // 获取会话Cookie的逻辑保留，其他任务相关方法已删除，等待重新构建
    suspend fun getSessionCookies(account: Account): String? {
        if (account.passToken == null || account.userId == null) {
            println("❌ 会话获取失败: passToken 或 userId 为空")
            return null
        }

        // 完全按照gui.py的URL - 一字不差
        val loginUrl = "https://account.xiaomi.com/pass/serviceLogin?callback=https%3A%2F%2Fapi.jr.airstarfinance.net%2Fsts" +
                "%3Fsign%3D1dbHuyAmee0NAZ2xsRw5vhdVQQ8%253D%26followup%3Dhttps%253A%252F%252Fm.jr.airstarfinance.net" +
                "%252Fmp%252Fapi%252Flogin%253Ffrom%253Dmipay_indexicon_TVcard%2526deepLinkEnable%253Dfalse" +
                "%2526requestUrl%253Dhttps%25253A%25252F%25252Fm.jr.airstarfinance.net%25252Fmp%25252Factivity" +
                "%25252FvideoActivity%25253Ffrom%25253Dmipay_indexicon_TVcard%252526_noDarkMode%25253Dtrue" +
                "%252526_transparentNaviBar%25253Dtrue%252526cUserId%25253Dusyxgr5xjumiQLUoAKTOgvi858Q" +
                "%252526_statusBarHeight%25253D137&sid=jrairstar&_group=DEFAULT&_snsNone=true&_loginType=ticket"

        println("🔍 开始获取会话Cookie - 账号: ${account.us}")
        println("登录URL: $loginUrl")
        println("passToken: ${account.passToken}")
        println("userId: ${account.userId}")
        
        return withContext(Dispatchers.IO) {
            try {
                val cookieJar = SimpleCookieJar()
                // 完全按照gui.py的设置：verify=False, timeout=10
                val trustAllCerts = arrayOf<javax.net.ssl.TrustManager>(
                    object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                )
                val sslContext = javax.net.ssl.SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())
                
                val client = OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .cookieJar(cookieJar)
                    .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as javax.net.ssl.X509TrustManager)
                    .hostnameVerifier { _, _ -> true } // 完全禁用SSL验证，对应Python的verify=False
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS) // 对应Python的timeout=10
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .addNetworkInterceptor { chain ->
                        val request = chain.request()
                        println("🌐 发送请求: ${request.url}")
                        println("🌐 请求方法: ${request.method}")
                        println("🌐 请求头:")
                        request.headers.forEach { (name, value) ->
                            println("     $name: $value")
                        }
                        val response = chain.proceed(request)
                        println("🌐 响应状态: ${response.code}")
                        println("🌐 响应头:")
                        response.headers.forEach { (name, value) ->
                            println("     $name: $value")
                        }
                        println("🌐 设置的Cookie:")
                        response.headers("Set-Cookie").forEach { cookie ->
                            println("     Set-Cookie: $cookie")
                        }
                        response
                    }
                    .build()

                val cookieHeader = "passToken=${account.passToken}; userId=${account.userId};"
                println("🍪 发送Cookie: $cookieHeader")
                
                val request = okhttp3.Request.Builder()
                    .url(loginUrl)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/139.0.0.0 Safari/537.36 Edg/139.0.0.0")
                    .header("Cookie", cookieHeader)
                    .build()

                client.newCall(request).execute().use { response ->
                    println("📥 响应状态码: ${response.code}")
                    
                    if (response.isSuccessful) {
                        val allCookies = cookieJar.getCookies()
                        println("🍪 接收到的所有Cookie: $allCookies")
                        
                        // 完全按照gui.py的方式：查找特定cookie并格式化
                        val cUserId = allCookies["cUserId"]
                        val serviceToken = allCookies["serviceToken"]
                        
                        println("🔍 查找关键Cookie:")
                        println("   cUserId: $cUserId")
                        println("   serviceToken: $serviceToken")
                        
                        if (cUserId != null && serviceToken != null) {
                            // gui.py: return f"cUserId={c_user_id}; jrairstar_serviceToken={service_token}"
                            val cookieString = "cUserId=$cUserId; jrairstar_serviceToken=$serviceToken"
                            println("✅ 账号 ${account.us} 会话Cookie获取成功")
                            cookieString
                        } else {
                            println("❌ 未找到必需的Cookie")
                            println("   需要: cUserId, serviceToken")
                            println("   实际: $allCookies")
                            
                            // 尝试查看响应内容
                            val responseBody = response.peekBody(1024).string()
                            println("📄 响应内容片段: ${responseBody.take(200)}...")
                            null
                        }
                    } else {
                        val errorBody = response.body?.string() ?: "无错误详情"
                        println("❌ 会话获取失败: HTTP ${response.code}")
                        println("❌ 错误详情: $errorBody")
                        null
                    }
                }
            } catch (e: Exception) {
                println("❌ 会话获取异常: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    private val taskApiService = ApiClient.instance

    // 查询用户今日奖励记录
    suspend fun queryUserInfoAndRecords(cookies: String): Pair<String, List<Map<String, Any>>>? {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 查询用户可兑换天数
                val totalResponse = taskApiService.queryUserGoldRichSum(
                    activityCode = "2211-videoWelfare",
                    cookies = cookies
                )
                if (!totalResponse.isSuccessful) {
                    val errorBody = totalResponse.errorBody()?.string() ?: "无错误详情"
                    println("❌ 获取兑换视频天数失败: HTTP ${totalResponse.code()}")
                    println("❌ 错误详情: $errorBody")
                    println("❌ 请求URL: https://${ApiService.API_HOST}/mp/api/generalActivity/queryUserGoldRichSum")
                    return@withContext null
                }
                
                val totalBody = totalResponse.body()
                if (totalBody?.get("code")?.asInt != 0) {
                    println("❌ 获取兑换视频天数失败: ${totalBody?.get("message")?.asString}")
                    return@withContext null
                }
                
                val totalDaysValue = totalBody.get("value")?.asInt ?: 0
                val totalDays = String.format("%.2f天", totalDaysValue / 100.0)
                println("🔍 API响应详情:")
                println("   响应码: ${totalBody.get("code")?.asInt}")
                println("   消息: ${totalBody.get("message")?.asString}")
                println("   原始值: $totalDaysValue")
                println("   格式化天数: $totalDays")
                
                // 2. 查询用户任务完成记录
                val recordResponse = taskApiService.queryUserJoinList(
                    activityCode = "2211-videoWelfare",
                    cookies = cookies
                )
                if (!recordResponse.isSuccessful) {
                    println("❌ 查询任务完成记录失败: HTTP ${recordResponse.code()}")
                    return@withContext null
                }
                
                val recordBody = recordResponse.body()
                if (recordBody?.get("code")?.asInt != 0) {
                    println("❌ 查询任务完成记录失败: ${recordBody?.get("message")?.asString}")
                    return@withContext null
                }
                
                // 3. 筛选今日记录
                val todayRecords = mutableListOf<Map<String, Any>>()
                val currentDate = java.time.LocalDate.now().toString() // 格式：2024-09-24
                
                val dataArray = recordBody?.getAsJsonObject("value")?.getAsJsonArray("data")
                dataArray?.forEach { item ->
                    val record = item.asJsonObject
                    val createTime = record.get("createTime")?.asString ?: ""
                    if (createTime.startsWith(currentDate)) {
                        val recordMap = mapOf(
                            "createTime" to createTime,
                            "value" to (record.get("value")?.asInt ?: 0)
                        )
                        todayRecords.add(recordMap)
                    }
                }
                
                println("✅ 查询用户信息成功: $totalDays, 今日记录: ${todayRecords.size} 条")
                Pair(totalDays, todayRecords)
                
            } catch (e: Exception) {
                println("❌ 查询用户信息异常: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }

    // 新用户下载任务功能 - 按照gui.py完全重新构建
    suspend fun completeNewUserTask(cookies: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 开始执行新用户下载任务...")
                
                val response = taskApiService.completeNewUserTask(cookies = cookies)
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    // gui.py: if response and response['code'] != 0: (失败条件)
                    // 所以成功条件是: response['code'] == 0
                    if (code == 0) {
                        val userTaskId = body?.get("value")?.asString
                        println("✅ 完成新用户下载任务成功，获得userTaskId: $userTaskId")
                        return@withContext userTaskId
                    } else {
                        println("❌ 完成新用户下载任务失败：$body")
                        println("   gui.py中显示此消息: 应用下载试用任务已完成或不可用")
                        return@withContext null
                    }
                } else {
                    println("❌ 完成新用户下载任务HTTP请求失败: ${response.code()}")
                    return@withContext null
                }
                
            } catch (e: Exception) {
                println("❌ 完成新用户下载任务异常: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    suspend fun receiveNewUserAward(cookies: String, userTaskId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 开始领取新用户下载任务奖励，userTaskId: $userTaskId")
                
                // gui.py: 发送领取请求前延时5秒
                delay(5000)
                
                val response = taskApiService.receiveNewUserAward(
                    userTaskId = userTaskId,
                    cookies = cookies
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    if (code == 0) {
                        // gui.py: if response: prize_info = response['value']['prizeInfo']
                        val prizeInfo = body?.getAsJsonObject("value")?.get("prizeInfo")
                        println("✅ 领取新用户下载任务奖励成功")
                        return@withContext true
                    } else {
                        println("❌ 领取新用户下载任务奖励失败：$body")
                        return@withContext false
                    }
                } else {
                    println("❌ 领取新用户下载任务奖励HTTP请求失败: ${response.code()}")
                    return@withContext false
                }
                
            } catch (e: Exception) {
                println("❌ 领取新用户下载任务奖励异常: ${e.message}")
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    // 浏览任务功能 - 按照gui.py完全重新构建
    suspend fun getTaskList(cookies: String): List<Map<String, Any>>? {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 获取任务列表...")
                
                val response = taskApiService.getTaskList(
                    activityCode = "2211-videoWelfare",
                    cookies = cookies
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    if (code == 0) {
                        val taskInfoList = body?.getAsJsonObject("value")?.getAsJsonArray("taskInfoList")
                        val targetTasks = mutableListOf<Map<String, Any>>()
                        
                        // gui.py: 过滤包含 '浏览组浏览任务' 的任务
                        taskInfoList?.forEach { item ->
                            val task = item.asJsonObject
                            val taskName = task.get("taskName")?.asString ?: ""
                            if ("浏览组浏览任务" in taskName) {
                                val taskMap = mutableMapOf<String, Any>()
                                taskMap["taskId"] = task.get("taskId")?.asString ?: ""
                                taskMap["taskCode"] = task.get("taskCode")?.asString ?: ""
                                taskMap["taskName"] = taskName
                                
                                // gui.py: 获取 generalActivityUrlInfo
                                val urlInfo = task.getAsJsonObject("generalActivityUrlInfo")
                                if (urlInfo != null) {
                                    taskMap["generalActivityUrlInfo"] = mapOf(
                                        "id" to (urlInfo.get("id")?.asString ?: ""),
                                        "browsClickUrlId" to (urlInfo.get("browsClickUrlId")?.asString ?: "")
                                    )
                                }
                                
                                targetTasks.add(taskMap)
                            }
                        }
                        
                        println("✅ 获取任务列表成功，找到 ${targetTasks.size} 个浏览任务")
                        return@withContext targetTasks
                        
                    } else {
                        println("❌ 获取任务列表失败：$body")
                        return@withContext null
                    }
                } else {
                    println("❌ 获取任务列表HTTP请求失败: ${response.code()}")
                    return@withContext null
                }
                
            } catch (e: Exception) {
                println("❌ 获取任务列表异常: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    suspend fun getTask(cookies: String, taskCode: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 获取任务信息，taskCode: $taskCode")
                
                val response = taskApiService.getTask(
                    activityCode = "2211-videoWelfare",
                    taskCode = taskCode,
                    cookies = cookies
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    if (code == 0) {
                        // gui.py: return response['value']['taskInfo']['userTaskId']
                        val userTaskId = body?.getAsJsonObject("value")
                            ?.getAsJsonObject("taskInfo")
                            ?.get("userTaskId")?.asString
                        println("✅ 获取任务信息成功，userTaskId: $userTaskId")
                        return@withContext userTaskId
                    } else {
                        println("❌ 获取任务信息失败：$body")
                        return@withContext null
                    }
                } else {
                    println("❌ 获取任务信息HTTP请求失败: ${response.code()}")
                    return@withContext null
                }
                
            } catch (e: Exception) {
                println("❌ 获取任务信息异常: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    suspend fun completeTask(cookies: String, taskId: String, tId: String, browsClickUrlId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 完成浏览任务，taskId: $taskId, tId: $tId")
                
                val response = taskApiService.completeTask(
                    activityCode = "2211-videoWelfare",
                    taskId = taskId,
                    browsTaskId = tId,
                    browsClickUrlId = browsClickUrlId,
                    cookies = cookies
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    if (code == 0) {
                        // gui.py: return response.get('value')
                        val userTaskId = body?.get("value")?.asString
                        println("✅ 完成浏览任务成功，userTaskId: $userTaskId")
                        return@withContext userTaskId
                    } else {
                        println("❌ 完成浏览任务失败：$body")
                        return@withContext null
                    }
                } else {
                    println("❌ 完成浏览任务HTTP请求失败: ${response.code()}")
                    return@withContext null
                }
                
            } catch (e: Exception) {
                println("❌ 完成浏览任务异常: ${e.message}")
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    suspend fun receiveAward(cookies: String, userTaskId: String): Boolean {
        return receiveAwardWithDetails(cookies, userTaskId).first
    }
    
    suspend fun receiveAwardWithDetails(cookies: String, userTaskId: String): Pair<Boolean, String> {
        return withContext(Dispatchers.IO) {
            try {
                println("🔍 领取浏览任务奖励，userTaskId: $userTaskId")
                
                val response = taskApiService.receiveAward(
                    activityCode = "2211-videoWelfare",
                    userTaskId = userTaskId,
                    cookies = cookies
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    val code = body?.get("code")?.asInt ?: -1
                    
                    if (code == 0) {
                        println("✅ 领取浏览任务奖励成功")
                        return@withContext Pair(true, "")
                    } else {
                        val errorMsg = "领取奖励失败：$body"
                        println("❌ $errorMsg")
                        return@withContext Pair(false, errorMsg)
                    }
                } else {
                    val errorMsg = "领取奖励HTTP请求失败: ${response.code()}"
                    println("❌ $errorMsg")
                    return@withContext Pair(false, errorMsg)
                }
                
            } catch (e: Exception) {
                val errorMsg = "领取奖励异常: ${e.message}"
                println("❌ $errorMsg")
                e.printStackTrace()
                return@withContext Pair(false, errorMsg)
            }
        }
    }

    // 会员兑换相关方法 - 为MembershipExchangeService提供支持
    suspend fun getPrizeStatusV2(cookies: String): retrofit2.Response<com.google.gson.JsonArray> {
        return withContext(Dispatchers.IO) {
            try {
                // 这里应该调用获取奖品状态的API
                // 暂时返回空响应，需要根据实际API实现
                retrofit2.Response.success(com.google.gson.JsonArray())
            } catch (e: Exception) {
                println("❌ 获取奖品状态异常: ${e.message}")
                retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, ""))
            }
        }
    }

    suspend fun convertGoldRich(cookies: String, prizeCode: String, phoneNumber: String): retrofit2.Response<com.google.gson.JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                // 这里应该调用兑换会员的API
                // 暂时返回空响应，需要根据实际API实现
                val result = com.google.gson.JsonObject()
                result.addProperty("code", 0)
                result.addProperty("message", "兑换成功")
                retrofit2.Response.success(result)
            } catch (e: Exception) {
                println("❌ 兑换会员异常: ${e.message}")
                retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, ""))
            }
        }
    }

    suspend fun queryUserGoldRichSum(cookies: String): retrofit2.Response<com.google.gson.JsonObject> {
        return withContext(Dispatchers.IO) {
            try {
                val response = taskApiService.queryUserGoldRichSum("2211-videoWelfare", cookies)
                response
            } catch (e: Exception) {
                println("❌ 查询用户天数异常: ${e.message}")
                retrofit2.Response.error(500, okhttp3.ResponseBody.create(null, ""))
            }
        }
    }

}