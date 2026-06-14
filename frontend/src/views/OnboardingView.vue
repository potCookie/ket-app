<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'
import { savePlanConfig } from '../api/plan.js'

const router = useRouter()
const authStore = useAuthStore()

const step = ref(0)
const saving = ref(false)

// Step 1: Child info
const form = ref({
  nickname: authStore.user?.nickname || '',
  grade: '二升三',
  target: 'KET',
})

// Step 2: Plan time
const plan = ref({
  startDate: (() => {
    const d = new Date()
    d.setDate(d.getDate() + 1)
    return d.toISOString().slice(0, 10)
  })(),
  endDate: (() => {
    const d = new Date()
    d.setDate(d.getDate() + 36)
    return d.toISOString().slice(0, 10)
  })(),
  dailyDuration: 35,
})

const totalDays = computed(() => {
  if (plan.value.startDate && plan.value.endDate) {
    const start = new Date(plan.value.startDate)
    const end = new Date(plan.value.endDate)
    return Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
  }
  return 0
})

// Preview: first 5 days themes
const previewDays = computed(() => {
  const themes = [
    '个人信息词汇', '家庭与朋友', '学校生活',
    '日常活动', '食物与饮料', '动物与自然',
    '颜色与形状', '身体部位', '衣服与购物',
  ]
  return Array.from({ length: Math.min(5, totalDays.value) }, (_, i) => ({
    day: i + 1,
    theme: themes[i] || `主题 ${i + 1}`,
  }))
})

function nextStep() {
  if (step.value === 0 && !form.value.nickname.trim()) {
    ElMessage.warning('请输入孩子昵称')
    return
  }
  step.value++
}

function prevStep() {
  step.value--
}

