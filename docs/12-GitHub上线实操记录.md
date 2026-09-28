# GitHub 上线实操记录

本文记录把本项目**上传 GitHub + 前端部署到 GitHub Pages** 的完整过程，
包括踩到的坑和最终方案。给后来的人（或未来的自己）参考。

时间：2026-09-28 ~ 2026-09-29
最终状态：前端线上可访问，后端本地运行

---

## 一、最终结果

| 项 | 地址 / 状态 |
| --- | --- |
| 代码仓库 | https://github.com/shisu0219/ai-collab-hub |
| 仓库可见性 | 公开（Public） |
| 前端线上 | https://shisu0219.github.io/ai-collab-hub/ |
| 后端 | 本地运行（28848 端口） |
| 分支 | `main`（源码）+ `gh-pages`（前端构建产物） |

**能做什么**：

- ✅ 别人随时打开网址看界面
- ✅ 老师能看源码和文档
- ❌ 线上不能登录（后端在本地，没有公网地址）—— **这是预期状态**

---

## 二、网络环境（关键前提）

本机网络对 GitHub 的连通性实测：

| 目标 | 结果 |
| --- | --- |
| `github.com`（HTTPS） | ❌ 连不上 |
| `api.github.com` | ❌ 连不上 |
| `raw.githubusercontent.com` | ❌ 连不上 |
| `github.com:22`（SSH 默认端口） | ❌ 不通 |
| **`ssh.github.com:443`** | ✅ **通** |
| `ghproxy.net` / `ghfast.top` | ✅ 通（只能下载，push 不支持） |

**结论**：走 **SSH + 443 端口**。

### 配置方法

```bash
# 生成密钥（如果没有）
ssh-keygen -t ed25519 -C "475721562@qq.com" -f ~/.ssh/id_ed25519 -N ""

# 公钥贴到 GitHub：Settings → SSH and GPG keys → New SSH key
cat ~/.ssh/id_ed25519.pub

# 配 SSH 走 443（关键！22 端口不通）
cat >> ~/.ssh/config << 'EOF'
Host github.com
    HostName ssh.github.com
    Port 443
    User git
    IdentityFile ~/.ssh/id_ed25519
EOF
chmod 600 ~/.ssh/config

# 测试（首次要输完整的 yes，不是 y）
ssh -T git@github.com
# 成功显示：Hi shisu0219! You've successfully authenticated...
```

> ⚠️ **拼写陷阱**：`HostName` 不是 `Hostname`，也不是 `HpstName`。
> 打错会报 `Bad configuration option: hpstname`。
> 配完一定要 `cat ~/.ssh/config` 回读确认。

---

## 三、上传前的准备

### 3.1 建 `.gitignore`

不建的话会把 `node_modules`（163 MB）传上去。

```gitignore
node_modules/
dist/
build/
target/
*.class
*.jar
logs/
*.log
.vite/
.idea/
.vscode/
*.iml
.DS_Store
*.bak
application-local.yaml
.env
.env.local
sql/_*.sql
```

**实测效果**：278 个文件被跟踪，`node_modules` / `dist` / `target` 全部排除。

### 3.2 密码脱敏

`application.yaml` 里的敏感值改成**环境变量 + 默认值**：

```yaml
# 改前
username: root
password: "123123"
secret: "AiCollabHubDefaultSigningValueChangeMe2026"

# 改后
username: ${DB_USERNAME:root}
password: ${DB_PASSWORD:123123}
secret: ${JWT_SECRET:AiCollabHubDefaultSigningValueChangeMe2026}
```

**好处**：别人 clone 下来用默认值就能跑，真实密码可以用环境变量覆盖。

