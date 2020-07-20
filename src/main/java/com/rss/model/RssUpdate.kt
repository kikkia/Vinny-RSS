package com.rss.model


/**
 * Post to forward to a channel that satisfies an rss subscription.
 */

data class RssUpdate(val id: Int, val channels: List<String>, val url: String)