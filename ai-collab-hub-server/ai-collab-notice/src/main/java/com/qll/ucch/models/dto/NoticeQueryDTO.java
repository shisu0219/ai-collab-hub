package com.qll.ucch.models.dto;

import java.io.Serializable;

/**
 * 消息查询 DTO。
 * <p>
 * msgType 是新增的：以前消息盒只有一锅乱炖，现在按分类看（审核结果 / 收到申请 /
 * 系统通知 / 会话消息）。title 做模糊搜索，也可以顺带当关键词用。
 *
 * @author qll
 */
public class NoticeQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 消息分类过滤：1审核结果 2收到申请 3系统通知 4会话消息，null 表示全部 */
    private Integer msgType;

    /** 标题模糊搜索 */
    private String title;

    /** 已读状态过滤：1已读 0未读，null 表示全部 */
    private Integer readStatus;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getMsgType() {
        return msgType;
    }

    public void setMsgType(Integer msgType) {
        this.msgType = msgType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        this.readStatus = readStatus;
    }
}
