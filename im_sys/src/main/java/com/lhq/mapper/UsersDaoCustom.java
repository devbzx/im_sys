package com.lhq.mapper;

import com.lhq.pojo.MyFriends;
import com.lhq.vo.FriendsRequestVO;
import com.lhq.vo.MyFriendVO;

import java.util.List;

public interface UsersDaoCustom {
    List<FriendsRequestVO> queryFriendsRequestList(String acceptUserId);
    List<MyFriendVO> queryMyFriend(String userId);
    void batchUpdateMsgSigned(List<String> msgList);
}
