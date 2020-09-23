package com.rss.clients.networking.address

import java.math.BigInteger
import java.net.InetAddress

abstract class IpBlock<I : InetAddress?> {
    abstract val randomAddress: I
    open fun getAddressAtIndex(index: Long): I {
        return getAddressAtIndex(BigInteger.valueOf(index))
    }

    open fun getAddressAtIndex(index: BigInteger): I {
        return getAddressAtIndex(index.toLong())
    }

    abstract val type: Class<I>?
    abstract val size: BigInteger?
    abstract val maskBits: Int
}