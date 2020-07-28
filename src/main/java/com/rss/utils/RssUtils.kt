package com.rss.utils

class RssUtils {
    companion object {
        fun getTwitterUrl(user: String): String {
            return "https://nitter.net/$user/rss"
        }

        fun getRedditUrl(subreddit: String): String {
            return "https://old.reddit.com/r/$subreddit/new/.json"
        }

        fun getYoutubeUrl(userId: String): String {
            return "https://www.youtube.com/feeds/videos.xml?channel_id=$userId"
        }
    }
}