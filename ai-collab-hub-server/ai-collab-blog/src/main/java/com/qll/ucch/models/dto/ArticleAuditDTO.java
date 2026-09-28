package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文章审核入参（单条 + 批量共用）。
 * 批量审核时 ids 传多个，reason 对整批生效。
 *
 * @author qll
 */
@Data
@Schema(description = "文章审核入参")
public class ArticleAuditDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章ID（单条审核时用）")
    private Long id;

    @Schema(description = "文章ID列表（批量审核时用）")
    private List<Long> ids;

    @Schema(description = "是否通过：1通过 0拒绝")
    @NotNull(message = "请选择审核结果")
    private Integer pass;

    @Schema(description = "审核意见/拒绝理由")
    private String reason;
}
