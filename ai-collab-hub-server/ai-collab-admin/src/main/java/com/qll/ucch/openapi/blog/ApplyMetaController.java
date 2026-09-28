package com.qll.ucch.openapi.blog;

import com.qll.ucch.models.Result;
import com.qll.ucch.security.IgnoreAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 申请表单用的固定选项（年级 / 班级 / 擅长技能）。
 *
 * 这四个接口都标了 @IgnoreAuth：纯字典数据、不含任何隐私，
 * 和 /blog/article/static/* 那批字典一个性质，未登录也能拿（否则用户看到内容想申请时
 * 还得先登录才能填表）。
 *
 * 为什么不落库：
 *   这三样是**学校层面的固定值**，一年顶多变一次，不像标签那样需要用户自己加。
 *   硬编码在这里比建三张字典表省事得多，也避免「字典表被清空导致下拉框是空的」这类事故。
 *   要改的时候直接改这个文件 —— 一处改，前后端都跟着变（前端通过接口拿）。
 *
 * 注意班级是跟年级绑定的：25 级的班级和 24 级的班级不是一套。
 * 所以用 Map<年级, List<班级>> 的结构返回，前端选完年级再筛班级。
 *
 * @author 人工智能学院双创平台
 */
@RestController
@RequestMapping("/blog/meta")
@Tag(name = "申请表单固定选项")
public class ApplyMetaController {

    /**
     * 年级选项。加新年级往这里加一行。
     */
    private static final List<String> GRADES = List.of("22级", "23级", "24级", "25级", "26级");

    /**
     * 年级 → 该年级的班级列表。
     *
     * 这里按「人工智能学院 + 大数据方向」的实际情况填，其他学院/专业的班级
     * 后续按需补。注意班级名带专业，别只写「1班」——同年级有多个专业。
     */
    private static final Map<String, List<String>> CLASS_MAP = new LinkedHashMap<>();

    static {
        List<String> common = List.of(
                "大数据1班", "大数据2班", "大数据3班", "大数据4班",
                "人工智能1班", "人工智能2班",
                "计算机1班", "计算机2班",
                "软件技术1班", "软件技术2班",
                "物联网1班", "物联网2班"
        );
        for (String g : GRADES) {
            CLASS_MAP.put(g, common);
        }
    }

    /**
     * 擅长部分（预设技能）。
     *
     * 用 name 给用户看、code 存库，避免以后改显示名把历史数据搞乱。
     */
    private static final List<Map<String, String>> SKILLS = List.of(
            skill("frontend", "前端开发"),
            skill("backend", "后端开发"),
            skill("mobile", "移动端 / 小程序"),
            skill("data", "数据分析"),
            skill("ai", "人工智能 / 算法"),
            skill("iot", "硬件 / 物联网"),
            skill("ui", "UI 设计 / 美工"),
            skill("doc", "文档撰写"),
            skill("ppt", "PPT / 答辩展示"),
            skill("video", "视频剪辑"),
            skill("test", "测试"),
            skill("ops", "部署运维"),
            skill("research", "资料调研")
    );

    private static Map<String, String> skill(String code, String name) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        return m;
    }

    @IgnoreAuth
    @GetMapping("/grades")
    @Operation(summary = "年级选项")
    public Result<List<String>> grades() {
        return Result.success(GRADES);
    }

    /**
     * 班级按年级取。前端选完年级再调这个。
     */
    @IgnoreAuth
    @GetMapping("/classes")
    @Operation(summary = "班级选项（按年级）")
    public Result<List<String>> classes(String grade) {
        if (grade == null || grade.isBlank()) {
            // 没传年级就返回全部班级的并集，前端可以自己再筛
            List<String> all = new ArrayList<>();
            for (List<String> v : CLASS_MAP.values()) {
                for (String c : v) {
                    if (!all.contains(c)) {
                        all.add(c);
                    }
                }
            }
            return Result.success(all);
        }
        return Result.success(CLASS_MAP.getOrDefault(grade, List.of()));
    }

    @IgnoreAuth
    @GetMapping("/skills")
    @Operation(summary = "擅长部分选项（预设技能）")
    public Result<List<Map<String, String>>> skills() {
        return Result.success(SKILLS);
    }

    /**
     * 一次拿全，前端少发两个请求。
     */
    @IgnoreAuth
    @GetMapping("/apply-options")
    @Operation(summary = "申请表单的全部选项（年级 + 班级 + 技能）")
    public Result<Map<String, Object>> applyOptions() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("grades", GRADES);
        data.put("classMap", CLASS_MAP);
        data.put("skills", SKILLS);
        return Result.success(data);
    }
}
