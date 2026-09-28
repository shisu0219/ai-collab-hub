package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 审核结果出参，批量审核时用。
 * 哪些成功了、哪些因为状态不对被跳过，一次性告诉前端，省得只弹一句「成功」让人摸不着头脑。
 *
 * @author qll
 */
@Data
@Schema(description = "审核结果")
public class ArticleAuditResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "实际处理成功的条数")
    private Integer successCount = 0;

    @Schema(description = "跳过/失败的条数")
    private Integer failCount = 0;

    @Schema(description = "提示信息")
    private String message;
}
