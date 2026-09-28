package com.qll.ucch.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qll.ucch.models.po.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话消息 Mapper（新增）。
 *
 * @author qll
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
