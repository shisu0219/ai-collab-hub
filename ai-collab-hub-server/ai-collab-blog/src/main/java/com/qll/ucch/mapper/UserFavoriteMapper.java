package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.UserFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收藏/关注 Mapper（新增功能）。
 *
 * @author qll
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavorite> {
}
