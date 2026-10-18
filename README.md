# 考研高等数学学习平台（v1）

Spring Boot 3 + MyBatis + MySQL + Vue 3 的学习平台。v1 已完成：L1～L6 全部落地，23 个验收测试全绿（4 冒烟 + 10 掌握度 + 8 复习调度 + 1 mapper XML 校验），并用临时 MySQL 8 + 后端 jar + Vite 代理走通了"建知识点 → 录题 / Excel 导入 → 练习判分 → 日报 / 掌握度 → 艾宾浩斯复习"全链路；`docker compose up -d --build` 三容器也实际启动验证通过。

> 当前进度（2026-10-18）：v1 功能完成；本地联调与 Docker Compose 三容器联调均通过（本机 3306 被占用，Compose 用 `MYSQL_PORT=3307`）；压测已有本机基线；下一步是 v2（Redis 报告缓存、大模型 SSE 讲解）。详细记录见 [`docs/VERIFICATION.md`](docs/VERIFICATION.md)。

配套设计文档：《v1 方案与分层学习路线图》（本地 `kaoyan-math-v1-plan`，未随仓库发布）

## 验收基线（当前）

| 检查项 | 当前结果 |
|--------|----------|
| `mvn compile` | BUILD SUCCESS |
| `SkeletonSmokeTest`（4 个用例） | 全绿 |
| `MasteryCalculatorTest`（10 个用例） | 全绿 |
| `EbbinghausSchedulerTest`（8 个用例） | 全绿 |
| `mvn test` | 23/23 通过 |
| L1～L5 业务接口 | 可用（知识点树 / 题库 / 练习 / 报告 / 复习） |
| 本地联调（L1～L6） | 通过（见 `docs/VERIFICATION.md`） |
| Docker Compose | 三容器 `up` 验证通过（3307 / 8080 / 5173） |

红灯不是缺陷，是完成定义——现在 23 个测试已经全部转绿。

## 验证记录（2026-10-18）

完整命令与返回见 [`docs/VERIFICATION.md`](docs/VERIFICATION.md)。摘要：

| 项 | 结果 |
|----|------|
| 单元测试 | 23/23 通过 |
| L1 知识点树 | 3 个根节点 / 6 个知识点 |
| L2 题库 | 手动录入 4 题；Excel 导入 3 行 → 2 入库 + 1 重复；重复导入 0 入库 + 3 重复 |
| L3 练习 | 4 条记录，服务端计时 2～3s，判分与自评正常 |
| L4 日报 | 4 题 / 对 3 / 正确率 75% / 平均 2s，掌握度与区间查询正常 |
| L5 复习 | 答错入队；记住 → stage 1、+2 天；忘了 → stage 0、+1 天 |
| L6 前端 | `vite build` 通过；dev server + `/api` 代理联调通过 |
| L7 部署 | `docker compose up -d --build` 三容器验证通过 |
| 压测（本机单实例） | 题库列表 342 QPS / p95 260ms；知识点树 1579 QPS / p95 27ms；日报快照 1317 QPS / p95 17ms |

## 怎么学（六步闭环）

L1～L5 已完成，下表保留作为复习参考；v2 是下一阶段。

对每一层：读契约（接口 javadoc / 测试）→ 合卷实现 → 跑测试验收 → 与 AI 生成的参考实现对比 → 记决策日志。

| 层 | 范围 | 完成标志 | 主要文件 |
|----|------|----------|----------|
| L1 | 数据模型：DDL 已给，自己补 resultMap 与知识点点树 | 知识点树接口可用 | `db/schema.sql`、`KnowledgePoint*` |
| L2 | 题库：录入 / 分页 / Excel 导入 | `QuestionServiceImpl` 全部方法可用 | `question/**`、`QuestionMapper.xml` |
| L3 | 练习：选题 / 服务端计时 / 判分 | 练习三接口可用 | `practice/**` |
| L4 | 报告：聚合 SQL + 掌握度 | `MasteryCalculatorTest` 全绿 | `report/domain`、`ReportServiceImpl` |
| L5 | 复习：艾宾浩斯调度 | `EbbinghausSchedulerTest` 全绿 + 三接口可用 | `review/**` |
| L6 | 前端：四页面 + KaTeX + ECharts | 走通"导入→练习→报告→复习" | `frontend/src/views/**` |
| L7 | 工程收尾：README / 压测数字 / 简历 bullet | `docker compose up -d --build` 三容器跑通 | 全仓库 |

