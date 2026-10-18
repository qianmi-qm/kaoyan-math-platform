# v1 联调与验收记录

> v2 增量（Redis 报告缓存 + AI SSE 讲解）见 [`V2.md`](V2.md)；本文保留 v1 的联调记录。

> 日期：2026-10-18
> 环境：Windows / JDK 17.0.20.1 / Maven 3.9.9 / Node 20 / MySQL 8.0.46（临时实例，端口 3307）/ Docker Desktop 4.95.0（Docker 29.8.2、Compose v5.6.0）/ 后端可执行 jar / Vite dev server
> 说明：本机 3306 的 MySQL `root` 密码未知，为避免影响现有数据库，用 `mysqld --initialize-insecure` 在临时目录起了一个 3307 实例做联调；联调结束后可整个目录删除。

## 0. 单元测试

```text
mvn test
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## 1. L1 知识点树

`GET /api/knowledge-points` 返回 3 个根节点、6 个知识点，父子关系正确：

```json
{"code":0,"message":"ok","data":[
  {"id":1,"name":"极限","parentId":null,"children":[{"id":6,"name":"洛必达法则","parentId":1,"children":[]}]},
  {"id":2,"name":"导数","parentId":null,"children":[]},
  {"id":3,"name":"积分","parentId":null,"children":[
    {"id":4,"name":"换元法","parentId":3,"children":[]},
    {"id":5,"name":"分部积分","parentId":3,"children":[]}
  ]}
]}
```

## 2. L2 题库

- 手动录入 4 题：单选、填空、大题各类型，含多知识点关联。
- `GET /api/questions` 列表接口不返回 `answer` / `analysis`（防答案泄露），详情接口返回。
- Excel 导入（3 行数据，其中 1 行是文件内重复）：

```json
{"totalRows":3,"imported":2,"duplicated":1,"failed":0,"failures":[]}
```

- 立刻重复导入同一文件：

```json
{"totalRows":3,"imported":0,"duplicated":3,"failed":0,"failures":[]}
```

说明 `content_hash` 唯一键 + 文件内 `seenHashes` 双层去重生效。

## 3. L3 练习

- `POST /api/practice/sessions`（RANDOM，4 题）生成 4 条记录。
- 逐条提交，服务端计时结果：`durationSec` = 2 / 2 / 2 / 3 秒。
- 判分：3 对 1 错；大题（type=3）带 `selfEval=true` 提交，返回 `correctAnswer` 与 `analysis` 供自评。
- 大题也支持两步自评：先提交占位答案（`isCorrect=null`），再 `POST /api/practice/records/{id}/self-eval` 传 `{correct}`；答错同样入复习队列。

## 4. L4 报告

`GET /api/reports/daily?date=2026-10-18`：

```json
{"totalCount":4,"correctCount":3,"correctRate":0.75,"avgDurationSec":2,
 "kpMastery":[
   {"kpId":1,"name":"极限","mastery":0.4889,"questionCount":2},
   {"kpId":3,"name":"积分","mastery":0.8667,"questionCount":1},
   {"kpId":4,"name":"换元法","mastery":0.8667,"questionCount":1},
   {"kpId":2,"name":"导数","mastery":0.8667,"questionCount":1},
   {"kpId":6,"name":"洛必达法则","mastery":0.3,"questionCount":1}
 ],
 "weakPoints":[]}
```

- 平均用时 9/4 = 2.25 秒，`ROUND(AVG(...))` 后为 2。
- 薄弱点为空：洛必达掌握度 0.3 但题量 1 < 3，符合阈值设计。
- `GET /api/reports/daily/range` 正常返回区间数据；历史日期首次生成后写 `daily_report` 快照，今天则实时计算（当天新做题不会被旧快照挡住），区间缺数据的日期补 0。

## 5. L5 艾宾浩斯复习

- 答错第 4 题后自动入队：`review_schedule` 1 条，`stage=0`，`next_review_date=明天`。
- `GET /api/reviews/stats`：`activeTotal=1, dueToday=0, byStage[0]=1`。
- 把该条 `next_review_date` 改成今天后，`GET /api/reviews/today` 返回 1 条任务。
- `POST /api/reviews/1/result?remembered=true`：`stage` 0 → 1，下次复习 +2 天。
- 再次置为今天并提交 `remembered=false`：`stage` 重置为 0，下次复习 +1 天。

## 6. L6 前端

- `vite build` 成功（产物写入 `frontend/dist`，仅提示 chunk 体积，无错误）。
- Vite dev server 5173 启动，`/api` 代理到 8080：
  - `GET http://127.0.0.1:5173/` 返回页面（title：考研高数学习平台）
  - `GET http://127.0.0.1:5173/api/knowledge-points` 返回与后端一致的数据
  - `GET http://127.0.0.1:5173/api/reports/daily?date=2026-10-18` 返回日报

## 7. L7 部署（Docker Compose 实跑）

- 安装 Docker Desktop 4.95.0 + WSL2，重启后引擎正常（Docker 29.8.2、Compose v5.6.0）。
- 本机 3306 已被现有 MySQL 占用，因此宿主端口改用 3307：

```bash
MYSQL_PORT=3307 docker compose up -d --build
```

- 三容器状态：`kaoyan-mysql` healthy（3307→3306）、`kaoyan-backend` up（8080）、`kaoyan-frontend` up（5173）。
- 验证：
  - `curl http://127.0.0.1:8080/api/knowledge-points` 返回中文知识点树（`极限 / 导数 / 积分` 等）
  - `curl http://127.0.0.1:5173/` 返回 200
  - `curl http://127.0.0.1:5173/api/knowledge-points` 经 Nginx 代理返回与后端一致
  - 容器内跑通 L2～L5：录题 → 练习判分（服务端计时）→ 日报 3 题 / 对 2 / 正确率 66.7% → 复习 stage 0→1、下次 +2 天
