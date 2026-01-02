package com.example.gamelogger

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import com.example.gamelogger.data.model.Cover
import com.example.gamelogger.data.model.Game
import com.example.gamelogger.ui.features.diary.DiaryViewModel
import com.example.gamelogger.ui.features.gallery.GalleryViewModel
import com.example.gamelogger.ui.features.loggame.LogGameViewModel
import com.example.gamelogger.ui.features.timer.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end integration tests simulating complete user workflows
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class EndToEndIntegrationTest {

    private lateinit var database: GameLoggerDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        // Use runBlocking to ensure all pending database operations complete
        kotlinx.coroutines.runBlocking {
            // Small delay to allow any pending operations to complete
            delay(500)
        }
        database.close()
    }

    @Test
    fun completeGameLoggingWorkflow() = runTest {
        // Scenario: User discovers a game, logs it, rates it, and adds a photo

        val gameId = "witcher3"
        val logViewModel = LogGameViewModel(database.gameLogDao(), gameId)
        val diaryViewModel = DiaryViewModel(database.gameLogDao())
        val galleryViewModel = GalleryViewModel(database.gameLogDao())

        // Step 1: User logs the game as "Playing"
        logViewModel.saveGameLog(
            status = GameStatus.PLAYING,
            playTime = 0,
            userRating = null,
            review = null
        )

        // Verify it appears in diary - wait for non-empty list with timeout (using real time)
        val playingLogs = withContext(Dispatchers.Default) {
            withTimeout(5000) {
                diaryViewModel.gameLogs.first { it.isNotEmpty() }
            }
        }
        assertEquals(1, playingLogs.size)
        assertEquals(GameStatus.PLAYING, playingLogs[0].status)

        // Step 2: User plays for some hours and updates
        logViewModel.saveGameLog(
            status = GameStatus.PLAYING,
            playTime = 10,
            userRating = 4.0f,
            review = "Great so far!"
        )

        val updated = database.gameLogDao().getGameLog(gameId).first()
        assertEquals(10L, updated?.playTime)
        assertEquals(4.0f, updated?.userRating)

        // Step 3: User finishes the game and adds a photo
        logViewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 50,
            userRating = 5.0f,
            review = "Masterpiece!",
            photoUri = "file:///photos/witcher3.jpg",
            locationName = "Home"
        )

        // Verify it's in the gallery - wait for non-empty list
        val photos = withContext(Dispatchers.Default) {
            withTimeout(5000) {
                galleryViewModel.photoLogs.first { it.isNotEmpty() }
            }
        }
        assertEquals(1, photos.size)
        assertEquals(gameId, photos[0].gameId)

        // Step 4: Filter diary by "Played" games
        diaryViewModel.setFilter(GameStatus.PLAYED)
        val playedGames = withContext(Dispatchers.Default) {
            withTimeout(5000) {
                diaryViewModel.gameLogs.first { it.isNotEmpty() }
            }
        }
        assertEquals(1, playedGames.size)
        assertEquals("Masterpiece!", playedGames[0].review)

        // Step 5: User updates just the review
        logViewModel.updateReview("Absolute masterpiece! Game of the year!").join()
        val finalLog = database.gameLogDao().getGameLog(gameId).first()
        assertEquals("Absolute masterpiece! Game of the year!", finalLog?.review)
        assertEquals(50L, finalLog?.playTime) // Preserved
        assertEquals(5.0f, finalLog?.userRating) // Preserved
    }

    @Test
    fun timerAndManualPlaytimeWorkflow() = runTest {
        // Scenario: User uses timer to track playtime, then manually adjusts

        val gameId = "zelda"
        val timerViewModel = TimerViewModel(database.gameLogDao(), gameId)
        val gameDetails = Game(id = 123, name = "Zelda BOTW", cover = Cover("zelda_cover"))

        // Step 1: Start timer
        timerViewModel.toggleTimer(gameDetails).join()

        val log1 = database.gameLogDao().getGameLogById(gameId)
        assertNotNull(log1?.timerStartTime)
        assertEquals(GameStatus.PLAYING, log1?.status)

        // Step 2: Stop timer after some time
        delay(1000) // Simulate 1 second of play
        timerViewModel.toggleTimer(null).join()

        val log2 = database.gameLogDao().getGameLogById(gameId)
        assertNull(log2?.timerStartTime)
        assertTrue("Total seconds should be >= 0", (log2?.totalSecondsPlayed ?: -1) >= 0)
        assertEquals(1, log2?.sessionCount)

        // Step 3: User manually adjusts playtime
        timerViewModel.updateManualPlaytime(gameDetails, 10, 30).join() // 10 hours 30 minutes

        val log3 = database.gameLogDao().getGameLogById(gameId)
        val expectedSeconds = (10 * 3600) + (30 * 60)
        assertEquals(expectedSeconds.toLong(), log3?.totalSecondsPlayed)

        // Step 4: Start timer again for another session
        timerViewModel.toggleTimer(gameDetails).join()
        timerViewModel.toggleTimer(null).join()

        val log4 = database.gameLogDao().getGameLogById(gameId)
        assertTrue("Session count should be incremented", (log4?.sessionCount ?: 0) > 1)
    }

    @Test
    fun multipleGamesLifecycle() = runTest {
        // Scenario: User manages multiple games through different states

        val diaryViewModel = DiaryViewModel(database.gameLogDao())

        // Add games in different states
        val game1 = LogGameViewModel(database.gameLogDao(), "game1")
        val game2 = LogGameViewModel(database.gameLogDao(), "game2")
        val game3 = LogGameViewModel(database.gameLogDao(), "game3")

        // Game 1: Completed
        game1.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 30,
            userRating = 5.0f,
            review = "Perfect!",
            photoUri = "file:///photo1.jpg"
        )

        // Game 2: Currently playing
        game2.saveGameLog(
            status = GameStatus.PLAYING,
            playTime = 10,
            userRating = 4.0f,
            review = "Good so far"
        )

        // Game 3: Backlogged
        game3.saveGameLog(
            status = GameStatus.BACKLOGGED,
            playTime = 0,
            userRating = null,
            review = null
        )

        // Verify all show up in diary - wait for all 3 games (using real time)
        val allGames = withContext(Dispatchers.Default) {
            withTimeout(5000) {
                diaryViewModel.gameLogs.first { it.size >= 3 }
            }
        }
        assertEquals(3, allGames.size)

        // Filter by status
        diaryViewModel.setFilter(GameStatus.PLAYED)
        assertEquals(1, withContext(Dispatchers.Default) { withTimeout(5000) { diaryViewModel.gameLogs.first { it.isNotEmpty() } } }.size)

        diaryViewModel.setFilter(GameStatus.PLAYING)
        assertEquals(1, withContext(Dispatchers.Default) { withTimeout(5000) { diaryViewModel.gameLogs.first { it.isNotEmpty() } } }.size)

        diaryViewModel.setFilter(GameStatus.BACKLOGGED)
        assertEquals(1, withContext(Dispatchers.Default) { withTimeout(5000) { diaryViewModel.gameLogs.first { it.isNotEmpty() } } }.size)

        // User decides to drop game 2
        game2.saveGameLog(
            status = GameStatus.DROPPED,
            playTime = 10,
            userRating = 2.0f,
            review = "Not for me"
        )

        diaryViewModel.setFilter(GameStatus.DROPPED)
        val droppedGames = withContext(Dispatchers.Default) {
            withTimeout(5000) {
                diaryViewModel.gameLogs.first { it.isNotEmpty() }
            }
        }
        assertEquals(1, droppedGames.size)
        assertEquals("game2", droppedGames[0].gameId)
    }

    @Test
    fun photoGalleryWorkflow() = runTest {
        // Scenario: User adds photos to multiple games and views them in gallery

        val galleryViewModel = GalleryViewModel(database.gameLogDao())

        // Add 5 games with photos
        for (i in 1..5) {
            val viewModel = LogGameViewModel(database.gameLogDao(), "game$i")
            viewModel.saveGameLog(
                status = GameStatus.PLAYED,
                playTime = i.toLong() * 5,
                userRating = (i % 5 + 1).toFloat(),
                review = "Review for game $i",
                photoUri = "file:///photo$i.jpg"
            )
        }

        // Verify all appear in gallery - wait for 5 photos
        val photos = galleryViewModel.photoLogs.first { it.size >= 5 }
        assertEquals(5, photos.size)

        // Add games without photos - shouldn't appear in gallery
        for (i in 6..8) {
            val viewModel = LogGameViewModel(database.gameLogDao(), "game$i")
            viewModel.saveGameLog(
                status = GameStatus.PLAYED,
                playTime = 10,
                userRating = 4.0f,
                review = "No photo"
            )
        }

        // Gallery should still only have 5 - just verify current state
        val photosAfter = galleryViewModel.photoLogs.first { it.size >= 5 }
        assertEquals(5, photosAfter.size)

        // User removes a photo from game1
        val game1 = LogGameViewModel(database.gameLogDao(), "game1")
        game1.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 5,
            userRating = 1.0f,
            review = "Review for game 1",
            photoUri = null // Remove photo
        )

        // Gallery should now have 4 - wait for size to drop
        val photosAfterRemoval = galleryViewModel.photoLogs.first { it.size == 4 }
        assertEquals(4, photosAfterRemoval.size)
    }

    @Test
    fun deleteGameFromDiary() = runTest {
        // Scenario: User deletes a game from their diary

        val diaryViewModel = DiaryViewModel(database.gameLogDao())
        val logViewModel = LogGameViewModel(database.gameLogDao(), "delete_test")

        // Add a game
        logViewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 3.0f,
            review = "Meh"
        )

        val beforeDelete = diaryViewModel.gameLogs.first { it.isNotEmpty() }
        assertEquals(1, beforeDelete.size)

        // Delete it
        diaryViewModel.deleteGameLog(beforeDelete[0])

        val afterDelete = diaryViewModel.gameLogs.first { it.isEmpty() }
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun locationTrackingWorkflow() = runTest {
        // Scenario: User logs games with location data

        val game1 = LogGameViewModel(database.gameLogDao(), "location1")
        val game2 = LogGameViewModel(database.gameLogDao(), "location2")

        // Log game at location 1
        game1.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 10,
            userRating = 4.5f,
            latitude = 51.5074,
            longitude = -0.1278,
            locationName = "London"
        )

        // Log game at location 2
        game2.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 8,
            userRating = 4.0f,
            latitude = 40.7128,
            longitude = -74.0060,
            locationName = "New York"
        )

        // Verify locations are saved
        val log1 = database.gameLogDao().getGameLog("location1").first()
        assertEquals("London", log1?.locationName)
        assertEquals(51.5074, log1?.latitude!!, 0.0001)

        val log2 = database.gameLogDao().getGameLog("location2").first()
        assertEquals("New York", log2?.locationName)
        assertEquals(40.7128, log2?.latitude!!, 0.0001)
    }

    @Test
    fun ratingsAndReviewsWorkflow() = runTest {
        // Scenario: User adds and updates ratings and reviews

        val logViewModel = LogGameViewModel(database.gameLogDao(), "rating_test")

        // Initial rating without review
        logViewModel.saveGameLog(
            status = GameStatus.PLAYING,
            playTime = 5,
            userRating = 3.5f,
            review = null
        )

        val log1 = database.gameLogDao().getGameLog("rating_test").first()
        assertEquals(3.5f, log1?.userRating)
        assertNull(log1?.review)

        // Add review
        logViewModel.updateReview("Starting to get good!").join() // Wait for completion

        val log2 = database.gameLogDao().getGameLog("rating_test").first()
        assertEquals("Starting to get good!", log2?.review)
        assertEquals(3.5f, log2?.userRating) // Preserved

        // Finish game and update rating
        logViewModel.saveGameLog(
            status = GameStatus.PLAYED,
            playTime = 20,
            userRating = 5.0f,
            review = "Absolutely amazing!"
        )

        val log3 = database.gameLogDao().getGameLog("rating_test").first()
        assertEquals(5.0f, log3?.userRating)
        assertEquals("Absolutely amazing!", log3?.review)
    }
}

