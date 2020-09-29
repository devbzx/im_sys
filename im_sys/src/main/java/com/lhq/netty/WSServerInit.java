package com.lhq.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;

public class WSServerInit extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel socketChannel){
        //获取管道
        ChannelPipeline pipeline = socketChannel.pipeline();
        //websocket基于http协议所需要的http编解码器
        pipeline.addLast(new HttpServerCodec());
        //在http上有一些数据流产生，有大有小，我们对其处理，既然如此，我们需要使用netty对数据流写 提供支持
        pipeline.addLast(new ChunkedWriteHandler());
        //对httpmessage进行聚合处理，聚合成request和response
        pipeline.addLast(new HttpObjectAggregator(1024*64));
        /**
         * 针对客户端在一分钟时间内没有向服务端发送读写心跳（ALL）主动断开连接
         * 有读有写空闲则不做处理
         */
        pipeline.addLast(new IdleStateHandler(8,10,12));
        //自定义空闲状态检测的handler
        pipeline.addLast(new HeartBeatHandler());
        /**
         * 本handler会帮你重点处理一些繁重复杂的事情
         * 会帮你处理握手动作：handshaking（close、ping、pong） ping+pong=心跳
         * 对于websocket来讲，都是以frames进行传输的，不同数量类型对应的frame也不同
         */
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));
        //自定义handler
        pipeline.addLast(new ChatHandler());


    }
}
