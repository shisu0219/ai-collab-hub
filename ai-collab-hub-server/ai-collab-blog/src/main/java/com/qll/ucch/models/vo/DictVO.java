package com.qll.ucch.models.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 静态字典聚合出参。
 * 前端发布页 / 筛选栏一次拿全部字典，省得开四五个接口。
 *
 * @author qll
 */
@Data
@Schema(description = "字典聚合信息")
public class DictVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文章类型列表")
    private List<TypeItem> typeList = new ArrayList<>();

    @Schema(description = "文章状态列表")
    private List<StatusItem> statusList = new ArrayList<>();

    @Schema(description = "进度列表")
    private List<ProgressItem> progressList = new ArrayList<>();

    @Schema(description = "标签列表")
    private List<TagItem> tagList = new ArrayList<>();

    @Schema(description = "对接进度列表")
    private List<CollabProgressItem> collabProgressList = new ArrayList<>();

    /** 类型项 */
    @Data
    @Schema(description = "类型项")
    public static class TypeItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String name;

        private String description;
    }

    /** 状态项 */
    @Data
    @Schema(description = "状态项")
    public static class StatusItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String name;

        private String description;
    }

    /** 进度项 */
    @Data
    @Schema(description = "进度项")
    public static class ProgressItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String name;

        private Long typeId;

        private String description;

        private Integer nodeCount;
    }

    /** 标签项 */
    @Data
    @Schema(description = "标签项")
    public static class TagItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Long id;

        private String tagName;
    }

    /** 对接进度项（新增的进度流转字典） */
    @Data
    @Schema(description = "对接进度项")
    public static class CollabProgressItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer code;

        private String name;
    }
}
