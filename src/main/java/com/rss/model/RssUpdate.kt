package com.rss.model


/**
 * Post to forward to a channel that satisfies an rss subscription.
 */

data class RssUpdate(val id: Int, val channel: String, val url: String, val provider: Int, val subject: String)