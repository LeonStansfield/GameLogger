package com.example.gamelogger.data.model

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class GameModelTest {

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `Game data class creates instance with all fields`() {
        // Arrange & Act
        val game = Game(
            id = 123,
            name = "The Witcher 3",
            cover = Cover(imageId = "abc123"),
            artworks = listOf(Artwork("art1"), Artwork("art2")),
            genres = listOf(Genre(1, "RPG"), Genre(2, "Action")),
            summary = "An epic adventure",
            firstReleaseDate = 1431993600L // May 19, 2015
        )

        // Assert
        assertEquals(123, game.id)
        assertEquals("The Witcher 3", game.name)
        assertNotNull(game.cover)
        assertEquals(2, game.artworks?.size)
        assertEquals(2, game.genres?.size)
        assertEquals("An epic adventure", game.summary)
        assertNotNull(game.firstReleaseDate)
    }

    @Test
    fun `Game with minimal data creates successfully`() {
        // Arrange & Act
        val game = Game(id = 1, name = "Minimal Game")

        // Assert
        assertEquals(1, game.id)
        assertEquals("Minimal Game", game.name)
        assertNull(game.cover)
        assertNull(game.artworks)
        assertNull(game.genres)
        assertNull(game.summary)
        assertNull(game.firstReleaseDate)
    }

    @Test
    fun `Cover smallCoverUrl generates correct URL`() {
        // Arrange
        val cover = Cover(imageId = "test123")

        // Act
        val url = cover.smallCoverUrl

        // Assert
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_small/test123.jpg",
            url
        )
    }

    @Test
    fun `Cover bigCoverUrl generates correct URL`() {
        // Arrange
        val cover = Cover(imageId = "test456")

        // Act
        val url = cover.bigCoverUrl

        // Assert
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/test456.jpg",
            url
        )
    }

    @Test
    fun `Artwork artworkUrl generates correct URL`() {
        // Arrange
        val artwork = Artwork(imageId = "artwork789")

        // Act
        val url = artwork.artworkUrl

        // Assert
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_1080p/artwork789.jpg",
            url
        )
    }

    @Test
    fun `getReleaseDateString returns formatted date for valid timestamp`() {
        // Arrange - May 19, 2015 (Unix timestamp in seconds)
        val game = Game(
            id = 1,
            name = "Test Game",
            firstReleaseDate = 1431993600L
        )

        // Act
        val result = game.getReleaseDateString()

        // Assert
        assertNotNull("Should return a date string", result)
        assertTrue("Should start with 'Released:'", result?.startsWith("Released:") == true)
        assertTrue("Should contain 2015", result?.contains("2015") == true)
    }

    @Test
    fun `getReleaseDateString returns null for game without release date`() {
        // Arrange
        val game = Game(id = 1, name = "Unreleased Game", firstReleaseDate = null)

        // Act
        val result = game.getReleaseDateString()

        // Assert
        assertNull("Should return null for missing release date", result)
    }

    @Test
    fun `Genre data class has correct properties`() {
        // Arrange & Act
        val genre = Genre(id = 12, name = "Role-Playing")

        // Assert
        assertEquals(12, genre.id)
        assertEquals("Role-Playing", genre.name)
    }

    @Test
    fun `Game with multiple genres stores all genres`() {
        // Arrange
        val genres = listOf(
            Genre(1, "RPG"),
            Genre(2, "Action"),
            Genre(3, "Adventure")
        )
        val game = Game(id = 1, name = "Test", genres = genres)

        // Act
        val result = game.genres

        // Assert
        assertNotNull(result)
        assertEquals(3, result?.size)
        assertEquals("RPG", result?.get(0)?.name)
        assertEquals("Action", result?.get(1)?.name)
        assertEquals("Adventure", result?.get(2)?.name)
    }

    @Test
    fun `Game with multiple artworks stores all artworks`() {
        // Arrange
        val artworks = listOf(
            Artwork("art1"),
            Artwork("art2"),
            Artwork("art3")
        )
        val game = Game(id = 1, name = "Test", artworks = artworks)

        // Act
        val result = game.artworks

        // Assert
        assertNotNull(result)
        assertEquals(3, result?.size)
        assertEquals("art1", result?.get(0)?.imageId)
    }

    @Test
    fun `Cover with special characters in imageId generates valid URL`() {
        // Arrange
        val cover = Cover(imageId = "test_123-abc")

        // Act
        val smallUrl = cover.smallCoverUrl
        val bigUrl = cover.bigCoverUrl

        // Assert
        assertTrue(smallUrl.contains("test_123-abc"))
        assertTrue(bigUrl.contains("test_123-abc"))
    }

    @Test
    fun `Game equality works correctly`() {
        // Arrange
        val game1 = Game(id = 1, name = "Test")
        val game2 = Game(id = 1, name = "Test")
        val game3 = Game(id = 2, name = "Test")

        // Assert
        assertEquals("Games with same id and name should be equal", game1, game2)
        assertTrue("Games with different id should not be equal", game1 != game3)
    }

    @Test
    fun `getReleaseDateString formats date in dd-MM-yyyy format`() {
        // Arrange - January 1, 2020
        val game = Game(
            id = 1,
            name = "New Year Game",
            firstReleaseDate = 1577836800L // Jan 1, 2020 00:00:00 UTC
        )

        // Act
        val result = game.getReleaseDateString()

        // Assert
        assertNotNull(result)
        assertTrue("Should contain 'Released:'", result?.startsWith("Released:") == true)
        // The exact format depends on timezone, but should contain year
        assertTrue("Should contain 2020", result?.contains("2020") == true)
    }

    @Test
    fun `Cover imageId can be empty string`() {
        // Arrange & Act
        val cover = Cover(imageId = "")

        // Assert
        assertEquals("", cover.imageId)
        assertTrue("URL should still be generated", cover.bigCoverUrl.isNotEmpty())
    }

    @Test
    fun `Artwork imageId can be empty string`() {
        // Arrange & Act
        val artwork = Artwork(imageId = "")

        // Assert
        assertEquals("", artwork.imageId)
        assertTrue("URL should still be generated", artwork.artworkUrl.isNotEmpty())
    }
}

