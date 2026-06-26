<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth.js'

const router = useRouter()
const authStore = useAuthStore()

// Use API base URL - in dev mode it's '/api' (proxied by Vite),
// in Capacitor mode it's the full backend URL
const API_BASE = import.meta.env.VITE_API_BASE || '/api'
const SERVER_ORIGIN = API_BASE.replace(/\/api$/, '')

const avatarFileInput = ref(null)
const uploadingAvatar = ref(false)

// Resolve avatar URL: prepend server origin if avatar path is relative
const avatarUrl = computed(() => {
  const av = authStore.user?.avatar
  if (!av) return null
  if (av.startsWith('http')) return av
  return SERVER_ORIGIN + av
})

const menuItems = [
  { icon: '📊', label: '学习报告', action: 'report' },
  { icon: '🗓️', label: '学习计划', action: 'plan' },
  { icon: '👨‍👩‍👧', label: '家长入口', action: 'parent' },
  { icon: '🔔', label: '消息通知', action: 'notifications' },
  { icon: '⚙️', label: '设置', action: 'settings' },
]

const longPressTimer = ref(null)

function handleMenuAction(action) {
  if (action === 'parent') {
    router.push('/parent')
  } else if (action === 'plan') {
    router.push('/plan-config')
  } else {
    ElMessage.info(`功能 "${action}" 即将上线`)
  }
}

function handleLogout() {
  authStore.logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}

function startLongPress() {
  longPressTimer.value = setTimeout(() => {
    router.push('/parent')
  }, 3000)
}

function cancelLongPress() {
  if (longPressTimer.value) {
    clearTimeout(longPressTimer.value)
    longPressTimer.value = null
  }
}

function triggerAvatarUpload() {
  avatarFileInput.value?.click()
}

async function onAvatarSelected(e) {
  const file = e.target.files?.[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像文件不能超过 2MB')
    return
  }

  uploadingAvatar.value = true
  try {
    await authStore.doUploadAvatar(file)
    ElMessage.success('头像已更新')
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    uploadingAvatar.value = false
    // Reset file input so same file can be selected again
    if (avatarFileInput.value) avatarFileInput.value.value = ''
  }
}

function userInitial() {
  return (authStore.user?.nickname || authStore.user?.username || '?').charAt(0).toUpperCase()
}
</script>

<template>
  <div class="page-container no-select">
    <!-- 顶部导航 -->
    <div class="app-bar sticky top-0 z-10">
      <span>👤</span>
      <span>我的</span>
    </div>

    <div class="animate-fade-in">
      <!-- 头像区域 -->
      <div class="avatar-section">
        <div
          class="avatar-circle"
          :class="{ 'has-photo': authStore.user?.avatar }"
          @click="triggerAvatarUpload"
          @mousedown="startLongPress"
          @mouseup="cancelLongPress"
          @mouseleave="cancelLongPress"
          @touchstart="startLongPress"
          @touchend="cancelLongPress"
        >
          <img
            v-if="avatarUrl"
            :src="avatarUrl"
            class="avatar-img"
            alt="头像"
          />
          <span v-else class="avatar-initial">{{ userInitial() }}</span>
          <div v-if="uploadingAvatar" class="avatar-uploading">
            <div class="spinner sm white"></div>
          </div>
          <div class="avatar-hover-tip">点击更换</div>
        </div>
        <input
          ref="avatarFileInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="onAvatarSelected"
        />
        <div class="avatar-name">{{ authStore.user?.nickname || authStore.user?.username || '未登录' }}</div>
        <div class="avatar-grade">
          <span v-if="authStore.user?.role === 'child'">👦 小学员</span>
          <span v-else-if="authStore.user?.role === 'parent'">👨‍👩‍👧 家长</span>
        </div>
        <div class="long-press-hint">长按头像 3 秒进入家长模式</div>
      </div>

      <!-- 菜单列表 -->
      <div class="menu-list">
        <div
          v-for="item in menuItems"
          :key="item.action"
          class="menu-item"
          @click="handleMenuAction(item.action)"
        >
          <span class="menu-icon">{{ item.icon }}</span>
          <span class="menu-label">{{ item.label }}</span>
          <span class="menu-arrow">›</span>
        </div>
      </div>

      <!-- 退出登录 -->
      <div class="logout-section">
        <button class="logout-btn" @click="handleLogout">
          🚪 退出登录
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
  gap: 10px;
  box-shadow: 0 2px 12px rgba(78, 205, 196, 0.3);
  margin: -16px -16px 0;
}

/* 头像区域 */
.avatar-section {
  text-align: center;
  padding: 28px 16px 20px;
  background: linear-gradient(180deg, #e0f7fa 0%, #F7F9FC 60%);
  border-radius: 0 0 24px 24px;
  margin: 0 -16px 18px;
}

.avatar-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, #4ECDC4, #26a69a, #00897b);
  color: white;
  font-size: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
  box-shadow: 0 6px 20px rgba(78, 205, 196, 0.4);
  border: 3px solid white;
  cursor: pointer;
  transition: transform 0.2s;
  position: relative;
  overflow: hidden;
}

.avatar-circle.has-photo {
  background: #e0e0e0;
}

.avatar-circle:active {
  transform: scale(0.95);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-initial {
  z-index: 1;
}

.avatar-uploading {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-hover-tip {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px 0;
  background: rgba(0,0,0,0.5);
  color: white;
  font-size: 10px;
  text-align: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-circle:hover .avatar-hover-tip {
  opacity: 1;
}

.spinner.sm {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255,255,255,0.3);
  border-top: 3px solid white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0;
}

.spinner.sm.white { }

@keyframes spin {
  to { transform: rotate(360deg); }
}

.avatar-name {
  font-size: 20px;
  font-weight: 800;
  color: #1a237e;
}

.avatar-grade {
  font-size: 12px;
  color: #78909c;
  margin-top: 4px;
}

.long-press-hint {
  font-size: 10px;
  color: #b0bec5;
  margin-top: 10px;
}

/* 菜单 */
.menu-list {
  display: flex;
  flex-direction: column;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
  color: #455a64;
  cursor: pointer;
  transition: all 0.15s;
}

.menu-item:hover {
  color: #4ECDC4;
}

.menu-item:active {
  opacity: 0.7;
}

.menu-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.menu-label {
  flex: 1;
}

.menu-arrow {
  color: #cfd8dc;
  font-size: 18px;
  transition: 0.2s;
}

.menu-item:hover .menu-arrow {
  color: #4ECDC4;
  transform: translateX(2px);
}

.logout-section {
  padding: 24px 0;
  text-align: center;
}

.logout-btn {
  padding: 12px 40px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  color: #78909c;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  border-color: #FF6B6B;
  color: #FF6B6B;
}
</style>
