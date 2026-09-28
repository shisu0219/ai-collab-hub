package com.qll.ucch.converter;

import com.qll.ucch.models.vo.FileInfoVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 文件相关对象转换器。
 * <p>
 * 主要干两件事：MultipartFile -> FileInfoVO 的基本属性搬运，
 * 以及文件地址列表和字符串之间的互转（老系统附件就是用逗号串存的）。
 *
 * @author qll
 */
public class FileConverter {

    private static final String SEPARATOR = ",";

    private FileConverter() {
    }

    /**
     * 上传前的文件信息预览（还没有访问地址时用，比如上传完再统一拼 URL）
     */
    public static FileInfoVO toPreviewVo(MultipartFile file) {
        if (file == null) {
            return null;
        }
        FileInfoVO vo = new FileInfoVO();
        vo.setOriginalName(file.getOriginalFilename());
        vo.setSize(file.getSize());
        vo.setContentType(file.getContentType());
        return vo;
    }

    /**
     * 多个文件转 VO 列表
     */
    public static List<FileInfoVO> toVoList(List<MultipartFile> files) {
        List<FileInfoVO> list = new ArrayList<>();
        if (files == null || files.isEmpty()) {
            return list;
        }
        for (MultipartFile file : files) {
            list.add(toPreviewVo(file));
        }
        return list;
    }

    /**
     * VO 列表 -> 逗号分隔的地址串
     */
    public static String toUrlString(List<FileInfoVO> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (FileInfoVO vo : list) {
            if (vo == null || vo.getAccessUrl() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(SEPARATOR);
            }
            sb.append(vo.getAccessUrl());
        }
        return sb.toString();
    }

    /**
     * 逗号分隔的地址串 -> 列表
     */
    public static List<String> toUrlList(String urls) {
        List<String> list = new ArrayList<>();
        if (urls == null || urls.isBlank()) {
            return list;
        }
        for (String item : urls.split(SEPARATOR)) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    /**
     * 取地址串里的第一个地址，封面 / 缩略图场景常用
     */
    public static String firstUrl(String urls) {
        List<String> list = toUrlList(urls);
        return list.isEmpty() ? null : list.get(0);
    }
}
