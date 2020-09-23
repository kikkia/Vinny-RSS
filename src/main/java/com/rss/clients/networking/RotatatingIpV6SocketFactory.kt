package com.rss.clients.networking

import com.rss.clients.networking.address.Ipv6Block
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.SocketFactory

class RotatatingIpV6SocketFactory(private val ipBlock: Ipv6Block) : SocketFactory() {
    private val systemFactory = getDefault()

    override fun createSocket(): Socket {
        val s = systemFactory.createSocket()
        s.bind(InetSocketAddress(ipBlock.randomAddress, 0))
        return s
    }

    override fun createSocket(host: String, port: Int): Socket {
        return systemFactory.createSocket(host, port, ipBlock.randomAddress, 0)
    }

    override fun createSocket(address: InetAddress, port: Int): Socket {
        return systemFactory.createSocket(address, port, ipBlock.randomAddress, 0)
    }

    override fun createSocket(
            host: String,
            port: Int,
            localAddr: InetAddress,
            localPort: Int
    ): Socket {
        return systemFactory.createSocket(host, port, localAddr, localPort)
    }

    override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddr: InetAddress,
            localPort: Int
    ): Socket {
        return systemFactory.createSocket(address, port, localAddr, localPort)
    }

    companion object {

        fun byIpv6CIDR(ipv6Cidr: String): SocketFactory? {
            return RotatatingIpV6SocketFactory(Ipv6Block(ipv6Cidr))
        }
    }
}