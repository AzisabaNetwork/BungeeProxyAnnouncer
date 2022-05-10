package net.azisaba.bungeeproxyannouncer.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.netty.channel.Channel;
import net.azisaba.bungeeproxyannouncer.BPACommand;
import net.azisaba.bungeeproxyannouncer.PlayerIPAddressList;
import net.azisaba.velocityredisbridge.VelocityRedisBridge;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Map;

public class PlayerUtil {
    private static final Gson GSON = new Gson();
    public static final String ID_ANNOUNCE = "bpa:announce";

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
        return player.getRemoteAddress().getAddress().getHostAddress().replaceFirst("(.*)%.*", "$1");
    }

    @NotNull
    public static Map.Entry<String, Integer> getIPAndPort(Player player) {
        return new AbstractMap.SimpleImmutableEntry<>(getIPAddress(player), player.getRemoteAddress().getPort());
    }

    public static void announce(@NotNull ProxyServer proxy, @NotNull String ip, @NotNull Component component, boolean notifyOthers) {
        if (notifyOthers) {
            JsonObject obj = new JsonObject();
            obj.add("data", BPACommand.GSON_COMPONENT_SERIALIZER.serializeToTree(component));
            obj.addProperty("ip", ip);
            VelocityRedisBridge.getApi().getPubSubHandler().publish(ID_ANNOUNCE, GSON.toJson(obj), true);
        }
        proxy.getAllPlayers()
                .stream()
                .filter(player -> ip.equals(PlayerIPAddressList.map.get(PlayerUtil.getIPAndPort(player))))
                .forEach(player -> player.sendMessage(component));
    }

    /**
     * Load PlayerUtil class so VelocityRedisBridge class can be loaded properly.
     */
    public static void load() {}
}
