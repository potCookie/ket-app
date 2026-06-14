# KET 儿童英语学习 App — 完整需求文档 + 技术方案

> 本文档用于向开发 IDE / 工程师完整描述项目，可直接作为 Prompt 输入。

---

## 一、项目背景

家长为孩子（8岁左右，二年级升三年级）备考 **剑桥 KET 考试**，
需要一个 Web 应用，让孩子每天在手机/平板上完成学习任务，
家长可查看进度。

**核心机制**：学习内容由 AI 自动生成，而非预先写死。
用户（家长）在首次使用时设定「计划开始时间」和「计划结束时间」，
系统根据 KET 考试大纲，在计划时间范围内，**每天自动生成**当天的学习内容（词汇、阅读、听力、口语、语法、写作）。
生成后的内容存储到数据库，孩子当天登录即可看到当日任务。

---

## 一、计划配置（新增 · 重要）

### 首次使用引导（Onboarding）

家长（或孩子）第一次打开 App 时，需完成计划配置：

| 字段 | 说明 | 示例 |
|------|------|------|
| 孩子昵称 | 用于头像旁显示 | 小明 |
| 年级 | 二升三 / 三升四 / 其他 | 二升三 |
| 备考目标 | KET / PET（当前仅 KET） | KET |
| **计划开始日期** | 从哪天开始学习 | 2026-06-15 |
| **计划结束日期** | 到哪天结束（自动计算总天数） | 2026-07-20 |
| 每日学习时长目标 | 用于首页展示建议时长 | 35分钟 |

配置完成后，系统：
1. 根据开始/结束日期，**计算总天数**（如 35 天）
2. 按照 KET 考试大纲，**分配每日主题**（词汇主题、语法点、阅读题材等）
3. **立即生成最近 7 天的学习内容**（确保首次使用有内容可学）
4. 设置**每日凌晨 1:00 自动生成**后续每天的内容（通过定时任务）

### 计划配置管理

- 计划配置后可修改（在「我的」→「学习计划」中）
- 修改开始/结束日期后，系统重新分配每日主题并补生成缺失的内容
- 已生成的内容不会被覆盖（除非手动清除）

### AI 内容生成规则

每日生成的学习内容包含以下模块（与 KET 考试题型对齐）：

| 模块 | 生成内容 |
|------|----------|
| 词汇 | 8-12 个单词，含中文释义 + 英文例句 |
| 阅读 | 1 篇短文（80-120 词）+ 2-3 道理解题 + 中文翻译 |
| 听力 | 4-5 道听力题，含题干、选项、答案、关键词、中文翻译 |
| 口语 | 1 个对话模板（填空形式）+ 3-5 个关键句型（中英对照）|
| 语法 | 1 个语法讲解 + 3-5 道练习题 + 答案 + 解析 |
| 写作（高阶段）| 1 个写作题目 + 范文 |

生成内容的质量要求：
- 阅读短文主题与当日词汇主题一致
- 听力音频文本用英式英语（en-GB）
- 题目难度符合 KET 水平（CEFR A2）
- 中文翻译准确、易懂（适合 8 岁儿童）

---

## 二、用户角色

| 角色 | 说明 |
|------|------|
| **孩子**（主用户） | 8岁左右，使用手机或平板完成每日学习 |
| **家长** | 查看孩子学习进度、手动标记完成、修改任务 |

---

## 三、核心功能需求

### 3.1 孩子端（4个Tab）

#### Tab 1 · 首页（🏠）
- 显示今日日期、第几周第几天、今日主题
- 今日任务卡片（主题色渐变背景）：
  - 学习模块标签（📚词汇 / 📖阅读 / 🎧听力 / 🗣️口语 / ✏️语法 / ✍️写作）
  - 建议学习时长
  - 「🚀 开始学习」大按钮
- 学习模块快捷入口（网格卡片，显示完成状态）
- 连续打卡天数

#### Tab 2 · 学习（📖）— 顺序流程，一页到底
- **顶部进度条**：①词汇 → ②阅读 → ③听力 → ④口语 → ⑤语法，当前步骤高亮，已完成打勾
- **词汇模块**：卡片翻页交互（点击英文卡片 → 翻转显示中文释义 + 例句）
- **阅读模块**：
  - 显示英文短文
  - 「🌐 查看中文翻译」按钮，点击展开（默认隐藏）
  - 下方显示阅读理解题（选择题），提交后显示正确答案
