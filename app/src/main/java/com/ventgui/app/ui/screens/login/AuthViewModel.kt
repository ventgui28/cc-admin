package com.ventgui.app.ui.screens.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ventgui.app.data.network.SupabaseClient
import com.ventgui.app.data.utils.UserLogger
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
}

class AuthViewModel(
    private val supabaseClient: io.github.jan.supabase.SupabaseClient? = null
) : ViewModel() {

    private val client: io.github.jan.supabase.SupabaseClient
        get() = supabaseClient ?: SupabaseClient.client

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Client-side rate limiting states
    private val _loginAttempts = MutableStateFlow(0)
    val loginAttempts: StateFlow<Int> = _loginAttempts.asStateFlow()

    private val _lockoutTimeRemaining = MutableStateFlow(0L)
    val lockoutTimeRemaining: StateFlow<Long> = _lockoutTimeRemaining.asStateFlow()

    init {
        // Collect session status from Supabase
        viewModelScope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _authState.value = AuthState.Authenticated
                    }
                    is SessionStatus.Initializing -> {
                        _authState.value = AuthState.Loading
                    }
                    else -> {
                        _authState.value = AuthState.Unauthenticated
                    }
                }
            }
        }
    }

    fun restoreSession(context: Context) {
        val authPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val wasLoggedIn = authPrefs.getBoolean("is_logged_in", false)
        if (wasLoggedIn) {
            _authState.value = AuthState.Authenticated
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String, rememberMe: Boolean, context: Context, onResult: (String?) -> Unit) {
        if (isLockedOut()) {
            onResult("Demasiadas tentativas. Tenta novamente mais tarde.")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                client.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password.trim()
                }
                
                // Clear attempts on success
                _loginAttempts.value = 0
                _lockoutTimeRemaining.value = 0
                
                // Log and save state
                UserLogger.log("Iniciou sessão")
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("is_logged_in", true)
                    .putBoolean("remember_me", rememberMe)
                    .apply()
                
                _authState.value = AuthState.Authenticated
                onResult(null)
            } catch (e: Exception) {
                // Increment failed attempts
                val attempts = _loginAttempts.value + 1
                _loginAttempts.value = attempts
                if (attempts >= 3) {
                    _lockoutTimeRemaining.value = System.currentTimeMillis() + 30000 // lock for 30s
                }

                _authState.value = AuthState.Unauthenticated
                val safeMessage = when {
                    e.message?.contains("Invalid login", ignoreCase = true) == true -> "Credenciais inválidas"
                    e.message?.contains("Email not confirmed", ignoreCase = true) == true -> "E-mail ainda não verificado. Verifica a tua caixa de entrada."
                    e.message?.contains("rate limit", ignoreCase = true) == true -> "Demasiadas tentativas. Aguarda um momento."
                    e.message?.contains("network", ignoreCase = true) == true -> "Erro de ligação. Verifica a tua internet."
                    else -> "Erro de autenticação. Tenta novamente."
                }
                onResult(safeMessage)
            }
        }
    }

    fun signUp(email: String, password: String, onResult: (String?) -> Unit) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                client.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password.trim()
                }
                UserLogger.log("Criou uma conta", "Email: ${email.trim()}")
                _authState.value = AuthState.Unauthenticated
                onResult(null)
            } catch (e: Exception) {
                _authState.value = AuthState.Unauthenticated
                val safeMessage = when {
                    e.message?.contains("already registered", ignoreCase = true) == true -> "Este e-mail já está registado."
                    e.message?.contains("network", ignoreCase = true) == true -> "Erro de ligação. Verifica a tua internet."
                    else -> "Erro ao criar conta. Tenta novamente."
                }
                onResult(safeMessage)
            }
        }
    }

    fun logout(context: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            try {
                client.auth.signOut()
            } catch (e: Exception) {
                // Ignore sign out errors
            } finally {
                val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .remove("remember_me")
                    .remove("is_logged_in")
                    .apply()
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private fun isLockedOut(): Boolean {
        val lockTime = _lockoutTimeRemaining.value
        if (lockTime == 0L) return false
        val diff = lockTime - System.currentTimeMillis()
        return if (diff > 0) {
            true
        } else {
            // Reset lockout
            _lockoutTimeRemaining.value = 0L
            _loginAttempts.value = 0
            false
        }
    }

    fun getSecondsRemaining(): Long {
        val lockTime = _lockoutTimeRemaining.value
        if (lockTime == 0L) return 0
        val diff = lockTime - System.currentTimeMillis()
        return if (diff > 0) diff / 1000 else 0
    }
}
