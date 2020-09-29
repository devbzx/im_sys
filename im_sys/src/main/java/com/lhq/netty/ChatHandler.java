package com.lhq.netty;

import com.lhq.SpringUtil;
import com.lhq.enums.MsgActionEnum;
import com.lhq.service.UserService;
import com.lhq.utils.JsonUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * 用于处理消息的handler
 * 由于它的传输数据的载体是frame在netty中，是用与为websocket专门处理文本对象的，frame是消息载体，此类叫TextWebSocketFrame
 */
public class ChatHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {
    //用于记录和管理所有客户端的channel
    public static ChannelGroup users = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        //1、获取客户端所传输的消息
        String content = msg.text();
        DataContent dataContent = JsonUtils.jsonToPojo(content, DataContent.class);
        Integer action = dataContent.getAction();
        Channel channel = ctx.channel();
        //2、判断消息类型根据不同类型来处理不同业务
        if (action == MsgActionEnum.CONNECT.type){
//            2.1当websocket第一次open的时候初始化channel，把channel和userid关联起来
            String senderId = dataContent.getChatMsg().getSenderId();
            UserChannerlRel.put(senderId,channel);
            for (Channel c:users
                 ) {
                System.out.println(c.id().asLongText());
            }
            UserChannerlRel.output();
        }else if (action==MsgActionEnum.CHAT.type){
//            2.2聊天类型的消息，把聊天记录保存到数据库中，同时标记消息的签收状态是[未签收]
            ChatMsg chatMsg = dataContent.getChatMsg();
            String msgContent = chatMsg.getMsg();
            String senderId = chatMsg.getSenderId();
            String receiverId = chatMsg.getReceiverId();
            //保存消息到数据库，并且标记为未签收
            UserService userService = (UserService) SpringUtil.getBean("userService");
            String msgId = userService.saveMsg(chatMsg);
            chatMsg.setMsgId(msgId);
            DataContent dataContentMsg = new DataContent();
            dataContentMsg.setChatMsg(chatMsg);
            //发送消息
            Channel receiverChannel = UserChannerlRel.get(receiverId);
            if (receiverChannel == null){
                //离线用户

            } else {
                //当receiverChannel不为空的时候，从ChannelGroup去查找对应的channel是否存在
                Channel findChannel = users.find(receiverChannel.id());
                if (findChannel != null){
                    //用户在线
                    receiverChannel.writeAndFlush(new TextWebSocketFrame(
                            JsonUtils.objectToJson(dataContentMsg)
                    ));
                } else {
                    //离线

                }
            }

        }else if (action==MsgActionEnum.SIGNED.type){
//            2.3签收消息类型，针对具体消息进行签收，修改数据库中对应的消息的签收状态[已签收]
            UserService userService = (UserService) SpringUtil.getBean("userService");
            //扩展字段signed 类型消息中，代表需要去签收的消息id，逗号间隔
            String msgIdsStr = dataContent.getExtand();
            String[] msgsId = msgIdsStr.split(",");

            List<String> msgList = new ArrayList<>();
            for (String mid:msgsId) {
                if (StringUtils.isNotBlank(mid)){
                    msgList.add(mid);
                }
            }
            System.out.println(msgList.toString());
            if (msgList!=null&&!msgList.isEmpty()&&msgList.size()>0){
                //批量签收
                userService.updateMsgSigned(msgList);
            }

        }else if (action==MsgActionEnum.KEEPALIVE.type){
//            2.4心跳类型的消息
            System.out.println("收到来自channel为：["+channel+"]的心跳包");
        }

        /**
         * 2.1当websocket第一次open的时候初始化channel，把channel和userid关联起来
         * 2.2聊天类型的消息，把聊天记录保存到数据库中，同时标记消息的签收状态是[未签收]
         * 2.3签收消息类型，针对具体消息进行签收，修改数据库中对应的消息的签收状态[已签收]
         * 2.4心跳类型的消息
         *
         */
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        users.add(ctx.channel());
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
//        client.remove(ctx.channel());
        String channelId = ctx.channel().id().asShortText();
        System.out.println("客户端被移除，channel对应的ID为："+channelId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        //发生了异常后关闭连接，同时从channelgroup移除
        ctx.channel().close();
        users.remove(ctx.channel());
    }
}
