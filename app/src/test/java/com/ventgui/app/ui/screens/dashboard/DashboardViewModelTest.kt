package com.ventgui.app.ui.screens.dashboard

import com.ventgui.app.R
import com.ventgui.app.data.model.*
import com.ventgui.app.data.repository.DashboardRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<DashboardRepository>()
    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DashboardViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Test
    fun testParseFlexibleDate_standardIso() {
        val result = viewModel.parseFlexibleDate("2026-06-20T12:00:00Z")
        assertNotNull(result)
        assertEquals(Instant.parse("2026-06-20T12:00:00Z"), result)
    }

    @Test
    fun testParseFlexibleDate_spaceSeparated() {
        val result = viewModel.parseFlexibleDate("20 June 2026")
        assertNotNull(result)
        val expected = java.time.LocalDate.of(2026, 6, 20)
            .atStartOfDay(ZoneId.systemDefault()).toInstant()
        assertEquals(expected, result)
    }

    @Test
    fun testParseFlexibleDate_invalid() {
        assertNull(viewModel.parseFlexibleDate(""))
        assertNull(viewModel.parseFlexibleDate("invalid-date"))
        assertNull(viewModel.parseFlexibleDate("20 June"))
    }

    @Test
    fun testLoadDashboard_success() {
        // Stub profile
        val profile = Profile(id = "user123", full_name = "Jane Doe")
        coEvery { repository.fetchProfile("user123") } returns profile

        // Stub races: one past race, one today/future race, one far future race
        val todayStr = java.time.LocalDate.now(ZoneId.systemDefault()).toString() + "T12:00:00Z"
        val pastRace = Race(id = "race1", title = "Past Race", date = "2020-01-01T12:00:00Z", category = "Estrada")
        val nextRace = Race(id = "race2", title = "Next Race", date = todayStr, category = "MTB")
        val futureRace = Race(id = "race3", title = "Future Race", date = "2030-12-31T12:00:00Z", category = "Pista")
        coEvery { repository.fetchRaces() } returns listOf(pastRace, nextRace, futureRace)

        // Stub athletes
        val athlete1 = Athlete(id = "ath1", name = "Athlete One", category = "Elite")
        val athlete2 = Athlete(id = "ath2", name = "Athlete Two", category = "Sub-23")
        coEvery { repository.fetchAthletes() } returns listOf(athlete1, athlete2)

        // Stub results: athlete1 won race1 (1st), athlete2 finished 2nd on race1
        val result1 = RaceResult(id = "res1", race_id = "race1", athlete_id = "ath1", position = 1, time = "1h30m")
        val result2 = RaceResult(id = "res2", race_id = "race1", athlete_id = "ath2", position = 2, time = "1h31m")
        val resultOther = RaceResult(id = "res3", race_id = "race1", athlete_id = "ath1", position = 4, time = "1h35m") // not podium
        coEvery { repository.fetchRaceResults() } returns listOf(result1, result2, resultOther)

        // Stub social posts
        coEvery { repository.fetchSocialPosts() } returns emptyList()

        // Stub weather
        coEvery { repository.fetchWeather() } returns Pair("25°", R.string.dashboard_weather_clear)

        // Load dashboard
        viewModel.loadDashboard("user123")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(profile, state.profile)
        
        // Assert next race selection (closest future or today race)
        assertNotNull(state.nextRace)
        assertEquals("race2", state.nextRace?.id)

        // Assert totals
        assertEquals(2, state.totalAthletes)
        assertEquals(3, state.totalRaces)
        assertEquals(2, state.totalPodiums) // result1 (1st) and result2 (2nd)
        assertEquals(1, state.totalVictories) // result1 (1st)

        // Assert podium list contains detailed results
        assertEquals(2, state.podiumsList.size)
        assertEquals("Athlete One", state.podiumsList[0].athleteName)
        assertEquals("Past Race", state.podiumsList[0].raceTitle)
        assertEquals(1, state.podiumsList[0].position)

        assertEquals("Athlete Two", state.podiumsList[1].athleteName)
        assertEquals(2, state.podiumsList[1].position)

        // Assert victories list
        assertEquals(1, state.victoriesList.size)
        assertEquals("Athlete One", state.victoriesList[0].athleteName)
        assertEquals(1, state.victoriesList[0].position)

        // Assert weather
        assertEquals("25°", state.temp)
        assertEquals(R.string.dashboard_weather_clear, state.weatherDescResId)
    }

    @Test
    fun testLoadDashboard_failure() {
        coEvery { repository.fetchProfile(any()) } throws Exception("Database failure")

        viewModel.loadDashboard("user123")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Database failure"))
    }
}
