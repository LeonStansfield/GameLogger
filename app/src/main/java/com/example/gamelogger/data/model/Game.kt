package com.example.gamelogger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

fun Game.getReleaseDateString(): String? {
    return firstReleaseDate?.let { timestamp ->
        val date = Date(timestamp * 1000)

        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

        "Released: ${formatter.format(date)}"
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