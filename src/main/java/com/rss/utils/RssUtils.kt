package com.rss.utils

class RssUtils {
    companion object {
        fun getTwitterUrl(user: String, nitPath: String): String {
            return "$nitPath/$user/rss"
        }

        fun getRedditUrl(subreddit: String): String {
            return "https://old.reddit.com/r/$subreddit/new/.json"
        }

        fun getYoutubeUrl(userId: String): String {
            return "https://www.youtube.com/feeds/videos.xml?channel_id=$userId"
        }
    }
}