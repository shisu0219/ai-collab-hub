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
 * 临时沟通房间实体，对应新增表 tmp_room。
 *
 * 【和站内会话（chat_session）的区别 —— 别搞混】
 *   chat_session  长期会话，靠 registration_id 关联，登录后出现在「站内沟通」列表里
 *   tmp_room      临时房间，靠随机 room_token 拼成链接，有效期内可凭链接进入，到期自动失效
 * 两套并存、互不影响：正式合作走会话，先聊聊看走临时房间。
 *
 * room_token 是随机生成的、不可猜的字符串，直接拼进链接。
 * 校验时必须同时检查 status 和 expire_time —— 只看其中一个会漏。
 *
 * @author 人工智能学院双创平台
 */
@Data
@TableName("tmp_room")
public class TmpRoom implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 房间ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 房间令牌（拼进链接，随机不可猜） */
    private String roomToken;

    /** 关联的对接申请ID */
    private Long registrationId;

    /** 关联的项目ID */
    private Long articleId;

    /** 参与人A（申请方） */
    private Long userA;

    /** 参与人B（项目组长 / 发布方） */
    private Long userB;

    /** 房间名，一般用项目标题 */
    private String title;

    /** 状态：1有效 0已关闭 */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
