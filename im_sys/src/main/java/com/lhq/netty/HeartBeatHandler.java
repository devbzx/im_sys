package com.lhq.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

/**
 * 用于检测channel的心跳handler
 * 继承ChannelInboundHandlerAdapter
 */
public class HeartBeatHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent){
            IdleStateEvent event = (IdleStateEvent) evt;//强转
            if (event.state()== IdleState.READER_IDLE){
                System.out.println("进入读空闲...");
            } else if (event.state() == IdleState.WRITER_IDLE){
                System.out.println("进入写空闲...");
            } else if (event.state() == IdleState.ALL_IDLE){
                System.out.println("channel 关闭之前：users的数量为："+ChatHandler.users.size());
                Channel channel = ctx.channel();
                //资源释放
                channel.close();
                System.out.println("channel 关闭之后：users的数量为："+ChatHandler.users.size());
            }
        }
    }
}
