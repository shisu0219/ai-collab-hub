package com.qll.ucch.constance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 附件上传相关常量。
 *
 * 【改动说明】原系统白名单里只有图片和文档格式，学生想传简历（doc/docx）、
 * 方案（ppt/pptx）、表格（xls/xlsx）、压缩包（zip/rar）全被挡住，
 * 这里把常用办公格式补全了。
 *
 * @author 人工智能学院双创平台
 */
public interface FileConst {

    /** 单次请求整体附件上限：1024MB */
    long MAX_TOTAL_SIZE = 1024L * 1024 * 1024;

    /** 单个附件上限：100MB */
    long MAX_SINGLE_SIZE = 100L * 1024 * 1024;

    /**
     * 允许上传的扩展名白名单。
     * 新增：doc、docx、xls、xlsx、ppt、pptx、zip、rar
     */
    Set<String> ALLOW_EXTENSIONS = new HashSet<>(Arrays.asList(
            // 图片
            "bmp", "gif", "jpg", "jpeg", "png", "svg", "tif", "webp",
            // 文档
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // 文本与数据
            "csv", "htm", "html", "xml",
            // 数据库文件
            "mdb",
            // 压缩包
            "zip", "rar"
    ));

    /** 相对上传根目录 */
    String UPLOAD_ROOT = "WebFile";

    /** 头像子目录 */
    String DIR_AVATAR = "avatar";

    /** 文章附件子目录 */
    String DIR_ARTICLE = "article";

    /** 审核材料子目录 */
    String DIR_REVIEW_FILE = "reviewfile";

    /** 会话聊天附件子目录 */
    String DIR_CHAT = "chat";
}
