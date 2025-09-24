package com.example.xiaomiwallet.data

import com.example.xiaomiwallet.data.model.Account
import com.example.xiaomiwallet.data.model.ExchangeResult
import com.example.xiaomiwallet.data.model.Membership
import com.example.xiaomiwallet.data.model.PredefinedMemberships
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.delay

class MembershipExchangeService(
    private val taskRepository: TaskRepository
) {
    
    suspend fun getAvailableMemberships(cookies: String): List<Membership> {
        return try {
            println("🔍 获取可兑换的会员列表...")
            val response = taskRepository.getPrizeStatusV2(cookies)
            
            if (response.isSuccessful) {
                val memberships = mutableListOf<Membership>()
                val prizeList = response.body()
                
                if (prizeList is JsonArray) {
                    println("📋 API返回奖品数量: ${prizeList.size()}")
                    for (prize in prizeList) {
                        try {
                            val prizeObj = prize.asJsonObject
                            val prizeId = prizeObj.get("prizeId")?.asString ?: continue
                            val prizeName = prizeObj.get("prizeName")?.asString ?: ""
                            val prizeBrand = prizeObj.get("prizeBrand")?.asString ?: ""
                            val needGoldRice = prizeObj.get("needGoldRice")?.asInt ?: 0
                            val prizeCode = prizeObj.get("prizeCode")?.asString ?: ""
                            val stockStatus = prizeObj.get("stockStatus")?.asInt ?: 0
                            val todayStockStatus = prizeObj.get("todayStockStatus")?.asInt ?: 0
                            val prizeType = prizeObj.get("prizeType")?.asInt ?: 0
                            
                            println("🔍 检查奖品: $prizeName")
                            println("   brand: $prizeBrand, stockStatus: $stockStatus, todayStockStatus: $todayStockStatus")
                            println("   prizeType: $prizeType, needGoldRice: $needGoldRice")
                            
                            // 计算消耗天数
                            val costDays = needGoldRice / 100.0
                            
            // 过滤条件：31天月卡、直接兑换、非特权、有基本库存
            val isDirectExchange = prizeType == 26
            val isMonthlyCard = costDays == 31.0
            val isNotPrivilege = !prizeName.contains("1分购") && !prizeName.contains("特权")
            val hasStock = stockStatus == 1
            
            println("   筛选结果: hasStock=$hasStock, isMonthlyCard=$isMonthlyCard, isDirectExchange=$isDirectExchange, isNotPrivilege=$isNotPrivilege")
            
            // 与gui.py一致：先检查基本条件，库存状态在后面设置
            if (hasStock && isMonthlyCard && isDirectExchange && isNotPrivilege) {
                println("   ✅ 添加到兑换列表: $prizeName")
                                val membership = Membership(
                                    id = prizeCode,
                                    prizeId = prizeId,
                                    name = prizeName,
                                    description = prizeObj.get("prizeDesc")?.asString ?: "",
                                    costDays = costDays,
                                    exchangeType = "direct",
                                    status = if (todayStockStatus == 1) "available" else "out_of_stock",
                                    stock = stockStatus,
                                    brand = prizeBrand,
                                    needGoldRice = needGoldRice,
                                    prizeBatchId = prizeObj.get("prizeBatchId")?.asString ?: "",
                                    prizeType = prizeType
                                )
                                memberships.add(membership)
                            } else {
                                println("   ❌ 不符合筛选条件，跳过")
                            }
                        } catch (e: Exception) {
                            println("⚠️ 解析奖品失败: ${e.message}")
                            continue
                        }
                    }
                    
                    if (memberships.isNotEmpty()) {
                        println("📺 API获取到 ${memberships.size} 个可兑换会员")
                        return memberships
                    }
                }
            }
            
            println("📺 API获取失败，使用预定义会员列表")
            return PredefinedMemberships.getDefault()
            
        } catch (e: Exception) {
            println("❌ 获取兑换列表失败：${e.message}")
            return PredefinedMemberships.getDefault()
        }
    }
    
    suspend fun findBestMatch(
        membershipType: String,
        availableMemberships: List<Membership>
    ): Membership? {
        val potentialMatches = mutableListOf<Membership>()
        
        println("🔍 匹配算法开始: '$membershipType'")
        
        // 1. 精确匹配 - 改进匹配逻辑
        availableMemberships.forEach { membership ->
            val exactMatch = when {
                // 直接名称匹配
                membershipType.equals(membership.brand, ignoreCase = true) -> true
                membershipType.contains(membership.brand, ignoreCase = true) -> true
                membership.name.contains(membershipType, ignoreCase = true) -> true
                
                // 中文名称映射匹配
                membershipType.contains("腾讯", ignoreCase = true) && membership.brand.equals("tencent", ignoreCase = true) -> true
                membershipType.contains("爱奇艺", ignoreCase = true) && membership.brand.equals("iqiyi", ignoreCase = true) -> true
                membershipType.contains("优酷", ignoreCase = true) && membership.brand.equals("youku", ignoreCase = true) -> true
                membershipType.contains("芒果", ignoreCase = true) && membership.brand.equals("mgtv", ignoreCase = true) -> true
                membershipType.contains("哔哩哔哩", ignoreCase = true) && membership.brand.equals("bilibili", ignoreCase = true) -> true
                membershipType.contains("B站", ignoreCase = true) && membership.brand.equals("bilibili", ignoreCase = true) -> true
                
                else -> false
            }
            
            if (exactMatch) {
                println("✅ 精确匹配: ${membership.name} (brand: ${membership.brand})")
                potentialMatches.add(membership)
            }
        }
        
        // 2. 如果没有精确匹配，尝试预定义列表匹配
        if (potentialMatches.isEmpty()) {
            println("🔄 尝试预定义列表匹配...")
            val fuzzyMatches = PredefinedMemberships.findByType(membershipType)
            println("📝 预定义匹配找到: ${fuzzyMatches.map { it.name }}")
            
            fuzzyMatches.forEach { predefined ->
                val availableMatch = availableMemberships.find { available -> 
                    available.brand.equals(predefined.brand, ignoreCase = true) 
                }
                if (availableMatch != null) {
                    println("✅ 预定义匹配成功: ${availableMatch.name}")
                    potentialMatches.add(availableMatch)
                }
            }
        }
        
        // 3. 如果还是没有匹配，使用预定义列表作为备用
        if (potentialMatches.isEmpty() && availableMemberships.isEmpty()) {
            println("🔄 使用预定义列表作为备用...")
            val backupMatches = PredefinedMemberships.findByType(membershipType)
            potentialMatches.addAll(backupMatches)
            println("📦 备用匹配: ${backupMatches.map { it.name }}")
        }
        
        // 4. 按优先级排序并返回（与gui.py一致：不过滤status，只排序）
        val result = potentialMatches
            .sortedWith(compareByDescending<Membership> { it.exchangeType == "direct" }
                .thenByDescending { it.status == "available" }  // 有库存的优先
                .thenByDescending { it.stock }
                .thenBy { it.costDays })
            .firstOrNull()
            
        println("🎯 最终选择: ${result?.name ?: "无匹配"}")
        return result
    }
    
    suspend fun exchangeMembership(
        cookies: String,
        membership: Membership,
        phoneNumber: String
    ): ExchangeResult {
        return try {
            println("🔍 尝试兑换 ${membership.name} (PrizeID: ${membership.prizeId})")
            println("📞 手机号: $phoneNumber")
            
            // 首先检查库存状态（与gui.py一致）
            if (membership.status != "available") {
                val errorMessage = "${membership.name} 今日无库存"
                println("❌ $errorMessage")
                return ExchangeResult(
                    membershipType = membership.name,
                    phoneNumber = phoneNumber,
                    success = false,
                    message = errorMessage
                )
            }
            
            val response = taskRepository.convertGoldRich(cookies, membership.id, phoneNumber)
            
            if (response.isSuccessful) {
                val result = response.body()
                if (result?.get("code")?.asInt == 0) {
                    val successMessage = "✅ 成功兑换 ${membership.name}"
                    println(successMessage)
                    ExchangeResult(
                        membershipType = membership.name,
                        phoneNumber = phoneNumber,
                        success = true,
                        message = successMessage
                    )
                } else {
                    val errorMessage = result?.get("message")?.asString ?: "未知错误"
                    println("❌ 兑换失败: $errorMessage")
                    ExchangeResult(
                        membershipType = membership.name,
                        phoneNumber = phoneNumber,
                        success = false,
                        message = "兑换失败: $errorMessage"
                    )
                }
            } else {
                val errorMessage = "网络请求失败: HTTP ${response.code()}"
                println("❌ $errorMessage")
                ExchangeResult(
                    membershipType = membership.name,
                    phoneNumber = phoneNumber,
                    success = false,
                    message = errorMessage
                )
            }
        } catch (e: Exception) {
            val errorMessage = "兑换异常: ${e.message}"
            println("❌ $errorMessage")
            ExchangeResult(
                membershipType = membership.name,
                phoneNumber = phoneNumber,
                success = false,
                message = errorMessage
            )
        }
    }
    
    suspend fun getUserDays(cookies: String): Double {
        return try {
            val response = taskRepository.queryUserGoldRichSum(cookies)
            if (response.isSuccessful) {
                val currentDays = response.body()?.get("value")?.asInt ?: 0
                currentDays / 100.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            println("❌ 查询用户天数失败: ${e.message}")
            0.0
        }
    }
    
    suspend fun performExchangeForAccount(
        account: Account,
        cookies: String
    ): List<ExchangeResult> {
        val results = mutableListOf<ExchangeResult>()
        
        if (account.exchangeConfigs.isEmpty()) {
            println("📺 账号 ${account.us} 未配置会员兑换，跳过")
            return results
        }
        
        println("📱 处理账号: ${account.us}")
        
        // 获取用户当前天数
        val currentDays = getUserDays(cookies)
        println("💰 当前拥有天数：${currentDays}天")
        
        // 获取可兑换的会员列表
        val availableMemberships = getAvailableMemberships(cookies)
        
        // 处理每个兑换配置
        for (config in account.exchangeConfigs) {
            println("🎯 检查 ${config.type} 兑换配置 (手机号: ${config.phone})")
            
            // 查找最佳匹配的会员
            println("🔍 搜索匹配: '${config.type}'")
            println("📋 可用会员列表:")
            availableMemberships.forEach { membership ->
                println("   - ${membership.name} (brand: ${membership.brand}, status: ${membership.status})")
            }
            
            val bestMatch = findBestMatch(config.type, availableMemberships)
            
            if (bestMatch == null) {
                println("❌ 无法匹配 '${config.type}'，尝试预定义列表...")
                val predefinedMatches = PredefinedMemberships.findByType(config.type)
                println("📝 预定义匹配结果: ${predefinedMatches.map { it.name }}")
                
                val errorResult = ExchangeResult(
                    membershipType = config.type,
                    phoneNumber = config.phone,
                    success = false,
                    message = "未找到匹配的会员类型"
                )
                results.add(errorResult)
                println("❌ ${errorResult.message}")
                continue
            }
            
            // 检查天数是否充足
            if (currentDays < bestMatch.costDays) {
                val errorResult = ExchangeResult(
                    membershipType = config.type,
                    phoneNumber = config.phone,
                    success = false,
                    message = "天数不足，需要${bestMatch.costDays}天，当前${currentDays}天"
                )
                results.add(errorResult)
                println("❌ ${errorResult.message}")
                continue
            }
            
            // 检查库存状态
            if (bestMatch.status != "available") {
                val errorResult = ExchangeResult(
                    membershipType = config.type,
                    phoneNumber = config.phone,
                    success = false,
                    message = "${bestMatch.name} 今日无库存"
                )
                results.add(errorResult)
                println("❌ ${errorResult.message}")
                continue
            }
            
            // 执行兑换
            val exchangeResult = exchangeMembership(cookies, bestMatch, config.phone)
            results.add(exchangeResult)
            
            // 如果兑换成功，添加延时避免频繁请求
            if (exchangeResult.success) {
                delay(2000)
            }
        }
        
        return results
    }
}
