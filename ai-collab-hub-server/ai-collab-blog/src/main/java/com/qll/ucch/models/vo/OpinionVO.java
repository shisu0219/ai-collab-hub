package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 答辩意见返回体。
 *
 * 【可见性说明 —— 这个是本模块最要紧的地方】
 * 组外人员看到的内容由后端裁剪过，前端拿到什么就渲染什么：
 *   - 可见全部：content 是完整正文
 *   - 可见摘要：content 被截断（如 50 字），且 truncated=true，前端显示「登录/加入小组查看全部」
 *   - 仅组内：这类记录根本不会返回给组外人员
 * 所以前端不要自己判断能不能看，按 truncated 字段决定要不要显示「查看全部」的提示即可。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "答辩意见")
public class OpinionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "意见ID")
    private Long id;

    @Schema(description = "关联小组ID")
    private Long groupId;

    @Schema(description = "小组名称")
    private String groupName;

    @Schema(description = "关联项目ID")
    private Long articleId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文（组外可能是截断后的摘要）")
    private String content;

    @Schema(description = "内容是否被截断（组外摘要模式为 true）")
    private Boolean truncated;

    @Schema(description = "类型：1答辩问题 2答辩意见 3修改建议")
    private Integer opinionType;

    @Schema(description = "类型名称")
    private String opinionTypeName;

    @Schema(description = "来源（自由文本）")
    private String source;

    @Schema(description = "资料地址列表")
    private List<String> attachments;

    @Schema(description = "这条意见实际生效的可见范围（算上了全局默认）")
    private Integer effectiveScope;

    /**
     * 这条意见自己设的覆盖值。null = 没单独设过、跟随小组默认。
     * 需要它是因为 effectiveScope 是算完的结果，分不出「跟随默认」和「单独设成了同一个值」——
     * 而前端要显示「（单独设）」标记、回显时也要知道该用哪个值。
     */
    @Schema(description = "单条覆盖值，null 表示跟随小组默认")
    private Integer scopeOverride;

    @Schema(description = "当前登录人是否可以看全部内容")
    private Boolean canViewAll;

    @Schema(description = "视频地址（下一轮启用）")
    private String videoUrl;

    @Schema(description = "上传的管理员用户ID（供上层回填昵称用）")
    private Long uploaderId;

    @Schema(description = "上传人昵称（由上层回填）")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