- **听力模块**：
  - 每题有音频播放器（可播放/暂停）
  - 「🌐 查看中文翻译」按钮，点击展开（默认隐藏）
  - 下方选择题，提交后判分
- **口语模块**：
  - 显示填空模板（如：My name is ____. I am ____ years old.）
  - 关键句型中英对照表
  - 录音按钮 🎙️，录音后可回放
- **语法模块**：
  - 显示语法讲解
  - 练习题（选择题），提交后显示正确答案和解析
- **模块完成**：每个模块学完点「✔ 下一步」解锁下一模块
- **全部完成**：🎉 动画 + 获得星星 ⭐

#### Tab 3 · 成就（🏆）
- 累计星星数（大字显示 + 动画）
- 勋章墙：
  - 🌟 打卡7天
  - 🔒 打卡30天（未解锁为灰色锁图标）
  - 🔒 全勤勋章（35天全部完成）
  - 🔒 满分学霸（做题正确率100%）
- 学习统计：
  - 已完成天数 / 总天数
  - 做题正确率
  - 最长连续打卡天数
  - 已学词汇数

#### Tab 4 · 我的（👤）
- 头像（默认用 emoji 随机生成，可更换）
- 昵称、年级（二升三）、备考目标（KET）
- 菜单列表：
  - 📊 学习报告
  - 🗓️ 学习计划（**新增** · 查看/修改开始结束日期、总天数）
  - 👨‍👩‍👧 家长入口（长按 Logo 3秒 或 菜单进入）
  - 🔔 消息通知
  - ⚙️ 设置（4位数字密码修改）

---

### 3.3 首次使用引导（Onboarding Wizard）

第一次打开 App 时，强制展示引导页（分步表单）：

```
第1步：孩子信息
  - 昵称（输入框）
  - 年级（选择器：二升三 / 三升四 / 其他）
  - 备考目标（选择器：KET / PET，当前仅 KET）

第2步：计划时间
  - 计划开始日期（日期选择器，默认明天）
  - 计划结束日期（日期选择器，默认开始后35天）
  - 每日学习时长目标（选择器：25分钟 / 35分钟 / 45分钟）

第3步：确认
  - 显示：孩子昵称、计划时间段、总天数、每日主题预览（前5天）
  - 按钮：「✅ 确认并开始」
```

点击确认后：
- 创建用户账号（自动生成 4 位密码，提示家长记住）
- 创建计划配置记录（plan_config 表）
- 立即生成前 7 天的学习内容
- 跳转到首页

### 3.4 家长入口
- 访问方式：`/parent` 路径 或 首页长按 Logo 3秒
- 功能：
  - 查看孩子学习记录（日期 / 完成状态 / 正确率）
  - 手动标记某天「已完成」
  - 查看学习报告（星星数 / 正确率趋势图）
  - **修改计划时间**（开始日期、结束日期）
  - **手动触发内容生成**（某天内容生成失败时，家长可手动重新生成）
  - （高级）查看/编辑某天的学习内容

---

## 四、非功能需求

| 项目 | 要求 |
|------|------|
| **平台** | 手机浏览器 + 平板浏览器（响应式，无需安装） |
| **适配** | 手机 375px起，平板 520px+，iPad Mini / 安卓平板 |
| **数据** | 云端同步（MySQL），换设备不丢进度 |
| **登录** | 孩子用 4位数字密码，家长用账号登录，JWT 鉴权 |
| **部署** | 后端 Jar 包可运行，前端静态文件可部署到任意 Web 服务器 |
| **音频** | 听力音频用 edge-tts（英式发音）自动合成，base64 嵌入或单独文件 |

---

## 五、技术架构

### 5.1 技术栈详解

#### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.4 | 渐进式响应式框架 |
| Vite | ^5.0 | 极速构建工具，热更新 |
| Pinia | ^2.1 | 轻量状态管理（替代 Vuex） |
| Vue Router | ^4.3 | SPA 路由管理 |
| **Element Plus** | ^2.7 | UI 组件库（按钮/卡片/对话框/消息/进度条） |
| **Tailwind CSS** | ^3.4 | 原子化 CSS 框架，快速实现排版+间距+响应式 |

