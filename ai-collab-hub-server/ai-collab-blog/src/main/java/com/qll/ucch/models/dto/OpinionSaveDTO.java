package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 答辩意见保存入参（管理员上传用）。
 *
 * content 故意不加 @NotBlank：答辩资料往往只有附件（一张表格、一份记录），
 * 正文可能是空的，强制要求正文会让管理员没法只传文件。
 * 真正的校验放在 Service 里 —— 「正文和附件至少有一个」。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "答辩意见入参")
public class OpinionSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "意见ID，修改时传")
    private Long id;

    @Schema(description = "小组ID")
    private Long groupId;

    @Schema(description = "关联项目ID，不传则取小组绑定的项目")
    private Long articleId;

    @Schema(description = "问题/意见标题")
    private String title;

    @Schema(description = "问题与意见正文")
    private String content;

    @Schema(description = "类型：1答辩问题 2答辩意见 3修改建议")
    private Integer opinionType;

    @Schema(description = "来源（自由文本，如「张老师」「校外专家」）")
    private String source;

    @Schema(description = "资料地址列表")
    private List<String> attachments;

    @Schema(description = "本条组外可见性覆盖：null跟随全局 0仅组内 1可见摘要 2可见全部")
    private Integer scopeOverride;
}
