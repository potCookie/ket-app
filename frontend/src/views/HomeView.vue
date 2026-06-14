<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTaskStore } from '../stores/task.js'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const taskStore = useTaskStore()
const authStore = useAuthStore()

const modules = computed(() => {
  const d = taskStore.todayData
  if (!d) return []
  return [
    { key: 'vocab', icon: '📚', label: '词汇', sub: `${d.vocab?.words?.length || 0}词` },
    { key: 'reading', icon: '📖', label: '阅读', sub: '1篇' },
    { key: 'listening', icon: '🎧', label: '听力', sub: `${d.listening?.questions?.length || 0}题` },
    { key: 'speaking', icon: '🗣️', label: '口语', sub: '录音' },
  ]
})

function startLearning() {
  taskStore.resetLearn()
  taskStore.setCurrentStep('vocab')
  router.push('/learn')
}

function reviewToday() {
  taskStore.resetLearn()
  taskStore.reviewMode = true
  taskStore.setCurrentStep('vocab')
  router.push('/learn')
}

onMounted(async () => {
  await authStore.restoreSession()
  if (!authStore.isLoggedIn) {
    router.push('/login')
    return
  }

  // Validate token by fetching user info first
  if (authStore.token) {
    try {
      await authStore.fetchMe()
    } catch {
      // fetchMe will trigger redirect via interceptor
      return
    }
  }

  // Check if plan is configured (for child users)
  if (authStore.isChild) {
    try {
      const { getPlanConfig } = await import('../api/plan.js')
      const plan = await getPlanConfig()
      if (!plan || !plan.startDate) {
        router.push('/onboarding')
        return
      }
    } catch {
      router.push('/onboarding')
      return
    }
  }

  // Fetch data (user is now confirmed valid)
  try {
    await taskStore.fetchTodayTask()
  } catch {
    // error handled by interceptor
    return
  }
  try {
    await taskStore.fetchStats()
  } catch {
    // non-critical
  }
})
</script>

