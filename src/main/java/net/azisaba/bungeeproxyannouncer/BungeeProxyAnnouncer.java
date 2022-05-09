package net.azisaba.bungeeproxyannouncer;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import javassist.ClassPool;
import net.azisaba.bungeeproxyannouncer.connection.HAProxyMessageHandler;
import net.azisaba.bungeeproxyannouncer.util.PlayerUtil;
import net.blueberrymc.nativeutil.NativeUtil;
import org.slf4j.Logger;

@Plugin(id = "bungee-proxy-announcer", name = "BungeeProxyAnnouncer", version = "2.0.0")
public class BungeeProxyAnnouncer {
    private final ProxyServer server;
    private final Logger logger;
    private final BPACommand command;

    @Inject
    public BungeeProxyAnnouncer(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.command = new BPACommand(server);
        try {
            transform();
        } catch (Exception | LinkageError e) {
            throw new RuntimeException("Could not register transformer", e);
        }
    }

    private void transform() throws Exception {
        var classpath = BungeeProxyAnnouncer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        NativeUtil.appendToSystemClassLoaderSearch(classpath);
        NativeUtil.registerClassLoadHook((classLoader, s, aClass, protectionDomain, bytes) -> {
            if (!s.equals("com/velocitypowered/proxy/connection/MinecraftConnection")) {
                return null;
            }
            try {
                var cp = ClassPool.getDefault();
                logger.info("Adding {} to classpath", classpath);
                cp.appendClassPath(classpath);
                var cc = cp.get(s.replace("/", "."));
                var methodChannelRead = cc.getMethod("channelRead", "(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V");
                var methodBody = """
                        try {
                            if ($2 instanceof io.netty.handler.codec.haproxy.HAProxyMessage) {
                                net.azisaba.bungeeproxyannouncer.PlayerIPAddressList.handle((io.netty.handler.codec.haproxy.HAProxyMessage) $2, channel.remoteAddress());
                            }
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                """;
                methodChannelRead.insertBefore(methodBody);
                return cc.toBytecode();
            } catch (Exception e) {
                logger.error("Failed to transform MinecraftConnection", e);
            }
            return null;
        });
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e) {
        server.getCommandManager().register(command.create());
    }

    @Subscribe
    public void onPreLogin(ConnectionHandshakeEvent e) {
        PlayerUtil.getChannel(e.getConnection())
                .pipeline()
                .addBefore("handler", "bungeeproxyannouncer", HAProxyMessageHandler.INSTANCE);
    }
}
