package com.lhq.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * chat_msg
 * @author 
 */
@Data
public class ChatMsg implements Serializable {
    private String id;

    private String sendUserId;

    private String acceptUserId;

    private String msg;

    /**
     * 消息是否签收状态
1：签收
0：未签收

     */
    private Integer signFlag;

    /**
     * 发送请求的事件
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}