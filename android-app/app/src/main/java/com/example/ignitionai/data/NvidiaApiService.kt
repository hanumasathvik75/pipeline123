package com.example.ignitionai.data

import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

@Serializable
data class Message(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.2,
    val max_tokens: Int = 1024,
    val stream: Boolean = false
)

@Serializable
data class Choice(val message: Message)

@Serializable
data class ChatResponse(val choices: List<Choice>)

interface NvidiaApiService {
    @Headers(
        "Authorization: Bearer ${Config.NVIDIA_API_KEY}",
        "Content-Type: application/json"
    )
    @POST("chat/completions")
    suspend fun generateContent(@Body request: ChatRequest): ChatResponse
}

object NvidiaApiClient {
    private const val BASE_URL = "https://integrate.api.nvidia.com/v1/"
    
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val retrofitService: NvidiaApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NvidiaApiService::class.java)
    }
}
