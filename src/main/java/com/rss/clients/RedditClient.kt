package com.rss.clients

import com.rss.config.properties.RedditProperties
import com.rss.model.RedditResponse
import com.rss.utils.RssUtils
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.ceil

@Component
class RedditClient(
    client: OkHttpClient,
    private val properties: RedditProperties,
    private val clock: Clock
) {
    @Autowired
    constructor(client: OkHttpClient, properties: RedditProperties) : this(client, properties, Clock.systemUTC())

    // Do not follow redirects with OAuth credentials, including same-host redirects.
    private val client = client.newBuilder().followRedirects(false).followSslRedirects(false).build()
    private var token: String? = null
    private var tokenExpiresAt: Instant = Instant.EPOCH
    @Volatile private var rateLimitedUntil: Instant = Instant.EPOCH

    @Throws(IOException::class)
    fun getNewPosts(subreddit: String): RedditResponse {
        val accessToken = getAccessToken()
        val response = getListing(subreddit, accessToken)
        if (response.status != 401) return response

        // A revoked token can fail before expires_in. Renew and retry only once.
        val replacement = getAccessToken(accessToken)
        val retried = getListing(subreddit, replacement)
        if (retried.status == 401) invalidateToken(replacement)
        return retried
    }

    @Synchronized
    private fun getAccessToken(rejectedToken: String? = null): String {
        checkRateLimit()
        if (token == rejectedToken) token = null
        val cached = token
        if (cached != null && clock.instant().plusSeconds(30).isBefore(tokenExpiresAt)) return cached

        require(properties.clientId.isNotBlank()) { "Configure reddit.client-id (REDDIT_CLIENTID)" }
        require(properties.userAgent.isNotBlank()) { "Configure reddit.user-agent (REDDIT_USERAGENT)" }
        require(properties.deviceId.length in 20..30 && properties.deviceId.all { it.code in 33..126 }) {
            "reddit.device-id must be a stable, unique 20-30 character ASCII identifier"
        }

        val request = Request.Builder()
            .url("https://www.reddit.com/api/v1/access_token")
            .header("User-Agent", properties.userAgent)
            .header("Authorization", Credentials.basic(properties.clientId, ""))
            .post(FormBody.Builder()
                .add("grant_type", "https://oauth.reddit.com/grants/installed_client")
                .add("device_id", properties.deviceId)
                .build())
            .build()

        val requestedAt = clock.instant()
        client.newCall(request).execute().use { response ->
            observeRateLimit(response)
            if (!response.isSuccessful) throw IOException("Reddit OAuth token request failed (HTTP ${response.code})")
            val json = parseJson(response.body?.string().orEmpty(), "OAuth token")
            val value = json.optString("access_token")
            val expiresIn = json.optLong("expires_in", 0)
            if (value.isBlank() || expiresIn <= 0 || !json.optString("token_type").equals("bearer", true)) {
                throw IOException("Reddit OAuth token response is missing a bearer token or valid expiry")
            }
            tokenExpiresAt = requestedAt.plusSeconds(expiresIn)
            token = value
            return value
        }
    }

    @Synchronized
    private fun invalidateToken(rejectedToken: String) {
        if (token == rejectedToken) token = null
    }

    private fun getListing(subreddit: String, accessToken: String): RedditResponse {
        checkRateLimit()
        val request = Request.Builder()
            .url(RssUtils.getRedditUrl(subreddit))
            .header("User-Agent", properties.userAgent)
            .header("Authorization", "Bearer $accessToken")
            .build()

        client.newCall(request).execute().use { response ->
            observeRateLimit(response)
            val body = response.body?.string().orEmpty()
            val json = if (response.isSuccessful) {
                parseJson(body, "listing")
            } else {
                // Reddit sometimes returns HTML errors. Preserve the status either way.
                try { JSONObject(body) } catch (_: JSONException) { JSONObject() }
            }
            return RedditResponse(json, response.code)
        }
    }

    private fun checkRateLimit() {
        if (clock.instant().isBefore(rateLimitedUntil)) {
            throw IOException("Reddit API rate limited until $rateLimitedUntil")
        }
    }

    @Synchronized
    private fun observeRateLimit(response: Response) {
        val exhausted = response.header("x-ratelimit-remaining")?.toDoubleOrNull()?.let { it <= 0 } ?: false
        if (response.code != 429 && !exhausted) return

        val now = clock.instant()
        val retryAfter = response.header("Retry-After")
        val seconds = retryAfter?.toDoubleOrNull()
            ?: response.header("x-ratelimit-reset")?.toDoubleOrNull()
        val retryDate = if (retryAfter != null && retryAfter.toDoubleOrNull() == null) {
            try {
                ZonedDateTime.parse(retryAfter, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            } catch (_: DateTimeParseException) { null }
        } else null
        val until = retryDate ?: now.plusSeconds(ceil(seconds?.takeIf { it.isFinite() && it > 0 } ?: 60.0).toLong())
        rateLimitedUntil = maxOf(rateLimitedUntil, now.plusSeconds(1), until)
        if (response.code == 429) throw IOException("Reddit API rate limited until $rateLimitedUntil")
    }

    private fun parseJson(body: String, description: String): JSONObject {
        try {
            return JSONObject(body)
        } catch (_: JSONException) {
            // Do not include response bodies or parser exceptions: token responses contain credentials.
            throw IOException("Reddit $description response was not valid JSON")
        }
    }
}
