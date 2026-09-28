package com.qll.ucch.service;

import com.qll.ucch.models.po.BlogArticleTag;
import com.qll.ucch.models.po.BlogArticleStatus;
import com.qll.ucch.models.po.BlogProgress;
import com.qll.ucch.models.po.BlogType;
import com.qll.ucch.models.vo.DictVO;

import java.util.List;

/**
 * 静态字典业务接口：类型、状态、进度、标签。
 * 标签支持按名字模糊查，也支持新增/删除（管理员和发布者都用得到）。
 *
 * @author qll
 */
public interface IBlogDictService {

    /** 类型列表 */
    List<BlogType> listType();

    /** 状态列表 */
    List<BlogArticleStatus> listStatus();

    /**
     * 进度列表。
     *
     * @param typeId 按类型过滤，null 表示查全部（前端按类型联动展示）
     */
    List<BlogProgress> listProgress(Long typeId);

    /**
     * 标签列表。
     *
     * @param tagName 标签名，模糊匹配，可为空
     */
    List<BlogArticleTag> listTag(String tagName);

    /**
     * 新增标签（管理员）。重名直接返回已有标签，避免撞唯一索引。
     *
     * @param tagName 标签名
     * @return 标签实体
     */
    BlogArticleTag addTag(String tagName);

    /**
     * 删除标签（管理员）。
     *
     * @param tagId 标签ID
     */
    void deleteTag(Long tagId);

    /**
     * 一次性拿全部字典，前端少发几次请求。
     *
     * @param typeId 进度字典按类型过滤，可为空
     */
    DictVO getAllDict(Long typeId);
}
