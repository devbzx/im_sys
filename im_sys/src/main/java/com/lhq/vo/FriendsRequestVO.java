package com.lhq.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * friends_request
 * @author 
 */
@Data
public class FriendsRequestVO implements Serializable {
    private String sendUserId;
    private String sendUserName;
    private String sendFaceImage;
    private String sendNickname;

    private static final long serialVersionUID = 1L;
}