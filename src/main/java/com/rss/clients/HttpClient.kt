package com.rss.clients

import com.rss.model.HttpRequestException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component class HttpClient(private val client: OkHttpClient) {
    private fun getStringResponse(url: String) : String {
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            try {
                return response.body!!.string()
            } catch (e : Exception) {
                throw HttpRequestException(e.message, response.code, e)
            }
        }
    }

    @Throws(HttpRequestException::class)
    fun getJsonResponse(url: String) : JSONObject {
        return JSONObject(getStringResponse(url))
    }

    fun getJsonResponseWithHeaders(url: String, headers : List<Pair<String, String>>) : JSONObject {
        val builder = Request.Builder()
                .url(url)

        for (header in headers) {
            builder.addHeader(header.first, header.second)
        }

        client.newCall(builder.build()).execute().use { response ->
            try {
                return JSONObject(response.body!!.string())
            } catch (e: Exception) {
                throw HttpRequestException(e.message, response.code, e)
            }
        }
    }
}