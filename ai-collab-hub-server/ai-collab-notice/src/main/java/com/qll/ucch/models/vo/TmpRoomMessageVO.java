package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 临时房间消息返回体。
 *
 * mine 用于前端区分左右气泡：true 表示自己发的。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "临时房间消息")
public class TmpRoomMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private Long id;

    @Schema(description = "房间ID")
    private Long roomId;

    @Schema(description = "发送人用户ID")
    private Long senderId;

    @Schema(description = "发送人昵称（上层回填）")
    private String senderName;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "附件地址列表")
    private String attachments;

    @Schema(description = "是不是自己发的")
    private Boolean mine;

    @Schema(description = "发送时间")
    private LocalDateTime createTime;
}