- 构建注意：BuildKit 的 RUN 容器不继承宿主代理，需通过 compose build args 传入
  `HTTP_PROXY` / `HTTPS_PROXY` / `MAVEN_OPTS`；MySQL 初始化脚本需 `SET NAMES utf8mb4` 才能正确写入中文。
- 真实模式 UI 用 Chrome headless 渲染 `/bank` `/practice` `/report` `/review`，字段正常、无 `NaN`、无控制台错误；
  `npm run check:contract` 通过。后端 healthcheck 通过后前端才启动（`depends_on: service_healthy`）。

## 8. 压测基线

本机单实例（后端与本机 MySQL 同机），`urllib` 多线程压测：

| 接口 | 并发 | 时长 | 请求数 | QPS | p50 | p95 | max |
|------|------|------|--------|-----|-----|-----|-----|
| `GET /api/questions?page=1&size=10` | 20 | 8s | 2739 | 342.4 | 15.9ms | 259.9ms | 1600.9ms |
| `GET /api/knowledge-points` | 20 | 8s | 12635 | 1579.4 | 10.6ms | 26.6ms | 109.5ms |
| `GET /api/reports/daily` | 10 | 5s | 6583 | 1316.6 | 6.1ms | 17.1ms | 99.3ms |

> 仅作为本地基线，不代表生产容量；题库列表 p95 偏高与分页查询、连接池预热有关。

## 9. 联调中发现并修复的问题

1. **XML 注释里的 `--` 导致应用起不来**：`PracticeRecordMapper.xml` 注释中的长横线分隔线违反 XML 规范（注释内容不能含 `--`），MyBatis 解析 mapper 时直接失败。单测全绿但 `java -jar` 启动失败，属于"测试覆盖不到启动"的典型问题。已改为 `====` 分隔线，并补了 `MapperXmlWellFormedTest` 防回归（25 个测试中的 1 个）。
2. **本机 MySQL 凭据与 `application.yml` 不一致**：默认 `root/root` 连不上现有 3306；改用临时实例 + 独立账号 `kaoyan` 联调，不动本机数据库。
3. **控制台 GBK 编码导致中文 JSON 乱码**：命令行直接传中文会被按 GBK 编码发送，Jackson 报 `Invalid UTF-8 middle byte`；改为 UTF-8 文件 + `curl --data-binary @file`，schema 导入加 `--default-character-set=utf8mb4`。
4. **Docker 初始化中文乱码**：`schema.sql` 没有显式 `SET NAMES utf8mb4`，MySQL 容器初始化时客户端按默认字符集写入，知识点变成 mojibake。已在 schema 首部加 `SET NAMES utf8mb4`，删除 volume 重新初始化后正常。
5. **BuildKit 构建无代理**：Docker Desktop 的 daemon 拉镜像能走系统代理，但 `RUN mvn ...` 的构建容器没有代理环境，Maven 下载卡住。用 compose build args 注入 `HTTP_PROXY` / `HTTPS_PROXY`，并给 Maven 传 `MAVEN_OPTS` 的 `-Dhttps.proxyHost/-Dhttps.proxyPort` 后通过。
6. **未提交记录泄露答案**：`listByDate` 对未提交记录也返回 `correctAnswer`/`analysis`，改按 `submittedAt` 判断；已补 `PracticeServiceImplTest` 回归。
7. **异常统一 HTTP 200**：改为 `ResponseEntity`，参数错误 400、资源不存在 404、唯一键冲突 409、系统错误 500；前端 axios 错误拦截器统一提示。
8. **前端 mock 与真实后端两套字段**：新增 `frontend/src/api/normalize.js` 适配层，页面不再直接猜 VO；`npm run check:contract` 对真实接口做字段检查。
9. **大题提交 NPE**：`Boolean` 三元表达式混用 `boolean` 导致 `selfEval=null` 时拆箱 NPE；改成显式分支后，大题两步自评可用。
10. **N+1 与复习并发**：题库/练习/报告/复习改为批量查询；`review_schedule` 增加 `active_key` 生成列 + 唯一索引，答错入队改为 `upsert`；提交/复习更新加条件与影响行数判断。

## 10. 复现步骤

```bash
# 0) Docker Compose（推荐，三容器）
MYSQL_PORT=3307 docker compose up -d --build
curl http://127.0.0.1:8080/api/knowledge-points
curl http://127.0.0.1:5173/

# 1) 单测
cd backend && mvn test

# 2) 起临时 MySQL（示例端口 3307），导入 schema
mysqld --no-defaults --initialize-insecure --basedir="<mysql>" --datadir="<tmp>/data"
mysqld --no-defaults --basedir="<mysql>" --datadir="<tmp>/data" --port=3307 --mysqlx=0
mysql -h127.0.0.1 -P3307 -uroot --default-character-set=utf8mb4 < backend/src/main/resources/db/schema.sql

# 3) 起后端（覆盖数据源）
java -jar backend/target/kaoyan-platform-1.0.0-SNAPSHOT.jar \
  --spring.datasource.url='jdbc:mysql://127.0.0.1:3307/kaoyan?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false' \
  --spring.datasource.username=kaoyan --spring.datasource.password=kaoyan

# 4) 起前端
cd frontend && npm run dev

# 5) 冒烟
curl http://127.0.0.1:8080/api/knowledge-points
```
