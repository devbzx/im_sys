package com.lhq.netty;

import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户id和channel的关联关系
 */
public class UserChannerlRel {
    private static HashMap<String, Channel> manage = new HashMap<>();
    public static void put(String sendId,Channel channel){
        manage.put(sendId, channel);
    }
    public static Channel get(String senderId){
        return manage.get(senderId);
    }
    public static void output(){
        for (Map.Entry<String, Channel> entry :manage.entrySet()) {
            System.out.println("UserId:"+entry.getKey()
                +",ChannelId:"+entry.getValue().id().asLongText()
            );
        }
    }
}
