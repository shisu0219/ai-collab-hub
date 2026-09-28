package com.qll.ucch.models.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 建组 / 改小组信息入参。
 *
 * 注意 articleId 故意不加 @NotNull：
 * 建组时往往还没填项目信息，是「先建组、再进组里填项目」的顺序，
 * 所以 articleId 允许为空，由后端在后续流程里补上。
 * 加了 @NotNull 会把「后端补全」这条路堵死 —— 参数校验发生在方法体之前。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "小组信息入参")
public class GroupSaveDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "小组ID，修改时传；新建时为空")
    private Long id;

    @Schema(description = "小组名称")
    @NotBlank(message = "请填写小组名称")
    private String name;

    @Schema(description = "小组简介")
    private String intro;

    @Schema(description = "关联的项目ID，一般由系统绑定，不用手填")
    private Long articleId;

    @Schema(description = "答辩意见组外可见性：0仅组内 1可见摘要 2可见全部")
    private Integer opinionScope;

    @Schema(description = "管理员代建时指定的组长用户ID；组长自己建组不用传")
    private Long leaderId;

    @Schema(description = "建组时一并邀请的成员用户ID列表")
    private List<Long> memberIds;

    @Schema(description = "建组时一并邀请的指导老师用户ID列表")
    private List<Long> teacherIds;
}
