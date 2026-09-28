package com.qll.ucch.models.vo;

import java.io.Serializable;

/**
 * 未读数量统计 VO（新增）。
 * <p>
 * 顶部小红点要显示总数，点开消息中心还想看到「审核结果 2 条 / 收到申请 5 条」
 * 这种分类角标，所以这里分类别都给出来。
 *
 * @author qll
 */
public class NoticeUnreadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全部未读总数 */
    private Long total;

    /** 审核结果未读（msgType=1） */
    private Long auditCount;

    /** 收到申请未读（msgType=2） */
    private Long applyCount;

    /** 系统通知未读（msgType=3） */
    private Long systemCount;

    /** 会话消息未读（msgType=4） */
    private Long chatCount;

    public NoticeUnreadVO() {
        this.total = 0L;
        this.auditCount = 0L;
        this.applyCount = 0L;
        this.systemCount = 0L;
        this.chatCount = 0L;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getAuditCount() {
        return auditCount;
    }

    public void setAuditCount(Long auditCount) {
        this.auditCount = auditCount;
    }

    public Long getApplyCount() {
        return applyCount;
    }

    public void setApplyCount(Long applyCount) {
        this.applyCount = applyCount;
    }

    public Long getSystemCount() {
        return systemCount;
    }

    public void setSystemCount(Long systemCount) {
        this.systemCount = systemCount;
    }

    public Long getChatCount() {
        return chatCount;
    }

    public void setChatCount(Long chatCount) {
        this.chatCount = chatCount;
    }
}
