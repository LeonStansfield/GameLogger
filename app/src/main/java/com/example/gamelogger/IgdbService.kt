package com.example.gamelogger

import com.example.gamelogger.BuildConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class IgdbService {

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    private var authToken: AuthToken? = null

    // Function to get the access token
    private suspend fun getAuthToken(): AuthToken {
        val response: AuthToken = httpClient.post("https://id.twitch.tv/oauth2/token") {
            parameter("client_id", BuildConfig.IGDB_CLIENT_ID)
            parameter("client_secret", BuildConfig.IGDB_CLIENT_SECRET)
            parameter("grant_type", "client_credentials")
        }.body()
        return response
    }

    // Main function to get the games
    suspend fun getTop20TrendingGames(): List<Game> {
        if (authToken == null) {
            authToken = getAuthToken()
        }

        return httpClient.post("https://api.igdb.com/v4/games") {
            headers {
                append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                append("Authorization", "Bearer ${authToken?.accessToken}")
            }
            contentType(ContentType.Application.Json)
            setBody("fields name, cover.image_id; sort popularity desc; limit 20;")
        }.body()
    }
}