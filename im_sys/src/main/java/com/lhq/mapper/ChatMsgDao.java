package com.lhq.mapper;

import com.lhq.pojo.ChatMsg;

import java.util.List;

public interface ChatMsgDao {
    int deleteByPrimaryKey(String id);

    int insert(ChatMsg record);

    int insertSelective(ChatMsg record);

    ChatMsg selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(ChatMsg record);

    int updateByPrimaryKey(ChatMsg record);
    List<ChatMsg> getUnReadMsgListByAcceptId(String acceptUserId);
}