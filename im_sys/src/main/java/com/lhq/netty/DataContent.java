package com.lhq.netty;

import lombok.Data;

import java.io.Serializable;
@Data
public class DataContent implements Serializable {
    private Integer action; //动作类型
    private ChatMsg chatMsg; //用户聊天内容
    private String extand;//扩展字段
}
