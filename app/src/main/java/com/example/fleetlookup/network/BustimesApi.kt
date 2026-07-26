package com.example.fleetlookup.network

import com.example.fleetlookup.model.VehicleListResponse
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface BustimesApi {
    @GET("api/vehicles/")
    suspend fun search(@Query("search") q: String, @Query("limit") limit: Int = 15, @Query("withdrawn") withdrawn: Boolean = false): VehicleListResponse

    companion object {
        fun create(): BustimesApi {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder()
                        .header("User-Agent", "FleetLookup/1.0 (Android; educational)")
                        .header("Accept", "application/json").build())
                }.build()
            return Retrofit.Builder()
                .baseUrl("https://bustimes.org/")
                .client(client)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(BustimesApi::class.java)
        }
    }
}
