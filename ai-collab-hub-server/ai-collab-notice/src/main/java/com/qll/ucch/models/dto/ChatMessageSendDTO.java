package com.qll.ucch.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import com.qll.ucch.exception.BusinessException;

/**
 * 发送会话消息 DTO（新增）。
 *
 * @author qll
 */
public class ChatMessageSendDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 会话ID */
    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /** 消息内容，发文件时可以为空 */
    private String content;

    /** 消息类型：1文本 2文件 3系统提示，默认文本 */
    private Integer msgKind = 1;

    /** 附件地址列表（逗号分隔） */
    private String attachments;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
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

    /**
     * 文本消息必须有内容；文件消息要有附件
     */
    public void validateContent() {
        boolean blankContent = content == null || content.trim().isEmpty();
        boolean blankAttach = attachments == null || attachments.trim().isEmpty();
        if (blankContent && blankAttach) {
            throw new BusinessException("消息内容和附件不能同时为空");
        }
        if (msgKind != null && msgKind == 2 && blankAttach) {
            throw new BusinessException("文件消息必须带附件");
        }
    }
}
