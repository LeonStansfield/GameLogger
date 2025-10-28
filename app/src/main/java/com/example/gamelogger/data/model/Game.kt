package com.example.gamelogger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Game(
    val id: Int,
    val name: String,
    val cover: Cover? = null,
)

@Serializable
data class Cover(
    @SerialName("image_id")
    val imageId: String
) {
    val smallCoverUrl: String
        get() = "https://images.igdb.com/igdb/image/upload/t_cover_small/${imageId}.jpg"
}