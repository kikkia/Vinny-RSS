package com.rss.clients

import com.rss.model.RedditResponse
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.stereotype.Component

@Component class HttpClient(private val client: OkHttpClient) {
    fun getStringResponse(url: String) : String {
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response -> return response.body!!.string() }
    }

    fun getJsonResponse(url: String) : JSONObject {
        return JSONObject(getStringResponse(url))
    }

    fun getRedditJsonResponse(url: String, sessionTracker: String) : RedditResponse {
        val builder = Request.Builder()
                .url(url)

        if (sessionTracker.isNotBlank()) {
            builder.addHeader("cookie", "session_tracker=" + sessionTracker)
        }

        client.newCall(builder.build()).execute().use { response ->
            var newSessionTracker = ""
            val iter = response.headers.iterator()
            var debug = ""
            while (iter.hasNext()) {
                val header = iter.next()
                if (header.first == "set-cookie") {
                    if (header.second.startsWith("session_tracker=")) {
                        newSessionTracker = header.second.split(";")[0].replace("session_tracker=", "")
                        break
                    }
                }
                if (header.first == "x-ratelimit-remaining") {
                    debug += header.second + " "
                } else if (header.first == "x-ratelimit-reset") {
                    debug += header.second + " "
                }
            }
            println(debug)
            var json = JSONObject()
            if (response.code == 200) {
                json = JSONObject(response.body!!.string())
            }
            return RedditResponse(json, newSessionTracker, response.code)
        }
    }

    fun postJsonResponse(url: String) : JSONObject {
        return JSONObject(postStringResponse(url))
    }

    fun getJsonResponseWithHeaders(url: String, headers : List<Pair<String, String>>) : JSONObject {
        val builder = Request.Builder()
                .url(url)

        for (header in headers) {
            builder.addHeader(header.first, header.second)
        }

        client.newCall(builder.build()).execute().use { response -> return JSONObject(response.body!!.string()) }
    }

    fun postStringResponse(url: String) : String {
        val request = Request.Builder().url(url).post(FormBody.Builder().build()).build()

        client.newCall(request).execute().use { response -> return response.body!!.string() }
    }
}