# KET 儿童英语学习 App

> 剑桥 KET 考试备考 Web 应用 — 孩子每天在手机/平板上完成学习任务，家长查看进度

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **前端** | Vue 3 + Vite + Pinia + Vue Router + Element Plus + Tailwind CSS | — |
| **后端** | Spring Boot + MyBatis-Plus + JJWT | 3.5.2 / JDK 21 |
| **数据库** | MySQL | 8.0+ |
| **构建** | Maven | 3.9+ |
| **运行** | Java 21 + Node.js 18+ | — |

## 快速开始

### 1. 数据库准备

```sql
CREATE DATABASE IF NOT EXISTS ketapp
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
```

修改 `backend/src/main/resources/application.yml` 中的数据库连接信息。

> 首次启动时，系统自动执行 `schema.sql` 建表。

### 2. 启动后端

```bash
cd backend
mvn clean package -DskipTests
java -jar target/ket-app-backend-1.0.0.jar
```

后端默认运行在 **http://localhost:8080**

### 3. 启动前端

```bash
cd frontend
npm install
npx vite --host 0.0.0.0 --port 3004
```

前端默认运行在 **http://localhost:3004**

### 4. 访问

- 孩子端：`http://localhost:3004` → 登录 → 首页
- 家长端：登录后 → 我的 → 家长入口（或长按头像 3 秒）

## 测试账号

| 用户名 | 密码 | 角色 | 昵称 |
|--------|------|------|------|
| child1 | 1234 | 孩子 | xiaoming |
| child2 | 1234 | 孩子 | 小花 |

## 项目结构

```
ket_app/
├── backend/                         # Spring Boot 后端
│   ├── src/main/java/com/ketapp/
│   │   ├── controller/              # REST API 控制器 (6个)
│   │   ├── service/                 # 业务逻辑 (6个)
│   │   ├── entity/                  # 实体类 (5个)
│   │   ├── dto/                     # 数据传输对象
│   │   ├── mapper/                  # MyBatis-Plus Mapper
│   │   ├── config/                  # Spring Security / JWT / 数据初始化
│   │   └── common/                  # Result / GlobalException / JwtUtil
│   ├── src/main/resources/
│   │   ├── application.yml          # 数据库 + 日志配置
│   │   └── schema.sql               # 建表脚本 (6张表)
│   └── pom.xml
│
├── frontend/                        # Vue 3 前端
│   ├── src/
│   │   ├── views/                   # 页面组件 (9个)
│   │   │   ├── LoginView.vue        # 登录/注册
│   │   │   ├── HomeView.vue         # 首页 (今日任务)
│   │   │   ├── LearnView.vue        # 学习页 (顺序流程)
│   │   │   ├── AchieveView.vue      # 成就页 (星星+勋章)
│   │   │   ├── ProfileView.vue      # 我的页 (头像+菜单)
│   │   │   ├── ParentView.vue       # 家长入口 (孩子管理)
│   │   │   ├── PlanConfigView.vue   # 学习计划配置
│   │   │   └── OnboardingView.vue   # 首次使用引导
│   │   ├── components/              # 可复用组件 (7个)
│   │   ├── router/index.js          # 路由 + 守卫
│   │   ├── stores/                  # Pinia 状态管理 (2个)
│   │   ├── api/                     # API 封装 (5个)
│   │   └── App.vue                  # 根组件 (4 Tab 导航)
│   ├── package.json
│   └── vite.config.js
│
├── PRD_TECH_SPEC.md                 # 完整需求文档
└── README.md                        # 本文件
```

## API 接口 (22个)

### 认证 (3个)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 → 返回 JWT |
| GET | `/api/auth/me` | 获取当前用户信息 |

### 任务 (3个)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/task/today` | 获取今日任务概览 |
| GET | `/api/task/date?date=...` | 获取指定日期任务 |
| GET | `/api/task/:id` | 获取任务详情（含全部模块数据）|

### 学习 (4个)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/study/start` | 开始学习某模块 |
| POST | `/api/study/finish` | 完成某模块 |
| POST | `/api/quiz/submit` | 提交做题答案 |
| POST | `/api/recording/upload` | 上传口语录音 |

### 打卡 & 成就 (3个)
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/checkin` | 打卡 |
| GET | `/api/stats` | 学习统计 |
| GET | `/api/badges` | 勋章列表 |

### 家长 (4个)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/parent/children` | 查看关联孩子列表 |
| GET | `/api/parent/report?childId=...` | 查看孩子学习报告 |
| GET | `/api/parent/records?childId=...` | 查看孩子学习记录 |
| POST | `/api/parent/checkin` | 手动标记打卡 |

### 计划配置 (2个)
| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/plan` | 获取学习计划 |
| POST | `/api/plan` | 保存/更新学习计划 |

## 数据库表 (6张)

| 表 | 说明 |
|------|------|
| `user` | 用户表 (child/parent) |
| `plan_config` | 学习计划配置 |
| `task` | 每日任务内容 (JSON字段) |
| `study_log` | 学习记录 |
| `recording` | 录音文件 |
| `badge` | 勋章 |

## 功能特性

### 孩子端 (4 Tab)
- 🏠 **首页** — 今日任务卡片、学习模块网格、连续打卡
- 📖 **学习** — 顺序流程：词汇翻卡 → 阅读 → 听力 → 口语 → 语法
- 🏆 **成就** — 星星数、勋章墙（打卡7天/30天/全勤/满分）
- 👤 **我的** — 头像、菜单、家长入口（长按3秒）

### 家长端
- 查看关联孩子列表
- 查看学习记录和统计报告
- 手动标记打卡
- 修改学习计划（起止日期、每日时长）

### 首次使用
- 3步引导向导：孩子信息 → 计划时间 → 确认
- 自动计算总天数，前5天主题预览

### 关键交互
- 词汇卡片点击翻转（3D 动画）
- 阅读/听力翻译点击展开
- 选择题自动判分（正确绿/错误红）
- Web Speech API 英式发音
- 打卡成功星星爆发动画
- 响应式布局（手机 375px ~ 平板 768px）

## 部署

### 后端打包
```bash
cd backend
mvn clean package -DskipTests
# 输出: target/ket-app-backend-1.0.0.jar
java -jar target/ket-app-backend-1.0.0.jar
```

### 前端打包
```bash
cd frontend
npm run build
# 输出: dist/
```

将 `dist/` 目录部署到任意 Web 服务器（Nginx/Apache/CDN），配置反向代理 `/api` → 后端 8080 端口。

### Nginx 示例
```nginx
server {
    listen 80;
    server_name ket.example.com;

    root /var/www/ket_app/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```