> **为什么同时用 Element Plus + Tailwind CSS？**
> - Element Plus 提供高质量交互组件，保证 UI 一致性和无障碍体验
> - Tailwind CSS 提供灵活的排版、间距、颜色、响应式工具类，快速实现精细化布局
> - 两者互补：复杂交互用 Element Plus 组件，细节样式用 Tailwind 微调

#### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| **JDK** | **21 LTS** | 长期支持版，包含虚拟线程、Record 类、pattern matching 等现代特性 |
| **Spring Boot** | **3.5.x（最新稳定版）** | 快速开发框架，内嵌 Tomcat |
| **MyBatis-Plus** | ^3.5.7 | 国产 ORM 框架，简化 CRUD，支持 Lambda 查询 |
| MySQL Connector | 8.0.x | 数据库驱动 |
| JJWT | 0.12.x | JWT 令牌生成与校验 |
| Lombok | — | 减少样板代码（@Data/@Builder 等） |
| edge-tts Java 绑定 | — | 调用微软 TTS 合成听力音频 |

### 5.2 项目目录结构

```
ket_app/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/ketapp/
│   │   ├── controller/          # REST API 控制器
│   │   │   ├── AuthController.java
│   │   │   ├── TaskController.java
│   │   │   ├── QuizController.java
│   │   │   ├── RecordController.java
│   │   │   ├── CheckInController.java
│   │   │   └── ParentController.java
│   │   ├── service/            # 业务逻辑
│   │   │   ├── UserService.java
│   │   │   ├── TaskService.java
│   │   │   ├── QuizService.java
│   │   │   └── StatsService.java
│   │   ├── entity/            # 实体类（对应数据库表）
│   │   │   ├── User.java
│   │   │   ├── Task.java
│   │   │   ├── StudyLog.java
│   │   │   ├── Recording.java
│   │   │   └── Badge.java
│   │   ├── dto/              # 数据传输对象
│   │   ├── mapper/           # MyBatis-Plus Mapper 接口
│   │   └── KetAppApplication.java
│   ├── resources/
│   │   ├── application.yml
│   │   ├── schema.sql        # 建表脚本
│   │   └── data.sql         # 初始数据（从 KET_daily_plan.json 导入）
│   └── pom.xml
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── views/            # 页面组件
│   │   │   ├── HomeView.vue      # 首页
│   │   │   ├── LearnView.vue     # 学习页（顺序流程）
│   │   │   ├── AchieveView.vue  # 成就页
│   │   │   ├── ProfileView.vue   # 我的页
│   │   │   └── ParentView.vue    # 家长入口页
│   │   ├── components/       # 可复用组件
│   │   │   ├── VocabCard.vue     # 词汇翻卡
│   │   │   ├── ReadingPassage.vue # 阅读短文（含翻译折叠）
│   │   │   ├── AudioPlayer.vue    # 音频播放器
│   │   │   ├── QuizQuestion.vue  # 选择题组件
│   │   │   ├──录音Recorder.vue   # 录音组件
│   │   │   ├── ProgressBar.vue   # 学习进度条
│   │   │   └── StarAnimation.vue # 打卡星星动画
│   │   ├── router/
│   │   │   └── index.js       # Vue Router 配置
│   │   ├── store/            # Pinia 状态管理
│   │   │   ├── auth.js
│   │   │   ├── task.js
│   │   │   └── stats.js
│   │   ├── api/              # 后端接口调用封装
│   │   │   └── index.js
│   │   ├── assets/
│   │   │   └── styles/
│   │   │       └── main.css   # 全局样式（含 CSS 变量）
│   │   └── App.vue
│   ├── public/
│   ├── package.json
│   └── vite.config.js
│
├── data/
│   └── KET_daily_plan.json   # 每日学习计划数据（已有）
│
└── README.md                  # 部署说明
```

---

## 六、数据库设计

