package com.example.xiaomiwallet.data

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.*

interface AuthApiService {
    // 完全按照gui.py的方式：POST请求，JSON数据，参数名为"key"和"device_id"
    @POST("verify")
    @Headers("Content-Type: application/json")
    suspend fun verifyLicense(@Body body: JsonObject): Response<JsonObject>
}
