package com.maka.pojo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * tb_message
 */
@Data
public class Message implements Serializable {
    private Integer messageId;

    /**
     * 接收者的ID
     */
    private Integer senderId;

    /**
     * 消息的内容
     */
    private String messageContent;

    /**
     * 消息的照片
     */
    private String messageImg;

    private Date messageTime;

    /**
     * 消息类型：1系统消息和0线索消息
     */
    private Boolean messageType;

    private Integer oldmanId;

    private static final long serialVersionUID = 1L;
}