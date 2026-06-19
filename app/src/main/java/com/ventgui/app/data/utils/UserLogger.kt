package com.ventgui.app.data.utils

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.ventgui.app.data.model.Profile
import com.ventgui.app.data.network.SupabaseClient

@Serializable
data class UserLog(
    val id: String? = null,
    val user_id: String?,
    val user_name: String,
    val action: String,
    val details: String? = null,
    val created_at: String? = null
)

object UserLogger {
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun initialize(coroutineScope: CoroutineScope) {
        scope = coroutineScope
    }

    fun log(action: String, details: String? = null) {
        scope.launch {
            try {
                val session = SupabaseClient.client.auth.currentSessionOrNull()
                val user = session?.user
                val userId = user?.id
                var userName = "Utilizador Anónimo"

                if (userId != null) {
                    try {
                        val profile = SupabaseClient.client.postgrest.from("profiles")
                            .select {
                                filter {
                                    eq("id", userId)
                                }
                            }.decodeSingleOrNull<Profile>()
                        
                        if (profile != null && !profile.full_name.isNullOrBlank()) {
                            userName = profile.full_name
                        } else if (!user.email.isNullOrBlank()) {
                            userName = user.email!!
                        }
                    } catch (e: Exception) {
                        if (user != null && !user.email.isNullOrBlank()) {
                            userName = user.email!!
                        }
                    }
                }

                val logEntry = UserLog(
                    user_id = userId,
                    user_name = userName,
                    action = action,
                    details = details
                )

                SupabaseClient.client.postgrest.from("user_logs").insert(logEntry)
            } catch (e: Exception) {
                // Fail silently to avoid crashing the app
            }
        }
    }
}
