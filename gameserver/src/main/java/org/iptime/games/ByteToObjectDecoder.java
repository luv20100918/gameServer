package org.iptime.games;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

@Sharable
public class ByteToObjectDecoder extends SimpleChannelInboundHandler<GameMessage> {
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, GameMessage msg) throws Exception {
		ctx.fireChannelRead(msg.getMessage());
	}

}
