package com.rss.utils

import com.rss.model.RssProvider

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

        fun get4ChanUrl(board: String) : String {
            return "https://boards.4chan.org/$board/index.rss"
        }

        fun getMinIntervalForProvider(provider: RssProvider) : Long {
            return when (provider) {
                RssProvider.CHAN -> 10000
                RssProvider.TWITTER -> 10000
                RssProvider.REDDIT -> 20000
                else -> 60000
            }
        }
    }
}