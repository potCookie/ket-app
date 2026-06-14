# KET App — Long-term Memory

## Project Overview
KET 儿童英语学习 Web App — 孩子每天在手机/平板上完成学习任务，家长查看进度。
- 前端: Vue 3 + Vite + Pinia + Vue Router + Element Plus + Tailwind CSS
- 后端: Spring Boot 3.5.2 + JDK 21 + MyBatis-Plus + JJWT
- 数据库: MySQL (ketapp)

## Current Status (2026-06-13)
- Phase 1 ✅ — 静态原型 (4 Tab + 学习页 + 全部组件)
- Phase 2 ✅ — Spring Boot 后端 (6表 + 22 API + JWT鉴权)
- Phase 3 ✅ — 前端接入后端 API (axios + 3 Store + 7视图全部重写)
- Phase 4 ✅ — 互动功能完善 (StarAnimation修复 + Web Speech API + 全部模块接入学习API)
- Phase 5 ✅ — 家长入口 + Onboarding引导 + 学习计划 + README部署文档

## Key Files
| 文件 | 用途 |
|------|------|
| `frontend/src/stores/task.js` | 核心 Store — 学习流程(currentStep/completedSteps) + API调用 |
| `frontend/src/stores/auth.js` | 认证 Store — Token持久化 localStorage |
| `frontend/src/api/index.js` | axios 拦截器 — 自动附加 Token + 解包 Result.data |
| `frontend/src/views/LearnView.vue` | 学习页调度中枢 — start→(quiz)→finish→checkin→StarAnimation |
| `frontend/src/components/StarAnimation.vue` | 星星爆发动画 — 8方位 burst + stars prop |
| `frontend/src/components/ListeningModule.vue` | 听力模块 — Web Speech API (en-GB) |

## API Design Pattern
- 前端 axios 拦截器自动解包 `{code, message, data}` → 返回 `data`
- 401 自动跳转 `/login` 并清除 Token
- 所有 API 请求在 `api/*.js` 中封装

## Known Pitfalls
- **Transition + Fragment Bug**: `<Transition mode="out-in">` in App.vue breaks when wrapped component has multiple root nodes (Vue fragment). Symptom: all pages blank after route switch. Fix: use single root element in all view components, and prefer `mode`-less `<Transition>` for robustness.

## Start Commands
```bash
# Backend (port 8080)
java -jar backend/target/ket-app-backend-1.0.0.jar

# Frontend (port 3004)  
cd frontend && npx vite --host 0.0.0.0 --port 3004
```

## Test Users
| username | password | role | nickname |
|----------|----------|------|----------|
| child1 | 1234 | child | xiaoming |
| child2 | 1234 | child | 小花 |

## New in Phase 5 (2026-06-13)
- **ParentView.vue**: 完整家长模式 — 孩子列表选择、统计报告、学习记录、手动打卡
- **PlanConfigView.vue**: 学习计划 CRUD — 起止日期、自动计算总天数、每日时长
- **OnboardingView.vue**: 3步引导向导 — 孩子信息→计划时间→确认（前5天预览）
- **PlanConfig API**: GET/POST `/api/plan` — 计划配置持久化
- **Parent API**: 4个端点(children/report/records/checkin)
- **README.md**: 完整部署文档 + API 接口文档 + Nginx 配置

## Key Routes (10 total)
| 路径 | 视图 | 说明 |
|------|------|------|
| `/login` | LoginView | 登录/注册 |
| `/home` | HomeView | 首页(自动检查 plan→/onboarding) |
| `/learn` | LearnView | 学习页(5模块顺序流程) |
| `/achieve` | AchieveView | 成就页 |
| `/profile` | ProfileView | 我的(长按3秒→/parent) |
| `/parent` | ParentView | 家长入口 |
| `/plan-config` | PlanConfigView | 学习计划配置 |
| `/onboarding` | OnboardingView | 首次使用引导 |
