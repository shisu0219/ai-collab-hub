package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 答辩意见批量导入的一行。
 *
 * 对应 Excel 模板的一行数据。管理员把答辩记录整理成表格，
 * 一行一条意见，前端解析后一次性提交，不用手工一条条敲。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "答辩意见导入行")
public class OpinionImportRowDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "类型：1答辩问题 2答辩意见 3修改建议")
    private Integer opinionType;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "来源")
    private String source;
}
