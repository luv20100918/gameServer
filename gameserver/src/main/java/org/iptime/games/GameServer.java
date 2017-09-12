package org.iptime.games;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

@Component
public class GameServer {

	@Autowired
	@Qualifier("tcpSocketAddress")
	private InetSocketAddress port;
	
	@Autowired
	@Qualifier("bossCount")
	private int bossCount;
	
	@Autowired
	@Qualifier("workerCount")
	private int workerCount;
	
	static final boolean SSL = System.getProperty("ssl") != null;
	
	public void start() throws Exception {
		final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
		
		EventLoopGroup bossGroup = new NioEventLoopGroup(bossCount);
		EventLoopGroup workerGroup = new NioEventLoopGroup(workerCount);
		
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
			.handler(new LoggingHandler(LogLevel.INFO))
			.childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    //p.addLast(new LoggingHandler(LogLevel.INFO));
                    p.addLast(new JsonObjectDecoder());
                    p.addLast(new EchoServerHandler());
                }
            });
			
			// Start the server.
            ChannelFuture f = b.bind(port).sync();

            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
		} catch(InterruptedException e) {
			e.printStackTrace();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	public static void main(String[] args) {
		AbstractApplicationContext springContext = null;
		
		try {
			springContext = new AnnotationConfigApplicationContext(GameServerConfig.class);
			
			springContext.registerShutdownHook();
			
			GameServer server = springContext.getBean(GameServer.class);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			springContext.close();
		}
	}
}
