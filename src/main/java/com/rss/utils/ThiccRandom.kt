package com.rss.utils

import java.math.BigInteger
import java.util.*


class ThiccRandom : Random() {
    fun nextThiccInt(bits: Int): BigInteger {
        var bits = bits
        if (bits < 32) {
            return BigInteger.valueOf(next(31).toLong())
        }
        var value = BigInteger.ZERO
        var index = 0
        while (bits >= 32) {
            bits -= 32
            value = value.add(BigInteger.valueOf(next(32).toLong()).shiftLeft(index++ * 32))
        }
        if (bits > 0) value = value.add(BigInteger.valueOf(next(bits).toLong()).shiftLeft(index * 32))
        return value
    }
}