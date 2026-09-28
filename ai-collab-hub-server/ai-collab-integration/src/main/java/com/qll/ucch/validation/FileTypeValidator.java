package com.qll.ucch.validation;

import cn.hutool.core.util.StrUtil;
import com.qll.ucch.models.enums.FileTypeEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link FileType} 的校验实现。
 * <p>
 * 支持两种入参：
 * - 单个文件地址：/upload/2026/09/21/xxx.docx
 * - 多个地址逗号分隔：a.pdf,b.xlsx
 * <p>
 * 只认后缀，不检查文件是否真的存在（那属于存储层的事）。
 *
 * @author qll
 */
public class FileTypeValidator implements ConstraintValidator<FileType, String> {

    private boolean allowEmpty;

    @Override
    public void initialize(FileType annotation) {
        this.allowEmpty = annotation.allowEmpty();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (StrUtil.isBlank(value)) {
            return allowEmpty;
        }
        String[] urls = value.split(",");
        for (String url : urls) {
            String trimmed = url.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // 去掉可能带的查询串，比如 ?v=1
            int queryIdx = trimmed.indexOf('?');
            if (queryIdx > 0) {
                trimmed = trimmed.substring(0, queryIdx);
            }
            String ext = extractExt(trimmed);
            if (ext.isEmpty() || !FileTypeEnum.contains(ext)) {
                // 自定义错误信息，把不合法的后缀带上，方便排查
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                        "附件类型不支持：" + (ext.isEmpty() ? trimmed : ext)
                                + "，仅允许 " + FileTypeEnum.extensionText()
                ).addConstraintViolation();
                return false;
            }
        }
        return true;
    }

    /**
     * 从路径里抠后缀
     */
    private String extractExt(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase();
    }
}
