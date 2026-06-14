<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getStudyHistory } from '../api/study.js'

const router = useRouter()

const loading = ref(true)
const error = ref('')
const historyList = ref([])

onMounted(() => {
  loadHistory()
})

async function loadHistory() {
  loading.value = true
  try {
    historyList.value = await getStudyHistory()
  } catch (e) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function getDayLabel(item) {
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  try {
    const d = new Date(item.date)
    const mm = d.getMonth() + 1
    const dd = d.getDate()
    const w = weekdays[d.getDay()]
    return `${mm}月${dd}日 周${w}`
  } catch {
    return item.date
  }
}
</script>

<template>
  <div class="page-container no-select">
    <!-- 顶部导航 -->
    <div class="app-bar sticky top-0 z-10">
      <span>📋</span>
      <span>学习记录</span>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-box">
      <div class="spinner"></div>
      <p>加载中...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="error-box">
      <p>{{ error }}</p>
      <button class="retry-btn" @click="loadHistory">重试</button>
    </div>

    <!-- Empty -->
    <div v-else-if="!historyList.length" class="empty-box">
      <div class="empty-icon">📭</div>
      <p>还没有学习记录</p>
      <p class="empty-hint">完成一天的学习后，记录会显示在这里</p>
      <button class="goto-learn-btn" @click="router.push('/learn')">去学习</button>
    </div>

    <!-- History list -->
    <div v-else class="animate-fade-in">
      <div
        v-for="item in historyList"
        :key="item.date"
        class="history-card"
        @click="router.push(`/learn?date=${item.date}`)"
      >
        <div class="h-left">
          <div class="h-date">{{ getDayLabel(item) }}</div>
          <div class="h-status">
            <span v-if="item.checkedIn" class="tag done">已打卡</span>
            <span class="tag progress">{{ item.completed }}/{{ item.modules }} 模块</span>
            <span v-if="item.stars" class="tag star">⭐ {{ item.stars }}</span>
          </div>
        </div>
        <div class="h-right">
          <span class="arrow">→</span>
        </div>
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
  gap: 10px;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  margin: -16px -16px 0;
}

.loading-box, .error-box, .empty-box {
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

@keyframes spin { to { transform: rotate(360deg); } }

.retry-btn, .goto-learn-btn {
  margin-top: 16px;
  padding: 10px 32px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-hint { font-size: 12px; color: #b0bec5; margin: 4px 0 16px; }

/* ====== History Cards ====== */
.history-card {
  background: white;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 18px;
}

.history-card:hover {
  box-shadow: 0 4px 16px rgba(78,205,196,0.15);
  border-color: #4ECDC4;
}

.h-date {
  font-size: 16px;
  font-weight: 700;
  color: #1a237e;
  margin-bottom: 6px;
}

.h-status { display: flex; gap: 6px; flex-wrap: wrap; }

.tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 8px;
  font-weight: 500;
}

.tag.done { background: #e8f5e9; color: #2e7d32; }
.tag.progress { background: #e3f2fd; color: #1565c0; }
.tag.star { background: #fff3e0; color: #e65100; }

.arrow { color: #b0bec5; font-size: 20px; }
</style>
