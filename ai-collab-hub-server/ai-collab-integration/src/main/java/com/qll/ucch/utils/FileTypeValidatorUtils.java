package com.qll.ucch.utils;

import com.qll.ucch.models.enums.FileTypeEnum;
import com.qll.ucch.constance.FileConst;
import com.qll.ucch.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 文件类型校验工具。
 * <p>
 * 白名单用 {@link FileTypeEnum}，这里只做校验，不做存储。
 *
 * @author qll
 */
public class FileTypeValidatorUtils {

    /** 单个附件大小上限：100MB（与 public 模块 FileConst 保持一致） */
    public static final long MAX_SINGLE_FILE_SIZE = FileConst.MAX_SINGLE_SIZE;

    /** 整体附件大小上限：1024MB */
    public static final long MAX_TOTAL_FILE_SIZE = FileConst.MAX_TOTAL_SIZE;

    private FileTypeValidatorUtils() {
    }

    /**
     * 取文件后缀（小写，不带点）
     *
     * @return 无后缀返回空字符串，入参为空返回空字符串
     */
    public static String getExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 判断后缀是否在白名单内
     */
    public static boolean isAllowedExt(String ext) {
        return FileTypeEnum.contains(ext);
    }

    /**
     * 单文件校验：非空 + 后缀白名单 + 大小不超限。
     * 不通过直接抛 BusinessException，交给全局异常处理器转成友好提示。
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (ext.isEmpty()) {
            throw new BusinessException("文件「" + originalName + "」没有后缀名，无法识别文件类型");
        }
        if (!isAllowedExt(ext)) {
            throw new BusinessException("不支持的文件类型：" + ext
                    + "，仅允许上传 " + FileTypeEnum.extensionText());
        }
        if (file.getSize() > MAX_SINGLE_FILE_SIZE) {
            throw new BusinessException("文件「" + originalName + "」超过单个附件上限 100MB");
        }
    }

    /**
     * 批量校验：逐个校验 + 累加体积不能超过 1024MB
     */
    public static void validateAll(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }
        long total = 0L;
        for (MultipartFile file : files) {
            validate(file);
            total += file.getSize();
        }
        if (total > MAX_TOTAL_FILE_SIZE) {
            throw new BusinessException("附件总大小超过上限 1024MB");
        }
    }

    /**
     * 是否图片类型（走图片预览逻辑时用）
     */
    public static boolean isImage(String fileName) {
        String ext = getExtension(fileName);
        return "bmp".equals(ext) || "gif".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext)
                || "png".equals(ext) || "svg".equals(ext) || "tif".equals(ext) || "webp".equals(ext);
    }
}
