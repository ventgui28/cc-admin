package com.ventgui.app.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.json.Json

class EncryptedSessionManager(context: Context) : SessionManager {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "supabase_session_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    override suspend fun saveSession(session: UserSession) {
        val sessionJson = json.encodeToString(UserSession.serializer(), session)
        sharedPreferences.edit()
            .putString(KEY_SESSION, sessionJson)
            .apply()
    }

    override suspend fun loadSession(): UserSession? {
        val sessionJson = sharedPreferences.getString(KEY_SESSION, null) ?: return null
        return try {
            json.decodeFromString(UserSession.serializer(), sessionJson)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteSession() {
        sharedPreferences.edit()
            .remove(KEY_SESSION)
            .apply()
    }

    companion object {
        private const val KEY_SESSION = "supabase_user_session"
    }
}
