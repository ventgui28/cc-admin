package com.ventgui.app.data.repository

import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.model.Race
import com.ventgui.app.data.model.RaceResult
import com.ventgui.app.ui.screens.races.RacesViewModel
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RacesRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = RacesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testFetchWeatherForDate_returnsValidDataOrFallback() = runTest {
        val result = repository.fetchWeatherForDate("2026-06-20T12:00:00Z")
        assertNotNull(result)
        assertTrue(result.first.contains("°"))
        assertNotNull(result.second)
    }

    @Test
    fun testResultsSortingLogic() = runTest {
        val mockRepo = mockk<RacesRepository>()
        
        // Mock data setup
        val race = Race(id = "race123", title = "Test Race", date = "2026-06-20T12:00:00Z", category = "Estrada")
        
        val athletes = listOf(
            Athlete(id = "ath_dsq", name = "Athlete DSQ", category = "Elite"),
            Athlete(id = "ath_pos2", name = "Athlete 2nd", category = "Elite"),
            Athlete(id = "ath_dnf", name = "Athlete DNF", category = "Elite"),
            Athlete(id = "ath_pos1", name = "Athlete 1st", category = "Elite"),
            Athlete(id = "ath_otl", name = "Athlete OTL", category = "Elite"),
            Athlete(id = "ath_dns", name = "Athlete DNS", category = "Elite"),
            Athlete(id = "ath_null", name = "Athlete Null", category = "Elite")
        )
        
        val results = listOf(
            RaceResult(race_id = "race123", athlete_id = "ath_dsq", position = null, time = "DSQ"),
            RaceResult(race_id = "race123", athlete_id = "ath_pos2", position = 2, time = "1h20m"),
            RaceResult(race_id = "race123", athlete_id = "ath_dnf", position = null, time = "DNF"),
            RaceResult(race_id = "race123", athlete_id = "ath_pos1", position = 1, time = "1h19m"),
            RaceResult(race_id = "race123", athlete_id = "ath_otl", position = null, time = "OTL"),
            RaceResult(race_id = "race123", athlete_id = "ath_dns", position = null, time = "DNS"),
            RaceResult(race_id = "race123", athlete_id = "ath_null", position = null, time = null)
        )
        
        coEvery { mockRepo.getRaceResults("race123") } returns results
        coEvery { mockRepo.getAthletes() } returns athletes
        coEvery { mockRepo.fetchWeatherForDate(any()) } returns Pair("20°", "Sol")
        
        val viewModel = RacesViewModel(mockRepo)
        viewModel.fetchDetailsForRace(race)
        
        val sortedResults = viewModel.uiState.value.selectedRaceResults
        assertEquals(7, sortedResults.size)
        
        // Assert expected sorted order:
        // 1st: Position 1 (ath_pos1)
        // 2nd: Position 2 (ath_pos2)
        // 3rd: OTL (ath_otl)
        // 4th: DSQ (ath_dsq)
        // 5th: DNS (ath_dns)
        // 6th: DNF (ath_dnf)
        // 7th: null time/pos (ath_null)
        assertEquals("ath_pos1", sortedResults[0].second.id)
        assertEquals("ath_pos2", sortedResults[1].second.id)
        assertEquals("ath_otl", sortedResults[2].second.id)
        assertEquals("ath_dsq", sortedResults[3].second.id)
        assertEquals("ath_dns", sortedResults[4].second.id)
        assertEquals("ath_dnf", sortedResults[5].second.id)
        assertEquals("ath_null", sortedResults[6].second.id)
        
        clearAllMocks()
    }
}
