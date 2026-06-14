<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'
import { getChildren, getChildReport, getChildRecords, manualCheckIn } from '../api/parent.js'

const router = useRouter()
const authStore = useAuthStore()

// --- State ---
const children = ref([])
const selectedChildId = ref(null)
const report = ref(null)
const records = ref([])
const loading = ref(false)
const recordsLoading = ref(false)
const checkingIn = ref(null)  // date being toggled

// --- Computed ---
const selectedChild = computed(() =>
  children.value.find(c => c.id === selectedChildId.value))

const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
}

function getWeekday(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return weekdays[d.getDay()]
}

function moduleLabel(mod) {
  const map = { vocab: '词汇', reading: '阅读', listening: '听力', speaking: '口语', grammar: '语法' }
  return map[mod] || mod
}

// --- API actions ---
async function loadChildren() {
  loading.value = true
  try {
    children.value = await getChildren()
    if (children.value.length > 0 && !selectedChildId.value) {
      selectedChildId.value = children.value[0].id
      await loadChildData(selectedChildId.value)
    }
  } catch (e) {
    ElMessage.error('加载孩子列表失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function loadChildData(childId) {
  try {
    const [rep, recs] = await Promise.all([
      getChildReport(childId),
      getChildRecords(childId)
    ])
    report.value = rep
    records.value = recs
  } catch (e) {
    ElMessage.error('加载孩子数据失败: ' + e.message)
  }
}

async function selectChild(childId) {
  selectedChildId.value = childId
  recordsLoading.value = true
  await loadChildData(childId)
  recordsLoading.value = false
}

async function toggleCheckIn(record) {
  if (checkingIn.value === record.taskDate) return
  checkingIn.value = record.taskDate
  try {
    await manualCheckIn(selectedChildId.value, record.taskDate)
    ElMessage.success('打卡状态已更新')
    await loadChildData(selectedChildId.value)
  } catch (e) {
    ElMessage.error('操作失败: ' + e.message)
  } finally {
    checkingIn.value = null
  }
}

function goToPlan() {
  router.push('/plan-config')
}

function backToProfile() {
  router.push('/profile')
}

onMounted(() => {
  loadChildren()
})
</script>

<template>
  <div class="page-container">
    <!-- Top bar -->
    <div class="app-bar">
      <span class="back-btn" @click="backToProfile">←</span>
      <span>家长入口</span>
      <span class="ml-auto text-xs opacity-70">🔒 家长模式</span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="empty-box">
      <p>🔄 加载中...</p>
    </div>

    <!-- No children -->
    <div v-else-if="children.length === 0" class="empty-box">
      <p>👨‍👩‍👧 暂未关联孩子</p>
      <p class="empty-hint">请让孩子注册时输入您的邀请码</p>
    </div>

    <!-- Parent dashboard -->
    <div v-else class="animate-fade-in">
      <!-- Child selector -->
      <div class="child-tabs" v-if="children.length > 1">
        <button
          v-for="child in children" :key="child.id"
          class="child-tab"
          :class="{ active: child.id === selectedChildId }"
          @click="selectChild(child.id)"
        >
          {{ child.nickname || child.username }}
        </button>
      </div>

      <!-- Child info card -->
      <div class="child-info" v-if="selectedChild">
        <div class="child-avatar">
          {{ (selectedChild.nickname || selectedChild.username || '?').charAt(0).toUpperCase() }}
        </div>
        <div>
          <div class="child-name">{{ selectedChild.nickname || selectedChild.username }}</div>
          <div class="child-meta">
            {{ selectedChild.grade || '未设置' }} · {{ selectedChild.target || 'KET' }} 备考
          </div>
        </div>
        <div class="child-stars">
          ⭐ {{ selectedChild.stars || 0 }}
        </div>
      </div>

      <!-- Stats overview -->
      <div class="stats-overview" v-if="report">
        <div class="stat-item">
          <div class="stat-value">{{ report.completedDays || 0 }}/<small>{{ report.totalDays || '?' }}</small></div>
          <div class="stat-label">完成天数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ report.accuracy || 0 }}%</div>
          <div class="stat-label">正确率</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ report.longestStreak || 0 }}天</div>
          <div class="stat-label">最长连续</div>
        </div>
      </div>

      <!-- Learning records -->
      <div class="section-title">📋 学习记录</div>
      <div v-if="recordsLoading" class="empty-box">
        <p>🔄 加载记录中...</p>
      </div>
      <div v-else-if="records.length === 0" class="empty-box">
        <p>📭 暂无学习记录</p>
      </div>
      <div v-else class="log-list">
        <div
          v-for="log in records" :key="log.id"
          class="log-item"
          :class="{ missed: !log.checkedIn }"
        >
          <div class="log-left">
            <div class="log-date">{{ formatDate(log.taskDate) }}</div>
            <div class="log-weekday">{{ getWeekday(log.taskDate) }}</div>
          </div>
          <div class="log-center">
            <div class="log-status" :class="{ completed: log.checkedIn, missed: !log.checkedIn }">
              {{ log.checkedIn ? '✅ 已完成' : '❌ 未完成' }}
            </div>
            <div class="log-detail">
              {{ moduleLabel(log.module) }}
              <span v-if="log.quizTotal"> · {{ log.quizScore }}/{{ log.quizTotal }} 题</span>
              <span v-if="log.starsEarned"> · ⭐{{ log.starsEarned }}</span>
            </div>
          </div>
          <button
            class="log-toggle"
            :disabled="checkingIn === log.taskDate"
            @click="toggleCheckIn(log)"
          >
            {{ checkingIn === log.taskDate ? '⏳' : (log.checkedIn ? '取消' : '标记') }}
          </button>
        </div>
      </div>

      <!-- Action buttons -->
      <div class="action-btns">
        <button class="action-btn primary" @click="goToPlan">
          🗓️ 修改学习计划
        </button>
        <button class="action-btn" @click="ElMessage.info('手动生成内容功能即将上线')">
          🔄 手动生成内容
        </button>
        <button class="action-btn" @click="ElMessage.info('详细报告功能即将上线')">
          📊 查看详细报告
        </button>
      </div>
    </div>
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
  gap: 12px;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  margin: -16px -16px 0;
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

