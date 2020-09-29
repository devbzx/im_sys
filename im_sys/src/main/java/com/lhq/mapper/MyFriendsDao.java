package com.lhq.mapper;

import com.lhq.pojo.MyFriends;

public interface MyFriendsDao {
    int deleteByPrimaryKey(String id);

    int insert(MyFriends record);

    int insertSelective(MyFriends record);

    MyFriends selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(MyFriends record);

    int updateByPrimaryKey(MyFriends record);
    MyFriends selectOneByExample(MyFriends mfe);
}