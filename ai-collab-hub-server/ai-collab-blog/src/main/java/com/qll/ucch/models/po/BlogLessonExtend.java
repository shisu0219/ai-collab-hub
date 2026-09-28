package com.qll.ucch.models.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 课程类扩展实体，对应 blog_lesson_extend。
 * 课程内容额外记录人数上限、当前报名人数、上课时间和详细地点。
 *
 * @author qll
 */
@Data
@TableName("blog_lesson_extend")
public class BlogLessonExtend implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 人数上限 */
    private Integer maxStudents;

    /** 当前人数 */
    private Integer currentStudents;

    /** 上课时间 */
    private String lessonTime;

    /** 详细地点 */
    private String locationDetail;
}
