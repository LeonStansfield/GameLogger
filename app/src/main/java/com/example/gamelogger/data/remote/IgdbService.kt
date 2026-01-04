package com.example.gamelogger.data.remote

import android.util.Log
import com.example.gamelogger.BuildConfig
import com.example.gamelogger.data.model.AuthToken
import com.example.gamelogger.data.model.Game
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
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
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.util.Calendar

/**
 * Custom exception for network-related errors.
 */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Custom exception for API-related errors.
 */
class ApiException(message: String, val statusCode: Int? = null) : Exception(message)

open class IgdbService {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            // Use the reusable instance here
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000 // 30 seconds
            connectTimeoutMillis = 15_000 // 15 seconds
            socketTimeoutMillis = 30_000 // 30 seconds
        }
    }

    private var authToken: AuthToken? = null
    private var tokenExpirationTime: Long = 0L

    // Reusable auth token function
    private suspend fun getAuthTokenIfNeeded(): AuthToken? {
        // Check if token is still valid (with 5 minute buffer)
        if (authToken != null && System.currentTimeMillis() < tokenExpirationTime - 300_000) {
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
                val token = response.body<AuthToken>()
                // Set expiration time (tokens typically last ~60 days, but we'll use expiresIn if available)
                tokenExpirationTime = System.currentTimeMillis() + (token.expiresIn * 1000L)
                token
            } else {
                val errorBody = response.bodyAsText()
                Log.e("IgdbService", "Error from auth server: ${response.status} - $errorBody")
                throw ApiException("Authentication failed: ${response.status}", response.status.value)
            }
        } catch (e: CancellationException) {
            // Re-throw cancellation to allow proper coroutine cancellation
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            Log.e("IgdbService", "An unexpected error occurred getting auth token.", e)
            throw NetworkException("Failed to authenticate with game service", e)
        }

        return authToken
    }


    // --- Discover Function ---
    open suspend fun getTop20TrendingGames(): List<Game> {
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
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${token.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    "fields id, name, cover.image_id, first_release_date, total_rating, total_rating_count; " +
                            "where first_release_date > $twoYearsAgoTimestamp & total_rating_count > 25; " +
                            "sort total_rating_count desc; " +
                            "limit 20;"
                )
            }.body()
        } catch (e: Exception) {
            Log.e("IgdbService", "Error fetching trending games", e)
            emptyList()
        }
    }

    // --- Search Function ---
    open suspend fun searchGames(query: String): List<Game> {
        val token = getAuthTokenIfNeeded()
        if (token == null) {
            Log.e("IgdbService", "Auth token is null, cannot search games.")
            return emptyList()
        }

        // Defensive sanitization - second layer of protection
        val sanitizedQuery = query.trim()
            .replace("\"", "")
            .replace(";", "")
            .replace("\\", "")
            .replace("'", "")
            .trim()
            .take(100)

        // Prevent empty or too short queries from hitting the API
        if (sanitizedQuery.length < 2) {
            Log.w("IgdbService", "Query too short after sanitization: '$query' -> '$sanitizedQuery'")
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
                    "search \"$sanitizedQuery\"; " +
                            "fields id, name, cover.image_id; " +
                            "limit 20;"
                )
            }.body()
        } catch (e: Exception) {
            Log.e("IgdbService", "Error searching games", e)
            emptyList()
        }
    }
    open suspend fun getGameDetails(gameId: Int): Game? {
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

    // --- Random Game Function (Time Traveler Strategy) ---
    open suspend fun getRandomGame(): Game? {
        val token = getAuthTokenIfNeeded()
        if (token == null) {
            Log.e("IgdbService", "Auth token is null, cannot fetch random game.")
            return null
        }

        // 1. Pick a random timestamp from the last 10 years.
        // This acts as a Random Seed
        val currentTimestamp = System.currentTimeMillis() / 1000
        val tenYearsInSeconds = 315_569_260L // Approx 10 years
        val startTimestamp = currentTimestamp - tenYearsInSeconds
        val randomDate = (startTimestamp..currentTimestamp).random()

        return try {
            httpClient.post("https://api.igdb.com/v4/games") {
                headers {
                    append("Client-ID", BuildConfig.IGDB_CLIENT_ID)
                    append("Authorization", "Bearer ${token.accessToken}")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    "fields id, name, cover.image_id, total_rating, total_rating_count, first_release_date; " +
                            // Find the first game released BEFORE random date.
                            "where first_release_date < $randomDate & total_rating_count > 5; " +
                            "sort first_release_date desc; " +
                            "limit 1;"
                )
            }.body<List<Game>>().firstOrNull()

        } catch (e: Exception) {
            Log.e("IgdbService", "Error fetching random game, falling back to top20trending game.", e)
            getTop20TrendingGames().randomOrNull()
        }
    }
}
