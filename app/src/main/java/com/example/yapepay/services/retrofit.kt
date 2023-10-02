package com.example.yapepay.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiRetrofit {

        @POST("/wp-admin/admin-ajax.php?action=yapepayments")
        fun sendData(@Body requestBody: RequestBody): Call<ResponseBody>

}
data class RequestBody(
        val title:String,
        val yapero:String,
        val monto:String
)
data class ResponseBody(

        val message: String )