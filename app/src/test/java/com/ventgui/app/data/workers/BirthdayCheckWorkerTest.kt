package com.ventgui.app.data.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.ventgui.app.data.model.Athlete
import com.ventgui.app.data.repository.BirthdayRepository
import com.ventgui.app.data.repository.TeamRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class BirthdayCheckWorkerTest {

    private lateinit var context: Context
    private val teamRepository: TeamRepository = mockk()
    private val birthdayRepository: BirthdayRepository = mockk()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        // Mocking constructors of repositories used inside the worker
        mockkConstructor(TeamRepository::class)
        mockkConstructor(BirthdayRepository::class)
        
        coEvery { anyConstructed<TeamRepository>().getAthletes() } returns emptyList()
        coEvery { anyConstructed<BirthdayRepository>().wasAlreadySent(any(), any(), any()) } returns false
        coEvery { anyConstructed<BirthdayRepository>().markAsSent(any(), any(), any()) } just Runs
    }

    @Test
    fun testDoWorkNoAthletes() = runBlocking {
        context = mockk(relaxed = true)
        val worker = TestListenableWorkerBuilder<BirthdayCheckWorker>(context).build()

        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun testDoWorkSendsNotificationForBirthdayInOneDay() = runBlocking {
        context = mockk(relaxed = true)
        val tomorrow = LocalDate.now().plusDays(1)
        val testAthlete = Athlete(
            id = "test-id-1",
            name = "João Silva",
            category = "Elite",
            birth_date = tomorrow.toString()
        )

        coEvery { anyConstructed<TeamRepository>().getAthletes() } returns listOf(testAthlete)
        coEvery { anyConstructed<BirthdayRepository>().wasAlreadySent("test-id-1", tomorrow.year, 1) } returns false

        val worker = TestListenableWorkerBuilder<BirthdayCheckWorker>(context).build()
        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        
        coVerify(exactly = 1) { 
            anyConstructed<BirthdayRepository>().markAsSent("test-id-1", tomorrow.year, 1)
        }
    }
}
