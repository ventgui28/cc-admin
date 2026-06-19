package com.ventgui.app.data.network

import android.content.Context
import io.github.jan.supabase.SupabaseClient as JanSupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

import com.ventgui.app.BuildConfig

object SupabaseClient {
    private val SUPABASE_URL = BuildConfig.SUPABASE_URL
    private val SUPABASE_KEY = BuildConfig.SUPABASE_KEY


    private lateinit var _client: JanSupabaseClient

    val client: JanSupabaseClient
        get() {
            if (!::_client.isInitialized) {
                throw IllegalStateException("SupabaseClient has not been initialized. Call initialize(context) first.")
            }
            return _client
        }

    fun initialize(context: Context) {
        if (::_client.isInitialized) return

        _client = createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_KEY
        ) {
            install(Postgrest)
            install(Storage)
            install(Auth) {
                alwaysAutoRefresh = true
                autoSaveToStorage = true
                sessionManager = EncryptedSessionManager(context)
            }
            install(Realtime)
            
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                explicitNulls = false
            })
        }
    }
}

