package com.qll.ucch.models.vo;

import com.qll.ucch.models.po.ChatSession;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话列表项 VO（新增）。
 * <p>
 * 前端会话列表要显示：对方是谁、最后一条说了啥、有没有未读、什么时候说的。
 * 这里把对方用户ID和会话状态都摊平，前端不用再自己算。
 *
 * @author qll
 */
public class ChatSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    private Long id;

    /** 关联的对接申请ID */
    private Long registrationId;

    /** 关联文章ID */
    private Long articleId;

    /** 对方用户ID */
    private Long targetUserId;

    /** 对方昵称（用户名，由调用方补充） */
    private String targetUserName;

    /** 对方头像 */
    private String targetAvatar;

    /** 最后一条消息内容（预览） */
    private String lastMessage;

    /** 最后一条消息时间 */
    private LocalDateTime lastMsgTime;

    /** 当前用户在这个会话里的未读数 */
    private Long unreadCount;

    public ChatSessionVO() {
    }

    /**
     * PO -> VO，需要传入当前登录用户ID，用来确定「对方」是谁
     */
    public static ChatSessionVO from(ChatSession po, Long currentUserId) {
        if (po == null) {
            return null;
        }
        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(po.getId());
        vo.setRegistrationId(po.getRegistrationId());
        vo.setArticleId(po.getArticleId());
        vo.setTargetUserId(po.getOtherUserId(currentUserId));
        vo.setLastMsgTime(po.getLastMsgTime());
        vo.setUnreadCount(0L);
        return vo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getTargetAvatar() {
        return targetAvatar;
    }

    public void setTargetAvatar(String targetAvatar) {
        this.targetAvatar = targetAvatar;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMsgTime() {
        return lastMsgTime;
    }

    public void setLastMsgTime(LocalDateTime lastMsgTime) {
        this.lastMsgTime = lastMsgTime;
    }

    public Long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Long unreadCount) {
        this.unreadCount = unreadCount;
    }
}
