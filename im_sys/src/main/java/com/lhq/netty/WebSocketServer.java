package com.lhq.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.springframework.stereotype.Component;

@Component
public class WebSocketServer {
    private static class SigletionWSServer{
        static final WebSocketServer instance = new WebSocketServer();
    }
    public static WebSocketServer getInstance(){
        return SigletionWSServer.instance;
    }
    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;
    public WebSocketServer(){
        mainGroup = new NioEventLoopGroup();
        subGroup = new NioEventLoopGroup();
        server = new ServerBootstrap();
        server.group(subGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new WSServerInit());
    }
    public void start(){
        this.future = server.bind(8888);
        System.err.println("netty websocket server 启动完毕...");
    }
}