/* Empty state */
.empty-box {
  text-align: center;
  padding: 48px 24px;
  color: #78909c;
  font-size: 15px;
}
.empty-hint {
  font-size: 12px;
  color: #b0bec5;
  margin-top: 8px;
}

/* Child tabs */
.child-tabs {
  display: flex;
  gap: 8px;
  margin: 16px 0;
}
.child-tab {
  flex: 1;
  padding: 10px 0;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  font-size: 14px;
  font-weight: 600;
  color: #78909c;
  cursor: pointer;
  transition: all 0.2s;
}
.child-tab.active {
  border-color: #4ECDC4;
  color: #4ECDC4;
  background: #e0f7fa;
}

/* Child info */
.child-info {
  background: white;
  border-radius: 16px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 0 0 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.child-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.child-name {
  font-size: 16px;
  font-weight: 700;
  color: #1a237e;
}
.child-meta {
  font-size: 12px;
  color: #78909c;
  margin-top: 2px;
}
.child-stars {
  margin-left: auto;
  font-size: 18px;
  font-weight: 700;
  color: #FF6B6B;
}

/* Stats */
.stats-overview {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.stat-item {
  flex: 1;
  background: white;
  border-radius: 14px;
  padding: 14px 10px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.stat-value {
  font-size: 20px;
  font-weight: 800;
  color: #1a237e;
}
.stat-value small {
  font-size: 14px;
  color: #78909c;
}
.stat-label {
  font-size: 11px;
  color: #78909c;
  margin-top: 4px;
}

/* Records */
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 12px;
}
.log-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}
.log-item {
  background: white;
  border-radius: 14px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-left: 4px solid #66bb6a;
}
.log-item.missed {
  border-left-color: #ef5350;
  opacity: 0.6;
}
.log-left {
  flex-shrink: 0;
  width: 80px;
}
.log-date {
  font-size: 13px;
  font-weight: 700;
  color: #37474f;
}
.log-weekday {
  font-size: 11px;
  color: #b0bec5;
}
.log-center {
  flex: 1;
}
.log-status {
  font-size: 13px;
  font-weight: 600;
}
.log-status.completed {
  color: #66bb6a;
}
.log-status.missed {
  color: #ef5350;
}
.log-detail {
  font-size: 11px;
  color: #b0bec5;
  margin-top: 2px;
}
.log-toggle {
  background: #f5f5f5;
  border: none;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 12px;
  color: #78909c;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.log-toggle:hover {
  background: #e8e8e8;
}
.log-toggle:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Actions */
.action-btns {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.action-btn {
  width: 100%;
  padding: 14px 0;
  border-radius: 14px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  background: white;
  color: #455a64;
  border: 1px solid #e8e8e8;
}
.action-btn.primary {
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  border: none;
  box-shadow: 0 4px 14px rgba(78, 205, 196, 0.35);
}
.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
</style>
