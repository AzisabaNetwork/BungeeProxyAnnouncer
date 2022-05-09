package net.azisaba.bungeeproxyannouncer.connection;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import net.azisaba.bungeeproxyannouncer.PlayerIPAddressList;
import org.jetbrains.annotations.NotNull;

@ChannelHandler.Sharable
public class HAProxyMessageHandler extends ChannelInboundHandlerAdapter {
    public static final HAProxyMessageHandler INSTANCE = new HAProxyMessageHandler();

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
        try {
            PlayerIPAddressList.remove(ctx.channel().remoteAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.channelInactive(ctx);
    }
}
