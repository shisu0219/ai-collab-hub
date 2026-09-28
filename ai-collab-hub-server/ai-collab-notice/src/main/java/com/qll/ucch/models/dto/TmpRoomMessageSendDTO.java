package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 临时房间发消息入参。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "临时房间发消息入参")
public class TmpRoomMessageSendDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "房间ID")
    @NotNull(message = "房间ID不能为空")
    private Long roomId;

    @Schema(description = "消息内容")
    @NotBlank(message = "消息内容不能为空")
    private String content;
}
