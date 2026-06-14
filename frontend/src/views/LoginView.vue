<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const mode = ref('login')
const loading = ref(false)

// Check if redirected here with ?register=1
onMounted(() => {
  if (route.query.register === '1' || sessionStorage.getItem('login_tab') === 'register') {
    sessionStorage.removeItem('login_tab')
    mode.value = 'register'
  }
})

// Login form
const loginForm = ref({ username: '', password: '' })

// Register form
const registerForm = ref({
  username: '',
  password: '',
  nickname: '',
  role: 'child',
})

async function handleLogin() {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await authStore.login(loginForm.value.username, loginForm.value.password)
    ElMessage.success('登录成功')
    router.push('/home')
  } catch (e) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.nickname) {
    ElMessage.warning('请填写完整信息')
    return
  }
  loading.value = true
  try {
    await authStore.register(registerForm.value)
    ElMessage.success('注册成功')
    router.push('/home')
  } catch (e) {
    ElMessage.error(e.message || '注册失败')
  } finally {
    loading.value = false
  }
}

function switchMode(m) {
  mode.value = m
}
</script>

<template>
  <div class="login-root">
    <!-- Header -->
    <div class="login-header">
      <div class="login-logo">🎓</div>
      <h1 class="login-title">KET 学习助手</h1>
      <p class="login-sub">每天进步一点点</p>
    </div>

    <!-- Tabs -->
    <div class="login-tabs">
      <button
        :class="['tab-btn', { active: mode === 'login' }]"
        @click="switchMode('login')"
      >登录</button>
      <button
        :class="['tab-btn', { active: mode === 'register' }]"
        @click="switchMode('register')"
      >注册</button>
    </div>

    <!-- Login Form -->
    <form v-if="mode === 'login'" class="login-form" @submit.prevent="handleLogin">
      <label class="form-label">用户名</label>
      <input
        v-model="loginForm.username"
        class="form-input"
        placeholder="请输入用户名"
        autocomplete="username"
      />

      <label class="form-label">密码</label>
      <input
        v-model="loginForm.password"
        type="password"
        class="form-input"
        placeholder="请输入密码"
        autocomplete="current-password"
      />

      <button type="submit" class="submit-btn" :disabled="loading">
        {{ loading ? '登录中...' : '🚀 登录' }}
      </button>

      <p class="hint-text">测试账号：child1 / 1234</p>
    </form>

    <!-- Register Form -->
    <form v-else class="login-form" @submit.prevent="handleRegister">
      <label class="form-label">昵称</label>
      <input
        v-model="registerForm.nickname"
        class="form-input"
        placeholder="给孩子起个名字吧"
      />

      <label class="form-label">用户名</label>
      <input
        v-model="registerForm.username"
        class="form-input"
        placeholder="字母或数字"
      />

      <label class="form-label">密码</label>
      <input
        v-model="registerForm.password"
        type="password"
        class="form-input"
        placeholder="至少4位密码"
      />

      <label class="form-label">角色</label>
      <div class="role-group">
        <label class="role-option" :class="{ checked: registerForm.role === 'child' }">
          <input type="radio" v-model="registerForm.role" value="child" />
          <span>👦 孩子</span>
        </label>
        <label class="role-option" :class="{ checked: registerForm.role === 'parent' }">
          <input type="radio" v-model="registerForm.role" value="parent" />
          <span>👨‍👩‍👧 家长</span>
        </label>
      </div>

      <button type="submit" class="submit-btn" :disabled="loading">
        {{ loading ? '注册中...' : '✨ 注册' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-root {
  max-width: 420px;
  margin: 0 auto;
  padding: 40px 24px 80px;
  min-height: 100vh;
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-logo {
  font-size: 64px;
  margin-bottom: 8px;
}

.login-title {
  font-size: 26px;
  font-weight: 800;
  color: #1a237e;
  margin: 0 0 4px;
}

.login-sub {
  font-size: 14px;
  color: #78909c;
  margin: 0;
}

.login-tabs {
  display: flex;
  background: #e8f5f0;
  border-radius: 14px;
  padding: 4px;
  margin-bottom: 24px;
}

.tab-btn {
  flex: 1;
  padding: 10px 0;
  border: none;
  border-radius: 11px;
  font-size: 14px;
  font-weight: 600;
  background: transparent;
  color: #78909c;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  background: white;
  color: #4ECDC4;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-label {
  font-size: 13px;
  font-weight: 600;
  color: #37474f;
  margin-bottom: -8px;
}

.form-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
  background: #fafafa;
}

.form-input:focus {
  border-color: #4ECDC4;
  background: white;
}

.role-group {
  display: flex;
  gap: 12px;
}

.role-option {
  flex: 1;
  padding: 12px;
  text-align: center;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
  color: #546e7a;
  transition: all 0.2s;
}

.role-option input {
  display: none;
}

.role-option.checked {
  border-color: #4ECDC4;
  background: #e0f7fa;
  color: #00695c;
}

.submit-btn {
  width: 100%;
  padding: 14px 0;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  color: white;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  cursor: pointer;
  margin-top: 8px;
  box-shadow: 0 4px 16px rgba(78, 205, 196, 0.35);
  transition: all 0.2s;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(78, 205, 196, 0.45);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.hint-text {
  text-align: center;
  font-size: 12px;
  color: #b0bec5;
  margin-top: 8px;
}
</style>