> ⚠️ 改完把**真实配置备份到项目外**（本项目备份在 `Desktop\双创平台-真实配置备份\`）。
> ⚠️ 改完要**回读文件**确认没被工具静默打码，并**重新编译起服务**确认还能跑。

### 3.3 移除真实账号信息

扫描全仓库，把下列内容替换掉：

| 类型 | 原值 | 替换为 |
| --- | --- | --- |
| 管理员密码哈希 | 真实 `Admin@123` 的哈希 | `ChangeMe@123` 的哈希 |
| 学校邮箱 | `@shzq.edu.cn` | `@example.edu.cn` |
| 测试手机号 | `13800000001` ~ `13800000005` | 移除 |
| 测试账号名 | `stu925392` / `teacher975555` | `stu01` / `teacher01` |
| 校内系统 URL | `cxycx.shzq.edu.cn` | 「某高校系统」 |

> ⚠️ **踩过的坑**：第一次换哈希时用了网上通用的 `$2a$10$N9qo8u...`，
> 注释写它对应 `ChangeMe@123`。**实测发现根本不对应**——
> 别人 clone 下来会登不进去。已换成自己生成并**用 BCrypt 实测验证过**的哈希。
>
> **教训**：换哈希必须实测明文对应关系，不能抄网上的。

---

## 四、前端部署到 GitHub Pages

### 4.1 一开始踩的坑

**坑1：私有仓库开不了 Pages**

GitHub 提示「升级或将此仓库公开以启用页面」。
免费版只有公开仓库能用。

**坑2：设置页选错了「来源」**

设置页里出现「GitHub Pages Jekyll」和「静态 HTML」两个选项 ——
**这两个是建站模板，选了不会跑我们的 workflow**，Pages 一直 404。

正确的是选 **「Deploy from a branch」**（从分支部署）。

**坑3：GitHub Actions 方式没跑起来**

配了 workflow，但 Pages 一直 404，且 Actions 标签里看不到运行记录。
（原因未查明，可能是首次启用时权限没同步。）

### 4.2 最终方案：手动推 gh-pages 分支

绕开 Actions，**直接推构建产物**，最稳。

```bash
# 1. 构建
cd ai-collab-hub-web
npm run build          # 产物在 dist/

# 2. 用 worktree 隔离部署分支（不污染 main）
cd ..
git worktree add -B gh-pages /tmp/ghp_deploy

