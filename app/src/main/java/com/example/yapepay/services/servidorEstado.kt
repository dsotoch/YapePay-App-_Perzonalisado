package com.example.yapepay.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface servidorEstado  {
    @POST("rest/v1/estado_cola")

    fun enviarData(
        @Header("apikey") headerValue: String,
        @Body requestBody: dataBody
    ): Call<Void>
}
data class dataBody(
    val rojo: Int? = null,
    val verde: Int? = null,
    val naranja: Int? = null
    )
