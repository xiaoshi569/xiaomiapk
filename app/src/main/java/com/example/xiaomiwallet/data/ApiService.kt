package com.example.xiaomiwallet.data

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    companion object {
        const val API_HOST = "m.jr.airstarfinance.net"
        const val USER_AGENT_MOBILE =
            "Mozilla/5.0 (Linux; U; Android 14; zh-CN; M2012K11AC Build/UKQ1.230804.001; " +
            "AppBundle/com.mipay.wallet; AppVersionName/6.89.1.5275.2323; AppVersionCode/20577595; " +
            "MiuiVersion/stable-V816.0.13.0.UMNCNXM; DeviceId/alioth; NetworkType/WIFI; " +
            "mix_version; WebViewVersion/118.0.0.0) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Version/4.0 Mobile Safari/537.36 XiaoMi/MiuiBrowser/4.3"
    }

    // 查询用户信息和记录的接口
    @GET("mp/api/generalActivity/queryUserGoldRichSum")
    suspend fun queryUserGoldRichSum(
        @Query("activityCode") activityCode: String,
        @Header("Cookie") cookies: String,
        @Query("app") app: String = "com.mipay.wallet",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "{\"platformType\":1,\"com.miui.player\":\"4.27.0.4\",\"com.miui.video\":\"v2024090290(MiVideo-UN)\",\"com.mipay.wallet\":\"6.83.0.5175.2256\"}"
    ): Response<JsonObject>

    @GET("mp/api/generalActivity/queryUserJoinList")
    suspend fun queryUserJoinList(
        @Query("activityCode") activityCode: String,
        @Query("pageNum") pageNum: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Header("Cookie") cookies: String,
        @Query("app") app: String = "com.mipay.wallet",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "{\"platformType\":1,\"com.miui.player\":\"4.27.0.4\",\"com.miui.video\":\"v2024090290(MiVideo-UN)\",\"com.mipay.wallet\":\"6.83.0.5175.2256\"}"
    ): Response<JsonObject>

    // 新用户下载任务相关接口 - 完全按照gui.py的URL重新构建
    @GET("mp/api/generalActivity/completeTask")
    suspend fun completeNewUserTask(
        @Query("activityCode") activityCode: String = "2211-videoWelfare",
        @Query("app") app: String = "com.mipay.wallet",
        @Query("oaid") oaid: String = "8c45c5802867e923",
        @Query("regId") regId: String = "KWkK5VsKXiIbAH8Rf6kgU6tpDPyNWgXY8YCM1mQtt5nd7i1/4BqzPq0uY7OlIEOd",
        @Query("versionCode") versionCode: String = "20577622",
        @Query("versionName") versionName: String = "6.96.0.5453.2620",
        @Query("isNfcPhone") isNfcPhone: String = "true",
        @Query("channel") channel: String = "mipay_indexicon_TVcard2test",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "{\"platformType\":1,\"com.miui.video\":\"v2023091090(MiVideo-ROM)\",\"com.mipay.wallet\":\"6.96.0.5453.2620\"}",
        @Query("taskCode") taskCode: String = "NEW_USER_CAMPAIGN",
        @Query("browsTaskId") browsTaskId: String = "",
        @Query("browsClickUrlId") browsClickUrlId: String = "1306285",
        @Query("adInfoId") adInfoId: String = "",
        @Query("triggerId") triggerId: String = "",
        @Header("Cookie") cookies: String,
        @Header("Connection") connection: String = "keep-alive",
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Cache-Control") cacheControl: String = "no-cache",
        @Header("X-Request-ID") xRequestId: String = "1281eea0-e268-4fcc-9a5f-7dc11475b7db",
        @Header("X-Requested-With") xRequestedWith: String = "com.mipay.wallet",
        @Header("Sec-Fetch-Site") secFetchSite: String = "same-origin",
        @Header("Sec-Fetch-Mode") secFetchMode: String = "cors",
        @Header("Sec-Fetch-Dest") secFetchDest: String = "empty",
        @Header("Accept-Encoding") acceptEncoding: String = "gzip, deflate",
        @Header("Accept-Language") acceptLanguage: String = "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"
    ): Response<JsonObject>

    @GET("mp/api/generalActivity/luckDraw")
    suspend fun receiveNewUserAward(
        @Query("imei") imei: String = "",
        @Query("device") device: String = "alioth",
        @Query("appLimit") appLimit: String = "%7B%22com.qiyi.video%22:false,%22com.youku.phone%22:false,%22com.tencent.qqlive%22:false,%22com.hunantv.imgo.activity%22:false,%22com.cmcc.cmvideo%22:false,%22com.sankuai.meituan%22:false,%22com.anjuke.android.app%22:false,%22com.tal.abctimelibrary%22:false,%22com.lianjia.beike%22:false,%22com.kmxs.reader%22:false,%22com.jd.jrapp%22:false,%22com.smile.gifmaker%22:true,%22com.kuaishou.nebula%22:false%7D",
        @Query("activityCode") activityCode: String = "2211-videoWelfare",
        @Query("userTaskId") userTaskId: String,
        @Query("app") app: String = "com.mipay.wallet",
        @Query("oaid") oaid: String = "8c45c5802867e923",
        @Query("regId") regId: String = "L522i5qLZR9%2Bs25kEqPBJYbbHqUS4LrpuTsgl9kdsbcyU7tjWmx1BewlRNSSZaOT",
        @Query("versionCode") versionCode: String = "20577622",
        @Query("versionName") versionName: String = "6.96.0.5453.2620",
        @Query("isNfcPhone") isNfcPhone: String = "true",
        @Query("channel") channel: String = "mipay_indexicon_TVcard2test",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "%7B%22platformType%22:1,%22com.miui.video%22:%22v2023091090(MiVideo-ROM)%22,%22com.mipay.wallet%22:%226.96.0.5453.2620%22%7D",
        @Header("Cookie") cookies: String,
        @Header("Connection") connection: String = "keep-alive",
        @Header("sec-ch-ua") secChUa: String = "\"Chromium\";v=\"118\", \"Android WebView\";v=\"118\", \"Not=A?Brand\";v=\"99\"",
        @Header("Accept") accept: String = "application/json, text/plain, */*",
        @Header("Cache-Control") cacheControl: String = "no-cache",
        @Header("sec-ch-ua-mobile") secChUaMobile: String = "?1",
        @Header("X-Request-ID") xRequestId: String = "c09abfa7-6ea4-4435-a741-dff3622215cf",
        @Header("sec-ch-ua-platform") secChUaPlatform: String = "\"Android\"",
        @Header("X-Requested-With") xRequestedWith: String = "com.mipay.wallet",
        @Header("Sec-Fetch-Site") secFetchSite: String = "same-origin",
        @Header("Sec-Fetch-Mode") secFetchMode: String = "cors",
        @Header("Sec-Fetch-Dest") secFetchDest: String = "empty",
        @Header("Accept-Encoding") acceptEncoding: String = "gzip, deflate, br",
        @Header("Accept-Language") acceptLanguage: String = "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7"
    ): Response<JsonObject>

    // 浏览任务相关接口 - 按照gui.py重新构建
    @FormUrlEncoded
    @POST("mp/api/generalActivity/getTaskList")
    suspend fun getTaskList(
        @Field("activityCode") activityCode: String,
        @Header("Cookie") cookies: String
    ): Response<JsonObject>

    @FormUrlEncoded
    @POST("mp/api/generalActivity/getTask")
    suspend fun getTask(
        @Field("activityCode") activityCode: String,
        @Field("taskCode") taskCode: String,
        @Field("jrairstar_ph") jrairstarPh: String = "98lj8puDf9Tu/WwcyMpVyQ==",
        @Header("Cookie") cookies: String
    ): Response<JsonObject>

    @GET("mp/api/generalActivity/completeTask")
    suspend fun completeTask(
        @Query("activityCode") activityCode: String,
        @Query("app") app: String = "com.mipay.wallet",
        @Query("isNfcPhone") isNfcPhone: String = "true",
        @Query("channel") channel: String = "mipay_indexicon_TVcard",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "{\"platformType\":1,\"com.miui.player\":\"4.27.0.4\",\"com.miui.video\":\"v2024090290(MiVideo-UN)\",\"com.mipay.wallet\":\"6.83.0.5175.2256\"}",
        @Query("taskId") taskId: String,
        @Query("browsTaskId") browsTaskId: String,
        @Query("browsClickUrlId") browsClickUrlId: String,
        @Query("clickEntryType") clickEntryType: String = "undefined",
        @Query("festivalStatus") festivalStatus: String = "0",
        @Header("Cookie") cookies: String
    ): Response<JsonObject>

    @GET("mp/api/generalActivity/luckDraw")
    suspend fun receiveAward(
        @Query("imei") imei: String = "",
        @Query("device") device: String = "manet",
        @Query("appLimit") appLimit: String = "{\"com.qiyi.video\":false,\"com.youku.phone\":true,\"com.tencent.qqlive\":true,\"com.hunantv.imgo.activity\":true,\"com.cmcc.cmvideo\":false,\"com.sankuai.meituan\":true,\"com.anjuke.android.app\":false,\"com.tal.abctimelibrary\":false,\"com.lianjia.beike\":false,\"com.kmxs.reader\":true,\"com.jd.jrapp\":false,\"com.smile.gifmaker\":true,\"com.kuaishou.nebula\":false}",
        @Query("activityCode") activityCode: String,
        @Query("userTaskId") userTaskId: String,
        @Query("app") app: String = "com.mipay.wallet",
        @Query("isNfcPhone") isNfcPhone: String = "true",
        @Query("channel") channel: String = "mipay_indexicon_TVcard",
        @Query("deviceType") deviceType: String = "2",
        @Query("system") system: String = "1",
        @Query("visitEnvironment") visitEnvironment: String = "2",
        @Query("userExtra") userExtra: String = "{\"platformType\":1,\"com.miui.player\":\"4.27.0.4\",\"com.miui.video\":\"v2024090290(MiVideo-UN)\",\"com.mipay.wallet\":\"6.83.0.5175.2256\"}",
        @Header("Cookie") cookies: String
    ): Response<JsonObject>

}

interface PushPlusApiService {
    @POST("send")
    suspend fun sendNotification(@Body body: JsonObject): Response<JsonObject>
}