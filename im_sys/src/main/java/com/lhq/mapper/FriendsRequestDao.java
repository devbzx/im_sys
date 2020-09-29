package com.lhq.mapper;

import com.lhq.pojo.FriendsRequest;

public interface FriendsRequestDao {
    int deleteByPrimaryKey(String id);

    int insert(FriendsRequest record);

    int insertSelective(FriendsRequest record);

    FriendsRequest selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(FriendsRequest record);

    int updateByPrimaryKey(FriendsRequest record);
    void save(FriendsRequest friendsRequest);
    //根据好友请求对象进行删除操作
    void deleteFriendRequest(FriendsRequest friendsRequest);
}