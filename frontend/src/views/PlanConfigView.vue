<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPlanConfig, savePlanConfig } from '../api/plan.js'

const router = useRouter()
const saving = ref(false)
const loading = ref(true)

const form = ref({
  startDate: '',
  endDate: '',
  dailyDuration: 35,
})

const totalDays = ref(0)

function calcTotalDays() {
  if (form.value.startDate && form.value.endDate) {
    const start = new Date(form.value.startDate)
    const end = new Date(form.value.endDate)
    const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24)) + 1
    totalDays.value = diff > 0 ? diff : 0
  }
}

async function loadPlan() {
  loading.value = true
  try {
    const plan = await getPlanConfig()
    form.value.startDate = plan.startDate || ''
    form.value.endDate = plan.endDate || ''
    form.value.dailyDuration = plan.dailyDuration || 35
    totalDays.value = plan.totalDays || 0
  } catch (e) {
    ElMessage.error('加载计划失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  if (!form.value.startDate || !form.value.endDate) {
    ElMessage.warning('请选择起止日期')
    return
  }
  if (new Date(form.value.endDate) <= new Date(form.value.startDate)) {
    ElMessage.warning('结束日期必须晚于开始日期')
    return
  }
  saving.value = true
  try {
    await savePlanConfig({
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      dailyDuration: form.value.dailyDuration,
    })
    ElMessage.success('学习计划已保存')
    calcTotalDays()
  } catch (e) {
    ElMessage.error('保存失败: ' + e.message)
  } finally {
    saving.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  loadPlan()
})
</script>

<template>
  <div class="page-container">
    <!-- Top bar -->
    <div class="app-bar">
      <span class="back-btn" @click="goBack">←</span>
      <span>学习计划</span>
    </div>

    <div v-if="loading" class="empty-box">
      <p>🔄 加载中...</p>
    </div>

    <div v-else class="animate-fade-in">
      <!-- Total days display -->
      <div class="total-days-card">
        <div class="total-days-num">{{ totalDays }}</div>
        <div class="total-days-label">学习总天数</div>
      </div>

      <!-- Plan form -->
      <div class="form-card">
        <div class="form-group">
          <label class="form-label">📅 计划开始日期</label>
          <el-date-picker
            v-model="form.startDate"
            type="date"
            placeholder="选择开始日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="calcTotalDays"
          />
        </div>

        <div class="form-group">
          <label class="form-label">📅 计划结束日期</label>
          <el-date-picker
            v-model="form.endDate"
            type="date"
            placeholder="选择结束日期"
            style="width: 100%"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            @change="calcTotalDays"
          />
        </div>

        <div class="form-group">
          <label class="form-label">⏱️ 每日学习时长目标</label>
          <div class="duration-options">
            <button
              v-for="d in [25, 35, 45]"
              :key="d"
              class="duration-btn"
              :class="{ active: form.dailyDuration === d }"
              @click="form.dailyDuration = d"
            >
              {{ d }}分钟
            </button>
          </div>
        </div>

        <button
          class="save-btn"
          :disabled="saving"
          @click="handleSave"
        >
          {{ saving ? '💾 保存中...' : '💾 保存计划' }}
        </button>
      </div>

      <!-- Tips -->
      <div class="tips-card">
        <div class="tips-title">💡 提示</div>
        <ul class="tips-list">
          <li>修改计划时间后，系统会自动重新计算总天数</li>
          <li>已生成的学习内容不会被覆盖</li>
          <li>建议每天学习 {{ form.dailyDuration }} 分钟左右</li>
          <li>孩子可在首页查看每日任务</li>
        </ul>
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
.empty-box {
  text-align: center;
  padding: 48px 24px;
  color: #78909c;
  font-size: 15px;
}

/* Total days card */
.total-days-card {
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  border-radius: 16px;
  padding: 24px;
  text-align: center;
  margin: 16px 0;
  color: white;
  box-shadow: 0 4px 16px rgba(78, 205, 196, 0.3);
}
.total-days-num {
  font-size: 42px;
  font-weight: 800;
}
.total-days-label {
  font-size: 14px;
  opacity: 0.85;
  margin-top: 4px;
}

/* Form */
.form-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 16px;
}
.form-group {
  margin-bottom: 18px;
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
  padding: 12px 14px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  font-size: 15px;
  color: #37474f;
  background: #fafafa;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-input:focus {
  border-color: #4ECDC4;
  background: white;
}
.duration-options {
  display: flex;
  gap: 10px;
}
.duration-btn {
  flex: 1;
  padding: 12px 0;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  font-size: 14px;
  font-weight: 600;
  color: #78909c;
  cursor: pointer;
  transition: all 0.2s;
}
.duration-btn.active {
  border-color: #4ECDC4;
  color: #4ECDC4;
  background: #e0f7fa;
}
.save-btn {
  width: 100%;
  padding: 14px 0;
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(78, 205, 196, 0.35);
}
.save-btn:hover {
  transform: translateY(-1px);
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Tips */
.tips-card {
  background: #fff8e1;
  border-radius: 14px;
  padding: 16px 18px;
  border: 1px solid #ffecb3;
}
.tips-title {
  font-size: 14px;
  font-weight: 700;
  color: #f57f17;
  margin-bottom: 8px;
}
.tips-list {
  margin: 0;
  padding-left: 18px;
}
.tips-list li {
  font-size: 13px;
  color: #6d4c41;
  line-height: 1.8;
}
</style>
