package com.qll.ucch.handler;

import cn.hutool.core.util.StrUtil;
import com.qll.ucch.models.vo.FileInfoVO;
import com.qll.ucch.utils.FileTypeValidatorUtils;
import com.qll.ucch.utils.FileUploadUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import com.qll.ucch.exception.BusinessException;

/**
 * 文件上传处理器。
 * <p>
 * Controller 层只管收文件，落盘、校验、拼访问地址都交给这里。设计成可实例化的
 * 普通组件，方便注入配置项（比如上传根目录由 application.yml 传进来）。
 * <p>
 * 注意这个类不在 Controller 层，也不做权限判断，权限由 Controller/拦截器负责。
 *
 * @author qll
 */
public class FileUploadHandler {

    /** 上传根目录，由外部注入（配置文件 file.upload-path） */
    private String uploadPath;

    public FileUploadHandler() {
    }

    public FileUploadHandler(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    public String getUploadPath() {
        return uploadPath;
    }

    public void setUploadPath(String uploadPath) {
        this.uploadPath = uploadPath;
    }

    /**
     * 上传单个文件（含白名单校验），返回可存库的地址串
     */
    public String uploadAndGetUrl(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        FileInfoVO vo = FileUploadUtils.upload(file, uploadPath);
        return vo.getAccessUrl();
    }

    /**
     * 上传单个文件，返回完整文件信息
     */
    public FileInfoVO upload(MultipartFile file) {
        return FileUploadUtils.upload(file, uploadPath);
    }

    /**
     * 批量上传，返回逗号拼接的地址串
     */
    public String uploadBatchAndGetUrls(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("上传文件不能为空");
        }
        List<FileInfoVO> list = FileUploadUtils.uploadBatch(files, uploadPath);
        List<String> urls = new ArrayList<>(list.size());
        for (FileInfoVO vo : list) {
            urls.add(vo.getAccessUrl());
        }
        return String.join(",", urls);
    }

    /**
     * 批量上传，返回文件信息列表
     */
    public List<FileInfoVO> uploadBatch(MultipartFile[] files) {
        return FileUploadUtils.uploadBatch(files, uploadPath);
    }

    /**
     * 把已有附件和本次新上传的合并成一个地址串，编辑场景用
     *
     * @param existUrls 原有地址（逗号分隔，可以为空）
     * @param files     本次新上传（可以为空）
     */
    public String mergeUrls(String existUrls, MultipartFile[] files) {
        StringBuilder sb = new StringBuilder();
        if (StrUtil.isNotBlank(existUrls)) {
            sb.append(existUrls.trim());
        }
        if (files != null && files.length > 0) {
            String newUrls = uploadBatchAndGetUrls(files);
            if (!newUrls.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(",");
                }
                sb.append(newUrls);
            }
        }
        return sb.toString();
    }

    /**
     * 附件是否超出上限（前端可先调这个做预检）
     */
    public static boolean isOverLimit(long totalSize) {
        return totalSize > FileTypeValidatorUtils.MAX_TOTAL_FILE_SIZE;
    }
}
