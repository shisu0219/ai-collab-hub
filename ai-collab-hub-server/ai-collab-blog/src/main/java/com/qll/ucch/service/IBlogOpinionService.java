package com.qll.ucch.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.qll.ucch.models.dto.OpinionImportRowDTO;
import com.qll.ucch.models.dto.OpinionSaveDTO;
import com.qll.ucch.models.po.BlogOpinion;
import com.qll.ucch.models.vo.OpinionVO;

import java.util.List;

/**
 * 答辩意见服务。
 *
 * 意见由管理员上传（答辩现场老师方/专业方给出的问题与意见），
 * 基本形态是「一份文档/表格 + 逐条整理出来的条目」。
 *
 * 【可见性是本服务的核心，规则如下】
 *   组长 / 组员 / 指导老师  -> 全部可见（组内人员）
 *   管理员                 -> 全部可见
 *   组外人员               -> 按 scope 取值：
 *                            0 仅组内：列表里根本不出现这类记录
 *                            1 可见摘要：content 截断，truncated=true
 *                            2 可见全部：原样返回
 *   取值优先级：scope_override（单条）> blog_group.opinion_scope（全局默认）
 *
 * 【这条过滤必须做在后端。前端藏按钮不算数 —— 别人直接调接口就看到了。】
 *
 * @author 人工智能学院双创平台
 */
public interface IBlogOpinionService extends IService<BlogOpinion> {

    /**
     * 分页查某小组的答辩意见（已按当前登录人的可见性裁剪）。
     *
     * @param groupId  小组ID
     * @param viewerId 当前登录人ID
     * @param isAdmin  当前登录人是不是管理员
     */
    IPage<OpinionVO> pageByGroup(Long groupId, Long viewerId, boolean isAdmin, Long pageNum, Long pageSize);

    /**
     * 分页查某个项目下的答辩意见（按可见性裁剪）。
     */
    IPage<OpinionVO> pageByArticle(Long articleId, Long viewerId, boolean isAdmin, Long pageNum, Long pageSize);

    /**
     * 意见详情（按可见性裁剪）。
     */
    OpinionVO getOpinionDetail(Long opinionId, Long viewerId, boolean isAdmin);

    /**
     * 管理员上传/保存一条意见。
     *
     * @return 意见ID
     */
    Long saveOpinion(Long adminId, OpinionSaveDTO dto);

    /**
     * 管理员批量导入（Excel 解析后一次提交）。
     *
     * @return 成功导入的条数
     */
    int importOpinions(Long adminId, Long groupId, List<OpinionImportRowDTO> rows);

    /**
     * 管理员删除意见（软删除）。
     */
    void deleteOpinion(Long adminId, Long opinionId);

    /**
     * 设置可见性。opinionId 为空 = 改小组全局默认；不为空 = 改单条覆盖。
     */
    void setScope(Long operatorId, Long groupId, Long opinionId, Integer scope);

    /**
     * 统计某小组的意见条数（组外只看可见的那些）。
     */
    int countVisible(Long groupId, Long viewerId, boolean isAdmin);

    /**
     * 取某小组的全部成员用户ID（发通知用）。
     */
    List<Long> listGroupMemberIds(Long groupId);
}
