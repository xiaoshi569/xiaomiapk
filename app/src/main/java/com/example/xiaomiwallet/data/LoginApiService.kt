package com.example.xiaomiwallet.data

import com.google.gson.JsonObject
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.QueryMap
import retrofit2.http.Url

interface LoginApiService {
    @GET("longPolling/loginUrl")
    suspend fun getLoginQr(@QueryMap params: Map<String, String>): Response<ResponseBody>

    @GET
    suspend fun checkLoginStatus(@Url url: String): Response<ResponseBody>
}
