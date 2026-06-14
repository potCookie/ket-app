<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTaskStore } from '../stores/task.js'
import VocabCard from '../components/VocabCard.vue'
import ReadingModule from '../components/ReadingModule.vue'
import ListeningModule from '../components/ListeningModule.vue'
import SpeakingModule from '../components/SpeakingModule.vue'
import GrammarModule from '../components/GrammarModule.vue'
import ProgressBar from '../components/ProgressBar.vue'
import StarAnimation from '../components/StarAnimation.vue'

const router = useRouter()
const route = useRoute()
const taskStore = useTaskStore()

// --- State ---
const allDone = ref(false)
const showStar = ref(false)
const completing = ref(false)
const stepCompleting = ref(false)
const isReview = computed(() => taskStore.reviewMode)

// Stars earned = number of completed modules today
const earnedStars = computed(() => taskStore.completedSteps.length)

// If coming from Home, default to vocab
if (!taskStore.currentStep) {
  taskStore.setCurrentStep('vocab')
}

// When entering a new step: call study/start API (skip in review mode)
watch(() => taskStore.currentStep, (step) => {
  if (step && taskStore.todayData && !isReview.value) {
    taskStore.startModuleApi(step).catch(() => {})
  }
})

// On mount: fetch task data + start first module
const historicalDate = ref('')

onMounted(async () => {
  const dateParam = route.query.date
  if (dateParam) {
    // Loading a historical date from the records page
    historicalDate.value = dateParam
    await taskStore.fetchTaskByDate(dateParam)
  } else if (!taskStore.todayData) {
    await taskStore.fetchTodayTask()
  }
  // In review mode, load previous answers
  if (isReview.value) {
    await taskStore.fetchStudyLogs()
  }
  if (taskStore.currentStep && !isReview.value) {
    taskStore.startModuleApi(taskStore.currentStep).catch(() => {})
  }
})

// --- Module done handler ---
async function onStepDone({ score, total, answers } = {}) {
  if (stepCompleting.value) return
  stepCompleting.value = true

  const step = taskStore.currentStep
  if (!step || !taskStore.todayData) {
    stepCompleting.value = false
    return
  }

  try {
    // Submit quiz score if applicable (before finish)
    if (score !== null && total !== null && !isReview.value) {
      await taskStore.submitQuizApi(step, score, total, answers ? JSON.stringify(answers) : null)
    }
    // Mark module finished (calls completeStep internally), skip API in review
    if (!isReview.value) {
      await taskStore.finishModuleApi(step, score, total, answers ? JSON.stringify(answers) : null)
    } else {
      taskStore.completeStep(step)
    }
  } catch (e) {
    // Still allow progression on API error
    taskStore.completeStep(step)
  }

  stepCompleting.value = false

  // Check if all done
  if (taskStore.completedSteps.length >= taskStore.stepOrder.length) {
    await finalizeAll()
  }
}

// --- Next step handler ---
async function handleNextStep() {
  taskStore.goToNextStep()
  if (!taskStore.currentStep) {
    // All steps done via button click
    await finalizeAll()
  }
}

function handlePrevStep() {
  taskStore.goToPrevStep()
}

// --- Finalize: checkin + celebration ---
async function finalizeAll() {
  if (completing.value) return
  completing.value = true

  if (!isReview.value) {
    try {
      await taskStore.doCheckIn()
    } catch (e) {
      // Checkin failure is non-critical
    }
  }

  allDone.value = true
  showStar.value = true
  completing.value = false
}

function backHome() {
  router.push(historicalDate.value ? '/history' : '/home')
}

// --- Computed ---
const currentModuleLabel = computed(() =>
  taskStore.stepLabels[taskStore.currentStep] || ''
)
const currentModuleIcon = computed(() =>
  taskStore.stepIcons[taskStore.currentStep] || ''
)
const currentStepIndex = computed(() =>
  taskStore.stepOrder.indexOf(taskStore.currentStep)
)
</script>

