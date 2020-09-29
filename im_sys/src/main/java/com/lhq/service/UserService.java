package com.lhq.service;

import com.lhq.netty.ChatMsg;
import com.lhq.pojo.FriendsRequest;
import com.lhq.pojo.MyFriends;
import com.lhq.pojo.Users;
import com.lhq.vo.FriendsRequestVO;
import com.lhq.vo.MyFriendVO;

import java.util.List;

public interface UserService {
    Users getUserById(String id);
    Users queryUsernameIsExit(String username);
    Users saveUser(Users users);
    Users updateUserInfo(Users users);
    //搜索好友的前置条件接口
    Integer preconditionsSearchFriends(String myUserId, String friendUsername);
    //发送好友请求
    void sendFriendRequest(String myUserId,String myFriendName);
    //好友请求列表查询
    List<FriendsRequestVO> queryFriendsRequestList(String acceptUserId);
    //处理好友请求-忽略好友请求
    void deleteFriendRequest(FriendsRequest friendsRequest);
    //处理好友请求-通过好友请求
    void passFriendRequest(String sendUserId,String acceptUserId);
    //好友列表查询
    List<MyFriendVO> queryMyFriend(String userId);
    //保存用户聊天消息
    String saveMsg(ChatMsg chatMsg);
    void updateMsgSigned(List<String> msgList);
    //获取未签收的消息列表
    List<com.lhq.pojo.ChatMsg> getUnReadMsgList(String acceptUserId);
}
