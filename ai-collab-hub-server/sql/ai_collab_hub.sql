-- ============================================================
-- 人工智能学院双创平台 数据库建表脚本
-- 库名：ai_collab_hub
-- 版本：1.0.0
-- 说明：本脚本包含「原有功能表」+「新增功能表」两部分
--       新增表在文件后半段，均以 -- [新增] 标注
-- ============================================================

CREATE DATABASE IF NOT EXISTS ai_collab_hub
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE ai_collab_hub;

-- ============================================================
-- 第一部分：系统用户与权限
-- ============================================================

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user
(
    id           BIGINT PRIMARY KEY COMMENT '用户ID',
    account      VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户账号',
    password     VARCHAR(255) NOT NULL COMMENT '用户密码（BCrypt 加密存储）',
    email        VARCHAR(128) NULL UNIQUE COMMENT '用户邮箱（选填；NULL 可重复，MySQL 唯一索引允许多个 NULL）',
    phone        VARCHAR(20) UNIQUE COMMENT '手机号（注册必填，找回密码要用来验证身份）',
    nickname     VARCHAR(64)  NOT NULL COMMENT '用户昵称',
    avatar       TEXT NULL COMMENT '用户头像访问地址',
    enable       TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1启用 0禁用',
    audit_status TINYINT      NOT NULL DEFAULT 0 COMMENT '审核状态：0待审 1通过 2拒绝',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    BIGINT NULL COMMENT '创建人ID',
    update_by    BIGINT NULL COMMENT '更新人ID',
    delete_time  DATETIME NULL DEFAULT NULL COMMENT '删除时间（逻辑删除）',
    INDEX idx_sys_user_audit (audit_status),
    INDEX idx_sys_user_enable (enable)
) COMMENT '系统用户表';

