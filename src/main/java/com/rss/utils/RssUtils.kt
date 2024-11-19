package com.rss.utils

import com.rss.model.RssProvider

class RssUtils {
    companion object {
        fun getTwitterUrl(user: String, nitPath: String): String {
            val cleanedUser = user.replace("@", "")
            return "$nitPath/$cleanedUser/rss"
        }

        fun getRedditUrl(subreddit: String): String {
            return "https://old.reddit.com/r/$subreddit/new/.json"
        }

        fun getYoutubeUrl(userId: String): String {
            return "https://www.youtube.com/feeds/videos.xml?channel_id=$userId"
        }

        fun getYoutubeLiveUrl(videoId: String, key: String) : String {
            return "https://www.googleapis.com/youtube/v3/videos?part=liveStreamingDetails&id=$videoId&key=$key"
        }

        fun get4ChanUrl(board: String) : String {
            return "https://boards.4chan.org/$board/index.rss"
        }

        fun getTwitchUrl(channelId: String) : String {
            return "https://api.twitch.tv/helix/streams?user_id=$channelId"
        }

        fun getTwitchUrlUsername(username: String): String {
            return "https://api.twitch.tv/helix/streams?user_login=$username"
        }

        fun getSteamUrl(gameId: String) : String {
            return "http://api.steampowered.com/ISteamNews/GetNewsForApp/v0002/?appid=$gameId&count=3&maxlength=300&format=json"
        }

        fun getMinIntervalForProvider(provider: RssProvider) : Long {
            return when (provider) {
                RssProvider.CHAN -> 20000
                RssProvider.TWITTER -> 20000
                RssProvider.REDDIT -> 60000
                RssProvider.TWITCH -> 20000
                else -> 60000
            }
        }

        fun getBdoUrls() : Map<String, String> {
            return mapOf(
                    Pair("news",
                        "https://community.blackdesertonline.com/index.php?forums/news-announcements.181/index.rss"),
                    Pair("patch notes",
                        "https://community.blackdesertonline.com/index.php?forums/patch-notes.5/index.rss"))
        }
    }
}