package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对接申请查询入参。
 * direction 决定查「我发出的」还是「我收到的」：我发出的按申请人筛，我收到的按文章发布者筛。
 *
 * @author qll
 */
@Data
@Schema(description = "对接申请查询入参")
public class RegistrationQueryDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 方向：sent 我发出的；received 我收到的 */
    public static final String DIRECTION_SENT = "sent";
    public static final String DIRECTION_RECEIVED = "received";

    @Schema(description = "查询方向：sent 我发出的 / received 我收到的，默认 sent")
    private String direction = DIRECTION_SENT;

    @Schema(description = "文章ID")
    private Long articleId;

    @Schema(description = "类型ID：1项目 2需求 3课程")
    private Long typeId;

    @Schema(description = "处理结果：1通过 0拒绝，不传表示不限")
    private Integer pass;

    @Schema(description = "对接进度：0待处理 1已通过 2洽谈中 3已合作 4已结束 5已拒绝")
    private Integer collabProgress;

    @Schema(description = "页码，从 1 开始")
    private Long pageNum = 1L;

    @Schema(description = "每页条数")
    private Long pageSize = 10L;
}
