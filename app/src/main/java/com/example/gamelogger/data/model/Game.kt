package com.example.gamelogger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: Int,
    val name: String,
    val cover: Cover? = null,
    val artworks: List<Artwork>? = null,
    val genres: List<Genre>? = null,
    val summary: String? = null,
    @SerialName("first_release_date")
    val firstReleaseDate: Long? = null
)

@Serializable
data class Artwork(
    @SerialName("image_id")
    val imageId: String
) {
    // Get a high-resolution artwork URL
    val artworkUrl: String
        get() = "https://images.igdb.com/igdb/image/upload/t_1080p/${imageId}.jpg"
}

@Serializable
data class Genre(
    val id: Int,
    val name: String
)

// You can also add a helper to format the date
fun Game.getReleaseDateString(): String? {
    // This is a basic example; you'd use platform-specific date formatters
    return firstReleaseDate?.let {
        // Placeholder for date conversion
        "Released: $it" // e.g., "Released: 1998-11-19"
    }
}

@Serializable
data class Cover(
    @SerialName("image_id")
    val imageId: String
) {
    val smallCoverUrl: String
        get() = "https://images.igdb.com/igdb/image/upload/t_cover_small/${imageId}.jpg"
    val bigCoverUrl: String
        get() = "https://images.igdb.com/igdb/image/upload/t_cover_big/${imageId}.jpg"
}