<template>
  <div class="page-container no-select">
    <!-- 顶部导航 -->
    <div class="app-bar sticky top-0 z-10">
      <span class="text-xl">🎓</span>
      <span class="font-bold">KET 学习</span>
    </div>

    <!-- Loading -->
    <div v-if="taskStore.taskLoading" class="loading-box">
      <div class="spinner"></div>
      <p>加载今日任务...</p>
    </div>

    <!-- Error -->
    <div v-else-if="taskStore.taskError" class="error-box">
      <p>😵 加载失败</p>
      <p class="error-msg">{{ taskStore.taskError }}</p>
      <button class="retry-btn" @click="taskStore.fetchTodayTask()">重试</button>
    </div>

    <!-- No data -->
    <div v-else-if="!taskStore.todayData" class="empty-box">
      <p>📭 暂无今日任务</p>
      <p class="empty-hint">请稍后再来看看</p>
    </div>

    <!-- Normal content -->
    <template v-else>
      <div class="animate-fade-in">
        <!-- 日期信息 -->
        <div class="flex items-center gap-2 mb-3 mt-1">
          <div class="date-chip">
            📅 {{ taskStore.todayData.date }} {{ taskStore.todayData.weekday }}
          </div>
        </div>
        <div class="text-[22px] font-extrabold text-[#1a237e] tracking-[-0.5px]">
          第{{ taskStore.todayData.day }}天
        </div>
        <div class="text-[13px] text-[#78909c] mb-4">
          第{{ taskStore.todayData.week }}周 · {{ taskStore.todayData.theme }}
        </div>

        <!-- 今日任务卡片 -->
        <div class="task-card" :class="{ done: taskStore.todayAllDone }">
          <h3 class="text-[15px] font-semibold opacity-90 mb-2 relative z-10">
            {{ taskStore.todayAllDone ? '🎉 今日学习已完成' : '🌟 今日学习任务' }}
          </h3>
          <div class="flex gap-2 flex-wrap mb-3 relative z-10">
            <span v-for="m in modules" :key="m.key" class="mod-tag">
              {{ m.icon }} {{ m.label }}
            </span>
          </div>
          <div class="text-[13px] opacity-85 mb-1 relative z-10">
            ⏱ 建议学习时长：{{ taskStore.todayData.duration }}
          </div>
          <button
            class="start-btn relative z-10"
            :class="{ review: taskStore.todayAllDone }"
            @click="taskStore.todayAllDone ? reviewToday() : startLearning()"
          >
            {{ taskStore.todayAllDone ? '📖 回顾今日学习' : '🚀 开始学习！' }}
          </button>
        </div>

        <!-- 学习模块快捷入口 -->
        <div class="section-title">📦 学习模块</div>
        <div class="module-grid">
          <div
            v-for="m in modules"
            :key="m.key"
            class="mod-card"
            :class="{ done: taskStore.isStepDone(m.key) }"
            @click="taskStore.todayAllDone ? reviewToday() : startLearning()"
          >
            <span class="mod-icon">{{ m.icon }}</span>
            <span class="mod-label">{{ m.label }}</span>
            <span class="mod-sub">{{ m.sub }}</span>
          </div>
        </div>

        <!-- 连续打卡 -->
        <div v-if="authStore.user" class="streak-bar">
          <div class="streak-num">🔥 {{ authStore.user.streak || 0 }}</div>
          <div>
            <div class="streak-label">连续学习天数</div>
            <div class="streak-sub">继续加油，坚持就是胜利！</div>
          </div>
        </div>

        <!-- 家长提示 -->
        <div v-if="taskStore.todayData.parent_note" class="parent-note mt-4">
          💡 {{ taskStore.todayData.parent_note }}
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.app-bar {
  background: linear-gradient(135deg, #4ECDC4, #2bbbad);
  color: white;
  padding: 12px 20px;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  margin: -16px -16px 0;
}

.loading-box {
  text-align: center;
  padding: 60px 0;
  color: #78909c;
  font-size: 14px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e0e0e0;
  border-top: 4px solid #4ECDC4;
  border-radius: 50%;
  margin: 0 auto 16px;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-box {
  text-align: center;
  padding: 60px 0;
}

.error-msg {
  font-size: 12px;
  color: #b0bec5;
  margin: 8px 0;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 24px;
  border: none;
  border-radius: 10px;
  background: #4ECDC4;
  color: white;
  font-weight: 600;
  cursor: pointer;
}

.empty-box {
  text-align: center;
  padding: 60px 0;
  color: #78909c;
  font-size: 15px;
}

.empty-hint {
  font-size: 12px;
  color: #b0bec5;
  margin-top: 4px;
}

.date-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
  color: #00695c;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 20px;
  box-shadow: 0 1px 4px rgba(0, 105, 92, 0.1);
}

.task-card {
  background: linear-gradient(135deg, #4ECDC4 0%, #26a69a 50%, #00897b 100%);
  border-radius: 20px;
  padding: 22px;
  color: white;
  margin-bottom: 18px;
  box-shadow: 0 8px 32px rgba(78, 205, 196, 0.35);
  position: relative;
  overflow: hidden;
}

.task-card::before,
.task-card::after {
  content: '';
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.task-card::before {
  top: -30px;
  right: -30px;
  width: 120px;
  height: 120px;
}

.task-card::after {
  bottom: -20px;
  left: -20px;
  width: 80px;
  height: 80px;
}

.mod-tag {
  background: rgba(255, 255, 255, 0.22);
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.start-btn {
  background: white;
  color: #00897b;
  border: none;
  border-radius: 14px;
  padding: 14px 0;
  width: 100%;
  font-size: 16px;
  font-weight: 700;
  margin-top: 16px;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  transition: all 0.2s;
}

.start-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.16);
}

.start-btn:active {
  transform: translateY(0);
}

.task-card.done {
  background: linear-gradient(135deg, #66bb6a, #43a047);
}

.start-btn.review {
  background: #fff3e0;
  color: #e65100;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.module-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 18px;
}

.mod-card {
  background: white;
  border-radius: 16px;
  padding: 16px 14px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 2px solid transparent;
  position: relative;
  overflow: hidden;
}

.mod-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: linear-gradient(90deg, #4ECDC4, #26a69a);
  opacity: 0;
  transition: 0.25s;
}

.mod-card:active {
  transform: scale(0.97);
}

.mod-card.done {
  border-color: #4ECDC4;
  background: linear-gradient(180deg, #e0f7fa 0%, white 40%);
}

.mod-card.done::before {
  opacity: 1;
}

.mod-icon {
  font-size: 32px;
  display: block;
  margin-bottom: 8px;
}

.mod-label {
  font-size: 13px;
  font-weight: 700;
  color: #263238;
}

.mod-sub {
  font-size: 11px;
  color: #b0bec5;
  margin-top: 3px;
}

.streak-bar {
  background: white;
  border-radius: 16px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.streak-num {
  font-size: 32px;
  font-weight: 800;
  color: #FF6B6B;
}

.streak-label {
  font-size: 12px;
  color: #78909c;
}

.streak-sub {
  font-size: 11px;
  color: #b0bec5;
  margin-top: 2px;
}

.parent-note {
  background: #e3f2fd;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 12px;
  color: #1565c0;
  line-height: 1.6;
  border: 1px solid #bbdefb;
}

/* 平板适配 */
@media (min-width: 520px) {
  .module-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
