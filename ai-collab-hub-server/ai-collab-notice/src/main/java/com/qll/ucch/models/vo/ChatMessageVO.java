package com.qll.ucch.models.vo;

import com.qll.ucch.models.po.ChatMessage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 会话消息 VO（新增）。
 *
 * @author qll
 */
public class ChatMessageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private Long sessionId;

    /** 发送人用户ID */
    private Long senderId;

    /** 发送人昵称 */
    private String senderName;

    /** 发送人头像 */
    private String senderAvatar;

    /** 是否是我发的（前端气泡左右两侧用） */
    private Boolean mine;

    /** 消息内容 */
    private String content;

    /** 消息类型：1文本 2文件 3系统提示 */
    private Integer msgKind;

    /** 附件地址列表（拆好的） */
    private List<String> attachmentList;

    /** 发送时间 */
    private LocalDateTime createTime;

    /** 发送时间格式化文本 */
    private String createTimeText;

    public ChatMessageVO() {
    }

    /**
     * PO -> VO
     *
     * @param po            消息实体
     * @param currentUserId 当前登录用户ID，用来判断 mine
     */
    public static ChatMessageVO from(ChatMessage po, Long currentUserId) {
        if (po == null) {
            return null;
        }
        ChatMessageVO vo = new ChatMessageVO();
        vo.setId(po.getId());
        vo.setSessionId(po.getSessionId());
        vo.setSenderId(po.getSenderId());
        vo.setContent(po.getContent());
        vo.setMsgKind(po.getMsgKind());
        vo.setCreateTime(po.getCreateTime());
        vo.setMine(currentUserId != null && currentUserId.equals(po.getSenderId()));
        vo.setAttachmentList(po.getAttachments() == null || po.getAttachments().isBlank()
                ? Collections.emptyList()
                : List.of(po.getAttachments().split(",")));
        return vo;
    }

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

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAvatar() {
        return senderAvatar;
    }

    public void setSenderAvatar(String senderAvatar) {
        this.senderAvatar = senderAvatar;
    }

    public Boolean getMine() {
        return mine;
    }

    public void setMine(Boolean mine) {
        this.mine = mine;
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

    public List<String> getAttachmentList() {
        return attachmentList;
    }

    public void setAttachmentList(List<String> attachmentList) {
        this.attachmentList = attachmentList;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getCreateTimeText() {
        return createTimeText;
    }

    public void setCreateTimeText(String createTimeText) {
        this.createTimeText = createTimeText;
    }
}
