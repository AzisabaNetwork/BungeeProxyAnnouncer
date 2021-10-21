package net.azisaba.bungeeProxyAnnouncer

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.CommandSender
import net.md_5.bungee.api.ProxyServer
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.connection.ProxiedPlayer
import net.md_5.bungee.api.plugin.Command
import net.md_5.bungee.api.plugin.TabExecutor
import net.md_5.bungee.chat.ComponentSerializer
import java.net.InetAddress
import java.net.InetSocketAddress

object BPACommand: Command("bpa", "bungeeproxyannouncer.command.bpa", "bungeeproxyannouncer"), TabExecutor {
    private val commands = listOf("announce", "a", "list", "l", "check", "c")

    override fun execute(sender: CommandSender, args: Array<String>) {
        if (args.isEmpty()) return sender.sendMessage(*TextComponent.fromLegacyText("/bpa [announce|list|check]"))
        if (args[0] == "announce" || args[0] == "a") {
            if (args.size < 3) {
                sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.RED}/bpa announce <IP Address of proxy server> <message>"))
                return
            }
            val list = args.toMutableList()
            list.removeAt(0)
            Thread {
                val ip = InetAddress.getByName(list.removeAt(0)).getIPAddress()
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
            }.start()
        } else if (args[0] == "list" || args[0] == "l") {
            if (args.size < 2) {
                sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.RED}/bpa list <IP Address of proxy server>"))
                return
            }
            Thread {
                val ip = InetAddress.getByName(args[1]).getIPAddress()
                val players = ChatColor.YELLOW.toString() + ProxyServer.getInstance().players
                    .toMutableList()
                    .filter { it.socketAddress is InetSocketAddress }
                    .filter { PlayerIPAddressList.map[it.getIPAndPort()] == ip }
                    .joinToString("${ChatColor.WHITE}, ${ChatColor.YELLOW}")
                sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.GREEN}${ip}で接続しているプレイヤー: $players"))
            }.start()
        } else if (args[0] == "check" || args[0] == "c") {
            if (args.size < 2) {
                sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.RED}/bpa check <Player>"))
                return
            }
            ProxyServer.getInstance().getPlayer(args[1])?.let { player ->
                if (player.socketAddress !is InetSocketAddress) return player.sendMessage(*TextComponent.fromLegacyText("?^1"))
                PlayerIPAddressList.map[player.getIPAndPort()]?.let { ip ->
                    sender.sendMessage(*TextComponent.fromLegacyText("${ChatColor.GREEN}${player.name}は${ChatColor.YELLOW}$ip${ChatColor.GREEN}から接続しています。"))
                }
            }
        }
    }

    override fun onTabComplete(sender: CommandSender, args: Array<String>): Iterable<String> {
        if (args.isEmpty()) return emptyList()
        if (args.size == 1) return commands.filter(args[0])
        if (args.size == 2) {
            if (args[0] == "announce" || args[0] == "a" || args[0] == "list" || args[0] == "l") {
                return PlayerIPAddressList.map.values.toList().filter(args[1])
            } else if (args[0] == "check" || args[0] == "c") {
                return ProxyServer.getInstance().players.map { it.name }.filter(args[1])
            }
        }
        return emptyList()
    }

    private fun List<String?>.filter(s: String): List<String> = distinct().filterNotNull().filter { s1 -> s1.lowercase().startsWith(s.lowercase()) }

    private fun InetSocketAddress.getIPAddress() = address.getIPAddress()
    private fun InetAddress.getIPAddress() = hostAddress.replaceFirst("(.*)%.*".toRegex(), "$1")
    private fun ProxiedPlayer.getIPAddress(): String {
        require(socketAddress is InetSocketAddress) { "Player $name is connecting via unix socket" }
        return (socketAddress as InetSocketAddress).getIPAddress()
    }
    private fun ProxiedPlayer.getIPAndPort(): Pair<String, Int> =
        getIPAddress() to (this.socketAddress as InetSocketAddress).port
}
