package com.qll.ucch.models.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 创建站内会话 DTO（新增）。
 * <p>
 * 一般不需要前端直接传，对接申请审核通过后由后台自动建会话；但保留这个 DTO
 * 给「管理员帮双方开个会话」这种运营场景用。
 *
 * @author qll
 */
public class ChatSessionCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联的对接申请ID */
    @NotNull(message = "对接申请ID不能为空")
    private Long registrationId;

    /** 参与人A用户ID（发起方）。前端不用传，后端会从对接申请里自动带出来 */
    private Long userA;

    /** 参与人B用户ID（接收方）。前端不用传，后端会从对接申请里自动带出来 */
    private Long userB;

    /** 关联文章ID */
    private Long articleId;

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
}
