package com.rss.model

import org.json.JSONObject

data class RedditResponse(val json: JSONObject, val sessionTracker: String, val status: Int) {
}