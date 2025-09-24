package com.example.xiaomiwallet.data.model

data class ExchangeResult(
    val membershipType: String,
    val phoneNumber: String,
    val success: Boolean,
    val message: String
)

data class Membership(
    val id: String,
    val prizeId: String,
    val name: String,
    val description: String,
    val costDays: Double,
    val exchangeType: String, // "direct" or "privilege"
    val status: String, // "available" or "out_of_stock"
    val stock: Int,
    val brand: String,
    val needGoldRice: Int,
    val prizeBatchId: String = "",
    val prizeType: Int = 26
)

object PredefinedMemberships {
    fun getDefault(): List<Membership> = listOf(
        Membership(
            id = "tencent_video_month",
            prizeId = "tencent_month",
            name = "腾讯视频VIP月卡",
            description = "腾讯视频VIP月卡",
            costDays = 31.0,
            exchangeType = "direct",
            status = "available",
            stock = 999,
            brand = "tencent",
            needGoldRice = 3100
        ),
        Membership(
            id = "iqiyi_video_month",
            prizeId = "iqiyi_month",
            name = "爱奇艺黄金VIP月卡",
            description = "爱奇艺黄金VIP月卡",
            costDays = 31.0,
            exchangeType = "direct",
            status = "available",
            stock = 999,
            brand = "iqiyi",
            needGoldRice = 3100
        ),
        Membership(
            id = "youku_video_month",
            prizeId = "youku_month",
            name = "优酷VIP月卡",
            description = "优酷VIP月卡",
            costDays = 31.0,
            exchangeType = "direct",
            status = "available",
            stock = 999,
            brand = "youku",
            needGoldRice = 3100
        ),
        Membership(
            id = "mgtv_video_month",
            prizeId = "mgtv_month",
            name = "芒果TV会员月卡",
            description = "芒果TV会员月卡",
            costDays = 31.0,
            exchangeType = "direct",
            status = "available",
            stock = 999,
            brand = "mgtv",
            needGoldRice = 3100
        ),
        Membership(
            id = "bilibili_video_month",
            prizeId = "bilibili_month",
            name = "哔哩哔哩大会员月卡",
            description = "哔哩哔哩大会员月卡",
            costDays = 31.0,
            exchangeType = "direct",
            status = "available",
            stock = 999,
            brand = "bilibili",
            needGoldRice = 3100
        )
    )
    
    fun findByType(membershipType: String): List<Membership> {
        return getDefault().filter { membership ->
            val typeMatch = membershipType.lowercase() in membership.name.lowercase() ||
                           membershipType.lowercase() in membership.brand.lowercase() ||
                           membership.brand.lowercase() in membershipType.lowercase()
            typeMatch
        }
    }
}
