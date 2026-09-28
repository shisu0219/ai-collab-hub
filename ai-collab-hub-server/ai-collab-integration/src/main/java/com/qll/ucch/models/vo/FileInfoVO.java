package com.qll.ucch.models.vo;

import java.io.Serializable;

/**
 * 文件信息 VO。
 * <p>
 * 上传成功后返回给前端，前端一般把 accessUrl 存到 attachments 字段里，
 * 用逗号拼接多个。
 *
 * @author qll
 */
public class FileInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原始文件名（含后缀），前端展示用 */
    private String originalName;

    /** 服务器保存的文件名（UUID，唯一） */
    private String fileName;

    /** 相对路径，如 upload/2026/09/21/xxx.png */
    private String relativePath;

    /** 访问地址，如 /upload/2026/09/21/xxx.png */
    private String accessUrl;

    /** 文件大小（字节） */
    private Long size;

    /** 文件大小可读文本，如 1.2 MB */
    private String sizeText;

    /** 后缀（小写，不带点） */
    private String ext;

    /** content-type */
    private String contentType;

    public FileInfoVO() {
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSizeText() {
        return sizeText;
    }

    public void setSizeText(String sizeText) {
        this.sizeText = sizeText;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String toString() {
        return "FileInfoVO{originalName='" + originalName + "', accessUrl='" + accessUrl
                + "', size=" + size + ", ext='" + ext + "'}";
    }
}
