package com.lhq.service.impl;

import com.idworker.Sid;
import com.lhq.enums.MsgActionEnum;
import com.lhq.enums.MsgSignFlagEnum;
import com.lhq.enums.SearchFriendsStatusEnum;
import com.lhq.mapper.*;
import com.lhq.netty.ChatMsg;
import com.lhq.netty.DataContent;
import com.lhq.netty.UserChannerlRel;
import com.lhq.pojo.FriendsRequest;
import com.lhq.pojo.MyFriends;
import com.lhq.pojo.Users;
import com.lhq.service.UserService;
import com.lhq.utils.FastDFSClient;
import com.lhq.utils.FileUtils;
import com.lhq.utils.JsonUtils;
import com.lhq.utils.QRCodeUtils;
import com.lhq.vo.FriendsRequestVO;
import com.lhq.vo.MyFriendVO;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service("userService")
public class UserServiceImpl implements UserService {
    @Autowired
    UsersDao usersDao;
    @Autowired
    QRCodeUtils qrCodeUtils;
    @Autowired
    FastDFSClient fastDFSClient;
    @Autowired
    Sid sid;
    @Autowired
    MyFriendsDao myFriendsDao;
    @Autowired
    FriendsRequestDao friendsRequestDao;
    @Autowired
    UsersDaoCustom usersDaoCustom;
    @Autowired
    ChatMsgDao chatMsgDao;
//    @Cacheable(cacheNames = "users")
    @Override
    public Users getUserById(String id) {
        Users users = usersDao.selectByPrimaryKey(id);
        return users;
    }

    @Override
    public Users queryUsernameIsExit(String username) {
        Users users = usersDao.selectByUsername(username);
        return users;
    }
//    @Cacheable(cacheNames = "users")
    @Override
    public Users saveUser(Users users) {
        users.setId(sid.nextShort());
        //为每一个注册用户生成一个唯一的二维码
        String qrCodePath="D://image/qrcode/user"+users.getId()+"qrcode.png";
        //创建二维码对象信息
        qrCodeUtils.createQRCode(qrCodePath,"im_qrcode:"+users.getUsername());
        MultipartFile qrcodeFile = FileUtils.fileToMultipart(qrCodePath);
        String qrCodeUrl="";
        try {
            qrCodeUrl = fastDFSClient.uploadQRCode(qrcodeFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        users.setQrcode(qrCodeUrl);
        usersDao.insert(users);
        return users;
    }
//    @CachePut(value = "users",key = "#result.id")
    @Override
    public Users updateUserInfo(Users users) {
        usersDao.updateByPrimaryKeySelective(users);
        Users result = usersDao.selectByPrimaryKey(users.getId());
        return result;
    }

    @Override
    public Integer preconditionsSearchFriends(String myUserId, String friendUsername) {
        Users users = usersDao.selectByUsername(friendUsername);
        if (users==null){
            return SearchFriendsStatusEnum.USER_NOT_EXIST.status;
        }
        if (myUserId.equals(users.getId())){
            return SearchFriendsStatusEnum.NOT_YOURSELF.status;
        }
        MyFriends myfriend = new MyFriends();
        myfriend.setMyUserId(myUserId);
        myfriend.setMyFriendUserId(users.getId());
        MyFriends myFr = myFriendsDao.selectOneByExample(myfriend);
        if (myFr!=null){
            return SearchFriendsStatusEnum.ALREADY_FRIENDS.status;
        }
        return SearchFriendsStatusEnum.SUCCESS.status;
    }

    @Override
    public void sendFriendRequest(String myUserId, String myFriendName) {
        Users users = queryUsernameIsExit(myFriendName);
        MyFriends myfriend = new MyFriends();
        myfriend.setMyUserId(myUserId);
        myfriend.setMyFriendUserId(users.getId());
        MyFriends myFr = myFriendsDao.selectOneByExample(myfriend);
        if (myFr==null){
            FriendsRequest friendsRequest = new FriendsRequest();
            String requestId = sid.nextShort();
            friendsRequest.setId(requestId);
            friendsRequest.setSendUserId(myUserId);
            friendsRequest.setAcceptUserId(users.getId());
            friendsRequest.setRequestDateTime(new Date());
            friendsRequestDao.save(friendsRequest);
        }
    }
//    @Cacheable(cacheNames = "friendsRequestVO")
    @Override
    public List<FriendsRequestVO> queryFriendsRequestList(String acceptUserId) {
        return usersDaoCustom.queryFriendsRequestList(acceptUserId);
    }
    @CacheEvict(value = "friendsRequestVO",key = "#friendsRequest")
    @Override
    public void deleteFriendRequest(FriendsRequest friendsRequest) {
        friendsRequestDao.deleteFriendRequest(friendsRequest);
    }

    @Override
    public void passFriendRequest(String sendUserId, String acceptUserId) {
        saveFriends(sendUserId,acceptUserId);
        saveFriends(acceptUserId,sendUserId);
        FriendsRequest friendsRequest = new FriendsRequest();
        friendsRequest.setSendUserId(sendUserId);
        friendsRequest.setAcceptUserId(acceptUserId);
        deleteFriendRequest(friendsRequest);



        Channel senderChannel = UserChannerlRel.get(sendUserId);
        if (senderChannel != null){
            //使用websocket主动推送消息到请求发起者，更新它的通讯录列表为最新
            DataContent dataContent = new DataContent();
            dataContent.setAction(MsgActionEnum.PULL_FRIEND.type);
            //消息推送
            senderChannel.writeAndFlush(new TextWebSocketFrame(JsonUtils.objectToJson(dataContent)));

        }

    }
//    @Cacheable(cacheNames = "myFriendVO")
    @Override
    public List<MyFriendVO> queryMyFriend(String userId) {
        return usersDaoCustom.queryMyFriend(userId);
    }

    @Override
    public String saveMsg(ChatMsg chatMsg) {
        com.lhq.pojo.ChatMsg msgDB = new com.lhq.pojo.ChatMsg();
        String msgId = sid.nextShort();
        msgDB.setId(msgId);
        msgDB.setAcceptUserId(chatMsg.getReceiverId());
        msgDB.setSendUserId(chatMsg.getSenderId());
        msgDB.setCreateTime(new Date());
        msgDB.setSignFlag(MsgSignFlagEnum.unsign.type);
        msgDB.setMsg(chatMsg.getMsg());
        int insert = chatMsgDao.insert(msgDB);
        return msgId;
    }

    @Override
    public void updateMsgSigned(List<String> msgList) {
        usersDaoCustom.batchUpdateMsgSigned(msgList);
    }

    @Override
    public List<com.lhq.pojo.ChatMsg> getUnReadMsgList(String acceptUserId) {
        List<com.lhq.pojo.ChatMsg> unReadMsgListByAcceptId = chatMsgDao.getUnReadMsgListByAcceptId(acceptUserId);
        return unReadMsgListByAcceptId;
    }

    //通过好友请求并保存数据到my_friend表中
    private void saveFriends(String sendUserId, String acceptUserId){
        MyFriends myFriends = new MyFriends();
        String id = sid.nextShort();
        myFriends.setId(id);
        myFriends.setMyUserId(sendUserId);
        myFriends.setMyFriendUserId(acceptUserId);
        myFriendsDao.insert(myFriends);
    }
}