```sql
-- 用户表
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,        -- BCrypt 加密
  role VARCHAR(10) NOT NULL,            -- 'child' / 'parent'
  avatar VARCHAR(200),
  invite_code VARCHAR(20),               -- 家长邀请码（唯一）
  parent_id BIGINT,                     -- 孩子的家长ID
  stars INT DEFAULT 0,
  streak INT DEFAULT 0,                 -- 连续打卡天数
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (parent_id) REFERENCES user(id)
);

-- 每日任务表（从 KET_daily_plan.json 导入）
CREATE TABLE task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_date DATE NOT NULL UNIQUE,
  week INT NOT NULL,
  day INT NOT NULL,
  theme VARCHAR(100) NOT NULL,
  duration VARCHAR(20),
  vocab_data JSON,           -- 词汇数据（words数组）
  grammar_data JSON,         -- 语法数据（point/exercises/answers）
  reading_data JSON,         -- 阅读数据（passage/text/translation/questions）
  listening_data JSON,       -- 听力数据（questions数组）
  speaking_data JSON,        -- 口语数据（template/phrases）
  writing_data JSON,         -- 写作数据（prompt/sample）
  parent_note TEXT,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 学习记录表
CREATE TABLE study_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  task_date DATE NOT NULL,
  module VARCHAR(20),          -- 'vocab'/'reading'/'listening'/'speaking'/'grammar'
  started_at DATETIME,
  finished_at DATETIME,
  quiz_score INT,             -- 做题得分（正确题数）
  quiz_total INT,             -- 总题数
  checked_in BOOLEAN DEFAULT FALSE,
  stars_earned INT DEFAULT 0,  -- 本次获得星星
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id),
  UNIQUEKEY (user_id, task_date, module)
);

-- 录音文件表
CREATE TABLE recording (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  task_date DATE NOT NULL,
  module VARCHAR(20) NOT NULL,   -- 'speaking'
  file_path VARCHAR(200),
  file_url VARCHAR(500),
  uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 勋章表
CREATE TABLE badged (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  badge_type VARCHAR(20) NOT NULL,  -- 'streak_7'/'streak_30'/'perfect_attendance'/'full_score'
  earned_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES user(id),
  UNIQUEKEY (user_id, badge_type)
);
```

---

## 七、后端 API 设计（共 18 个接口）

### 认证相关
| 接口 | 方法 | 说明 | 请求体 |
|------|------|------|--------|
| `/api/auth/register` | POST | 注册（孩子/家长） | `{ name, role, pin(4位数字), parentInviteCode }` |
| `/api/auth/login` | POST | 登录（返回 JWT） | `{ userId, pin, role }` |
| `/api/auth/me` | GET | 获取当前用户信息 | Header: `Authorization: Bearer <token>` |

### 任务相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/task/today` | GET | 获取今日任务 |
| `/api/task/date?date=2026-06-13` | GET | 获取指定日期任务 |
| `/api/task/:id` | GET | 获取任务详情（含全部模块数据）|
| `/api/task` | POST | 创建/更新任务（导入 JSON 用）|

### 学习相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/study/start` | POST | 开始学习某模块（记录 started_at）|
| `/api/study/finish` | POST | 完成某模块（记录 finished_at + stars）|
| `/api/quiz/submit` | POST | 提交做题答案（自动判分）|
| `/api/recording/upload` | POST | 上传口语录音（multipart/form-data）|

### 打卡 & 成就
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/checkin` | POST | 打卡（全部模块完成后调用）|
| `/api/stats` | GET | 学习统计（星星/天数/正确率）|
| `/api/badges` | GET | 获取用户勋章列表 |

### 家长相关
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/parent/children` | GET | 查看关联的孩子列表 |
| `/api/parent/report?childId=xxx` | GET | 查看孩子学习报告 |
| `/api/parent/checkin` | POST | 家长手动标记某天已完成 |

---

## 八、UI 设计规范

### 8.1 配色方案
| 用途 | 色值 | 说明 |
|------|------|------|
| 主色（品牌绿）| `#4ECDC4` | 按钮、高亮、进度条 |
| 辅色（珊瑚红）| `#FF6B6B` | 星星、连续打卡、强调 |
| 背景色 | `#F7F9FC` | 页面背景 |
| 卡片白 | `#FFFFFF` | 卡片背景 |
| 文字主色 | `#263238` | 标题 |
| 文字辅色 | `#455A64` | 正文 |
| 文字提示色 | `#78909C` | 辅助文字 |
| 成功绿 | `#66BB6A` | 正确、完成 |
| 错误红 | `#EF5350` | 错误 |