每层的实现提示都写在对应 Service 接口的 javadoc 和 Mapper XML 注释里。

## 运行

### 方式一：Docker 一键启动（推荐，装好 Docker 后）

```bash
docker compose up -d --build
# 前端 http://localhost:5173
# 后端 http://localhost:8080  Swagger http://localhost:8080/swagger-ui.html
# MySQL 默认映射宿主 3306（root/root），首次启动自动执行 schema.sql 建表
# 本机 3306 已被占用时（常见）：
#   MYSQL_PORT=3307 docker compose up -d --build

# 构建需要代理时（可选）：把 HTTP_PROXY / HTTPS_PROXY / MAVEN_OPTS 作为 build args 传入，
# 其中 Maven 还需要 MAVEN_OPTS 里的 -Dhttps.proxyHost / -Dhttps.proxyPort。
```

### 方式二：本地开发

1. 准备 JDK 17+、Maven、Node 18+、MySQL 8
2. 建库：`mysql -uroot -p < backend/src/main/resources/db/schema.sql`
3. 后端：`cd backend && mvn spring-boot:run`
4. 前端：`cd frontend && npm install && npm run dev`（已配 /api 代理到 8080）

### 前端 Mock 模式（不启动后端也能体验完整界面）

前端内置 mock 数据层（12 道真题 + 14 天趋势数据）。访问 `http://localhost:5173/?mock=1` 即可开启；
`?mock=0` 关闭后走真实后端接口。mock 通过自定义 axios adapter 在请求层拦截，接口契约与后端
`Result{code,message,data}` 完全一致，切换无需改代码。

### 验证命令

```bash
cd backend
mvn compile        # 编译
mvn test           # 23 个验收测试全绿
```

## 目录结构

```
backend/
  src/main/java/com/kaoyan/platform/
    common/      # Result 统一响应 / 全局异常 / Clock / 当前用户（已实现，可读可测）
    question/    # 题库模块（L1/L2）
    practice/    # 练习模块（L3）
    report/      # 报告模块 + MasteryCalculator 掌握度算法（L4）
    review/      # 复习模块 + EbbinghausScheduler 调度算法（L5）
  src/main/resources/
    db/schema.sql        # 7 张表 DDL + 初始数据（已实现）
    mapper/*.xml         # MyBatis SQL（L1~L5）
  src/test/java/         # 验收测试：23 个用例（冒烟 + 掌握度 + 复习调度 + mapper XML）
frontend/
  src/views/     # 四页面（题库/练习/报告/复习，已实现）
  src/api/       # 调后端的封装（已实现）
  src/mock/      # 前端 mock 数据层（无后端演示用）
  src/utils/     # math.js 公式渲染 / ui.js 公共样式映射
docs/DECISION_LOG.md     # 决策日志（架构决策 + 联调踩坑）
docs/VERIFICATION.md     # v1 联调与压测记录
docker-compose.yml       # mysql + backend + frontend 一键启动
```

## 常用面试问题索引（先自测再看提示）

- 为什么 Service 层要接口 + 实现？项目里每个模块都这么拆了。
- `(user_id, practice_date)` 联合索引的最左前缀：`PracticeRecordMapper.selectByUserAndDate` 必须让 user_id 出现在等值条件里。
- 为什么 `selectDailyAgg` 里正确数用 `COUNT(is_correct = 1 OR NULL)` 而不是 `SUM(is_correct)`？——有 NULL 行时两者结果不同。
- `@Scheduled` 多实例会重复执行怎么办？——见 `ReviewServiceImpl` 的思考题注释。
- 前端计时为什么不能作数？——见 `PracticeService.submit` javadoc 第 1 条。
