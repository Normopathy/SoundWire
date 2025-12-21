package com.soundwire

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiProvider {

    @Volatile private var retrofit: Retrofit? = null
    @Volatile private var lastBaseUrl: String? = null

    fun api(context: Context): SoundWireApi {
        val baseUrl = ServerConfig.getBaseUrl(context)
        if (retrofit == null || lastBaseUrl != baseUrl) {
            synchronized(this) {
                if (retrofit == null || lastBaseUrl != baseUrl) {
                    retrofit = buildRetrofit(context.applicationContext, baseUrl)
                    lastBaseUrl = baseUrl
                }
            }
        }
        return retrofit!!.create(SoundWireApi::class.java)
    }

    private fun buildRetrofit(appCtx: Context, baseUrl: String): Retrofit {
        val session = SessionManager(appCtx)

        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val token = session.token()
                val req = if (token.isNullOrBlank()) {
                    original
                } else {
                    original.newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                }
                chain.proceed(req)
            }
            .addInterceptor(logger)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }
}
