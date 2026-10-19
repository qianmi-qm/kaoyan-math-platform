# 考研高等数学学习平台（v3）

Spring Boot 3 + MyBatis + MySQL + Vue 3 的考研数学学习平台。v3 在 v2（题库 / 练习 / 日报 / 艾宾浩斯复习 / Redis / AI 讲解）基础上加入 **JWT 认证、多用户隔离、管理员后台、题库编辑、token 刷新/撤销、Redis 限流、AI 每日配额、修改密码、邮箱验证与找回密码**；57 个测试全绿，Docker Compose 四容器（MySQL / Redis / 后端 / 前端）实际启动验证通过。

> 版本说明：`v1.1.0-interview` 是面试稳定版；`v2.0.3` 是单用户技术版；当前 `v3` 分支开始产品化认证。v1 面试包装见 [`docs/INTERVIEW.md`](docs/INTERVIEW.md)，v2 见 [`docs/V2.md`](docs/V2.md)，v3 认证设计见 [`docs/V3.md`](docs/V3.md)。

配套设计文档：《v1 方案与分层学习路线图》（本地 `kaoyan-math-v1-plan`，未随仓库发布）

## 验收基线（当前）

| 检查项 | 当前结果 |
|--------|----------|
| `mvn compile` | BUILD SUCCESS |
| `SkeletonSmokeTest`（4 个用例） | 全绿 |
| `MasteryCalculatorTest`（10 个用例） | 全绿 |
| `EbbinghausSchedulerTest`（8 个用例） | 全绿 |
| `mvn test` | 57/57 通过 |
| v1 业务链路（L1～L6） | 通过（见 `docs/VERIFICATION.md`） |
| v3 认证 / 多用户 | 登录、注册、401、user_id 隔离通过 |
| v3 权限 | 普通用户题库写接口/详情 403；管理员可导入 |
| v3 限流 | 同一账号+IP 5 次错误 → 429；同 IP 30 次跨账号错误 → 429（Redis 计数） |
| v3 管理员引导 | ADMIN_USERNAME / ADMIN_PASSWORD 可独立创建/提升管理员 |
| v3 用户管理 | ADMIN 可在 `/admin` 查看用户、调整角色，不能改自己 |
| v3 题库编辑 | ADMIN 可在 `/admin/questions` 新增/编辑题目 |
| v3 邮箱/找回 | 验证邮箱、忘记密码、重置密码（token 单次 30 分钟） |
| v3 token | access 2h + refresh 7d，刷新轮换，登出/改密递增 token_version 撤销旧 token |
| v3 AI 配额 | 每用户每日 20 次真实模型调用，Redis/MySQL 记录用量；缓存与降级不扣 |
| Redis 报告缓存 | 命中 / 提交后失效 / Redis 宕机降级通过 |
| AI 错题讲解 | SSE 首调 / 二次读库 / 无 key 降级通过 |
| Docker Compose | 四容器 `up`（3307 / 8080 / 5173；Redis 内部） |

红灯不是缺陷，是完成定义——现在 57 个测试已经全部转绿。

## 验证记录（2026-10-19）

完整命令与返回见 [`docs/VERIFICATION.md`](docs/VERIFICATION.md)。摘要：

| 项 | 结果 |
|----|------|
| 单元测试 | 57/57 通过 |
| v3 认证 | 无 token 401；learner + 本地配置密码登录 200；错密码 401 |
| v3 权限 | bob（USER）创建/导入题目 403；learner（ADMIN）可管理；详情接口仅 ADMIN |
| v3 限流 | 同一账号+IP 5 次密码错误后第 6 次 429；跨账号同 IP 30 次也 429 |
| v3 token | refresh 轮换成功；旧 access/旧 refresh 均 401；logout 后 access 401 |
| v3 AI 配额 | mock 一次调用后 used=1、token_estimate>0；无 key 降级不扣配额 |
| v3 密码 | 修改密码后旧 token 401、旧密码 401、新密码 200 |
| v3 多用户 | learner 日报 total=1；新用户 alice 日报 total=0、练习记录 0 |
| v3 前端 | 登录页渲染；表单登录后跳 `/bank`；未登录访问 `/bank` 跳 `/login` |
| L1 知识点树 | 3 个根节点 / 6 个知识点 |
| L2 题库 | 手动录入 4 题；Excel 导入 3 行 → 2 入库 + 1 重复；重复导入 0 入库 + 3 重复 |
| L3 练习 | 4 条记录，服务端计时 2～3s，判分与自评正常 |
| L4 日报 | 4 题 / 对 3 / 正确率 75% / 平均 2s，掌握度与区间查询正常 |
| L5 复习 | 答错入队；记住 → stage 1、+2 天；忘了 → stage 0、+1 天 |
| L6 前端 | `vite build` 通过；dev server + `/api` 代理联调通过 |
| L7 部署 | `docker compose up -d --build` 四容器验证通过 |
| v2 Redis | `report:1:{date}` 命中，TTL≈25h；提交后 key 删除并重新生成 |
| v2 AI | 本地 mock OpenAI SSE 首调逐段返回并落库；二次读库不再调模型；无 key 降级 200 |
| 压测（本机单实例） | 题库列表 342 QPS / p95 260ms；知识点树 1579 QPS / p95 27ms；日报快照 1317 QPS / p95 17ms |

