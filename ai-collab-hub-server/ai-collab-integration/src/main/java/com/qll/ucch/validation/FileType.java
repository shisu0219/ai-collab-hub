package com.qll.ucch.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 附件类型校验注解。
 * <p>
 * 用在 DTO 的附件地址字段上，校验后缀是否在 {@code FileTypeEnum} 白名单里。
 * 字段可以是单个地址，也可以是逗号分隔的多个地址（原系统附件就是这么存的）。
 * <p>
 * 用法：
 * <pre>
 * &#64;FileType(message = "简历格式不支持")
 * private String attachments;
 * </pre>
 *
 * @author qll
 */
@Documented
@Constraint(validatedBy = FileTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface FileType {

    /** 校验不通过时的提示 */
    String message() default "附件类型不支持，请上传常见文档、图片或压缩包格式";

    /** 分组 */
    Class<?>[] groups() default {};

    /** 负载 */
    Class<? extends Payload>[] payload() default {};

    /** 是否允许空值，默认允许（空值由 @NotBlank 等注解负责） */
    boolean allowEmpty() default true;
}
