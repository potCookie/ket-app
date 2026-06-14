<script setup>
import { computed, onMounted } from 'vue'
import { useTaskStore } from '../stores/task.js'

const taskStore = useTaskStore()
const stats = computed(() => taskStore.stats ?? {})

const statsRows = computed(() => [
  { label: '✅ 已完成天数', value: `${stats.value.completedDays || 0} / ${stats.value.totalDays || 0}` },
  { label: '📝 做题正确率', value: `${stats.value.accuracy || 0}%` },
  { label: '🔥 最长连续打卡', value: `${stats.value.longestStreak || 0}天` },
  { label: '📚 已学词汇', value: `${stats.value.learnedWords || 0}个` },
])

onMounted(async () => {
  if (!taskStore.stats) {
    await taskStore.fetchStats()
  }
})
</script>

<template>
  <div class="page-container no-select">
    <!-- 顶部导航 -->
    <div class="app-bar sticky top-0 z-10">
      <span>🏆</span>
      <span>我的成就</span>
    </div>

    <!-- Loading -->
    <div v-if="taskStore.statsLoading" class="loading-box">
      <div class="spinner"></div>
      <p>加载成就数据...</p>
    </div>

    <!-- Normal content -->
    <template v-else>
      <div class="animate-fade-in">
        <!-- 星星展示 -->
        <div class="star-hero">
          <div class="star-num">⭐ {{ stats.stars || 0 }}</div>
          <div class="star-label">累计获得星星</div>
          <div class="star-icons">🎉 🌟 ⭐ 🇫🇷 🏅</div>
        </div>

        <!-- 勋章墙 -->
        <div class="section-title">🏅 勋章墙</div>
        <div class="badge-grid">
          <div
            v-for="badge in taskStore.badges"
            :key="badge.type"
            class="badge-item"
            :class="{ earned: badge.earned }"
          >
            <span class="badge-icon">{{ badge.icon }}</span>
            <span class="badge-name">{{ badge.name }}</span>
          </div>
        </div>
        <div v-if="!taskStore.badges.length" class="empty-hint">
          还没有获得勋章，开始学习吧！
        </div>

        <!-- 学习统计 -->
        <div class="section-title">📊 学习统计</div>
        <div class="stats-card">
          <div
            v-for="row in statsRows"
            :key="row.label"
            class="stats-row"
          >
            <span>{{ row.label }}</span>
            <span class="stats-val">{{ row.value }}</span>
          </div>
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

.empty-hint {
  text-align: center;
  font-size: 12px;
  color: #b0bec5;
  padding: 20px 0;
}

/* 星星展示 */
.star-hero {
  background: linear-gradient(135deg, #FF6B6B 0%, #ff8a80 40%, #ffab91 100%);
  border-radius: 20px;
  padding: 28px 24px;
  text-align: center;
  color: white;
  margin: 20px 0 18px;
  box-shadow: 0 8px 32px rgba(255, 107, 107, 0.3);
  position: relative;
  overflow: hidden;
}

.star-hero::before {
  content: '';
  position: absolute;
  top: -40px;
  right: -40px;
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
}

.star-hero::after {
  content: '';
  position: absolute;
  bottom: -30px;
  left: -30px;
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.07);
}

.star-num {
  font-size: 52px;
  font-weight: 900;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: relative;
  z-index: 1;
}

.star-label {
  font-size: 14px;
  opacity: 0.9;
  margin-top: 4px;
  position: relative;
  z-index: 1;
}

.star-icons {
  font-size: 24px;
  margin-top: 10px;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
  position: relative;
  z-index: 1;
}

/* 勋章墙 */
.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.badge-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 18px;
}

.badge-item {
  aspect-ratio: 1;
  border-radius: 50%;
  background: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 3px solid #e8e8e8;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  position: relative;
}

.badge-item.earned {
  border-color: #FF6B6B;
  background: linear-gradient(135deg, #fff3e0, #ffe0b2);
  box-shadow: 0 4px 16px rgba(255, 107, 107, 0.25);
}

.badge-icon {
  font-size: 28px;
}

.badge-name {
  font-size: 8px;
  color: #b0bec5;
  margin-top: 4px;
  font-weight: 500;
}

.badge-item.earned .badge-name {
  color: #FF6B6B;
  font-weight: 600;
}

/* 统计 */
.stats-card {
  background: white;
  border-radius: 16px;
  padding: 6px 18px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;
  color: #546e7a;
}

.stats-row:last-child {
  border-bottom: none;
}

.stats-val {
  font-weight: 700;
  color: #1a237e;
  font-size: 15px;
}

/* 平板 */
@media (min-width: 520px) {
  .badge-grid {
    grid-template-columns: repeat(6, 1fr);
  }
}
</style>
