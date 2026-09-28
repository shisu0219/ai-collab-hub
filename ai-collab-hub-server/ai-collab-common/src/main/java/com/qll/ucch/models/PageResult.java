package com.qll.ucch.models;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页返回结果。
 * 列表类接口统一返回这个结构，字段名跟前端约定好别乱改。
 *
 * @author 人工智能学院双创平台
 */
@Data
@Schema(description = "分页返回结果")
public class PageResult<T> implements Serializable {

    /** 当前页数据 */
    @Schema(description = "当前页数据列表")
    private List<T> records = new ArrayList<>();

    /** 总条数 */
    @Schema(description = "总条数")
    private Long total = 0L;

    /** 当前页码 */
    @Schema(description = "当前页码，从 1 开始")
    private Long pageNumber = 1L;

    /** 每页条数 */
    @Schema(description = "每页条数")
    private Long pageSize = 10L;

    /** 总页数 */
    @Schema(description = "总页数")
    private Long pages = 0L;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long pageNumber, Long pageSize) {
        this.records = records;
        this.total = total;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        if (pageSize != null && pageSize > 0) {
            this.pages = (total + pageSize - 1) / pageSize;
        }
    }

    /** 由 MyBatis-Plus 的分页对象直接转换 */
    public static <T> PageResult<T> of(com.baomidou.mybatisplus.core.metadata.IPage<T> page) {
        return new PageResult<>(page.getRecords(), page.getTotal(),
                page.getCurrent(), page.getSize());
    }

    /** 手动构造，数据少的时候用 */
    public static <T> PageResult<T> of(List<T> records, Long total, Long pageNumber, Long pageSize) {
        return new PageResult<>(records, total, pageNumber, pageSize);
    }
}
