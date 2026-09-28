package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息实体（新增功能）。
 * <p>
 * 注意 chat_message 表只有 create_time，没有 update_time，也没有逻辑删除字段，
 * 消息发出去就是发出去了，不让改也不让删（真要删由管理员直接操作数据库）。
 *
 * @author qll
 */
@TableName("chat_message")
public class ChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 会话ID */
    @TableField("session_id")
    private Long sessionId;

    /** 发送人用户ID */
    @TableField("sender_id")
    private Long senderId;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息类型：1文本 2文件 3系统提示 */
    @TableField("msg_kind")
    private Integer msgKind;

    /** 附件地址列表（逗号分隔） */
    @TableField("attachments")
    private String attachments;

    /** 对方是否已读：1已读 0未读 */
    @TableField("read_status")
    private Integer readStatus;

    /** 发送时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMsgKind() {
        return msgKind;
    }

    public void setMsgKind(Integer msgKind) {
        this.msgKind = msgKind;
    }

    public String getAttachments() {
        return attachments;
    }

    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }

    public Integer getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        this.readStatus = readStatus;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "ChatMessage{id=" + id + ", sessionId=" + sessionId + ", senderId=" + senderId
                + ", msgKind=" + msgKind + "}";
    }
}
