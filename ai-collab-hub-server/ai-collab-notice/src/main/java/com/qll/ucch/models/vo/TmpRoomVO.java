package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 临时沟通房间返回体。
 *
 * roomToken 会拼成前端链接：/tmp-room/{roomToken}
 * 前端拿这个链接给双方用。remainingHours 是剩余有效时长，
 * 前端显示「本通道还剩 X 小时」，让双方知道这个通道是临时的。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "临时沟通房间")
public class TmpRoomVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "房间ID")
    private Long id;

    @Schema(description = "房间令牌，拼链接用")
    private String roomToken;

    @Schema(description = "可直接分享的访问路径")
    private String accessPath;

    @Schema(description = "关联申请ID")
    private Long registrationId;

    @Schema(description = "关联项目ID")
    private Long articleId;

    @Schema(description = "项目标题")
    private String articleTitle;

    @Schema(description = "房间名")
    private String title;

    @Schema(description = "参与人A用户ID")
    private Long userA;

    @Schema(description = "参与人A昵称（上层回填）")
    private String userAName;

    @Schema(description = "参与人B用户ID")
    private Long userB;

    @Schema(description = "参与人B昵称（上层回填）")
    private String userBName;

    @Schema(description = "状态：1有效 0已关闭")
    private Integer status;

    @Schema(description = "是否已过期/失效")
    private Boolean expired;

    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @Schema(description = "剩余有效时长（小时，向上取整；已过期则为 0）")
    private Long remainingHours;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
