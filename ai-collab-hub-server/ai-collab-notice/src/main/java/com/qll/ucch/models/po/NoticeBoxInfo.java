package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 消息盒实体。
 * <p>
 * 对应表 notice_box_info。原表名 noice_box_info 是拼错的（少了 t），
 * 建表脚本里已经统一改成 notice_box_info，这里跟着改，千万别再写错。
 *
 * @author qll
 */
@TableName("notice_box_info")
public class NoticeBoxInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 接收人用户ID */
    @TableField("user_id")
    private Long userId;

    /** 消息标题 */
    @TableField("title")
    private String title;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /**
     * 消息分类：1审核结果 2收到申请 3系统通知 4会话消息
     */
    @TableField("msg_type")
    private Integer msgType;

    /** 是否已读：1已读 0未读 */
    @TableField("read_status")
    private Integer readStatus;

    /**
     * 关联对象ID。
     * <p>
     * msg_type=4（会话消息）时存**会话ID**，用于精确联动已读：
     * 读完某个会话时，只把 ref_id = 该会话 的通知标已读，别的会话不受影响。
     * 其它类型（1审核结果 / 2收到申请 / 3系统通知）不关联具体对象，留空。
     */
    @TableField("ref_id")
    private Long refId;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人ID */
    @TableField("create_by")
    private Long createBy;

    /** 更新人ID */
    @TableField("update_by")
    private Long updateBy;

    /** 删除时间（逻辑删除标记） */
    @TableField("delete_time")
    @TableLogic(value = "null", delval = "now()")
    private LocalDateTime deleteTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Integer readStatus) {
        this.readStatus = readStatus;
    }

    public Long getRefId() {
        return refId;
    }

    public void setRefId(Long refId) {
        this.refId = refId;
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

    public Long getCreateBy() {
        return createBy;
    }

    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }

    public Long getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getDeleteTime() {
        return deleteTime;
    }

    public void setDeleteTime(LocalDateTime deleteTime) {
        this.deleteTime = deleteTime;
    }

    @Override
    public String toString() {
        return "NoticeBoxInfo{id=" + id + ", userId=" + userId + ", title='" + title
                + "', msgType=" + msgType + ", readStatus=" + readStatus + "}";
    }
}
