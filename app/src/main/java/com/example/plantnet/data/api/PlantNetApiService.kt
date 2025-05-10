package com.example.plantnet.data.api

import com.example.plantnet.data.model.PlantNetResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface PlantNetApiService {
    
    @Multipart
    @POST("v2/identify/all")
    suspend fun identifyPlant(
        @Part images: List<MultipartBody.Part>,
        @Query("api-key") apiKey: String,
        @Query("include-related-images") includeRelatedImages: Boolean = true
    ): Response<PlantNetResponse>
}
