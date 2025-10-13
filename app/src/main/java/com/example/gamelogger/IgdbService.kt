package com.example.gamelogger

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.Calendar

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
    private suspend fun getAuthToken(): AuthToken? {
        try {
            val response: HttpResponse = httpClient.post("https://id.twitch.tv/oauth2/token") {
                parameter("client_id", BuildConfig.IGDB_CLIENT_ID)
                parameter("client_secret", BuildConfig.IGDB_CLIENT_SECRET)
                parameter("grant_type", "client_credentials")
            }

            // Check if the request was successful
            if (response.status == HttpStatusCode.OK) {
                return response.body<AuthToken>()
            } else {
                // If not successful, log the error response from the server
                val errorBody = response.bodyAsText()
                Log.e("IgdbService", "Error from auth server: ${response.status} - $errorBody")
                return null
            }
        } catch (e: Exception) {
            Log.e("IgdbService", "An unexpected error occurred getting auth token.", e)
            return null
        }
    }


    // Main function to get the games
    suspend fun getTop20TrendingGames(): List<Game> {
        if (authToken == null) {
            authToken = getAuthToken()
        }

        if (authToken == null) {
            Log.e("IgdbService", "Auth token is null, cannot fetch games.")
            return emptyList()
        }

        // Calculate the timestamp for two years ago to get recent games
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2)
        val twoYearsAgoTimestamp = calendar.timeInMillis / 1000

        return try {
            httpClient.post("https://api.igdb.com/v4/games") {
                headers {
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${authToken?.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    "fields name, cover.image_id, first_release_date, total_rating, total_rating_count; " +
                            "where first_release_date > $twoYearsAgoTimestamp & total_rating_count > 25; " +
                            "sort total_rating desc; " +
                            "limit 20;"
                )
            }.body()
        } catch (e: Exception) {
            Log.e("IgdbService", "Error fetching games", e)
            emptyList()
        }
    }
}