package net.azisaba.bungeeproxyannouncer;

import io.netty.handler.codec.haproxy.HAProxyMessage;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerIPAddressList {
    public static final Map<Map.Entry<String, Integer>, String> map = new ConcurrentHashMap<>();

    @SuppressWarnings("unused") // used
    public static void handle(HAProxyMessage message, SocketAddress proxyHost) {
        if (!(proxyHost instanceof InetSocketAddress address)) return;
        map.put(new AbstractMap.SimpleImmutableEntry<>(message.sourceAddress(), message.sourcePort()), address.getAddress().getHostAddress().replaceFirst("(.*)%.*", "$1"));
    }

    public static void remove(SocketAddress playerHost) {
        if (!(playerHost instanceof InetSocketAddress address)) return;
        map.remove(new AbstractMap.SimpleImmutableEntry<>(address.getAddress().getHostAddress(), address.getPort()));
    }
}