# 3. 清空内容（保留 .git），放入构建产物
find /tmp/ghp_deploy -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
cp -r ai-collab-hub-web/dist/* /tmp/ghp_deploy/
touch /tmp/ghp_deploy/.nojekyll        # 关键！

# 4. 提交推送
cd /tmp/ghp_deploy
git add -A
git commit -m "部署前端构建产物到 GitHub Pages"
git push origin gh-pages

# 5. 清理临时 worktree
cd -
git worktree remove /tmp/ghp_deploy --force
git worktree prune
```

**然后 GitHub 设置**：

    Settings → Pages → Source 选「Deploy from a branch」
    → 分支 gh-pages → 目录 / (root) → 保存

### 4.3 前端代码改的四处

详见 `04-部署指南.md` 第八节，这里只列要点：

| 改动 | 文件 | 为什么 |
| --- | --- | --- |
| 路由改 hash 模式 | `src/router/index.js` | history 模式刷新会 404 |
| base 指向子路径 | `vite.config.js` + `.env.production` | Pages 地址带仓库名前缀 |
| API 地址可配置 | `src/api/request.js` | 静态托管没代理，要直连后端 |
| 后端加 CORS | `CorsConfig.java` | 跨域要被浏览器拦 |

### 4.4 ⚠️ `.nojekyll` 必须加

产物根目录放一个**空文件** `.nojekyll`。

不加的话 GitHub 会用 Jekyll 处理，**下划线开头的目录/文件被忽略**，
`assets` 里的资源可能 404。

---

## 五、后端公网方案（当前未采用）

前端上线后接口连不上，因为后端在本地。三种方案对比：

| 方案 | 费用 | 稳定性 | 结论 |
| --- | --- | --- | --- |
| 云服务器 | 学生机 ~100/年 | ✅ 长期稳定 | **以后正式上线首选** |
| 内网穿透 | 免费 | ⚠️ 地址会变，本机得开着 | 临时应急 |
| 本机演示 | 0 | 只有现场能用 | **本项目当前采用** |

### 尝试内网穿透遇到的问题

试了 Cloudflare Tunnel（cloudflared）：

- 从 GitHub releases 下载 → 直连不通，用 ghproxy 镜像下载成功（2.1 MB，PE 头正常）
- **但执行时系统报「不是此操作系统平台的有效应用程序」/「拒绝访问」**
- 排查：文件头 `MZ` 正确、PE 签名正确、架构 x64 —— 文件本身没问题
- 怀疑是杀毒软件拦截或执行环境限制，**未继续深挖**

> 按「多次尝试失败就停止重试」的原则，没继续折腾。

### 当前采用：本机演示 + 线上看界面

    前端线上    https://shisu0219.github.io/ai-collab-hub/
                老师随时能打开看 UI
    后端本地    本机启动，现场投屏演示完整功能

**老师问「为什么线上登录不了」的标准回答**：

> 这是前后端分离项目。前端已部署到 GitHub Pages（免费静态托管），
> 但后端是 Java 服务 + MySQL 数据库，静态托管跑不了，需要独立服务器资源。
> 演示时我在本机启动完整环境。正式上线把后端部署到云服务器、配好域名即可。

---

## 六、验证方法

### 6.1 部署是否成功

```bash
# 页面能打开吗（期望 200）
curl -s -o /dev/null -w "%{http_code}\n" "https://shisu0219.github.io/ai-collab-hub/"

# 资源路径对不对（应带仓库名前缀）
curl -s "https://shisu0219.github.io/ai-collab-hub/" | grep -oE '(src|href)="[^"]*"' | head -4
```

### 6.2 仓库是公开还是私有（无 token 时）

```bash
git -c credential.helper= ls-remote https://github.com/shisu0219/ai-collab-hub.git
```

- 返回分支列表 → **公开**
- 报 `could not read Username` → **私有**（要求认证）

### 6.3 本地构建产物路径

```bash
grep -oE '"/[^"]*assets/[^"]*"' dist/index.html | head -3
# 应该带 /ai-collab-hub/ 前缀
```

---

## 七、日常更新流程

### 改前端后重新部署

```bash
# 1. 提交源码
cd "C:/Users/47572/Desktop/人工智能学院双创平台"
git add . && git commit -m "改动说明" && git push

# 2. 重新构建 + 推 gh-pages
cd ai-collab-hub-web && npm run build && cd ..
git worktree add -B gh-pages /tmp/ghp_deploy
find /tmp/ghp_deploy -mindepth 1 -maxdepth 1 ! -name '.git' -exec rm -rf {} +
cp -r ai-collab-hub-web/dist/* /tmp/ghp_deploy/
touch /tmp/ghp_deploy/.nojekyll
cd /tmp/ghp_deploy && git add -A && git commit -m "更新前端" && git push origin gh-pages -f
cd - && git worktree remove /tmp/ghp_deploy --force && git worktree prune
```

### 只改后端

后端不用重新部署（反正跑在本地），只要提交源码即可。

---

## 八、答辩前检查清单

- [ ] 后端在跑：`netstat -ano | grep 28848`
- [ ] 前端本地在跑：访问 http://localhost:3000
- [ ] MySQL 在跑：3306
- [ ] Redis 在跑：6379
- [ ] 线上地址能打开：https://shisu0219.github.io/ai-collab-hub/
- [ ] 演示账号能登录（admin / Admin@123）
- [ ] 投屏测试过了

**启动命令**（电脑重启后用）：

```bash
cd "C:/Users/47572/Desktop/人工智能学院双创平台/ai-collab-hub-server"
"D:/java/bin/java.exe" -jar ai-collab-admin/target/AiCollabHub.jar
```
