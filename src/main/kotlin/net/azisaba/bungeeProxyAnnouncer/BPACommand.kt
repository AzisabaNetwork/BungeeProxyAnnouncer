package net.azisaba.bungeeProxyAnnouncer

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.chat.ComponentSerializer
import java.net.InetAddress
import java.net.InetSocketAddress

object BPACommand: Command("bpa", "bungeeproxyannouncer.command.bpa", "bungeeproxyannouncer") {
    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.size < 2) {
            sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.RED}/bpa <IP Address of proxy server> <message>"))
            return
        }
        val list = args.toMutableList()
        val ip = list.removeAt(0)
        val message = ChatColor.translateAlternateColorCodes('&', list.joinToString(" "))
        ProxyServer.getInstance().players
            .filter { it.socketAddress is InetSocketAddress }
            .filter { PlayerIPAddressList.map[it.getIPAndPort()] == ip }
            .forEach {
                if (!message.startsWith("[") && !message.startsWith("{")) {
                    return@forEach it.sendMessage(*TextComponent.fromLegacyText(message))
                }
                try {
                    it.sendMessage(*ComponentSerializer.parse(message))
                } catch (e: Exception) {
                    it.sendMessage(*TextComponent.fromLegacyText(message))
                }
            }
    }

    private fun InetSocketAddress.getIPAddress() = address.getIPAddress()
    private fun InetAddress.getIPAddress() = hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")
    private fun ProxiedPlayer.getIPAddress(): String {
        require(socketAddress is InetSocketAddress) { "Player $name is connecting via unix socket" }
        return (socketAddress as InetSocketAddress).getIPAddress()
    }
    private fun ProxiedPlayer.getIPAndPort(): Pair<String, Int> =
        getIPAddress() to (this.socketAddress as InetSocketAddress).port
}
