package com.qll.ucch.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 发送消息通知 DTO（管理员或系统内部用）。
 *
 * @author qll
 */
public class NoticeSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 接收人用户ID */
    @NotNull(message = "接收人不能为空")
    private Long userId;

    /** 标题 */
    @NotBlank(message = "消息标题不能为空")
    private String title;

    /** 内容 */
    private String content;

    /** 消息分类：1审核结果 2收到申请 3系统通知 4会话消息，不传默认系统通知 */
    private Integer msgType = 3;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }
}
