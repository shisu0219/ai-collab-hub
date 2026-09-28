package com.qll.ucch.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qll.ucch.models.vo.FavoriteVO;

import java.util.List;

/**
 * 收藏 / 关注业务接口（新增功能）。
 * target_type=1 收藏文章，target_type=2 关注用户。
 *
 * @author qll
 */
public interface IUserFavoriteService {

    /**
     * 收藏（幂等：已收藏直接返回 true）。
     *
     * @param userId     当前用户ID
     * @param targetType 1文章 2用户
     * @param targetId   目标ID
     * @return 是否收藏成功
     */
    boolean addFavorite(Long userId, Integer targetType, Long targetId);

    /**
     * 取消收藏（物理删除，表上有唯一索引，重复取消也当成功）。
     *
     * @param userId     当前用户ID
     * @param targetType 1文章 2用户
     * @param targetId   目标ID
     * @return 是否取消成功
     */
    boolean cancelFavorite(Long userId, Integer targetType, Long targetId);

    /**
     * 判断是否已收藏。
     *
     * @param userId     当前用户ID
     * @param targetType 1文章 2用户
     * @param targetId   目标ID
     */
    boolean isFavorited(Long userId, Integer targetType, Long targetId);

    /**
     * 查我的收藏列表。
     *
     * @param userId     当前用户ID
     * @param targetType 1文章 2用户，为空表示全部
     * @param pageNum    页码
     * @param pageSize   每页条数
     */
    Page<FavoriteVO> pageMyFavorite(Long userId, Integer targetType, Long pageNum, Long pageSize);

    /**
     * 查我的收藏ID集合，用于列表页批量回显收藏状态。
     *
     * @param userId     当前用户ID
     * @param targetType 收藏类型
     * @return 目标ID列表
     */
    List<Long> listMyFavoriteIds(Long userId, Integer targetType);

    /**
     * 统计某目标的被收藏数。
     *
     * @param targetType 收藏类型
     * @param targetId   目标ID
     */
    Long countByTarget(Integer targetType, Long targetId);
}