## 怎么学（六步闭环）

L1～L5 已完成，下表保留作为 v1 复习参考；v2 的 Redis/AI 增量见 [`docs/V2.md`](docs/V2.md)，v3 认证见 [`docs/V3.md`](docs/V3.md)。

对每一层：读契约（接口 javadoc / 测试）→ 合卷实现 → 跑测试验收 → 与 AI 生成的参考实现对比 → 记决策日志。

| 层 | 范围 | 完成标志 | 主要文件 |
|----|------|----------|----------|
| L1 | 数据模型：DDL 已给，自己补 resultMap 与知识点点树 | 知识点树接口可用 | `db/schema.sql`、`KnowledgePoint*` |
| L2 | 题库：录入 / 分页 / Excel 导入 | `QuestionServiceImpl` 全部方法可用 | `question/**`、`QuestionMapper.xml` |
| L3 | 练习：选题 / 服务端计时 / 判分 | 练习三接口可用 | `practice/**` |
| L4 | 报告：聚合 SQL + 掌握度 | `MasteryCalculatorTest` 全绿 | `report/domain`、`ReportServiceImpl` |
| L5 | 复习：艾宾浩斯调度 | `EbbinghausSchedulerTest` 全绿 + 三接口可用 | `review/**` |
| L6 | 前端：四页面 + KaTeX + ECharts | 走通"导入→练习→报告→复习" | `frontend/src/views/**` |
| L7 | 工程收尾：README / 压测数字 / 简历 bullet | `docker compose up -d --build` 四容器跑通 | 全仓库 |

每层的实现提示都写在对应 Service 接口的 javadoc 和 Mapper XML 注释里。

## 运行

### 方式一：Docker 一键启动（推荐，装好 Docker 后）

```bash
docker compose up -d --build
# 前端 http://localhost:5173
# 后端 http://localhost:8080  Swagger http://localhost:8080/swagger-ui.html
# Redis 只在 compose 内部使用（6379，不映射宿主）
# MySQL 默认映射宿主 3306（root/root）；公网/共享环境建议：
#   cp .env.example .env && 修改 MYSQL_ROOT_PASSWORD / JWT_SECRET
# JWT_SECRET 无默认值，且示例值会被启动校验拒绝
# access token 默认 2h，refresh token 默认 7d；可用 JWT_EXPIRE_HOURS / JWT_REFRESH_DAYS 调整
# 本地演示需显式设 DEMO_USER_ENABLED=true + 非默认 DEMO_USER_PASSWORD；默认关闭
# 生产设置 ADMIN_USERNAME / ADMIN_PASSWORD 创建独立管理员；ADMIN_REQUIRED=true 时缺失会启动失败
# 默认不会覆盖管理员在页面改过的密码；需要 env 轮换时设 ADMIN_PASSWORD_FORCE=true
# 演示账号由部署者配置，不再是固定的 learner123
# AI 讲解可选：在 .env 填 AI_API_KEY；不填则自动降级为参考解析
# 本机 3306 已被占用时（常见）：
#   MYSQL_PORT=3307 docker compose up -d --build
# schema 变更后需要重建数据库卷（会清空数据）：
#   docker compose down -v && docker compose up -d --build
# 注意：v2 answer_hash 与 v3 password_hash 都已内置启动迁移，通常不需要清库

# 构建需要代理时（可选）：把 HTTP_PROXY / HTTPS_PROXY / MAVEN_OPTS 作为 build args 传入，
# 其中 Maven 还需要 MAVEN_OPTS 里的 -Dhttps.proxyHost / -Dhttps.proxyPort。
```

### 方式二：本地开发

1. 准备 JDK 17+、Maven、Node 18+、MySQL 8；v2 还需要 Redis 7（没有 Redis 时缓存自动降级）
2. 建库：`mysql -uroot -p < backend/src/main/resources/db/schema.sql`
3. 后端：`cd backend && mvn spring-boot:run`（`JWT_SECRET`、`AI_API_KEY` 通过环境变量传入）
4. 前端：`cd frontend && npm install && npm run dev`（已配 /api 代理到 8080）

