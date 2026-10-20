# 4 周代码导览计划（AI 全栈方向）

> 基础假设：看过 Java / Spring / SQL 语法，但没有独立写过项目；每天约 3 小时。
> 目标不是 4 周达到大厂面试水平，而是：**能跑、能讲清三条主链路、能独立做小改动**。
> 4 周后进入算法/八股/项目深挖阶段。

## 时间怎么用

每天 3 小时，建议：

- 20 分钟：复习昨天 + 看今天要读的入口
- 70 分钟：顺着调用链读代码
- 60 分钟：动手改一个小东西
- 20 分钟：写笔记 / 跑测试 / 记录问题

每周 6 天，留 1 天休息或补进度。

## 第 1 周：跑起来 + 登录链路

### 目标

- 能启动四容器
- 能说清“登录 → JWT → 下一个请求怎么识别我”
- 能改登录页一个小功能

### 阅读顺序

1. `frontend/src/views/LoginView.vue`
2. `frontend/src/api/index.js` 的 `login`
3. `backend/.../auth/controller/AuthController.java`
4. `backend/.../auth/service/AuthService.java`
5. `backend/.../auth/security/JwtService.java`
6. `backend/.../auth/security/JwtAuthenticationFilter.java`
7. `backend/.../common/support/CurrentUser.java`
8. `backend/.../auth/config/SecurityConfig.java`

### 要能回答

- 密码怎么存、怎么校验？
- access / refresh 各多久？
- 为什么 refresh 要轮换？
- `token_version` 为什么能撤销旧 token？
- 下一个请求怎么从 token 变出 `userId`？

### 动手

- 登录页加“显示/隐藏密码”
- 改完跑 `npm run build`
- 用 learner 登录一次，看 Network 里 `/auth/login` 和 `/auth/me`

### 验收

画一张登录时序图，不看代码讲 3 分钟。

## 第 2 周：题库 + 练习 + 日报

### 目标

- 理解 MyBatis XML 和分页
- 理解练习提交、判分、错题入复习
- 理解日报/整体分析的区别

### 阅读顺序

1. `QuestionBankView.vue`
2. `QuestionController` / `QuestionServiceImpl` / `QuestionMapper.xml`
3. `PracticeView.vue` 的 `phase` 三个状态
4. `PracticeController` / `PracticeServiceImpl`
5. `AnswerMatcher`
6. `ReviewService.enqueueIfWrong`
7. `ReportController` / `ReportServiceImpl`
8. `MasteryCalculator` / `MasteryAggregator`

### 要能回答

- 题目列表为什么不返回答案？
- 提交答案为什么计时在服务端？
- `1/2` 和 `0.5` 怎么判对？
- 错题什么时候进复习队列？
- 日报和整体分析分别统计什么？

### 动手

- 题库加“难度筛选”
- 或日报加“今日总用时”
- 改完跑 `mvn test` + `npm run build`

### 验收

用一句话解释：
“用户点提交后，后端做了哪 5 件事？”

## 第 3 周：AI 讲解 + 拍照搜题 + Vue 前端

### 目标

- 理解 OpenAI 兼容接口、SSE、JSON 解析、配额
- 理解图片不落盘、失败降级、答案补算
- 能改一个 AI 相关交互

### 阅读顺序

1. `PracticeView.vue` 的 `explainWithAi`
2. `AiExplanationService`
3. `AiChatClient`
4. `AiQuotaService`
5. `PhotoSearchView.vue`
6. `PhotoSearchService`
7. `PhotoHistoryService`
8. `AiController`

### 要能回答

- SSE 和 WebSocket 为什么选 SSE？
- 为什么图片不落盘？
- 为什么缓存/无 key 不扣配额？
- 为什么模型失败要退款？
- 为什么拍照结果要用户确认后才保存？

### 动手

- 给 AI 讲解加“停止生成”
- 或给拍照结果加“复制答案”
- 改完手工走一遍 AI/拍照

### 验收

画一张“拍照搜题”的时序图：前端 → 后端 → 模型 → 历史。

## 第 4 周：全栈小功能 + 测试 + 部署 + 表达

### 目标

- 独立完成一个跨前后端的小功能
- 会加一个后端测试
- 会用 Docker/Nginx 解释部署
- 形成面试表达

### 动手项目（选一个）

1. 搜题历史按知识点筛选
2. 日报增加“本周总用时”
3. 题库题目增加“复制题干”
4. AI 讲解增加“重新生成”

要求：

- 前端页面
- 后端接口
- 一个测试
- 手工回归
- 写 1 页设计说明

### 阅读/操作

- `Dockerfile` / `docker-compose.yml` / `nginx.conf`
- `docs/LEARNING_GUIDE.md`
- `docs/DECISION_LOG.md`
- 跑一次 `mvn test`、`npm run build`、`docker compose up -d`

### 验收

- 5 分钟 demo
- 10 个项目追问
- 一页“问题 → 方案 → 取舍 → 结果”

## 4 周后你应该达到

- 能独立启动、改页面、加接口、跑测试
- 能讲清登录、练习、报告、AI 四条链路
- 能说清至少 5 个设计取舍
- 但还不足以过大厂面试：算法/八股/系统设计要继续

## 每天不要做什么

- 不要从 111 个 Java 文件第一个开始读
- 不要先读 `V2SchemaMigration`
- 不要先读 mock
- 不要一上来重构 `PracticeView`
- 不要同时学 K8s、微服务、消息队列

## 第 4 周之后

1. 算法 250～300 题（2～3 个月）
2. Java/MySQL/Redis/网络/OS 八股
3. 把项目做一个可量化深度点（压测/缓存/可观测性）
4. 简历 + 模拟面试

目标岗位 AI 全栈，重点补：

- Vue 组件化与状态管理
- REST/错误处理
- LLM API、SSE、结构化输出
- 配额/成本/隐私
- Docker/Nginx/基础可观测性
