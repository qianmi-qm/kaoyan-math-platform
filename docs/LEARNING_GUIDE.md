# 从零读懂这个项目

这份文档按“一个请求怎么走”来读，不按文件逐个背。  
建议顺序：**跑起来 → 登录 → 题库 → 练习 → 报告 → 复习 → AI**。

## 1. 先知道它是什么

- 后端：Spring Boot 3 + Spring Security 6 + MyBatis + MySQL 8 + Redis
- 前端：Vue 3 + Vite + Tailwind + ECharts
- 部署：Docker Compose（MySQL / Redis / backend / frontend）+ Nginx
- AI：DeepSeek `deepseek-flash`（视觉）+ `deepseek-chat`（文本）

入口：

- 前端 `http://localhost:5173`
- 后端 `http://127.0.0.1:8080`
- Swagger `http://127.0.0.1:8080/swagger-ui.html`

## 2. 启动

```bash
cd "/f/the first ai program/kaoyan-math-platform"
docker compose up -d
docker compose ps
```

看日志：

```bash
docker logs -f kaoyan-backend
docker logs -f kaoyan-frontend
```

前端构建：

```bash
cd frontend
npm ci
npm run build
```

测试：

```bash
cd backend
mvn test
```

## 3. 目录地图

```text
backend/src/main/java/com/kaoyan/platform
├── auth       登录、JWT、刷新、限流、邮箱、管理员
├── user       用户表
├── question   题库、知识点、Excel 导入
├── practice   开始练习、提交答案、自评
├── report     日报、趋势、整体学习分析
├── review     艾宾浩斯复习队列
├── ai         AI 讲解、拍照搜题、配额、历史
└── common     统一响应、异常、迁移、当前用户
```

前端：

```text
frontend/src
├── views      页面
├── api        axios 封装 + 字段适配
├── auth.js    token / 登录态 / 多标签同步
├── mock       本地 mock（?mock=1）
└── utils      KaTeX 渲染等
```

## 4. 第一个请求：登录

调用链：

```text
LoginView.vue
  → api.login()
  → POST /api/auth/login
  → AuthController.login()
  → AuthService.login()
  → UserMapper.selectByUsername()
  → BCrypt 校验
  → JwtService.generate()/generateRefresh()
  → AuthResponse
```

重点文件：

- `auth/controller/AuthController.java`
- `auth/service/AuthService.java`
- `auth/security/JwtService.java`
- `auth/security/JwtAuthenticationFilter.java`

要能回答：

- 为什么密码用 BCrypt，不用 MD5？
- access token 和 refresh token 分别多久？
- 为什么 refresh 要轮换？
- `token_version` 为什么能撤销旧 token？

## 5. 第二个请求：带 token 访问题库

```text
axios 请求拦截器加 Authorization
  → JwtAuthenticationFilter
  → 解析 token，查 user，比较 token_version
  → SecurityContext 放 LoginUser
  → CurrentUser 取 userId
  → QuestionController.page()
  → QuestionServiceImpl.page()
  → QuestionMapper.selectPage()
```

看 `auth/security/JwtAuthenticationFilter.java` 和 `common/support/CurrentUser.java`。

## 6. 练习与判分

```text
PracticeView.vue
  → POST /api/practice/sessions
  → PracticeServiceImpl.startSession()
  → 选题（KP / RANDOM / WRONG / QUESTION）
  → 插 practice_record
  → 返回不含答案的 PracticeRecordVO

提交答案
  → POST /api/practice/records/{id}/submit
  → 服务端计时
  → AnswerMatcher 判分
  → 错题入复习队列
  → 删日报缓存
```

重点：

- 为什么计时在服务端做？
- 为什么提交后要删缓存，而不是更新缓存？
- `AnswerMatcher` 怎么处理 `1/2` 和 `0.5`？

## 7. 日报与整体分析

今天：

```text
GET /api/reports/daily?date=...
  → ReportServiceImpl.getDaily()
  → 今天实时算；历史读 daily_report 快照
```

整体：

```text
GET /api/reports/overview?days=30
  → LearningOverviewService.overview()
  → 聚合近 N 天：开始/提交/判分、分题型、分难度、复习执行
```

两者共用：

- `MasteryCalculator`：题目级公式
- `MasteryAggregator`：最近 5 次、streak、整体加权

要能回答：

- 日报的“正确率”分母是什么？
- 为什么日报和整体分析要分开？
- 综合掌握度为什么按题量加权？

## 8. 复习

```text
GET /api/reviews/today
  → ReviewServiceImpl.todayList()
POST /api/reviews/{id}/result?remembered=true
  → EbbinghausScheduler.nextStage()
```

复习队列由答错触发：`PracticeServiceImpl.submit()` 里调用 `reviewService.enqueueIfWrong()`。

## 9. AI 讲解与拍照搜题

文本讲解：

```text
POST /api/ai/explain/{recordId}
  → AiExplanationService.stream()
  → 查缓存讲解
  → 无 key/失败：返回参考解析
  → 有 key：SSE 流式调用 DeepSeek
```

拍照：

```text
POST /api/ai/photo-search (multipart)
  → PhotoSearchService.search()
  → 校验图片 magic bytes
  → 扣配额
  → 图片转 data URL，调 vision
  → 解析 JSON
  → 答案缺失时用文本模型补算
```

公共客户端：`ai/service/AiChatClient.java`。  
它只负责 URL / Bearer / JSON，不掺业务。

要能回答：

- 为什么图片不落盘？
- 为什么缓存/降级不扣配额？
- 为什么 AI 配额失败要退款？
- 为什么拍照结果要用户确认后才保存？

## 10. 前端怎么读

推荐顺序：

1. `App.vue`：整体布局、导航、登录态
2. `router/index.js`：页面和守卫
3. `auth.js`：token / refresh / 多标签
4. `api/index.js`：所有接口入口
5. `api/normalize.js`：后端字段 → 页面字段
6. 具体页面：`LoginView` → `QuestionBankView` → `PracticeView` → `ReportView` → `ReviewView` → `PhotoSearchView`

`PracticeView.vue` 是最大的页面，先看 `phase` 三个状态：`setup / practicing / result`。

## 11. 建议练习

每读完一块，做一个小改动：

1. 登录页加“显示密码”
2. 题库按难度筛选
3. 日报加一个“今日总用时”
4. 复习页加“撤销上一次”
5. 拍照历史按知识点筛选

每改一个，跑：

```bash
cd backend && mvn test
cd frontend && npm run build
```

## 12. 不要一上来读全部

先抓三条线：

1. **认证线**：Login → JWT → Filter → CurrentUser
2. **练习线**：Session → Submit → AnswerMatcher → Review
3. **报告线**：PracticeRecord → ReportService → Mastery

其他都是支线。  
读不懂时先看 Controller 的入参出参，再往下追 Service，最后看 Mapper XML。

## 13. 常用排错

```bash
docker compose ps
docker logs --tail 200 kaoyan-backend
docker exec kaoyan-mysql mysql -uroot -proot kaoyan -e "SHOW TABLES;"
docker exec kaoyan-redis redis-cli --scan --pattern 'report:*'
```

前端 502：后端没起或 Nginx 解析不到 backend。  
登录 401：token 过期 / token_version 不匹配。  
拍照失败：检查 AI key、relay、配额和后端日志。
