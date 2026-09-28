package com.qll.ucch.models.vo;

import com.qll.ucch.models.po.NoticeBoxInfo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息列表项 VO。
 * <p>
 * 比 PO 多带了 msgTypeName，前端直接显示「审核结果 / 系统通知」这种中文，
 * 不用自己维护一份映射表。
 *
 * @author qll
 */
public class NoticeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /** 消息分类：1审核结果 2收到申请 3系统通知 4会话消息 */
    private Integer msgType;

    /** 分类中文名 */
    private String msgTypeName;

    /** 是否已读：1已读 0未读 */
    private Integer readStatus;

    /** 是否已读（布尔方便前端判断） */
    private Boolean read;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 创建时间格式化文本，列表直接展示 */
    private String createTimeText;

    public NoticeVO() {
    }

    /**
     * PO -> VO
     */
    public static NoticeVO from(NoticeBoxInfo po) {
        if (po == null) {
            return null;
        }
        NoticeVO vo = new NoticeVO();
        vo.setId(po.getId());
        vo.setTitle(po.getTitle());
        vo.setContent(po.getContent());
        vo.setMsgType(po.getMsgType());
        vo.setMsgTypeName(msgTypeName(po.getMsgType()));
        vo.setReadStatus(po.getReadStatus());
        vo.setRead(po.getReadStatus() != null && po.getReadStatus() == 1);
        vo.setCreateTime(po.getCreateTime());
        return vo;
    }

    /**
     * 消息分类转中文
     */
    public static String msgTypeName(Integer msgType) {
        if (msgType == null) {
            return "系统通知";
        }
        switch (msgType) {
            case 1:
                return "审核结果";
            case 2:
                return "收到申请";
            case 3:
                return "系统通知";
            case 4:
                return "会话消息";
            default:
                return "其他";
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getMsgTypeName() {
        return msgTypeName;
    }

    public void setMsgTypeName(String msgTypeName) {
        this.msgTypeName = msgTypeName;
    }

    public Integer getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        this.readStatus = readStatus;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
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