-- 系统角色表
CREATE TABLE IF NOT EXISTS sys_role
(
    id          BIGINT PRIMARY KEY COMMENT '角色ID',
    name        VARCHAR(64) NOT NULL COMMENT '角色名称',
    code        VARCHAR(64) NOT NULL COMMENT '角色标识符（STUDENT/TEACHER/ADMIN）',
    description VARCHAR(255) NULL COMMENT '角色描述',
    create_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role
(
    id          BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    role_id     BIGINT NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_role (user_id, role_id)
) COMMENT '用户角色关联表';

-- 权限表
CREATE TABLE IF NOT EXISTS sys_permission
(
    id          BIGINT PRIMARY KEY COMMENT '权限ID',
    code        VARCHAR(128) NOT NULL COMMENT '权限标识符',
    description VARCHAR(255) NULL COMMENT '权限描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '系统权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS sys_role_permission
(
    id            BIGINT PRIMARY KEY COMMENT '主键ID',
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_role_perm (role_id, permission_id)
) COMMENT '角色权限关联表';

-- 用户审核记录表
CREATE TABLE IF NOT EXISTS sys_user_review
(
    id          BIGINT PRIMARY KEY COMMENT '审核记录ID',
    user_id     BIGINT NOT NULL COMMENT '被审核用户ID',
    role_id     BIGINT NOT NULL COMMENT '申请的角色ID',
    audit_status TINYINT NOT NULL DEFAULT 0 COMMENT '审核结果：0待审 1通过 2拒绝',
    credentials TEXT NULL COMMENT '证明材料（附件地址，逗号分隔）',
    reason      VARCHAR(500) NULL COMMENT '审核意见/拒绝原因',
    reviewer_id BIGINT NULL COMMENT '审核人ID',
    review_time DATETIME NULL COMMENT '审核时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_review_status (audit_status)
) COMMENT '用户审核记录表';

-- 邮箱配置表
CREATE TABLE IF NOT EXISTS sys_email_config
(
    id                 BIGINT PRIMARY KEY COMMENT '配置ID',
    smtp_host          VARCHAR(128) NOT NULL COMMENT 'SMTP 服务器地址',
    smtp_port          INT          NOT NULL COMMENT 'SMTP 端口',
    smtp_username      VARCHAR(128) NOT NULL COMMENT '发信账号',
    smtp_password      VARCHAR(255) NOT NULL COMMENT '发信密码/授权码',
    priority           INT          NOT NULL DEFAULT 1 COMMENT '优先级，数字越小越优先',
    ssl_enable         TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用 SSL：1是 0否',
    connection_timeout INT          NOT NULL DEFAULT 5000 COMMENT '连接超时（毫秒）',
    read_timeout       INT          NOT NULL DEFAULT 5000 COMMENT '读取超时（毫秒）',
    create_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT '邮箱配置表';

-- 【已删除】原企业资料表（平台不再有企业端）

-- ============================================================
-- 第二部分：合作内容（文章）
-- ============================================================

-- 文章状态字典表
CREATE TABLE IF NOT EXISTS blog_article_status
(
    id          BIGINT PRIMARY KEY COMMENT '状态ID',
    name        VARCHAR(32) NOT NULL COMMENT '状态名称',
    description VARCHAR(255) NULL COMMENT '状态描述'
) COMMENT '文章状态字典表';

-- 文章类型字典表（项目/需求/课程）
CREATE TABLE IF NOT EXISTS blog_type
(
    id          BIGINT PRIMARY KEY COMMENT '类型ID',
    name        VARCHAR(32) NOT NULL COMMENT '类型名称',
    description VARCHAR(255) NULL COMMENT '类型描述'
) COMMENT '文章类型字典表';

-- 文章标签表
CREATE TABLE IF NOT EXISTS blog_article_tag
(
    id       BIGINT PRIMARY KEY COMMENT '标签ID',
    tag_name VARCHAR(64) NOT NULL COMMENT '标签名称',
    UNIQUE KEY uk_tag_name (tag_name)
) COMMENT '文章标签表';

-- 进度字典表
CREATE TABLE IF NOT EXISTS blog_progress
(
    id          BIGINT PRIMARY KEY COMMENT '进度ID',
    name        VARCHAR(64) NOT NULL COMMENT '进度名称',
    type_id     BIGINT NULL COMMENT '关联文章类型ID',
    description VARCHAR(255) NULL COMMENT '进度描述',
    node_count  INT NOT NULL DEFAULT 0 COMMENT '节点数量'
) COMMENT '进度字典表';

-- 文章主表
CREATE TABLE IF NOT EXISTS blog_article_info
(
    id          BIGINT PRIMARY KEY COMMENT '文章ID',
    user_id     BIGINT       NOT NULL COMMENT '组长用户ID（原“发布者”，小组模式下即组长）',
    group_id    BIGINT NULL COMMENT '所属小组ID（一个小组只能有一个项目）',
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    type_id     BIGINT       NOT NULL COMMENT '类型ID（1项目 2需求 3课程）',
    content     TEXT NULL COMMENT '详细描述',
    location    VARCHAR(128) NULL COMMENT '地区/地点',
    tag_id      BIGINT NULL COMMENT '标签ID',
    status_id   BIGINT       NOT NULL DEFAULT 1 COMMENT '状态ID',
    progress_id BIGINT NULL COMMENT '进度ID',
    attachments TEXT NULL COMMENT '附件地址列表（逗号分隔）',
    video_url   VARCHAR(500) NULL COMMENT '视频访问地址（下一轮启用）',
    video_cover VARCHAR(500) NULL COMMENT '视频封面图（下一轮启用）',
    view_count  INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    create_by   BIGINT NULL COMMENT '创建人ID',
    update_by   BIGINT NULL COMMENT '更新人ID',
    INDEX idx_article_user (user_id),
    INDEX idx_article_group (group_id),
    INDEX idx_article_type (type_id),
    INDEX idx_article_status (status_id),
    INDEX idx_article_create (create_time)
) COMMENT '文章主表';

-- 文章审核记录表
CREATE TABLE IF NOT EXISTS blog_article_review
(
    id          BIGINT PRIMARY KEY COMMENT '审核记录ID',
    article_id  BIGINT NOT NULL COMMENT '文章ID',
    reviewer_id BIGINT NOT NULL COMMENT '审核人ID',
    pass        TINYINT(1) NOT NULL COMMENT '是否通过：1通过 0拒绝',
    reason      VARCHAR(500) NULL COMMENT '审核意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_article_review (article_id)
) COMMENT '文章审核记录表';

-- 项目类扩展表（学生发布的项目）
CREATE TABLE IF NOT EXISTS blog_project_extend
(
    id         BIGINT PRIMARY KEY COMMENT '主键ID',
    article_id BIGINT NOT NULL UNIQUE COMMENT '文章ID',
    budget     VARCHAR(64) NULL COMMENT '项目预算',
    start_date DATE NULL COMMENT '开始日期',
    end_date   DATE NULL COMMENT '结束日期'
) COMMENT '项目类扩展表';

-- 需求类扩展表（学生发布的合作需求）
CREATE TABLE IF NOT EXISTS blog_demand_extend
(
    id               BIGINT PRIMARY KEY COMMENT '主键ID',
    article_id       BIGINT NOT NULL UNIQUE COMMENT '文章ID',
    urgency_level    VARCHAR(32) NULL COMMENT '紧急程度',
    expected_deadline DATE NULL COMMENT '期望完成日期'
) COMMENT '需求类扩展表';

-- 课程类扩展表
CREATE TABLE IF NOT EXISTS blog_lesson_extend
(
    id             BIGINT PRIMARY KEY COMMENT '主键ID',
    article_id     BIGINT NOT NULL UNIQUE COMMENT '文章ID',
    max_students   INT NULL COMMENT '人数上限',
    current_students INT NOT NULL DEFAULT 0 COMMENT '当前人数',
    lesson_time    VARCHAR(128) NULL COMMENT '上课时间',
    location_detail VARCHAR(255) NULL COMMENT '详细地点'
) COMMENT '课程类扩展表';

-- 对接申请表
-- 【本轮改造】申请内容从「申请原因 + 联系方式」改为
--   「年级 + 班级 + 擅长部分」——年级班级拆两个下拉，擅长用预设技能。
--   reason 保留但降级为「补充说明」（选填）；
--   contact_way / contact_way_value 保留但改为选填
--   （因为申请通过后可以走临时沟通通道，不必先互换联系方式）。
CREATE TABLE IF NOT EXISTS blog_registration
(
    id                BIGINT PRIMARY KEY COMMENT '申请ID',
    article_id        BIGINT NOT NULL COMMENT '目标文章ID',
    type_id           BIGINT NOT NULL COMMENT '文章类型ID',
    user_id           BIGINT NOT NULL COMMENT '申请人用户ID',
    grade             VARCHAR(16) NULL COMMENT '年级，如 25级',
    class_name        VARCHAR(32) NULL COMMENT '班级，如 大数据4班',
    skills            VARCHAR(255) NULL COMMENT '擅长部分（预设技能，逗号分隔）',
    reason            TEXT NULL COMMENT '补充说明（选填）',
    contact_way       VARCHAR(32) NULL COMMENT '联系方式类型（选填）',
    contact_way_value VARCHAR(128) NULL COMMENT '联系方式值（选填）',
    attachments       TEXT NULL COMMENT '附件地址列表',
    pass              TINYINT(1) NULL COMMENT '处理结果：1通过 0拒绝 NULL待处理',
    review_message    VARCHAR(500) NULL COMMENT '回复内容',
    -- 对接进度状态：0待处理 1已通过 2洽谈中 3已合作 4已结束 5已拒绝
    collab_progress   TINYINT NOT NULL DEFAULT 0 COMMENT '对接进度状态',
    create_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         BIGINT NULL COMMENT '创建人ID',
    update_by         BIGINT NULL COMMENT '更新人ID',
    delete_time       DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    INDEX idx_reg_article (article_id),
    INDEX idx_reg_user (user_id),
    INDEX idx_reg_pass (pass)
) COMMENT '对接申请表';

-- ============================================================
-- 第三部分：消息通知
-- ============================================================

-- 消息盒表（原表名 noice_box_info 拼写有误，此处修正为 notice_box_info）
CREATE TABLE IF NOT EXISTS notice_box_info
(
    id          BIGINT PRIMARY KEY COMMENT '消息ID',
    user_id     BIGINT       NOT NULL COMMENT '接收人用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '消息标题',
    content     TEXT NULL COMMENT '消息内容',
    -- [新增] 消息分类：1审核结果 2收到申请 3系统通知 4会话消息
    msg_type    TINYINT      NOT NULL DEFAULT 3 COMMENT '消息分类',
    read_status TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否已读：1已读 0未读',
    -- [新增] 关联对象ID：msg_type=4(会话消息) 时存会话ID，用于两个红点联动已读
    -- 读完某个会话只清 ref_id = 该会话 的通知，别的会话不受影响
    ref_id      BIGINT NULL COMMENT '关联对象ID（会话消息存会话ID）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   BIGINT NULL COMMENT '创建人ID',
    update_by   BIGINT NULL COMMENT '更新人ID',
    delete_time DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    INDEX idx_notice_user (user_id),
    INDEX idx_notice_read (read_status),
    -- [新增] 联动已读按 (user_id, msg_type, ref_id) 查
    INDEX idx_notice_user_type_ref (user_id, msg_type, ref_id)
) COMMENT '消息盒表';

-- ============================================================
-- 第四部分：[新增] 功能扩展表
-- ============================================================

-- [新增] 站内会话表：对接申请通过后建立的双方会话
CREATE TABLE IF NOT EXISTS chat_session
(
    id            BIGINT PRIMARY KEY COMMENT '会话ID',
    registration_id BIGINT NOT NULL COMMENT '关联的对接申请ID',
    user_a        BIGINT NOT NULL COMMENT '参与人A（发起方）用户ID',
    user_b        BIGINT NOT NULL COMMENT '参与人B（接收方）用户ID',
    article_id    BIGINT NULL COMMENT '关联文章ID',
    last_msg_time DATETIME NULL COMMENT '最后一条消息时间',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_session_registration (registration_id),
    INDEX idx_chat_user_a (user_a),
    INDEX idx_chat_user_b (user_b)
) COMMENT '站内会话表';

-- [新增] 会话消息表
CREATE TABLE IF NOT EXISTS chat_message
(
    id          BIGINT PRIMARY KEY COMMENT '消息ID',
    session_id  BIGINT NOT NULL COMMENT '会话ID',
    sender_id   BIGINT NOT NULL COMMENT '发送人用户ID',
    content     TEXT NULL COMMENT '消息内容',
    msg_kind    TINYINT NOT NULL DEFAULT 1 COMMENT '消息类型：1文本 2文件 3系统提示',
    attachments TEXT NULL COMMENT '附件地址列表',
    read_status TINYINT(1) NOT NULL DEFAULT 0 COMMENT '对方是否已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_chat_msg_session (session_id),
    INDEX idx_chat_msg_sender (sender_id)
) COMMENT '会话消息表';

-- [新增] 内容收藏/关注表
CREATE TABLE IF NOT EXISTS user_favorite
(
    id          BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id     BIGINT NOT NULL COMMENT '用户ID',
    target_type TINYINT NOT NULL COMMENT '收藏对象类型：1文章 2用户',
    target_id   BIGINT NOT NULL COMMENT '收藏对象ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_favorite (user_id, target_type, target_id),
    INDEX idx_favorite_user (user_id)
) COMMENT '内容收藏/关注表';

-- [新增] 操作日志表
CREATE TABLE IF NOT EXISTS sys_operation_log
(
    id          BIGINT PRIMARY KEY COMMENT '日志ID',
    user_id     BIGINT NULL COMMENT '操作人用户ID',
    user_account VARCHAR(64) NULL COMMENT '操作人账号（冗余，便于查询）',
    module      VARCHAR(64) NULL COMMENT '模块（用户审核/内容审核/发布/对接等）',
    action      VARCHAR(128) NULL COMMENT '操作动作',
    target_id   BIGINT NULL COMMENT '操作对象ID',
    detail      VARCHAR(1000) NULL COMMENT '操作详情',
    ip          VARCHAR(64) NULL COMMENT '操作IP',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_log_user (user_id),
    INDEX idx_log_module (module),
    INDEX idx_log_create (create_time)
) COMMENT '操作日志表';

-- [新增] 拒绝理由模板表（管理员审核时下拉选择）
CREATE TABLE IF NOT EXISTS sys_reject_template
(
    id          BIGINT PRIMARY KEY COMMENT '模板ID',
    scene       TINYINT NOT NULL COMMENT '适用场景：1账户审核 2内容审核',
    content     VARCHAR(500) NOT NULL COMMENT '模板内容',
    sort        INT NOT NULL DEFAULT 0 COMMENT '排序',
    enable      TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '拒绝理由模板表';

-- [新增] 数据看板统计快照表（可选，用于缓存统计结果，减轻实时聚合压力）
CREATE TABLE IF NOT EXISTS sys_stat_snapshot
(
    id          BIGINT PRIMARY KEY COMMENT '主键ID',
    stat_date   DATE NOT NULL COMMENT '统计日期',
    stat_key    VARCHAR(64) NOT NULL COMMENT '统计项标识',
    stat_value  VARCHAR(255) NOT NULL COMMENT '统计值',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_stat (stat_date, stat_key)
) COMMENT '数据看板统计快照表';

-- ============================================================
-- 第五部分：初始化数据
-- ============================================================

-- 角色
INSERT INTO sys_role (id, name, code, description) VALUES
    (1, '系统管理员', 'ADMIN', '平台管理员，负责审核与运维'),
    (2, '学生', 'STUDENT', '在校学生用户'),
    (4, '老师', 'TEACHER', '校内指导老师，可发布课题需求并指导学生项目')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 文章状态
INSERT INTO blog_article_status (id, name, description) VALUES
    (1, '待审核', '已提交，等待管理员审核'),
    (2, '已发布', '审核通过，已上线展示'),
    (3, '已拒绝', '审核未通过'),
    (4, '已下架', '管理员或发布者下架')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 文章类型
INSERT INTO blog_type (id, name, description) VALUES
    (1, '项目', '学生发布的合作项目'),
    (2, '需求', '学生发布的合作需求'),
    (3, '课程', '培训课程类内容')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 进度
INSERT INTO blog_progress (id, name, type_id, description, node_count) VALUES
    (1, '招募中', 1, '正在寻找合作伙伴', 3),
    (2, '进行中', 1, '项目已启动', 3),
    (3, '已完成', 1, '项目已结束', 3)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 标签
INSERT INTO blog_article_tag (id, tag_name) VALUES
    (1, '人工智能'), (2, '大数据'), (3, '软件开发'), (4, '物联网'),
    (5, '新媒体运营'), (6, 'UI设计'), (7, '市场营销'), (8, '数据分析')
ON DUPLICATE KEY UPDATE tag_name = VALUES(tag_name);

-- 拒绝理由模板
INSERT INTO sys_reject_template (id, scene, content, sort, enable) VALUES
    (1, 1, '材料不全，请补充资质证明后重新提交', 1, 1),
    (2, 1, '填写信息与实际不符，请核实后重新提交', 2, 1),
    (3, 1, '该账号已重复注册', 3, 1),
    (4, 1, '非本校学生/非本校老师，暂不开放注册', 4, 1),
    (5, 2, '内容描述过于简略，请补充完整信息', 5, 1),
    (6, 2, '内容与实际需求不符，请修改后重新提交', 6, 1),
    (7, 2, '含违规信息，不予通过', 7, 1),
    (8, 2, '联系方式缺失，请补充后重新提交', 8, 1)
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- ============================================================
-- 默认管理员账号
-- ============================================================
--
-- ⚠️ 首次部署后请**立即按下面步骤自己生成密码**，不要用示例值。
--
-- 【为什么要你自己生成】
--   这里没有写死任何真实密码的密文。下面那个 password 值是 BCrypt 的
--   占位示例（对应明文 "ChangeMe@123"），只是为了建库脚本能直接跑通。
--   正式使用前请把管理员密码改成你自己的。
--
-- 【怎么生成你自己的 BCrypt 密文】
--   方式1（推荐）：先用示例密码登录后台，在「个人中心 → 修改密码」里改
--   方式2：用 Java 生成，然后手动 update 数据库：
--          new BCryptPasswordEncoder().encode("你的新密码")
--   方式3：用在线工具（注意选 BCrypt / $2a$ 版本），生成后替换下面这行
--
-- 【密文格式说明】
--   $2a$10$...  = BCrypt，strength=10
--   与 Spring Security 的 BCryptPasswordEncoder 完全兼容
INSERT INTO sys_user (id, account, password, email, phone, nickname, enable, audit_status)
VALUES (1, 'admin', '$2a$10$9DuI4j3KxIbhnJcgydCMPOyyAHGFmrtMAvqNPZQ9.36I0nonXqRGO',
        'admin@example.edu.cn', NULL, '系统管理员', 1, 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO sys_user_role (id, user_id, role_id) VALUES (1, 1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);


-- ============================================================
-- 第二部分：小组与答辩意见（本轮新增）
-- ============================================================

-- 项目小组表
-- 一个小组只能有一个项目，用 UNIQUE KEY 在库层面卡死，不靠代码判断。
CREATE TABLE IF NOT EXISTS blog_group
(
    id            BIGINT PRIMARY KEY COMMENT '小组ID',
    name          VARCHAR(100) NOT NULL COMMENT '小组名称',
    article_id    BIGINT NULL COMMENT '关联的项目ID（一个小组只能有一个项目）',
    leader_id     BIGINT NOT NULL COMMENT '组长用户ID',
    intro         VARCHAR(500) NULL COMMENT '小组简介',
    opinion_scope TINYINT NOT NULL DEFAULT 1 COMMENT '答辩意见组外可见性（全局默认）：0仅组内 1可见摘要 2可见全部',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '小组状态：1正常 0已解散',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by     BIGINT NULL COMMENT '创建人ID',
    update_by     BIGINT NULL COMMENT '更新人ID',
    delete_time   DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    UNIQUE KEY uk_group_article (article_id),
    INDEX idx_group_leader (leader_id)
) COMMENT '项目小组表';

-- 小组成员表
-- member_role：1组长 2组员（学生）3指导老师（指导老师属于“组内人员”，可看全部答辩意见）
-- status：0待同意 1已加入 2已拒绝 3已退出
CREATE TABLE IF NOT EXISTS blog_group_member
(
    id          BIGINT PRIMARY KEY COMMENT '主键ID',
    group_id    BIGINT NOT NULL COMMENT '小组ID',
    user_id     BIGINT NOT NULL COMMENT '成员用户ID',
    member_role TINYINT NOT NULL DEFAULT 2 COMMENT '成员角色：1组长 2组员 3指导老师',
    join_type   TINYINT NOT NULL DEFAULT 1 COMMENT '进组方式：1邀请后同意 2管理员直接拉',
    status      TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0待同意 1已加入 2已拒绝 3已退出',
    join_time   DATETIME NULL COMMENT '加入时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    delete_time DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    UNIQUE KEY uk_group_user (group_id, user_id),
    INDEX idx_gm_user (user_id),
    INDEX idx_gm_status (status)
) COMMENT '小组成员表';

-- 答辩意见表
-- 由管理员上传（答辩时老师方/专业方给出的问题与意见）。
-- scope_override 为 NULL 时跟随 blog_group.opinion_scope（全局默认），有值则覆盖本条。
CREATE TABLE IF NOT EXISTS blog_opinion
(
    id             BIGINT PRIMARY KEY COMMENT '意见ID',
    article_id     BIGINT NULL COMMENT '关联项目ID',
    group_id       BIGINT NOT NULL COMMENT '关联小组ID',
    title          VARCHAR(200) NULL COMMENT '问题/意见标题',
    content        TEXT NOT NULL COMMENT '问题与意见正文',
    opinion_type   TINYINT NOT NULL DEFAULT 1 COMMENT '类型：1答辩问题 2答辩意见 3修改建议',
    source         VARCHAR(200) NULL COMMENT '来源（自由文本，如“张老师”“校外专家”）',
    attachments    TEXT NULL COMMENT '关联资料地址列表（管理员上传的文档/表格）',
    scope_override TINYINT NULL COMMENT '本条组外可见性（覆盖全局）：NULL跟随小组 0仅组内 1可见摘要 2可见全部',
    uploader_id    BIGINT NOT NULL COMMENT '上传的管理员用户ID',
    video_url      VARCHAR(500) NULL COMMENT '视频访问地址（下一轮启用）',
    create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by      BIGINT NULL COMMENT '创建人ID',
    update_by      BIGINT NULL COMMENT '更新人ID',
    delete_time    DATETIME NULL DEFAULT NULL COMMENT '删除时间',
    INDEX idx_op_group (group_id),
    INDEX idx_op_article (article_id)
) COMMENT '答辩意见表';

-- 临时沟通房间表
-- 【本轮新增】申请通过后为双方开一个有时效的沟通通道。
-- 和站内会话（chat_session）是两套东西、并存：
--   chat_session  长期会话，靠 registration_id 关联，登录后在我的会话列表里；
--   tmp_room      临时房间，靠随机 token 拼成链接，可在有效期内免登录进入，到期自动失效。
CREATE TABLE IF NOT EXISTS tmp_room
(
    id            BIGINT PRIMARY KEY COMMENT '房间ID',
    room_token    VARCHAR(64) NOT NULL COMMENT '房间令牌（拼进链接用，随机不可猜）',
    registration_id BIGINT NULL COMMENT '关联的对接申请ID',
    article_id    BIGINT NULL COMMENT '关联的项目ID',
    user_a        BIGINT NOT NULL COMMENT '参与人A（申请方）',
    user_b        BIGINT NOT NULL COMMENT '参与人B（项目组长/发布方）',
    title         VARCHAR(200) NULL COMMENT '房间名，一般用项目标题',
    status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1有效 0已关闭',
    expire_time   DATETIME NOT NULL COMMENT '过期时间，到点自动失效',
    create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_room_token (room_token),
    INDEX idx_room_reg (registration_id),
    INDEX idx_room_user_a (user_a),
    INDEX idx_room_user_b (user_b),
    INDEX idx_room_expire (expire_time)
) COMMENT '临时沟通房间表';

-- 临时房间消息表
CREATE TABLE IF NOT EXISTS tmp_room_message
(
    id          BIGINT PRIMARY KEY COMMENT '消息ID',
    room_id     BIGINT NOT NULL COMMENT '房间ID',
    sender_id   BIGINT NOT NULL COMMENT '发送人用户ID',
    content     TEXT NOT NULL COMMENT '消息内容',
    attachments TEXT NULL COMMENT '附件地址列表',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_tm_room (room_id),
    INDEX idx_tm_create (create_time)
) COMMENT '临时房间消息表';
