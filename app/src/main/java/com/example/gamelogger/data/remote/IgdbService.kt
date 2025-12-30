package com.example.gamelogger.data.remote

import android.util.Log
import com.example.gamelogger.BuildConfig
import com.example.gamelogger.data.model.AuthToken
import com.example.gamelogger.data.model.Game
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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

    // Reusable auth token function
    private suspend fun getAuthTokenIfNeeded(): AuthToken? {
        if (authToken != null) {
            return authToken
        }

        authToken = try {
            val response: HttpResponse = httpClient.post("https://id.twitch.tv/oauth2/token") {
                // These references will now be resolved
                parameter("client_id", BuildConfig.IGDB_CLIENT_ID)
                parameter("client_secret", BuildConfig.IGDB_CLIENT_SECRET)
                parameter("grant_type", "client_credentials")
            }

            if (response.status == HttpStatusCode.OK) {
                response.body<AuthToken>()
            } else {
                val errorBody = response.bodyAsText()
                Log.e("IgdbService", "Error from auth server: ${response.status} - $errorBody")
                null
            }
        } catch (e: Exception) {
            Log.e("IgdbService", "An unexpected error occurred getting auth token.", e)
            null
        }

        return authToken
    }


    // --- Discover Function ---
    suspend fun getTop20TrendingGames(): List<Game> {
        val token = getAuthTokenIfNeeded()
        if (token == null) {
            Log.e("IgdbService", "Auth token is null, cannot fetch trending games.")
            return emptyList()
        }

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -2)
        val twoYearsAgoTimestamp = calendar.timeInMillis / 1000

        return try {
            httpClient.post("https://api.igdb.com/v4/games") {
                headers {
                    // This reference will now be resolved
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${token.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    "fields id, name, cover.image_id, first_release_date, total_rating, total_rating_count; " +
                            "where first_release_date > $twoYearsAgoTimestamp & total_rating_count > 25; " +
                            "sort total_rating desc; " +
                            "limit 20;"
                )
            }.body()
        } catch (e: Exception) {
            Log.e("IgdbService", "Error fetching trending games", e)
            emptyList()
        }
    }

    // --- Search Function ---
    suspend fun searchGames(query: String): List<Game> {
        val token = getAuthTokenIfNeeded()
        if (token == null) {
            Log.e("IgdbService", "Auth token is null, cannot search games.")
            return emptyList()
        }

        return try {
            httpClient.post("https://api.igdb.com/v4/games") {
                headers {
                    // This reference will now be resolved
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${token.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    // Using the "search" keyword and limiting to 20 results
                    "search \"$query\"; " +
                            "fields id, name, cover.image_id; " +
                            "limit 20;"
                )
            }.body()
        } catch (e: Exception) {
            Log.e("IgdbService", "Error searching games", e)
            emptyList()
        }
    }
    suspend fun getGameDetails(gameId: Int): Game? {
        val token = getAuthTokenIfNeeded()
        if (token == null) {
            Log.e("IgdbService", "Auth token is null, cannot fetch game details.")
            return null
        }

        // This APICalypse query requests all the fields from your GameDetails model
        val apiQuery = "fields id, name, cover.image_id, artworks.image_id, genres.name, " +
                "summary, first_release_date; " +
                "where id = $gameId; " +
                "limit 1;"

        return try {
            val response = httpClient.post("https://api.igdb.com/v4/games") {
                headers {
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${token.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(apiQuery)
            }

            // The API returns a list, even for a single ID. We take the first result.
            response.body<List<Game>>().firstOrNull()

        } catch (e: Exception) {
            Log.e("IgdbService", "Error fetching game details for ID $gameId", e)
            null
        }
    }
}