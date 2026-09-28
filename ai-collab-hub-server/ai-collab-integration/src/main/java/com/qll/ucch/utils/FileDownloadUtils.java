package com.qll.ucch.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.qll.ucch.exception.BusinessException;

/**
 * 文件下载工具。
 * <p>
 * Controller 里拿到文件后，主要用这里的方法生成响应头，尤其是中文文件名的
 * 编码，不然浏览器下下来的名字是乱码。
 *
 * @author qll
 */
public class FileDownloadUtils {

    private FileDownloadUtils() {
    }

    /**
     * 生成 Content-Disposition 头，兼容中文文件名
     */
    public static String buildContentDisposition(String fileName) {
        String encodeName = StrUtil.isBlank(fileName) ? "download" : fileName;
        try {
            // RFC 5987，现代浏览器都认
            String encoded = URLEncoder.encode(encodeName, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            return "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
        } catch (UnsupportedEncodingException e) {
            // UTF-8 一般不会走到这
            return "attachment; filename=\"download\"";
        }
    }

    /**
     * 内联展示（图片、PDF 直接在浏览器打开）
     */
    public static String buildInlineDisposition(String fileName) {
        return buildContentDisposition(fileName).replaceFirst("attachment", "inline");
    }

    /**
     * 校验下载文件是否存在且是文件
     */
    public static void checkFile(File file) {
        if (file == null || !file.exists()) {
            throw new BusinessException("文件不存在或已被删除");
        }
        if (file.isDirectory()) {
            throw new BusinessException("目标路径是目录，无法下载");
        }
    }

    /**
     * 按访问地址下载
     *
     * @param accessUrl 如 /upload/2026/09/21/xxx.png
     * @param basePath  存储根目录
     * @return 磁盘文件
     */
    public static File resolve(String accessUrl, String basePath) {
        if (StrUtil.isBlank(accessUrl)) {
            throw new BusinessException("文件地址不能为空");
        }
        File file = FileUploadUtils.toLocalFile(accessUrl, basePath);
        checkFile(file);
        return file;
    }

    /**
     * 读取文件字节，小文件（流程里做附件打包预览）用
     */
    public static byte[] readBytes(File file) {
        checkFile(file);
        try {
            return FileUtil.readBytes(file);
        } catch (Exception e) {
            throw new IllegalStateException("文件读取失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除本地文件，失败不抛异常（清理临时文件用）
     */
    public static boolean deleteQuietly(File file) {
        if (file == null || !file.exists()) {
            return false;
        }
        try {
            return FileUtil.del(file);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 复制文件到目标位置，目标目录自动创建
     */
    public static File copyTo(File source, File target) throws IOException {
        checkFile(source);
        FileUtil.mkParentDirs(target);
        return FileUtil.copy(source, target, true);
    }

    /**
     * 下载文件名兜底：没名字就按时间戳起一个
     */
    public static String fallbackName(String fileName) {
        return StrUtil.isBlank(fileName) ? System.currentTimeMillis() + ".dat" : fileName;
    }
}
