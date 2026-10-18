# 考研高等数学学习平台（v1）

Spring Boot 3 + MyBatis + MySQL + Vue 3 的学习平台。L1～L3（数据模型 / 题库 / 练习）与前端四个页面已实现；**L4 掌握度、L5 艾宾浩斯调度仍留作练习**，对应的 18 个验收测试是红的——这是"骨架剥离学习法"的作业，不是故障。

> 当前进度（2026-10-18）：L1～L3、L6 前端已完成；L4 / L5 待实现；L7 的 Docker 编排已配置，但本机未装 Docker、尚未实测；v2（Redis 报告缓存、大模型 SSE 讲解）未开始。

配套设计文档：《v1 方案与分层学习路线图》（本地 `kaoyan-math-v1-plan`，未随仓库发布）

## 验收基线（当前）

| 检查项 | 当前结果 |
|--------|----------|
| `mvn compile` | BUILD SUCCESS |
| `SkeletonSmokeTest`（4 个用例） | 全绿 |
| `MasteryCalculatorTest`（10 个用例） | 全红（UnsupportedOperationException）——待完成的 L4 作业 |
| `EbbinghausSchedulerTest`（8 个用例） | 全红——待完成的 L5 作业 |
| L1～L3 业务接口 | 可用（知识点树 / 题库 / 练习） |
| L4 / L5 相关接口 | 算法补完前会抛 `UnsupportedOperationException` |

红灯不是缺陷，是完成定义：实现对应模块让灯变绿。

## 怎么学（六步闭环）

L1～L3 已完成，下表保留作为复习参考；当前待做的是 L4 / L5。

对每一层：读契约（接口 javadoc / 测试）→ 合卷实现 → 跑测试验收 → 与 AI 生成的参考实现对比 → 记决策日志。

| 层 | 范围 | 完成标志 | 主要文件 |
|----|------|----------|----------|
| L1 | 数据模型：DDL 已给，自己补 resultMap 与知识点点树 | 知识点树接口可用 | `db/schema.sql`、`KnowledgePoint*` |
| L2 | 题库：录入 / 分页 / Excel 导入 | `QuestionServiceImpl` 全部方法可用 | `question/**`、`QuestionMapper.xml` |
| L3 | 练习：选题 / 服务端计时 / 判分 | 练习三接口可用 | `practice/**` |
| L4 | 报告：聚合 SQL + 掌握度 | `MasteryCalculatorTest` 全绿 | `report/domain`、`ReportServiceImpl` |
| L5 | 复习：艾宾浩斯调度 | `EbbinghausSchedulerTest` 全绿 + 三接口可用 | `review/**` |
| L6 | 前端：四页面 + KaTeX + ECharts | 走通"导入→练习→报告→复习" | `frontend/src/views/**` |
| L7 | 工程收尾：README / 压测数字 / 简历 bullet | `docker-compose up` 一键跑 | 全仓库 |

每层的实现提示都写在对应 Service 接口的 javadoc 和 Mapper XML 注释里。

## 运行

### 方式一：Docker 一键启动（推荐，装好 Docker 后）

```bash
docker-compose up --build
# 前端 http://localhost:5173
# 后端 http://localhost:8080  Swagger http://localhost:8080/swagger-ui.html
# MySQL 3306（root/root），首次启动自动执行 schema.sql 建表
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
mvn test           # 冒烟绿 + 两个算法测试红 = 骨架正常
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
    mapper/*.xml         # 全部 SQL 待实现（L1~L5）
  src/test/java/         # 验收测试：冒烟(绿) + 算法(红)
frontend/
  src/views/     # 四页面（题库/练习/报告/复习，已实现）
  src/api/       # 调后端的封装（已实现）
  src/mock/      # 前端 mock 数据层（无后端演示用）
  src/utils/     # math.js 公式渲染 / ui.js 公共样式映射
docs/DECISION_LOG.md     # 决策日志（含 8 条已定决策 + 空行待填）
docker-compose.yml       # mysql + backend + frontend 一键启动
```

## 常用面试问题索引（先自测再看提示）

- 为什么 Service 层要接口 + 实现？骨架里每个模块都这么拆了。
- `(user_id, practice_date)` 联合索引的最左前缀：`PracticeRecordMapper.selectByUserAndDate` 必须让 user_id 出现在等值条件里。
- 为什么 `selectDailyAgg` 里正确数用 `COUNT(is_correct = 1 OR NULL)` 而不是 `SUM(is_correct)`？——有 NULL 行时两者结果不同。
- `@Scheduled` 多实例会重复执行怎么办？——见 `ReviewServiceImpl` 的思考题注释。
- 前端计时为什么不能作数？——见 `PracticeService.submit` javadoc 第 1 条。
