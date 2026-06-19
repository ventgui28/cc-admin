package com.ventgui.app.ui.screens.login

import android.content.Context
import android.content.SharedPreferences
import com.ventgui.app.data.utils.UserLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mockClient = mockk<SupabaseClient>()
    private val mockAuth = mockk<Auth>()
    private val sessionStatusFlow = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)

    private val context = mockk<Context>(relaxed = true)
    private val sharedPrefs = mockk<SharedPreferences>(relaxed = true)
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic("io.github.jan.supabase.auth.AuthKt")
        mockkObject(UserLogger)
        
        every { mockClient.auth } returns mockAuth
        every { mockAuth.sessionStatus } returns sessionStatusFlow
        
        // Mock SharedPreferences
        every { context.getSharedPreferences(any(), any()) } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        
        // Mock UserLogger static calls
        every { UserLogger.log(any(), any()) } just runs

        viewModel = AuthViewModel(mockClient)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun testSessionStatus_initialState_isLoading() {
        assertEquals(AuthState.Loading, viewModel.authState.value)
    }

    @Test
    fun testSessionStatus_transitions() = runTest {
        // Transition to Authenticated
        sessionStatusFlow.value = SessionStatus.Authenticated(mockk(relaxed = true))
        assertEquals(AuthState.Authenticated, viewModel.authState.value)

        // Transition to NotAuthenticated
        sessionStatusFlow.value = SessionStatus.NotAuthenticated(isSignOut = false)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun testRestoreSession_wasLoggedIn() {
        every { sharedPrefs.getBoolean("is_logged_in", false) } returns true
        viewModel.restoreSession(context)
        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun testRestoreSession_wasNotLoggedIn() {
        every { sharedPrefs.getBoolean("is_logged_in", false) } returns false
        viewModel.restoreSession(context)
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)
    }

    @Test
    fun testRateLimitingLockout() = runTest {
        // Fail 1st attempt (mockAuth is non-relaxed, so signInWith throws a MockKException by default)
        var errorMsg: String? = null
        viewModel.login("user@test.com", "wrong", false, context) { errorMsg = it }
        assertEquals("Erro de autenticação. Tenta novamente.", errorMsg)
        assertEquals(1, viewModel.loginAttempts.value)
        assertEquals(0L, viewModel.lockoutTimeRemaining.value)

        // Fail 2nd attempt
        viewModel.login("user@test.com", "wrong", false, context) { errorMsg = it }
        assertEquals("Erro de autenticação. Tenta novamente.", errorMsg)
        assertEquals(2, viewModel.loginAttempts.value)
        assertEquals(0L, viewModel.lockoutTimeRemaining.value)

        // Fail 3rd attempt -> lockout triggered
        viewModel.login("user@test.com", "wrong", false, context) { errorMsg = it }
        assertEquals("Erro de autenticação. Tenta novamente.", errorMsg)
        assertEquals(3, viewModel.loginAttempts.value)
        assertTrue(viewModel.lockoutTimeRemaining.value > System.currentTimeMillis())
        assertTrue(viewModel.getSecondsRemaining() > 0)

        // Try a 4th login attempt while locked out
        var lockedOutMsg: String? = null
        viewModel.login("user@test.com", "wrong", false, context) { lockedOutMsg = it }
        assertEquals("Demasiadas tentativas. Tenta novamente mais tarde.", lockedOutMsg)
    }
}
