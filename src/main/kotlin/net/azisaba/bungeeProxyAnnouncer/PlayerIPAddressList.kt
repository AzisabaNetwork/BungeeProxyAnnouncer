package net.azisaba.bungeeProxyAnnouncer

import io.netty.handler.codec.haproxy.HAProxyMessage
import java.net.InetSocketAddress
import java.net.SocketAddress

object PlayerIPAddressList {
    val map = mutableMapOf<Pair<String, Int>, String>()

    @Suppress("unused")
    @JvmStatic
    fun handle(message: HAProxyMessage, proxyHost: SocketAddress) {
        if (proxyHost !is InetSocketAddress) return
        message.sourceAddress()
        map[message.sourceAddress() to message.sourcePort()] = proxyHost.address.hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")
    }

    @Suppress("unused")
    @JvmStatic
    fun remove(playerHost: SocketAddress) {
        if (playerHost !is InetSocketAddress) return
        map.remove(playerHost.address.hostAddress to playerHost.port)
    }
}
