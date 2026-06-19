package com.ventgui.app.data.repository

import com.ventgui.app.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRepositoryTest {

    private val repository = DashboardRepository()

    @Test
    fun testFetchWeather_returnsValidDataOrFallback() = runTest {
        val result = repository.fetchWeather()
        assertNotNull(result)
        
        // Assert temperature has a degree symbol
        assertTrue(result.first.contains("°"))
        
        // Assert resource ID is a valid android resource ID (non-zero)
        assertTrue(result.second != 0)
    }
}