### 8.2 字体规范
- 中文字体：`-apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif`
- 英文/数字：`system-ui, -apple-system, sans-serif`
- 字号：
  - 大标题：20px
  - 卡片标题：16-17px
  - 正文：14-15px
  - 辅助文字：11-13px

### 8.3 组件规范
- 按钮：大圆角 `border-radius: 12-16px`，高度 44-48px（适合儿童手指点击）
- 卡片：圆角 `14-16px`，轻微阴影 `box-shadow: 0 1px 6px rgba(0,0,0,0.06)`
- 图标：emoji 或 SVG，尺寸 20-28px
- 间距：模块间距 16px，卡片内边距 14-20px

### 8.4 交互规范
- 所有「翻译」默认隐藏，点击展开（`<details>` 标签或 JS 控制）
- 词汇卡片：点击翻转，动画 `transform: rotateY(180deg)` 0.4s
- 做题选项：点击后变蓝（selected），提交后正确变绿、错误变红
- 打卡成功：🎉 星星飞入动画（CSS animation + JS 触发）
- 音频播放：播放时显示声波动画

---

## 九、开发阶段划分

### Phase 1 · 静态原型（Mock 数据，无后端）
> 目标：确认 UI 效果，可点击切换 Tab

- [ ] 搭建 Vue 3 + Vite 项目骨架
- [ ] 实现 4个 Tab 页面（首页/学习/成就/我的）
- [ ] 实现学习页顺序流程（进度条 + 模块切换）
- [ ] Mock 数据渲染（直接写死 JSON 在组件里）
- [ ] 响应式布局（手机 + 平板预览）
- [ ] 输出：可本地运行的 Vue 项目

### Phase 2 · Spring Boot 后端搭建
- [ ] 初始化 Spring Boot 项目（**JDK 21 + Spring Boot 3.5.x**）
- [ ] 配置 MySQL 连接 + MyBatis-Plus（application.yml）
- [ ] 创建 Entity + Mapper + Service + Controller
- [ ] 实现 JWT 鉴权（登录/注册接口，jjwt 0.12.x）
- [ ] 编写建表 SQL（schema.sql）
- [ ] 编写数据导入脚本（读取 KET_daily_plan.json 写入 task 表）
- [ ] 输出：后端 API 可本地运行，Postman 可测试

### Phase 3 · 前端接入后端
- [ ] 封装 API 调用层（frontend/src/api/index.js）
- [ ] 实现 Pinia store（auth.js / task.js / stats.js）
- [ ] 首页：从后端获取今日任务数据并渲染
- [ ] 学习页：点击「开始学习」→ 调用 `/api/study/start`
- [ ] 做题提交：调用 `/api/quiz/submit` 并展示判分结果
- [ ] 打卡：全部完成后调用 `/api/checkin`
- [ ] 输出：完整前后端联通可跑

### Phase 4 · 互动功能完善
- [ ] 词汇翻卡组件（VocabCard.vue，含翻转动画）
- [ ] 音频播放器组件（AudioPlayer.vue，含播放/暂停/进度条）
- [ ] 录音组件（Recorder.vue，使用 Web API MediaRecorder）
- [ ] 翻译折叠（阅读 + 听力，点击展开/收起）
- [ ] 打卡星星动画（StarAnimation.vue）
- [ ] 错误答案高亮 + 正确答案解析展示
- [ ] 输出：全部互动功能可用

### Phase 5 · 家长入口 + 部署
- [ ] 家长页面（ParentView.vue）
- [ ] 家长 API（查看报告/手动打卡）
- [ ] 长按 Logo 3秒进入家长模式（前端逻辑）
- [ ] 后端打包为可执行 Jar（含 H2 嵌入式数据库选项）
- [ ] 前端 `npm run build` 打包静态文件
- [ ] 编写 README.md（部署步骤 + API 文档）
- [ ] 输出：可部署版本

---

## 十、现有数据说明

已有文件 `KET_daily_plan.json`，结构如下（需按此格式导入数据库）：

