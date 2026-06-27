<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useTaskStore } from './stores/task.js'

const route = useRoute()
const router = useRouter()
const taskStore = useTaskStore()

const tabs = [
  { path: '/home', icon: '🏠', label: '首页' },
  { path: '/learn', icon: '📖', label: '学习' },
  { path: '/history', icon: '📋', label: '记录' },
  { path: '/achieve', icon: '🏆', label: '成就' },
  { path: '/profile', icon: '👤', label: '我的' },
]

const activeTab = computed(() => {
  const idx = tabs.findIndex(t => route.path === t.path)
  return idx >= 0 ? idx : 0
})

const isTabPage = computed(() => tabs.some(t => route.path === t.path))

function switchTab(path) {
  router.push(path)
}
</script>

<template>
  <div class="app-root min-h-screen bg-[#F7F9FC]">
    <!-- 页面内容 -->
    <router-view v-slot="{ Component }">
      <transition name="fade">
        <component :is="Component" />
      </transition>
    </router-view>

    <!-- 底部 TabBar -->
    <nav v-if="isTabPage" class="tab-bar">
      <div
        v-for="(tab, i) in tabs"
        :key="tab.path"
        class="tab-item"
        :class="{ active: activeTab === i }"
        @click="switchTab(tab.path)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-label">{{ tab.label }}</span>
      </div>
    </nav>
  </div>
</template>

<style scoped>
.app-root {
  position: relative;
  max-width: 480px;
  margin: 0 auto;
  box-shadow: 0 0 40px rgba(0, 0, 0, 0.08);
  /* Status bar safe area padding */
  padding-top: env(safe-area-inset-top, 0px);
}

.tab-bar {
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  display: flex;
  background: white;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.06);
  padding: 4px 0 8px;
  z-index: 100;
  padding-bottom: calc(env(safe-area-inset-bottom, 0px) + 8px);
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 6px 0 2px;
  font-size: 10px;
  color: #b0bec5;
  cursor: pointer;
  transition: all 0.25s;
  position: relative;
}

.tab-item.active {
  color: #4ECDC4;
}

.tab-item.active::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: #4ECDC4;
  border-radius: 0 0 3px 3px;
}

.tab-icon {
  font-size: 22px;
  display: block;
  margin-bottom: 3px;
}

.tab-item.active .tab-icon {
  filter: drop-shadow(0 2px 4px rgba(78, 205, 196, 0.4));
}

.tab-label {
  font-size: 10px;
  font-weight: 500;
}

/* 路由过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 平板适配 */
@media (min-width: 520px) {
  .app-root {
    max-width: 768px;
  }
  .tab-bar {
    max-width: 768px;
  }
  .tab-item {
    font-size: 12px;
  }
  .tab-icon {
    font-size: 24px;
  }
  .tab-label {
    font-size: 12px;
  }
}
</style>
