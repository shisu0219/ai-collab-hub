package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 对接申请提交入参。
 *
 * 【本轮改造】申请内容以「年级 + 班级 + 擅长部分」为主：
 *   grade / className 是下拉选的，skills 是预设技能多选。
 *   reason（原来叫"申请原因"）降级为选填的补充说明；
 *   contactWay / contactWayValue 也从必填改为选填 ——
 *   因为申请通过后可以走临时沟通通道，不必先互换微信。
 *
 * 注意这几个字段都不加 @NotNull：
 * 校验放在 Service 里做，前端也不强制，避免"参数校验挡在方法体之前"导致
 * 后端补全逻辑永远跑不到。
 *
 * @author qll
 */
@Data
@Schema(description = "对接申请入参")
public class RegistrationSubmitDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标文章ID")
    @NotNull(message = "请选择要对接的内容")
    private Long articleId;

    @Schema(description = "年级，如 25级")
    private String grade;

    @Schema(description = "班级，如 大数据4班")
    private String className;

    @Schema(description = "擅长部分（预设技能，多个用逗号分隔）")
    private String skills;

    @Schema(description = "补充说明（选填）")
    private String reason;

    @Schema(description = "联系方式类型：微信/QQ/电话/邮箱（选填）")
    private String contactWay;

    @Schema(description = "联系方式值（选填）")
    private String contactWayValue;

    @Schema(description = "附件地址列表，逗号分隔")
    private String attachments;
}
