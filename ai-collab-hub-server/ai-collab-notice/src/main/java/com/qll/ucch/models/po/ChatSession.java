package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内会话实体（新增功能）。
 * <p>
 * 一个对接申请（blog_registration）通过之后，双方就开一个会话，后面洽谈
 * 直接在站内聊，不用再换微信来回加好友。registration_id 上有唯一索引，
 * 所以同一个申请只会有一个会话。
 *
 * @author qll
 */
@TableName("chat_session")
public class ChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 关联的对接申请ID */
    @TableField("registration_id")
    private Long registrationId;

    /** 参与人A（发起方，一般是申请人）用户ID */
    @TableField("user_a")
    private Long userA;

    /** 参与人B（接收方，一般是需求/项目发布者）用户ID */
    @TableField("user_b")
    private Long userB;

    /** 关联文章ID */
    @TableField("article_id")
    private Long articleId;

    /** 最后一条消息时间，会话列表按这个倒序 */
    @TableField("last_msg_time")
    private LocalDateTime lastMsgTime;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

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

    public Long getUserA() {
        return userA;
    }

    public void setUserA(Long userA) {
        this.userA = userA;
    }

    public Long getUserB() {
        return userB;
    }

    public void setUserB(Long userB) {
        this.userB = userB;
    }

    public Long getArticleId() {
        return articleId;
    }

    public void setArticleId(Long articleId) {
        this.articleId = articleId;
    }

    public LocalDateTime getLastMsgTime() {
        return lastMsgTime;
    }

    public void setLastMsgTime(LocalDateTime lastMsgTime) {
        this.lastMsgTime = lastMsgTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * 判断某个用户是不是这个会话的参与人，权限校验时用
     */
    public boolean containsUser(Long userId) {
        if (userId == null) {
            return false;
        }
        return userId.equals(this.userA) || userId.equals(this.userB);
    }

    /**
     * 取会话中「对方」的用户ID
     */
    public Long getOtherUserId(Long selfId) {
        if (selfId == null) {
            return null;
        }
        return selfId.equals(this.userA) ? this.userB : this.userA;
    }

    @Override
    public String toString() {
        return "ChatSession{id=" + id + ", registrationId=" + registrationId
                + ", userA=" + userA + ", userB=" + userB + "}";
    }
}
