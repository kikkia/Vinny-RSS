package com.rss.clients.networking.address

import com.rss.utils.ThiccRandom
import org.slf4j.LoggerFactory
import java.math.BigInteger
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.regex.Pattern
import kotlin.experimental.and


class Ipv6Block(cidr: String) : IpBlock<Inet6Address?>() {
    private val cidr: String
    override val maskBits: Int
    private val prefix: BigInteger


    override val randomAddress: Inet6Address
        get() {
            if (maskBits == IPV6_BIT_SIZE) return longToAddress(prefix)
            val randomAddressOffset: BigInteger = random.nextThiccInt(IPV6_BIT_SIZE - (maskBits + 1)).abs()
            val inetAddress = longToAddress(prefix.add(randomAddressOffset))
            log.info(inetAddress.toString())
            return inetAddress
        }

    override fun getAddressAtIndex(index: Long): Inet6Address {
        return getAddressAtIndex(BigInteger.valueOf(index))
    }

    override fun getAddressAtIndex(index: BigInteger): Inet6Address {
        require(index.compareTo(size) <= 0) { "Index out of bounds for provided CIDR Block" }
        return longToAddress(prefix.add(index))
    }

    override val size: BigInteger
        get() = TWO.pow(IPV6_BIT_SIZE - maskBits)

    override fun toString(): String {
        return cidr
    }

    companion object {
        fun isIpv6CidrBlock(cidr: String): Boolean {
            var cidr = cidr
            if (!cidr.contains("/")) cidr += "/128"
            return CIDR_REGEX.matcher(cidr).matches()
        }

        private val TWO = BigInteger.valueOf(2)
        private val BITS1 = BigInteger.valueOf(-1)
        val BLOCK64_IPS = TWO.pow(64)
        const val IPV6_BIT_SIZE = 128
        private val random: ThiccRandom = ThiccRandom()
        private val CIDR_REGEX = Pattern.compile("([\\da-f:]+)/(\\d{1,3})")
        private val log = LoggerFactory.getLogger(Ipv6Block::class.java)

        private fun longToAddress(l: BigInteger): Inet6Address {
            val b = ByteArray(IPV6_BIT_SIZE / 8)
            val start = (b.size - 1) * 8
            for (i in b.indices) {
                val shift = start - i * 8
                if (shift > 0) b[i] = l.shiftRight(start - i * 8).toByte() else b[i] = l.toByte()
            }
            return try {
                Inet6Address.getByAddress(b) as Inet6Address
            } catch (e: UnknownHostException) {
                throw RuntimeException(e) // This should not happen, as we do not do a DNS lookup
            }
        }

        private fun addressToLong(address: Inet6Address): BigInteger {
            return bytesToLong(address.address)
        }

        private fun bytesToLong(b: ByteArray): BigInteger {
            var value = BigInteger.valueOf(0)
            val start = (b.size - 1) * 8
            value = value.or(BigInteger.valueOf(b[0].toLong()).shiftLeft(start))
            for (i in 1 until b.size) {
                val shift = start - i * 8
                value = if (shift > 0) {
                    value.or(BigInteger.valueOf((b[i] and 0xff.toByte()).toLong()).shiftLeft(shift))
                }
                else {
                    value.or(BigInteger.valueOf((b[i] and 0xff.toByte()).toLong()))
                }
            }
            return value
        }
    }

    init {
        var cidr = cidr
        if (!cidr.contains("/")) cidr += "/128"
        this.cidr = cidr.toLowerCase()
        val matcher = CIDR_REGEX.matcher(this.cidr)
        require(matcher.find()) { "$cidr does not appear to be a valid CIDR." }
        val unboundedPrefix: BigInteger
        unboundedPrefix = try {
            val address = InetAddress.getByName(matcher.group(1))
            addressToLong(address as Inet6Address)
        } catch (e: UnknownHostException) {
            throw IllegalArgumentException("Invalid IPv6 address", e)
        }
        maskBits = matcher.group(2).toInt()
        val prefixMask = BITS1.shiftLeft(IPV6_BIT_SIZE - maskBits - 1)
        prefix = unboundedPrefix.and(prefixMask)
        log.info("Using Ipv6Block with {} addresses", size)
    }

    override val type: Class<Inet6Address?>?
        get() = Inet6Address::class.java as Class<Inet6Address?>?
}