启动后先访问 `/login`；演示账号是否可用取决于 `.env` 的 `DEMO_USER_ENABLED` / `DEMO_USER_PASSWORD`，管理员可由 `ADMIN_USERNAME` / `ADMIN_PASSWORD` 独立创建。新用户默认 USER，只能管理自己的学习数据；题库写接口和题目详情需要 ADMIN。

### 前端 Mock 模式（不启动后端也能体验完整界面）

前端内置 mock 数据层（12 道真题 + 14 天趋势数据）。访问 `http://localhost:5173/?mock=1` 即可开启；
`?mock=0` 关闭后走真实后端接口。mock 与真实后端的字段差异统一在 `src/api/normalize.js` 适配，
页面代码不用改；跑真实接口后可以用 `npm run check:contract` 做一次字段契约检查。

### 验证命令

```bash
cd backend
mvn compile        # 编译
mvn test           # 57 个验收测试全绿
```

## 目录结构

```
backend/
  src/main/java/com/kaoyan/platform/
    common/      # Result 统一响应 / 全局异常 / Clock / CurrentUser / 启动迁移
    auth/        # v3：JWT / Spring Security / 登录注册
    user/        # v3：用户实体与 UserMapper
    question/    # 题库模块（L1/L2）
    practice/    # 练习模块（L3）
    report/      # 报告模块 + MasteryCalculator + Redis 缓存（L4/v2）
    review/      # 复习模块 + EbbinghausScheduler 调度算法（L5）
    ai/          # v2：错题 AI 讲解（SSE + 落库 + 降级）
  src/main/resources/
    db/schema.sql        # 10 张表 DDL + 初始数据
    mapper/*.xml         # MyBatis SQL
  src/test/java/         # 验收测试：57 个用例（冒烟 + 掌握度 + 复习调度 + mapper XML + 练习 + v2/v3 安全/配额/邮件）
frontend/
  src/views/     # 六页面（登录/题库/练习/报告/复习/账号）+ 管理员用户页
  src/auth.js    # token 持久化 / 登录态 / mock 模式 / 多标签同步
  src/api/       # 请求封装 + 401 拦截 + 后端/mock 字段统一适配
  src/mock/      # 前端 mock 数据层（无后端演示用）
  scripts/       # contract-check.mjs：真实接口字段契约检查
  src/utils/     # math.js 公式渲染 / ui.js 公共样式映射
docs/DECISION_LOG.md     # 决策日志（架构决策 + 联调踩坑）
docs/VERIFICATION.md     # v1 联调与压测记录
docs/INTERVIEW.md        # v1 面试版：演示脚本 / 追问 / 简历 bullet
docs/V2.md               # v2：Redis 缓存 + AI SSE 设计/联调记录
docs/V2_PACKAGING.md     # v2：面试/产品两用包装
docs/V3.md               # v3：认证 / 多用户 / JWT 设计与验收
docker-compose.yml       # mysql + redis + backend + frontend 一键启动
```

## 常用面试问题索引（先自测再看提示）

- 为什么 Service 层要接口 + 实现？项目里每个模块都这么拆了。
- `(user_id, practice_date)` 联合索引的最左前缀：`PracticeRecordMapper.selectByUserAndDate` 必须让 user_id 出现在等值条件里。
- 为什么 `selectDailyAgg` 里正确数用 `COUNT(is_correct = 1 OR NULL)` 而不是 `SUM(is_correct)`？——有 NULL 行时两者结果不同。
- `@Scheduled` 多实例会重复执行怎么办？——见 `ReviewServiceImpl` 的思考题注释。
- 前端计时为什么不能作数？——见 `PracticeService.submit` javadoc 第 1 条。
- v2 为什么先更 DB 再删缓存？——Cache-Aside 下避免旧值回填；见 `ReportCacheService`。
- Redis 穿透/击穿/雪崩怎么防？——空值缓存可扩展、TTL 加随机抖动、故障 30 秒熔断降级。
- 为什么用 SSE 不用 WebSocket？——AI 讲解是单向推送，SSE 基于 HTTP、断线重连简单。
- AI 讲解怎么控 token？——成功后按 `(user_id, question_id, answer_hash)` 落库复用，prompt 截断，二次查看不再调模型。
- 没有 AI key 会怎样？——降级为参考解析 + 正确答案，HTTP 200，不阻塞练习流程。
- v3 为什么用 JWT + 无状态 Session？——前端是 SPA，JWT 放 Authorization 头；服务端不存 session，扩容不用粘性会话。
- 多用户怎么保证隔离？——`CurrentUser` 从 SecurityContext 取 `user_id`，所有练习/报告/复习查询都带 user_id；测试里 learner 有 1 条、alice 为 0。
- SSE 为什么在异步线程拿不到登录用户？——SecurityContext 默认不跨线程；在请求线程取 userId 再传入 `ai-explain` 线程池，异步 dispatch 放行 ASYNC。
