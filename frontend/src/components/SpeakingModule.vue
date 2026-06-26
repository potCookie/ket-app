<script setup>
import { ref, computed, onMounted } from 'vue'
import { useTaskStore } from '../stores/task.js'

const props = defineProps({
  reviewMode: { type: Boolean, default: false },
  prevAnswers: { type: Object, default: null }
})

const emit = defineEmits(['done'])
const taskStore = useTaskStore()
const speaking = taskStore.todayData.speaking

const isRecording = ref(false)
const hasRecorded = ref(false)
const audioUrl = ref(null)
const uploadedUrl = ref(null)
const uploading = ref(false)
const uploadFailed = ref(false)
const uploadErrorMsg = ref('')

// Historical recordings for review
const historicRecordings = ref([])

let mediaRecorder = null
let chunks = []
let currentBlob = null

onMounted(async () => {
  if (props.reviewMode) {
    await taskStore.fetchRecordings()
    historicRecordings.value = taskStore.todayRecordings
    hasRecorded.value = true
  }
})

async function toggleRecording() {
  if (isRecording.value) {
    stopRecording()
  } else {
    await startRecording()
  }
}

async function startRecording() {
  try {
    // Check if mediaDevices API is available
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      uploadFailed.value = true
      uploadErrorMsg.value = '当前浏览器不支持录音'
      return
    }

    const stream = await navigator.mediaDevices.getUserMedia({ audio: true })

    // Determine preferred mime type for Android WebView
    const mimeType = MediaRecorder.isTypeSupported('audio/webm;codecs=opus')
      ? 'audio/webm;codecs=opus'
      : MediaRecorder.isTypeSupported('audio/webm')
        ? 'audio/webm'
        : 'audio/mp4'

    const options = mimeType ? { mimeType } : {}
    mediaRecorder = new MediaRecorder(stream, options)
    chunks = []

    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) chunks.push(e.data)
    }

    mediaRecorder.onerror = () => {
      uploadFailed.value = true
      uploadErrorMsg.value = '录音出错，请重试'
      isRecording.value = false
      stream.getTracks().forEach(t => t.stop())
    }

    mediaRecorder.onstop = async () => {
      currentBlob = new Blob(chunks, { type: mimeType || 'audio/webm' })

      if (currentBlob.size === 0) {
        uploadFailed.value = true
        uploadErrorMsg.value = '未录制到音频'
        stream.getTracks().forEach(t => t.stop())
        return
      }

      audioUrl.value = URL.createObjectURL(currentBlob)
      hasRecorded.value = true
      stream.getTracks().forEach(t => t.stop())

      // Upload to backend
      uploading.value = true
      uploadFailed.value = false
      uploadErrorMsg.value = ''
      try {
        console.log('[Speaking] Uploading recording, size:', currentBlob.size, 'bytes')
        const url = await taskStore.doUploadRecording(currentBlob)
        if (url) {
          uploadedUrl.value = url
          console.log('[Speaking] Uploaded successfully:', url)
        } else {
          uploadFailed.value = true
          uploadErrorMsg.value = '服务器返回空'
          console.warn('[Speaking] Upload returned null')
        }
      } catch (e) {
        uploadFailed.value = true
        uploadErrorMsg.value = e.message || '未知错误'
        console.error('[Speaking] Upload error:', e)
      } finally {
        uploading.value = false
      }
    }

    mediaRecorder.start()
    isRecording.value = true
  } catch (err) {
    console.error('[Speaking] Failed to start recording:', err)
    uploadFailed.value = true
    uploadErrorMsg.value = err.name === 'NotAllowedError'
      ? '麦克风权限被拒绝，请在系统设置中允许录音权限'
      : err.name === 'NotFoundError'
        ? '未检测到麦克风设备'
        : '无法启动录音: ' + (err.message || '未知错误')
  }
}

function stopRecording() {
  if (mediaRecorder) {
    mediaRecorder.stop()
  }
  isRecording.value = false
}

function playRecording() {
  const url = audioUrl.value
  if (url) {
    const audio = new Audio(url)
    audio.play()
  }
}

function playHistoricRecording(rec) {
  const audio = new Audio()
  // Resolve relative URL for Capacitor mode
  const API_BASE = import.meta.env.VITE_API_BASE || '/api'
  const SERVER_ORIGIN = API_BASE.replace(/\/api$/, '')
  const fileUrl = rec.fileUrl?.startsWith('http') ? rec.fileUrl : SERVER_ORIGIN + (rec.fileUrl || '')
  audio.src = fileUrl
  audio.play()
}

function completeSpeaking() {
  emit('done', { score: null, total: null })
}

const templateHtml = computed(() => {
  return speaking.template.replace(/____/g, '<span class="blank">______</span>')
})
</script>

