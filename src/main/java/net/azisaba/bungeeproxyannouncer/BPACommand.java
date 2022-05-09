package net.azisaba.bungeeproxyannouncer;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.azisaba.bungeeproxyannouncer.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BPACommand(ProxyServer proxy) {
    private static final GsonComponentSerializer GSON_COMPONENT_SERIALIZER = GsonComponentSerializer.gson();
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .extractUrls()
            .build();

    public static LiteralArgumentBuilder<CommandSource> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    public static <T> RequiredArgumentBuilder<CommandSource, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestHosts() {
        return (source, builder) -> suggest(PlayerIPAddressList.map.values().stream(), builder);
    }

    @Contract(pure = true)
    public static @NotNull SuggestionProvider<CommandSource> suggestPlayers(ProxyServer proxy) {
        return (source, builder) -> suggest(proxy.getAllPlayers().stream().map(Player::getUsername), builder);
    }

    @NotNull
    public static CompletableFuture<Suggestions> suggest(@NotNull Stream<String> suggestions, @NotNull SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase(Locale.ROOT);
        suggestions.filter((suggestion) -> matchesSubStr(input, suggestion.toLowerCase(Locale.ROOT))).forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static boolean matchesSubStr(@NotNull String input, @NotNull String suggestion) {
        for(int i = 0; !suggestion.startsWith(input, i); ++i) {
            i = suggestion.indexOf('_', i);
            if (i < 0) {
                return false;
            }
        }
        return true;
    }

    public BrigadierCommand create() {
        return new BrigadierCommand(literal("bpa")
                .requires(source -> source.hasPermission("bungeeproxyannouncer.command.bpa"))
                .then(literal("announce")
                        .then(argument("host", StringArgumentType.word())
                                .suggests(suggestHosts())
                                .then(argument("message", StringArgumentType.greedyString())
                                        .executes(context -> executeAnnounce(context.getSource(), StringArgumentType.getString(context, "host"), StringArgumentType.getString(context, "message")))
                                )
                        )
                )
                .then(literal("list")
                        .then(argument("host", StringArgumentType.word())
                                .suggests(suggestHosts())
                                .executes(context -> executeList(context.getSource(), StringArgumentType.getString(context, "host")))
                        )
                )
                .then(literal("check")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(suggestPlayers(proxy))
                                .executes(context -> executeCheck(context.getSource(), StringArgumentType.getString(context, "player")))
                        )
                )
        );
    }

    private int executeAnnounce(CommandSource source, String host, String message) {
        try {
            final Component component;
            Component component1 = null;
            try {
                if (message.startsWith("{") && message.startsWith("[")) {
                    component1 = GSON_COMPONENT_SERIALIZER.deserialize(message);
                }
            } catch (Exception ignore) {
            }
            if (component1 == null) {
                component1 = LEGACY_COMPONENT_SERIALIZER.deserialize(message);
            }
            component = component1;
            var ip = InetAddress.getByName(host).getHostAddress().replaceFirst("(.*)%.*", "$1");
            proxy.getAllPlayers()
                    .stream()
                    .filter(player -> player.getRemoteAddress() != null)
                    .filter(player -> ip.equals(PlayerIPAddressList.map.get(PlayerUtil.getIPAndPort(player))))
                    .forEach(player -> player.sendMessage(component));
            source.sendMessage(Component.text("中継鯖 " + ip + " から接続している全プレイヤーに以下のメッセージを表示しました", NamedTextColor.GREEN));
            source.sendMessage(component);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int executeList(CommandSource source, String host) {
        try {
            var ip = InetAddress.getByName(host).getHostAddress().replaceFirst("(.*)%.*", "$1");
            var players = proxy
                    .getAllPlayers()
                    .stream()
                    .filter(player -> player.getRemoteAddress() != null)
                    .filter(player -> ip.equals(PlayerIPAddressList.map.get(PlayerUtil.getIPAndPort(player))))
                    .map(player -> Component.text(player.getUsername(), NamedTextColor.YELLOW))
                    .toList();
            source.sendMessage(Component.text("Players on " + ip + " (in current proxy): ", NamedTextColor.GREEN)
                    .append(Component.join(Component.text(", ", NamedTextColor.WHITE), players)));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int executeCheck(CommandSource source, String playerName) {
        proxy.getPlayer(playerName).ifPresent(player -> {
            var ip = PlayerIPAddressList.map.get(PlayerUtil.getIPAndPort(player));
            source.sendMessage(Component.empty()
                    .append(Component.text("Player " + playerName + " is connecting from ", NamedTextColor.GREEN))
                    .append(Component.text(String.valueOf(ip), NamedTextColor.YELLOW))
                    .append(Component.text(".", NamedTextColor.GREEN)));
        });
        return 0;
    }
}
