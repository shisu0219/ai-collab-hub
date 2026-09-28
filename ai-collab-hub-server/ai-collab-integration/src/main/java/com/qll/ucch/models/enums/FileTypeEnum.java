package com.qll.ucch.models.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 文件类型枚举（上传白名单）。
 * <p>
 * 原来系统的白名单只放了图片和几个文档，导致学生传不了简历（doc/docx）、
 * 方案（xls/xlsx/ppt/pptx）和打包代码（zip/rar），这次一并补齐。
 * <p>
 * 每一项对应一个允许的后缀，extensions() 返回全部允许的后缀集合。
 *
 * @author qll
 */
public enum FileTypeEnum {

    /** 图片 */
    BMP("bmp", "BMP 图片"),
    GIF("gif", "GIF 图片"),
    JPG("jpg", "JPG 图片"),
    JPEG("jpeg", "JPEG 图片"),
    PNG("png", "PNG 图片"),
    SVG("svg", "SVG 矢量图"),
    TIF("tif", "TIF 图片"),
    WEBP("webp", "WEBP 图片"),

    /** 文档 */
    PDF("pdf", "PDF 文档"),
    DOC("doc", "Word 文档"),
    DOCX("docx", "Word 文档"),
    XLS("xls", "Excel 表格"),
    XLSX("xlsx", "Excel 表格"),
    PPT("ppt", "PPT 演示"),
    PPTX("pptx", "PPT 演示"),

    /** 数据 / 标记语言 */
    CSV("csv", "CSV 表格"),
    XML("xml", "XML 文件"),
    MDB("mdb", "Access 数据库"),
    HTM("htm", "网页文件"),
    HTML("html", "网页文件"),

    /** 压缩包 */
    ZIP("zip", "ZIP 压缩包"),
    RAR("rar", "RAR 压缩包");

    /** 后缀（小写，不带点） */
    private final String ext;

    /** 中文描述，用于错误提示 */
    private final String desc;

    FileTypeEnum(String ext, String desc) {
        this.ext = ext;
        this.desc = desc;
    }

    public String getExt() {
        return ext;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 全部允许的后缀集合（保持声明顺序，便于前端展示）
     */
    public static Set<String> extensions() {
        Set<String> set = new LinkedHashSet<>();
        for (FileTypeEnum item : values()) {
            set.add(item.ext);
        }
        return Collections.unmodifiableSet(set);
    }

    /**
     * 全部允许的后缀，逗号拼接（错误提示里用）
     */
    public static String extensionText() {
        return String.join(",", extensions());
    }

    /**
     * 判断后缀是否在白名单内
     *
     * @param ext 后缀，大小写不敏感，可带点
     */
    public static boolean contains(String ext) {
        if (ext == null || ext.isBlank()) {
            return false;
        }
        String normalized = ext.startsWith(".") ? ext.substring(1) : ext;
        return Arrays.stream(values())
                .anyMatch(item -> item.ext.equalsIgnoreCase(normalized));
    }

    /**
     * 根据后缀反查枚举，找不到返回 null
     */
    public static FileTypeEnum of(String ext) {
        if (ext == null || ext.isBlank()) {
            return null;
        }
        String normalized = ext.startsWith(".") ? ext.substring(1) : ext;
        return Arrays.stream(values())
                .filter(item -> item.ext.equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(null);
    }
}
