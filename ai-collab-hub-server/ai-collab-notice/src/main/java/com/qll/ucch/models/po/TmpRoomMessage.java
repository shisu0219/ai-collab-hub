package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 临时房间消息实体，对应新增表 tmp_room_message。
 *
 * 房间到期或关闭后，这里的历史消息保留（便于追溯），
 * 但接口层不再返回 —— 靠房间状态挡住，不是靠删数据。
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("tmp_room_message")
public class TmpRoomMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 房间ID */
    private Long roomId;

    /** 发送人用户ID */
    private Long senderId;

    /** 消息内容 */
    private String content;

    /** 附件地址列表 */
    private String attachments;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
