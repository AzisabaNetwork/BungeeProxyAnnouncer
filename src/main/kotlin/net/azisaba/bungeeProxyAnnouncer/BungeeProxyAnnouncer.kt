package net.azisaba.bungeeProxyAnnouncer

import javassist.ClassPool
import net.blueberrymc.native_util.NativeUtil
import net.md_5.bungee.api.plugin.Plugin

@Suppress("unused")
class BungeeProxyAnnouncer: Plugin() {
    override fun onEnable() {
        val classPath = BungeeProxyAnnouncer::class.java.protectionDomain.codeSource.location.toURI().path
        NativeUtil.appendToSystemClassLoaderSearch(classPath)
        NativeUtil.registerClassLoadHook { _, s, _, _, _ ->
            if (s != "net/md_5/bungee/netty/HandlerBoss") return@registerClassLoadHook null
            try {
                val cp = ClassPool.getDefault()
                println("Adding $classPath to classpath")
                cp.appendClassPath(classPath)
                val cc = cp.get(s.replace("/", "."))
                val methodChannelRead = cc.getMethod("channelRead", "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V")
                methodChannelRead.insertBefore("""
                    if ($2 instanceof io.netty.handler.codec.haproxy.HAProxyMessage) {
                        net.azisaba.bungeeProxyAnnouncer.PlayerIPAddressList.handle((io.netty.handler.codec.haproxy.HAProxyMessage) $2, channel.getRemoteAddress());
                    }
                """.trimIndent())
                val methodChannelInactive = cc.getMethod("channelInactive", "(Lio/netty/channel/ChannelHandlerContext;)V")
                methodChannelInactive.insertBefore("""
                    if (handler != null) {
                        net.azisaba.bungeeProxyAnnouncer.PlayerIPAddressList.remove(channel.getRemoteAddress());
                    }
                """.trimIndent())
                return@registerClassLoadHook cc.toBytecode()
            } catch (e: Exception) {
                logger.severe("Failed to transform net/md_5/bungee/netty/HandlerBoss")
                e.printStackTrace()
            }
            return@registerClassLoadHook null
        }
        @Suppress("unused")
        proxy.pluginManager.registerCommand(this, BPACommand)
    }
}
