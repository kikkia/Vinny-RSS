package com.rss.clients

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component class HttpClient(private val client: OkHttpClient) {
    private fun getStringResponse(url: String) : String {
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response -> return response.body!!.string() }
    }

    fun getJsonResponse(url: String) : JSONObject {
        return JSONObject(getStringResponse(url))
    }
}