```json
{
  "meta": { "title": "...", "total_days": 35, "start_date": "2026-06-13" },
  "days": {
    "2026-06-13": {
      "week": 1, "day": 1, "weekday": "周六",
      "theme": "入门启动 · 个人信息词汇",
      "duration": "35分钟",
      "vocab": {
        "group": "个人信息",
        "words": [
          { "en": "name", "zh": "名字", "eg": "My name is Lily." },
          ...
        ]
      },
      "grammar": {
        "point": "be动词（am/is/are）",
        "explanation": "...",
        "exercises": ["I ___ a student.", ...],
        "answers": ["am", ...]
      },
      "reading": {
        "passage": {
          "title": "All About Me",
          "text": "Hello! My name is Lily...",
          "translation": "你好！我叫莉莉..."
        },
        "questions": [
          { "q": "How old is Lily?", "options": ["A. Six", "B. Eight", "C. Nine"], "answer": "B" }
        ]
      },
      "listening": {
        "questions": [
          {
            "id": 1,
            "scenario": "What time does Tom get up?",
            "translation": "Tom几点起床？",
            "audio_text": "What time does Tom get up?",
            "options": ["A. 6:30", "B. 7:00", "C. 7:30"],
            "answer": "B",
            "key_word": "seven o'clock"
          },
          ...（共5题）
        ]
      },
      "speaking": {
        "template": "Hello! My name is ____. I am ____ years old...",
        "phrases": [
          { "en": "My name is ...", "zh": "我叫……" },
          ...
        ]
      },
      "writing": {
        "prompt": "...",
        "sample": "..."
      },
      "parent_note": "今天是第一天，核心就两个事..."
    },
    "2026-06-14": { ... },
    ...（共35天）
  }
}
```

**注意**：目前只有 Day 1（2026-06-13）的数据是完整的，
其余34天需要按同样标准补充（可 AI 生成后由家长审核）。

---

## 十一、交付要求

1. **前端**：Vue 3 项目，可 `npm run dev` 本地运行，可 `npm run build` 打包
2. **后端**：Spring Boot 项目，可 `mvn spring-boot:run` 本地运行，可 `mvn package` 打包为 Jar
3. **数据库**：提供 schema.sql + data-import 脚本（从 JSON 导入）
4. **文档**：README.md 包含：
   - 项目启动步骤（后端 + 前端）
   - API 接口列表
   - 数据库配置说明
   - 部署说明
5. **音频**：听力音频自动合成（edge-tts），无需手动准备

---

## 十二、无缝衔接提示词（直接复制给 IDE）

```
我需要开发一个 KET 儿童英语学习 Web App，以下是完整需求，请按 Phase 1 开始实现。

【技术栈】前端 Vue3（Vite）+ Pinia + Vue Router + **Element Plus + Tailwind CSS**；后端 **JDK 21 + Spring Boot 3.5.x**（最新版）+ MySQL；音频用 edge-tts 合成。

【用户】孩子8岁左右，手机+平板浏览器使用，无需安装。

【功能】4个Tab：首页（今日任务+开始学习按钮）、学习页（顺序流程：词汇翻卡→阅读→听力→口语→语法，顶部进度条）、成就页（星星+勋章墙+统计）、我的页（头像+菜单+家长入口）。

【关键交互】
- 阅读/听力翻译默认隐藏，点击展开
- 词汇卡片点击翻转（英文↔中文）
- 做题选择题点击选项，提交后判分（正确变绿/错误变红）
- 打卡成功星星动画
- 听力音频可播放

【现有数据】KET_daily_plan.json（35天学习计划），Day 1 数据完整，其余待补充。数据格式见项目目录下的需求文档。

【开发顺序】请按以下 Phase 顺序开发，每完成一个 Phase 告诉我，我确认后再继续：
Phase 1：Vue 静态原型（Mock 数据，4个Tab 可点击切换，学习页顺序流程可演示）
Phase 2：Spring Boot 后端（Entity/Mapper/Controller，JWT 鉴权，JSON 数据导入）
Phase 3：前端接入后端 API
Phase 4：互动功能（翻卡/音频/录音/动画）
Phase 5：家长入口 + 部署文档

请先开始 Phase 1，搭建 Vue 3 + Vite 项目骨架，实现4个Tab页面和可演示的学习顺序流程。
```

---

*文档版本：v1.0 | 更新时间：2026-06-13 | 作者：WorkBuddy AI*
