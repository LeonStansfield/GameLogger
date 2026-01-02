package com.example.gamelogger.ui.features.gallery

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.gamelogger.data.db.GameLog
import com.example.gamelogger.data.db.GameLoggerDatabase
import com.example.gamelogger.data.db.GameStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class GalleryViewModelIntegrationTest {

    private lateinit var database: GameLoggerDatabase
    private lateinit var viewModel: GalleryViewModel

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            GameLoggerDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        viewModel = GalleryViewModel(database.gameLogDao())
    }

    @After
    fun tearDown() {
        // Use runBlocking to ensure all pending database operations complete
        kotlinx.coroutines.runBlocking {
            // Small delay to allow any pending operations to complete
            kotlinx.coroutines.delay(500)
        }
        database.close()
    }

    @Test
    fun galleryShowsOnlyLogsWithPhotos() = runTest {
        // Arrange
        val withPhoto1 = GameLog(
            "1", GameStatus.PLAYED, 10, 4.5f,
            photoUri = "file:///photo1.jpg"
        )
        val withPhoto2 = GameLog(
            "2", GameStatus.PLAYED, 5, 4.0f,
            photoUri = "content://media/photo2.jpg"
        )
        val withoutPhoto = GameLog(
            "3", GameStatus.PLAYED, 15, 5.0f,
            photoUri = null
        )

        database.gameLogDao().insertOrUpdateGameLog(withPhoto1)
        database.gameLogDao().insertOrUpdateGameLog(withPhoto2)
        database.gameLogDao().insertOrUpdateGameLog(withoutPhoto)

        // Act
        val photos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertEquals(2, photos.size)
        assertTrue(photos.all { !it.photoUri.isNullOrEmpty() })
    }

    @Test
    fun galleryEmptyWhenNoPhotos() = runTest {
        // Arrange
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("1", GameStatus.PLAYED, 10, 4.5f, photoUri = null)
        )
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("2", GameStatus.PLAYED, 5, 4.0f, photoUri = "")
        )

        // Act
        val photos = viewModel.photoLogs.first()

        // Assert
        assertTrue(photos.isEmpty())
    }

    @Test
    fun galleryUpdatesWhenPhotoAdded() = runTest {
        // Arrange - initial state should be empty
        val initialPhotos = viewModel.photoLogs.first()
        assertEquals(0, initialPhotos.size)

        // Act
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("1", GameStatus.PLAYED, 10, 4.5f, photoUri = "file:///photo.jpg")
        )
        val updatedPhotos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertEquals(1, updatedPhotos.size)
    }

    @Test
    fun galleryPreservesGameMetadata() = runTest {
        // Arrange
        val logWithPhoto = GameLog(
            gameId = "123",
            status = GameStatus.PLAYED,
            playTime = 25,
            userRating = 5.0f,
            review = "Amazing game!",
            title = "The Witcher 3",
            posterUrl = "https://example.com/poster.jpg",
            photoUri = "file:///photo.jpg",
            locationName = "Home"
        )
        database.gameLogDao().insertOrUpdateGameLog(logWithPhoto)

        // Act
        val photos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertEquals(1, photos.size)
        val photo = photos[0]
        assertEquals("123", photo.gameId)
        assertEquals(GameStatus.PLAYED, photo.status)
        assertEquals(25L, photo.playTime)
        assertEquals(5.0f, photo.userRating)
        assertEquals("Amazing game!", photo.review)
        assertEquals("The Witcher 3", photo.title)
        assertEquals("Home", photo.locationName)
    }

    @Test
    fun galleryHandlesDifferentPhotoUriFormats() = runTest {
        // Arrange
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("1", GameStatus.PLAYED, 10, 4.5f, photoUri = "file:///sdcard/photo1.jpg")
        )
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("2", GameStatus.PLAYED, 5, 4.0f, photoUri = "content://media/external/images/123")
        )
        database.gameLogDao().insertOrUpdateGameLog(
            GameLog("3", GameStatus.PLAYED, 8, 3.5f, photoUri = "/absolute/path/photo.png")
        )

        // Act
        val photos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertEquals(3, photos.size)
        assertEquals("file:///sdcard/photo1.jpg", photos.find { it.gameId == "1" }?.photoUri)
        assertEquals("content://media/external/images/123", photos.find { it.gameId == "2" }?.photoUri)
        assertEquals("/absolute/path/photo.png", photos.find { it.gameId == "3" }?.photoUri)
    }

    @Test
    fun galleryUpdatesWhenPhotoRemoved() = runTest {
        // Arrange
        val log = GameLog("1", GameStatus.PLAYED, 10, 4.5f, photoUri = "file:///photo.jpg")
        database.gameLogDao().insertOrUpdateGameLog(log)

        val initialPhotos = viewModel.photoLogs.drop(1).first()
        assertEquals(1, initialPhotos.size)

        // Act - Remove photo by setting to null
        val updated = log.copy(photoUri = null)
        database.gameLogDao().insertOrUpdateGameLog(updated)
        val updatedPhotos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertTrue(updatedPhotos.isEmpty())
    }

    @Test
    fun galleryShowsMultiplePhotosFromDifferentGames() = runTest {
        // Arrange
        for (i in 1..10) {
            database.gameLogDao().insertOrUpdateGameLog(
                GameLog(
                    gameId = i.toString(),
                    status = GameStatus.PLAYED,
                    playTime = i.toLong(),
                    userRating = (i % 5 + 1).toFloat(),
                    photoUri = "file:///photo$i.jpg"
                )
            )
        }

        // Act
        val photos = viewModel.photoLogs.drop(1).first()

        // Assert
        assertEquals(10, photos.size)
        assertEquals(10, photos.map { it.gameId }.toSet().size) // All unique game IDs
    }
}

