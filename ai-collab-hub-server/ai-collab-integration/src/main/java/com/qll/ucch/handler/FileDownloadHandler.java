package com.qll.ucch.handler;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 文件下载处理器。
 * <p>
 * 两种用法：
 * 1) {@link #download(String, String, String)} 返回 ResponseEntity，Controller 直接 return；
 * 2) {@link #writeToResponse} 直接往 HttpServletResponse 写，适合需要自己控制响应流的场景。
 *
 * @author qll
 */
public class FileDownloadHandler {

    /** 上传根目录，由外部注入 */
    private String uploadPath;

    public FileDownloadHandler() {
    }

    public FileDownloadHandler(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    /**
     * 按访问地址构造下载响应
     *
     * @param accessUrl  文件访问地址
     * @param basePath   存储根目录，传空用本类的 uploadPath
     * @param showName   下载时展示的文件名（中文也能正常显示）
     */
    public ResponseEntity<Resource> download(String accessUrl, String basePath, String showName) {
        File file = com.qll.ucch.utils.FileDownloadUtils.resolve(
                accessUrl, basePath == null ? uploadPath : basePath);
        String fileName = com.qll.ucch.utils.FileDownloadUtils.fallbackName(
                showName != null ? showName : file.getName());

        Resource resource = new FileSystemResource(file);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                com.qll.ucch.utils.FileDownloadUtils.buildContentDisposition(fileName));
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.setContentLength(file.length());
        headers.setContentType(resolveMediaType(file));

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * 直接把文件写进响应流，一般用于批量打包下载或权限校验后放行
     */
    public void writeToResponse(HttpServletResponse response, File file, String showName) throws IOException {
        com.qll.ucch.utils.FileDownloadUtils.checkFile(file);
        String fileName = com.qll.ucch.utils.FileDownloadUtils.fallbackName(showName);

        response.reset();
        response.setContentType(resolveMediaType(file).toString());
        response.setContentLengthLong(file.length());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                com.qll.ucch.utils.FileDownloadUtils.buildContentDisposition(encode(fileName)));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file.toPath(), out);
            out.flush();
        }
    }

    /**
     * 根据后缀判断 content-type，识别不出来就给二进制流
     */
    private MediaType resolveMediaType(File file) {
        String type = null;
        try {
            type = Files.probeContentType(file.toPath());
        } catch (IOException ignored) {
            // 探测失败就走兜底
        }
        if (type == null) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(type);
        } catch (Exception e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 中文名编码，防止 header 里出现非法字符
     */
    private String encode(String name) {
        return URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
