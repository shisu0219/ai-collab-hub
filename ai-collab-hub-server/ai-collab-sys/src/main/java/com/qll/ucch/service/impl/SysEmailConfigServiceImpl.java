package com.qll.ucch.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qll.ucch.exception.SysBizException;
import com.qll.ucch.mapper.SysEmailConfigMapper;
import com.qll.ucch.models.dto.EmailConfigDTO;
import com.qll.ucch.models.po.SysEmailConfig;
import com.qll.ucch.models.vo.EmailConfigVO;
import com.qll.ucch.service.SysEmailConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮箱配置业务实现。
 * 只负责配置的增删查，发信逻辑不在这里。
 */
@Service
public class SysEmailConfigServiceImpl extends ServiceImpl<SysEmailConfigMapper, SysEmailConfig>
        implements SysEmailConfigService {

    @Override
    public List<EmailConfigVO> listConfigs() {
        List<SysEmailConfig> list = list(new LambdaQueryWrapper<SysEmailConfig>()
                .orderByAsc(SysEmailConfig::getPriority)
                .orderByDesc(SysEmailConfig::getCreateTime));
        List<EmailConfigVO> result = new ArrayList<>(list.size());
        for (SysEmailConfig config : list) {
            result.add(toVO(config));
        }
        return result;
    }

    @Override
    public EmailConfigVO getConfigById(Long id) {
        if (id == null) {
            throw new SysBizException("配置ID不能为空");
        }
        SysEmailConfig config = getById(id);
        if (config == null) {
            throw new SysBizException("邮箱配置不存在");
        }
        return toVO(config);
    }

    @Override
    public Long saveOrUpdateConfig(EmailConfigDTO dto) {
        SysEmailConfig config;
        if (dto.getId() != null) {
            // 修改：先确认存在
            config = getById(dto.getId());
            if (config == null) {
                throw new SysBizException("邮箱配置不存在");
            }
        } else {
            config = new SysEmailConfig();
        }

        config.setSmtpHost(dto.getSmtpHost());
        config.setSmtpPort(dto.getSmtpPort());
        config.setSmtpUsername(dto.getSmtpUsername());
        config.setSmtpPassword(dto.getSmtpPassword());
        config.setPriority(dto.getPriority());
        config.setSslEnable(dto.getSslEnable() == null ? 1 : dto.getSslEnable());
        config.setConnectionTimeout(dto.getConnectionTimeout() == null ? 5000 : dto.getConnectionTimeout());
        config.setReadTimeout(dto.getReadTimeout() == null ? 5000 : dto.getReadTimeout());

        saveOrUpdate(config);
        return config.getId();
    }

    @Override
    public void removeConfig(Long id) {
        if (id == null) {
            throw new SysBizException("配置ID不能为空");
        }
        SysEmailConfig config = getById(id);
        if (config == null) {
            throw new SysBizException("邮箱配置不存在");
        }
        removeById(id);
    }

    @Override
    public SysEmailConfig getAvailableConfig() {
        return getOne(new LambdaQueryWrapper<SysEmailConfig>()
                .orderByAsc(SysEmailConfig::getPriority)
                .last("limit 1"));
    }

    private EmailConfigVO toVO(SysEmailConfig config) {
        if (config == null) {
            return null;
        }
        EmailConfigVO vo = new EmailConfigVO();
        BeanUtils.copyProperties(config, vo);
        return vo;
    }

    /**
     * 留个口子：以后要按发信账号找配置（比如不同业务用不同邮箱）可以用这个。
     */
    @SuppressWarnings("unused")
    private SysEmailConfig getByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysEmailConfig>()
                .eq(SysEmailConfig::getSmtpUsername, username)
                .last("limit 1"));
    }
}
