package com.rss.db.model

import com.rss.utils.RssProvider
import java.sql.Timestamp
import java.time.Instant

data class RssSubscriptionDTO(val id: Int, val url: String, val provider: Int, var lastScanAttempt: Instant, var lastScanComplete: Instant)