<template>
  <div>
    <!-- Review mode badge -->
    <div v-if="props.reviewMode" class="review-badge">
      📋 历史学习记录
    </div>

    <!-- 对话模板 -->
    <div class="template-card">
      <div class="template-title">🗣️ 自我介绍模板</div>
      <div class="template-text" v-html="templateHtml"></div>
    </div>

    <!-- 关键句型 -->
    <div class="phrases-section">
      <div class="section-label">📋 关键句型</div>
      <div class="phrase-list">
        <div
          v-for="(phrase, i) in speaking.phrases"
          :key="i"
          class="phrase-item"
        >
          <div class="phrase-en">{{ phrase.en }}</div>
          <div class="phrase-zh">{{ phrase.zh }}</div>
        </div>
      </div>
    </div>

    <!-- ====== Review mode: show historic recordings ====== -->
    <template v-if="props.reviewMode">
      <div class="record-section">
        <div class="section-label">🎙️ 历史录音</div>
        <div v-if="historicRecordings.length === 0" class="record-hint">
          暂无录音记录
        </div>
        <div
          v-for="(rec, i) in historicRecordings"
          :key="rec.id"
          class="history-recording"
        >
          <div class="history-label">录音 {{ i + 1 }}</div>
          <div class="history-date">{{ rec.uploadedAt?.substring(0, 19).replace('T', ' ') || '' }}</div>
          <button class="replay-btn" @click="playHistoricRecording(rec)">▶️ 播放录音</button>
        </div>
      </div>
    </template>

    <!-- ====== Normal mode: record ====== -->
    <template v-else>
      <div class="record-section">
        <div class="section-label">🎙️ 录音练习</div>
        <p class="record-hint">
          按照上面的模板，介绍一下你自己吧！
        </p>

        <div class="record-area">
          <button
            class="record-btn"
            :class="{ recording: isRecording }"
            @click="toggleRecording"
          >
            <span v-if="!isRecording">🎙️</span>
            <span v-else class="pulse-dot"></span>
          </button>
          <div class="record-text">
            {{ uploading ? '上传中...' : (isRecording ? '正在录音...' : (uploadFailed ? ('上传失败: ' + uploadErrorMsg) : (hasRecorded ? '录音完成 ✓' : '点击开始录音'))) }}
          </div>
        </div>

        <!-- 回放 -->
        <div v-if="hasRecorded && audioUrl" class="replay-area">
          <button class="replay-btn" @click="playRecording">▶️ 回放录音</button>
        </div>
      </div>

      <!-- 完成按钮 -->
      <button class="complete-btn" @click="completeSpeaking">
        ✔ 完成口语练习
      </button>
    </template>
  </div>
</template>

<style scoped>
.template-card {
  background: linear-gradient(135deg, #fff3e0, #ffe0b2);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 16px;
  border: 2px solid #ffcc80;
}

.template-title {
  font-size: 15px;
  font-weight: 700;
  color: #e65100;
  margin-bottom: 12px;
}

.template-text {
  font-size: 16px;
  color: #37474f;
  line-height: 2;
}

.template-text :deep(.blank) {
  color: #4ECDC4;
  font-weight: 700;
  border-bottom: 2px dashed #4ECDC4;
  padding: 0 4px;
}

.phrases-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.phrase-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.phrase-item {
  background: white;
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.03);
}

.phrase-en {
  font-size: 14px;
  color: #1a237e;
  font-weight: 600;
  margin-bottom: 4px;
}

.phrase-zh {
  font-size: 12px;
  color: #78909c;
}

.record-section {
  margin-bottom: 16px;
}

.record-hint {
  font-size: 12px;
  color: #78909c;
  margin-bottom: 14px;
  line-height: 1.6;
}

.record-area {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border-radius: 14px;
  padding: 16px;
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.record-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 3px solid #e0e0e0;
  background: white;
  font-size: 24px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.record-btn:hover {
  border-color: #FF6B6B;
  box-shadow: 0 0 0 6px rgba(255, 107, 107, 0.1);
}

.record-btn.recording {
  background: #FF6B6B;
  border-color: #FF6B6B;
  box-shadow: 0 0 0 8px rgba(255, 107, 107, 0.15);
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: white;
  animation: pulse 1s ease infinite;
}

@keyframes pulse {
  0% { transform: scale(0.8); opacity: 0.8; }
  50% { transform: scale(1.2); opacity: 1; }
  100% { transform: scale(0.8); opacity: 0.8; }
}

.record-text {
  font-size: 14px;
  color: #546e7a;
  font-weight: 500;
}

.replay-area {
  margin-top: 10px;
  text-align: center;
}

.replay-btn {
  background: #e8eaf6;
  color: #3949ab;
  border: none;
  border-radius: 10px;
  padding: 10px 20px;
  font-size: 13px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.2s;
}

.replay-btn:hover {
  background: #c5cae9;
}

.history-recording {
  background: white;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 10px;
  border: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.history-label {
  font-size: 14px;
  font-weight: 600;
  color: #37474f;
}

.history-date {
  font-size: 11px;
  color: #90a4ae;
  margin-left: auto;
}

.complete-btn {
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  border: none;
  border-radius: 14px;
  padding: 14px 0;
  width: 100%;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 4px 14px rgba(78, 205, 196, 0.35);
  margin-top: 8px;
}

.complete-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(78, 205, 196, 0.45);
}

.review-badge {
  background: linear-gradient(135deg, #e8eaf6, #c5cae9);
  color: #3949ab;
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  padding: 8px 12px;
  border-radius: 10px;
  margin-bottom: 12px;
  border: 1px solid #9fa8da;
}
</style>
