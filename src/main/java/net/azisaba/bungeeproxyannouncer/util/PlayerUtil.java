package net.azisaba.bungeeproxyannouncer.util;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.AbstractMap;
import java.util.Map;

public class PlayerUtil {
    @SuppressWarnings("InfiniteRecursion") // not actually
    @NotNull
    public static Channel getChannel(@NotNull InboundConnection connection) {
        try {
            Field delegateField = connection.getClass().getDeclaredField("delegate");
            delegateField.setAccessible(true);
            Object delegate = delegateField.get(connection);
            return getChannel((InboundConnection) delegate);
        } catch (ReflectiveOperationException ignore) {}
        try {
            Object minecraftConnection = connection.getClass().getMethod("getConnection").invoke(connection);
            return (Channel) minecraftConnection.getClass().getMethod("getChannel").invoke(minecraftConnection);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public static String getIPAddress(Player player) {
        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) throw new RuntimeException("player is not connected via inet socket address");
        return address.getAddress().getHostAddress().replaceFirst("(.*)%.*", "$1");
    }

    @NotNull
    public static Map.Entry<String, Integer> getIPAndPort(Player player) {
        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) throw new RuntimeException("player is not connected via inet socket address");
        return new AbstractMap.SimpleImmutableEntry<>(getIPAddress(player), address.getPort());
    }
}
