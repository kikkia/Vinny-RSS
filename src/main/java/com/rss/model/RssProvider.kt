package com.rss.model

enum class RssProvider(val value: Int) {
    REDDIT(1), TWITTER(2), OTHER(0);

    companion object {
        fun getProvider(value: Int): RssProvider {
            return when (value) {
                1 -> REDDIT
                2 -> TWITTER
                else -> OTHER
            }
        }
    }
}