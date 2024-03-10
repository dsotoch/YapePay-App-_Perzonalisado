package com.example.yapepay.services

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface ApiRetrofit {
        @POST
        fun sendData(
                @Url url: String,
                @Header("apikey") headerValue: String,
                @Body requestBody: RequestBody
        ): Call<Void>
}

data class RequestBody(
        val medio_pago:String,
        val nombre:String,
        val amount:String
)