async function handleSubmit() {
  saving.value = true
  try {
    await savePlanConfig({
      startDate: plan.value.startDate,
      endDate: plan.value.endDate,
      dailyDuration: plan.value.dailyDuration,
    })
    ElMessage.success('学习计划创建成功！🎉')
    router.push('/home')
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="onboarding-page">
    <!-- Header -->
    <div class="onboarding-header">
      <div class="step-indicator">
        <span
          v-for="i in 3" :key="i"
          class="step-dot"
          :class="{ active: step >= i - 1, done: step > i - 1 }"
        ></span>
      </div>
      <h1 class="onboarding-title">
        {{ step === 0 ? '孩子信息' : step === 1 ? '计划时间' : '确认计划' }}
      </h1>
      <p class="onboarding-subtitle">
        {{ step === 0 ? '让我们了解您孩子的基本情况' : step === 1 ? '设定学习计划时间' : '一切准备就绪' }}
      </p>
    </div>

    <!-- Step 1: Child Info -->
    <div v-if="step === 0" class="step-body animate-fade-in">
      <div class="form-group">
        <label class="form-label">👶 孩子昵称</label>
        <input
          v-model="form.nickname"
          type="text"
          placeholder="如：小明"
          maxlength="10"
          class="form-input"
        />
      </div>

      <div class="form-group">
        <label class="form-label">📚 年级</label>
        <div class="option-group">
          <button
            v-for="g in ['二升三', '三升四', '其他']" :key="g"
            class="option-btn"
            :class="{ active: form.grade === g }"
            @click="form.grade = g"
          >
            {{ g }}
          </button>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">🎯 备考目标</label>
        <div class="option-group">
          <button
            v-for="t in ['KET', 'PET']" :key="t"
            class="option-btn"
            :class="{ active: form.target === t }"
            :disabled="t === 'PET'"
            @click="form.target = t"
          >
            {{ t }}
            <span v-if="t === 'PET'" class="coming-soon">即将上线</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Step 2: Plan Time -->
    <div v-if="step === 1" class="step-body animate-fade-in">
      <div class="form-group">
        <label class="form-label">📅 计划开始日期</label>
        <input
          v-model="plan.startDate"
          type="date"
          class="form-input"
        />
      </div>

      <div class="form-group">
        <label class="form-label">📅 计划结束日期</label>
        <input
          v-model="plan.endDate"
          type="date"
          class="form-input"
        />
      </div>

      <div class="total-days-badge">
        共 <strong>{{ totalDays }}</strong> 天学习计划
      </div>

      <div class="form-group">
        <label class="form-label">⏱️ 每日学习时长目标</label>
        <div class="option-group">
          <button
            v-for="d in [25, 35, 45]" :key="d"
            class="option-btn"
            :class="{ active: plan.dailyDuration === d }"
            @click="plan.dailyDuration = d"
          >
            {{ d }}分钟
          </button>
        </div>
      </div>
    </div>

    <!-- Step 3: Confirm -->
    <div v-if="step === 2" class="step-body animate-fade-in">
      <div class="confirm-card">
        <div class="confirm-row">
          <span class="confirm-label">👶 孩子</span>
          <span class="confirm-value">{{ form.nickname }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">📚 年级</span>
          <span class="confirm-value">{{ form.grade }} · {{ form.target }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">📅 时间</span>
          <span class="confirm-value">{{ plan.startDate }} ~ {{ plan.endDate }}</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">📊 总计</span>
          <span class="confirm-value"><strong>{{ totalDays }}</strong> 天</span>
        </div>
        <div class="confirm-row">
          <span class="confirm-label">⏱️ 每日</span>
          <span class="confirm-value">{{ plan.dailyDuration }}分钟</span>
        </div>
      </div>

      <div class="preview-title">📋 前 {{ previewDays.length }} 天计划预览</div>
      <div class="preview-list">
        <div v-for="d in previewDays" :key="d.day" class="preview-item">
          <span class="preview-day">Day {{ d.day }}</span>
          <span class="preview-theme">{{ d.theme }}</span>
        </div>
      </div>
    </div>

    <!-- Bottom buttons -->
    <div class="onboarding-footer">
      <button v-if="step > 0" class="btn-secondary" @click="prevStep">
        ← 上一步
      </button>
      <button
        v-if="step < 2"
        class="btn-primary"
        :style="{ flex: step === 0 ? '1' : '' }"
        @click="nextStep"
      >
        下一步 →
      </button>
      <button
        v-if="step === 2"
        class="btn-confirm"
        :disabled="saving"
        @click="handleSubmit"
      >
        {{ saving ? '⏳ 创建中...' : '✅ 确认并开始' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.onboarding-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e0f7fa 0%, #F7F9FC 30%);
  display: flex;
  flex-direction: column;
  padding: 0 20px;
}

.onboarding-header {
  text-align: center;
  padding: 32px 0 24px;
}
.step-indicator {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}
.step-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #e0e0e0;
  transition: all 0.3s;
}
.step-dot.active {
  background: #4ECDC4;
  transform: scale(1.3);
}
.step-dot.done {
  background: #26a69a;
}
.onboarding-title {
  font-size: 24px;
  font-weight: 800;
  color: #1a237e;
  margin: 0 0 6px;
}
.onboarding-subtitle {
  font-size: 14px;
  color: #78909c;
  margin: 0;
}

.step-body {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 16px;
}

/* Form */
.form-group {
  margin-bottom: 20px;
}
.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #455a64;
  margin-bottom: 8px;
}
.form-input {
  width: 100%;
  padding: 14px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 14px;
  font-size: 16px;
  color: #37474f;
  background: white;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: #4ECDC4;
}
.option-group {
  display: flex;
  gap: 10px;
}
.option-btn {
  flex: 1;
  padding: 14px 0;
  border: 2px solid #e0e0e0;
  border-radius: 14px;
  background: white;
  font-size: 15px;
  font-weight: 600;
  color: #78909c;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.option-btn.active {
  border-color: #4ECDC4;
  color: #4ECDC4;
  background: #e0f7fa;
}
.option-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.coming-soon {
  display: block;
  font-size: 10px;
  color: #b0bec5;
  font-weight: 400;
}

.total-days-badge {
  text-align: center;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  padding: 12px;
  border-radius: 12px;
  font-size: 16px;
  margin-bottom: 20px;
}

/* Confirm */
.confirm-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 16px;
}
.confirm-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}
.confirm-row:last-child {
  border-bottom: none;
}
.confirm-label {
  font-size: 14px;
  color: #78909c;
}
.confirm-value {
  font-size: 14px;
  color: #1a237e;
  font-weight: 600;
}
.preview-title {
  font-size: 14px;
  font-weight: 700;
  color: #455a64;
  margin-bottom: 10px;
}
.preview-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.preview-item {
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
}
.preview-day {
  background: #e0f7fa;
  color: #00897b;
  font-size: 12px;
  font-weight: 700;
  padding: 4px 10px;
  border-radius: 8px;
}
.preview-theme {
  font-size: 14px;
  color: #455a64;
}

/* Footer */
.onboarding-footer {
  display: flex;
  gap: 12px;
  padding: 16px 0 32px;
}
.btn-secondary {
  padding: 16px 24px;
  border: 2px solid #e0e0e0;
  border-radius: 14px;
  background: white;
  font-size: 15px;
  font-weight: 600;
  color: #78909c;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-primary {
  flex: 1;
  padding: 16px 0;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(78, 205, 196, 0.35);
  transition: all 0.2s;
}
.btn-confirm {
  flex: 1;
  padding: 16px 0;
  border: none;
  border-radius: 14px;
  background: linear-gradient(135deg, #FF6B6B, #ee5a5a);
  color: white;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(255, 107, 107, 0.35);
  transition: all 0.2s;
}
.btn-confirm:hover {
  transform: translateY(-1px);
}
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.35s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
