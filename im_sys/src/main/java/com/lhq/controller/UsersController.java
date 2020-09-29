package com.lhq.controller;

import com.lhq.bo.UserBO;
import com.lhq.enums.OperatorFriendRequestTypeEnum;
import com.lhq.enums.SearchFriendsStatusEnum;
import com.lhq.pojo.ChatMsg;
import com.lhq.pojo.FriendsRequest;
import com.lhq.pojo.Users;
import com.lhq.service.UserService;
import com.lhq.utils.FastDFSClient;
import com.lhq.utils.FileUtils;
import com.lhq.utils.JSONResult;
import com.lhq.utils.MD5Utils;
import com.lhq.vo.MyFriendVO;
import com.lhq.vo.UserVo;
import org.apache.catalina.User;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;

@Controller
@RequestMapping("/user")
public class UsersController {
    @Autowired
    UserService userService;
    @Autowired
    FastDFSClient fastDFSClient;
    @RequestMapping("/registerOrLogin")
    @ResponseBody
    //用户登录与注册一体化方法
    public JSONResult registerOrLogin(Users users){
        Users userResult = userService.queryUsernameIsExit(users.getUsername());
        if (userResult != null) {
            if(!userResult.getPassword().equals(MD5Utils.getPwd(users.getPassword()))){
                return JSONResult.errorMsg("密码不正确");
            }
        } else {
                users.setNickname(users.getUsername());
                users.setQrcode("");
                users.setPassword(MD5Utils.getPwd(users.getPassword()));
                users.setFaceImage("");
                users.setFaceImageBig("");
                userResult = userService.saveUser(users);
            }
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(userResult,userVo);
            return JSONResult.ok(userVo);
    }
    //修改昵称
    @RequestMapping("setNickName")
    @ResponseBody
    public JSONResult setNickName(Users users){
        Users userResult = userService.updateUserInfo(users);

        return JSONResult.ok(userResult);
    }
    @RequestMapping("uploadFaceBase64")
    @ResponseBody
    //头像上传
    public JSONResult uploadFaceBase64(@RequestBody UserBO userBO) throws Exception {
        //获取前端传过来的Base64字符串，然后转化为文件对象再进行上传
        String base64Data = userBO.getFaceData();
        String userFacePath = "D://image/userFace/"+userBO.getUserId()+"userFaceBase64.png";
        //调用FileUtils类中的方法将base64字符串转化为对象
        FileUtils.base64ToFile(userFacePath,base64Data);
        MultipartFile multipartFile = FileUtils.fileToMultipart(userFacePath);
        //获取fastDFS上传图片的路径
        String url = fastDFSClient.uploadBase64(multipartFile);
        String thump = "_150x150.";
        String[] arr = url.split("\\.");
        String thumpImgUrl = arr[0]+thump+arr[1];
        //更新用户头像
        Users users = new Users();
        users.setId(userBO.getUserId());
        users.setFaceImage(thumpImgUrl);
        users.setFaceImageBig(url);
        Users result = userService.updateUserInfo(users);
        return JSONResult.ok(result);
    }
    //搜索好友方法
    @RequestMapping("searchFriend")
    @ResponseBody
    public JSONResult searchFriend(String myUserId,String friendUserName){
        /*
        * 1、如果不存在，返回无此用户
        * 2、返回账号是自己，返回不能添加自己
        * 3、搜索的朋友已是好友，返回已是好友
        * */
        Integer status = userService.preconditionsSearchFriends(myUserId, friendUserName);
        if (status== SearchFriendsStatusEnum.SUCCESS.status){
            Users users = userService.queryUsernameIsExit(friendUserName);
            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(users,userVo);
            return JSONResult.ok(userVo);
        }else {
            String msg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(msg);
        }
    }
    @RequestMapping("addFriendRequest")
    @ResponseBody
    public JSONResult addFriendRequest(String myUserId,String friendUserName){
        if (StringUtils.isBlank(myUserId)||StringUtils.isBlank(friendUserName)){
            return JSONResult.errorMsg("好友信息为空");
        }
        Integer status = userService.preconditionsSearchFriends(myUserId, friendUserName);
        if (status==SearchFriendsStatusEnum.SUCCESS.status){
            userService.sendFriendRequest(myUserId, friendUserName);
        }else {
            String msg = SearchFriendsStatusEnum.getMsgByKey(status);
            return JSONResult.errorMsg(msg);
        }

        return JSONResult.ok();
    }
    //好友请求列表查询
    @RequestMapping("queryFriendRequest")
    @ResponseBody
    public JSONResult queryFriendRequest(String userId){
        return JSONResult.ok(userService.queryFriendsRequestList(userId));
    }
    @RequestMapping("operactionFriendRequest")
    @ResponseBody
    public JSONResult operactionFriendRequest(String acceptUserId,String sendUserId,Integer operType){
        FriendsRequest friendsRequest = new FriendsRequest();
        friendsRequest.setAcceptUserId(acceptUserId);
        friendsRequest.setSendUserId(sendUserId);
        if (operType == OperatorFriendRequestTypeEnum.IGNORE.getType()){
            userService.deleteFriendRequest(friendsRequest);
        }else if (operType == OperatorFriendRequestTypeEnum.PASS.getType()){
            userService.passFriendRequest(sendUserId,acceptUserId);
        }
        List<MyFriendVO> myFriend = userService.queryMyFriend(acceptUserId);
        return JSONResult.ok(myFriend);
    }
    //好友请求列表查询
    @RequestMapping("myFriends")
    @ResponseBody
    public JSONResult getMyFriends(String userId){
        return JSONResult.ok(userService.queryMyFriend(userId));
    }
    @RequestMapping("getUser")
    public String getUserById(String id, Model model){
        System.out.println(id);
        Users user = userService.getUserById(id);
        model.addAttribute("user",user);
        System.out.println(user);
        return "user_list";
    }

    /**
     * 用户手机端未签收的消息列表
     * @param acceptUserId
     * @return
     */
    @RequestMapping("getUnReadMsgList")
    @ResponseBody
    public JSONResult getUnReadMsgList(String acceptUserId){
       if (StringUtils.isBlank(acceptUserId)){
           return JSONResult.errorMsg("接收者id不能为空");
       }
       //根据接收者id查询未签收的消息列表
        List<ChatMsg> unReadMsgList = userService.getUnReadMsgList(acceptUserId);
       return JSONResult.ok(unReadMsgList);
    }

}