<template>
  <div class="learn-root">
    <div class="page-container no-select" v-if="!allDone">
      <!-- 顶部导航 -->
      <div class="learn-app-bar">
        <span class="back-btn" @click="router.push(historicalDate ? '/history' : '/home')">←</span>
        <span>{{ historicalDate ? '历史回顾' : '今日学习' }}</span>
      </div>

      <div class="animate-fade-in">
        <!-- 进度条 (skip for history review) -->
        <ProgressBar v-if="!historicalDate" />

        <!-- 模块标题 -->
        <div class="learn-mod-title">
          {{ currentModuleIcon }} {{ currentModuleLabel }}{{ isReview ? '回顾' : '学习' }}
        </div>

        <!-- 回顾模式提示 -->
        <div v-if="historicalDate" class="review-banner">
          📖 正在查看 {{ historicalDate }} 的学习内容。不会重复记录。
        </div>
        <div v-else-if="isReview" class="review-banner">
          📖 今日学习已完成，你可以自由回顾学习内容。不会重复记录。
        </div>

        <!-- 词汇模块 -->
        <VocabCard
          v-if="taskStore.currentStep === 'vocab'"
          @done="onStepDone"
        />

        <!-- 阅读模块 -->
        <ReadingModule
          v-else-if="taskStore.currentStep === 'reading'"
          :review-mode="isReview"
          :prev-answers="isReview ? taskStore.getModuleAnswers('reading') : null"
          @done="onStepDone"
        />

        <!-- 听力模块 -->
        <ListeningModule
          v-else-if="taskStore.currentStep === 'listening'"
          :review-mode="isReview"
          :prev-answers="isReview ? taskStore.getModuleAnswers('listening') : null"
          @done="onStepDone"
        />

        <!-- 口语模块 -->
        <SpeakingModule
          v-else-if="taskStore.currentStep === 'speaking'"
          :review-mode="isReview"
          @done="onStepDone"
        />

        <!-- 语法模块 -->
        <GrammarModule
          v-else-if="taskStore.currentStep === 'grammar'"
          :review-mode="isReview"
          :prev-answers="isReview ? taskStore.getModuleAnswers('grammar') : null"
          @done="onStepDone"
        />

        <!-- Navigation buttons -->
        <div v-if="taskStore.currentStep" class="nav-buttons">
          <!-- Review mode: prev + next -->
          <template v-if="isReview">
            <button
              class="nav-btn secondary"
              :disabled="currentStepIndex <= 0"
              @click="handlePrevStep"
            >
              ← 上一个模块
            </button>
            <button
              class="nav-btn"
              :disabled="currentStepIndex >= taskStore.stepOrder.length - 1"
              @click="handleNextStep"
            >
              下一个模块 →
            </button>
          </template>
          <!-- Normal mode: only next -->
          <button
            v-else
            class="nav-btn next-btn"
            :disabled="stepCompleting"
            @click="handleNextStep"
          >
            {{ stepCompleting ? '提交中...' : `✔ 完成${currentModuleLabel}，继续学习 →` }}
          </button>
        </div>
      </div>
    </div>

    <!-- 全部完成庆祝页 -->
    <div v-else class="complete-page">
      <div class="complete-content">
        <div class="star-burst">{{ isReview ? '📖' : '🎉' }}</div>
        <div class="complete-title">{{ isReview ? '回顾完成' : '太棒了！' }}</div>
        <div class="complete-sub">{{ isReview ? '今日学习内容已全部回顾' : '今日学习全部完成' }}</div>
        <div v-if="!isReview" class="star-earned animate-star-pop">
          ⭐ +{{ earnedStars }}
        </div>
        <div v-if="!isReview" class="star-row">🌟 ⭐ 🎉 ⭐ 🌟</div>
        <p class="complete-msg">
          你又进步了一点点！<br/>明天继续加油哦~
        </p>
        <button class="next-btn mt-4" @click="backHome">
          🏠 返回首页
        </button>
      </div>
    </div>

    <!-- 打卡星星动画 -->
    <StarAnimation
      :visible="showStar"
      :stars="earnedStars"
      @done="showStar = false"
    />
  </div>
</template>

<style scoped>
.learn-app-bar {
  background: linear-gradient(135deg, #4ECDC4, #2bbbad);
  color: white;
  padding: 12px 20px;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  margin: -16px -16px 12px;
}

.back-btn {
  font-size: 22px;
  cursor: pointer;
  opacity: 0.9;
  transition: 0.2s;
}

.back-btn:hover {
  opacity: 1;
  transform: translateX(-2px);
}

.learn-mod-title {
  font-size: 17px;
  font-weight: 700;
  color: #1a237e;
  margin: 16px 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.review-banner {
  background: linear-gradient(135deg, #e3f2fd, #bbdefb);
  border: 1px solid #90caf9;
  border-radius: 12px;
  padding: 10px 16px;
  font-size: 12px;
  color: #1565c0;
  margin-bottom: 12px;
  text-align: center;
}

.nav-btn {
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  border: none;
  border-radius: 14px;
  padding: 14px 24px;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(78, 205, 196, 0.35);
}

.nav-btn.secondary {
  background: linear-gradient(135deg, #e0e0e0, #bdbdbd);
  color: #546e7a;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.nav-btn.secondary:hover:not(:disabled) {
  background: linear-gradient(135deg, #cfd8dc, #b0bec5);
}

.nav-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(78, 205, 196, 0.45);
}

.nav-btn:disabled {
  opacity: 0.6;
  cursor: default;
  transform: none;
}

.nav-btn.next-btn {
  width: 100%;
  margin-top: 16px;
}

.nav-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.nav-buttons .nav-btn {
  flex: 1;
}

/* 完成页 */
.complete-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(180deg, #e8f5e9 0%, #f1f8e9 50%, #fff 100%);
  padding: 20px;
}

.complete-content {
  text-align: center;
  animation: fade-in 0.6s ease;
}

@keyframes fade-in {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

.star-burst {
  font-size: 72px;
  margin-bottom: 16px;
  animation: bounce 1s ease infinite;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-16px); }
}

.complete-title {
  font-size: 28px;
  font-weight: 800;
  color: #1a237e;
  margin-bottom: 8px;
}

.complete-sub {
  font-size: 15px;
  color: #78909c;
  margin-bottom: 20px;
}

.star-earned {
  font-size: 36px;
  font-weight: 900;
  color: #FF6B6B;
  margin-bottom: 12px;
}

.star-row {
  font-size: 24px;
  margin-bottom: 16px;
}

.complete-msg {
  font-size: 14px;
  color: #546e7a;
  line-height: 1.8;
}

.mt-4 { margin-top: 16px; }
</style>
