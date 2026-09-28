package com.qll.ucch.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.qll.ucch.constance.FileConst;
import com.qll.ucch.exception.BusinessException;
import com.qll.ucch.models.vo.FileInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 文件上传工具。
 * <p>
 * 存储策略：根目录/{UPLOAD_ROOT}/{业务子目录}/年/月/日/uuid.后缀
 * 例如：D:/ai-collab-file/WebFile/chat/2026/09/21/8f7c....png
 * 访问地址：/WebFile/chat/2026/09/21/8f7c....png
 * <p>
 * 根目录由 application.yml 里的 file.upload-path 指定（对应静态资源映射的
 * WebFile 目录），不传就落到用户目录下的 ai-collab-upload，方便本地先跑起来。
 * <p>
 * 纯静态工具，不依赖任何配置类，谁都能调。
 *
 * @author qll
 */
public class FileUploadUtils {

    /** 相对上传根目录，与公共模块 FileConst 保持一致 */
    public static final String UPLOAD_DIR = FileConst.UPLOAD_ROOT;

    /** 访问前缀，需与静态资源映射保持一致 */
    public static final String ACCESS_PREFIX = "/" + FileConst.UPLOAD_ROOT;

    /** 未配置时兜底目录 */
    private static final String DEFAULT_BASE_PATH =
            System.getProperty("user.home") + File.separator + "ai-collab-upload";

    private FileUploadUtils() {
    }

    // ==================== 上传入口 ====================

    /**
     * 单文件上传，自动校验类型和大小，默认放根目录
     */
    public static FileInfoVO upload(MultipartFile file, String basePath) {
        return upload(file, basePath, null);
    }

    /**
     * 单文件上传，可指定业务子目录（如 chat / article / reviewfile）
     *
     * @param bizDir 业务子目录，可空
     */
    public static FileInfoVO upload(MultipartFile file, String basePath, String bizDir) {
        FileTypeValidatorUtils.validate(file);
        return doUpload(file, basePath, bizDir, file.getOriginalFilename());
    }

    /**
     * 单文件上传，跳过白名单校验（仅内部可信场景使用，比如后台数据迁移）
     */
    public static FileInfoVO uploadWithoutCheck(MultipartFile file, String basePath, String bizDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        return doUpload(file, basePath, bizDir, file.getOriginalFilename());
    }

    /**
     * 多文件上传，一次校验总额
     */
    public static List<FileInfoVO> uploadBatch(MultipartFile[] files, String basePath) {
        return uploadBatch(files, basePath, null);
    }

    /**
     * 多文件上传，带业务子目录
     */
    public static List<FileInfoVO> uploadBatch(MultipartFile[] files, String basePath, String bizDir) {
        FileTypeValidatorUtils.validateAll(files);
        List<FileInfoVO> list = new ArrayList<>(files.length);
        for (MultipartFile file : files) {
            list.add(doUpload(file, basePath, bizDir, file.getOriginalFilename()));
        }
        return list;
    }

    // ==================== 落盘 ====================

    /**
     * 真正落盘
     *
     * @param bizDir     业务子目录
     * @param originName 原始文件名
     */
    private static FileInfoVO doUpload(MultipartFile file, String basePath, String bizDir, String originName) {
        String ext = FileTypeValidatorUtils.getExtension(originName);

        // 按日期分目录，避免单个目录文件太多
        String datePath = DateUtil.format(new Date(), DatePattern.NORM_DATE_PATTERN).replace("-", "/");
        // 用 UUID 命名，防重名也防路径遍历
        String newName = IdUtil.simpleUUID() + (StrUtil.isBlank(ext) ? "" : "." + ext);

        // 拼接相对路径：WebFile/[业务目录/]年/月/日/文件名
        StringBuilder relativePath = new StringBuilder(UPLOAD_DIR);
        if (StrUtil.isNotBlank(bizDir)) {
            relativePath.append("/").append(trimSlash(bizDir));
        }
        relativePath.append("/").append(datePath).append("/").append(newName);

        File dest = new File(resolveBasePath(basePath), relativePath.toString());
        // 目录不存在就建
        FileUtil.mkParentDirs(dest);
        try {
            file.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException("文件保存失败：" + e.getMessage());
        }

        FileInfoVO vo = new FileInfoVO();
        vo.setOriginalName(originName);
        vo.setFileName(newName);
        vo.setRelativePath(relativePath.toString());
        vo.setAccessUrl("/" + relativePath);
        vo.setExt(ext);
        vo.setSize(file.getSize());
        vo.setSizeText(FileUtil.readableFileSize(file.getSize()));
        vo.setContentType(file.getContentType());
        return vo;
    }

    // ==================== 路径处理 ====================

    /**
     * 解析存储根目录：优先用配置值，其次默认目录
     */
    public static File resolveBasePath(String basePath) {
        String path = StrUtil.isBlank(basePath) ? DEFAULT_BASE_PATH : basePath;
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * 由访问地址反推磁盘文件。
     * <p>
     * 落盘的相对路径是 WebFile/年月日/xxx，访问地址是 /WebFile/年月日/xxx，
     * 两者只差最前面那个斜杠，所以这里只去掉开头的 '/'，不能把 WebFile 段也砍掉，
     * 否则会出现「上传成功但下载找不到文件」。
     *
     * @param accessUrl 如 /WebFile/2026/09/21/xxx.png
     */
    public static File toLocalFile(String accessUrl, String basePath) {
        if (StrUtil.isBlank(accessUrl)) {
            return null;
        }
        String relative = accessUrl.replace("\\", "/");
        if (relative.startsWith(ACCESS_PREFIX + "/")) {
            // 已经是完整的 WebFile 相对路径，直接用
            relative = relative.substring(1);
        } else if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return new File(resolveBasePath(basePath), relative);
    }

    /**
     * 拼一个业务访问地址（前端传相对地址时补前缀用）
     */
    public static String buildAccessUrl(String bizDir) {
        if (StrUtil.isBlank(bizDir)) {
            return ACCESS_PREFIX;
        }
        return ACCESS_PREFIX + "/" + trimSlash(bizDir);
    }

    /**
     * 去掉首尾斜杠，防止拼出 // 或者 \\ 这种畸形路径
     */
    private static String trimSlash(String s) {
        String r = s.trim().replace("\\", "/");
        while (r.startsWith("/")) {
            r = r.substring(1);
        }
        while (r.endsWith("/")) {
            r = r.substring(0, r.length() - 1);
        }
        return r;
    }
}
