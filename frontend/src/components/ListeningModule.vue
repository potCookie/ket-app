<script setup>
import { ref, onMounted } from 'vue'
import { useTaskStore } from '../stores/task.js'

const props = defineProps({
  reviewMode: { type: Boolean, default: false },
  prevAnswers: { type: Object, default: null }
})

const emit = defineEmits(['done'])
const taskStore = useTaskStore()
const listening = taskStore.todayData?.listening

const playing = ref({})
const showTranslation = ref({})
const showAudioText = ref({})
const answers = ref({})
const submitted = ref(props.reviewMode)
const score = ref(0)
const audioError = ref('')

// In review mode, pre-fill answers from previous attempt
onMounted(() => {
  if (props.reviewMode && props.prevAnswers) {
    Object.entries(props.prevAnswers).forEach(([key, val]) => {
      answers.value[parseInt(key)] = parseInt(val)
    })
    // Calculate score
    score.value = listening.questions.reduce((acc, q, i) =>
      acc + (answers.value[i] === letterToIndex(q.answer) ? 1 : 0), 0)
  }
})

// Play locally generated WAV audio via backend
let currentAudio = null
// Cache: text -> url to avoid repeated hash computation
const urlCache = {}

async function getAudioUrl(text) {
  if (urlCache[text]) return urlCache[text]
  // Same SHA-256 as backend AudioService
  const hash = await sha256(text)
  const url = `/api/audio/${hash}.wav`
  urlCache[text] = url
  return url
}

async function sha256(text) {
  const encoder = new TextEncoder()
  const data = encoder.encode(text)
  const hashBuffer = await crypto.subtle.digest('SHA-256', data)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(b => b.toString(16).padStart(2, '0')).join('')
}

async function togglePlay(qId) {
  if (!listening?.questions) return
  const q = listening.questions[qId]

  // STOP: user clicked playing item to stop it
  if (playing.value[qId]) {
    if (currentAudio) {
      currentAudio.pause()
      currentAudio = null
    }
    playing.value[qId] = false
    return
  }

  // Stop any currently playing audio
  if (currentAudio) {
    currentAudio.pause()
    currentAudio = null
    for (const k of Object.keys(playing.value)) {
      playing.value[k] = false
    }
  }

  const text = q.audio_text || q.scenario

  try {
    const url = await getAudioUrl(text)
    const audio = new Audio()
    audio.src = url

    playing.value[qId] = true
    currentAudio = audio

    audio.onended = () => {
      playing.value[qId] = false
      if (currentAudio === audio) currentAudio = null
    }
    audio.onerror = async () => {
      // File may not exist yet - trigger generation and retry
      const resp = await fetch(`/api/audio/generate?text=${encodeURIComponent(text)}`)
      const result = await resp.json()
      if (result.url) {
        audio.src = result.url
        audio.play().catch(() => {
          playing.value[qId] = false
          if (currentAudio === audio) currentAudio = null
          audioError.value = '语音播放失败，请重试'
          setTimeout(() => { audioError.value = '' }, 3000)
        })
      } else {
        playing.value[qId] = false
        if (currentAudio === audio) currentAudio = null
        audioError.value = '语音播放失败，请重试'
        setTimeout(() => { audioError.value = '' }, 3000)
      }
    }

    audio.play().catch(() => {
      playing.value[qId] = false
      if (currentAudio === audio) currentAudio = null
      audioError.value = '语音播放失败，请重试'
      setTimeout(() => { audioError.value = '' }, 3000)
    })
  } catch (e) {
    audioError.value = '语音播放失败'
    setTimeout(() => { audioError.value = '' }, 3000)
  }
}

function toggleTranslation(qId) {
  showTranslation.value[qId] = !showTranslation.value[qId]
}

function toggleAudioText(qId) {
  showAudioText.value[qId] = !showAudioText.value[qId]
}

function selectOption(qIndex, optIndex) {
  if (submitted.value || props.reviewMode) return
  answers.value[qIndex] = optIndex
}

function letterToIndex(letter) {
  return letter ? letter.charCodeAt(0) - 65 : -1
}

function submitAnswers() {
  if (!listening?.questions) return
  submitted.value = true
  score.value = listening.questions.reduce((acc, q, i) => {
    return acc + (answers.value[i] === letterToIndex(q.answer) ? 1 : 0)
  }, 0)
  emit('done', { score: score.value, total: listening.questions.length, correct: score.value === listening.questions.length, answers: answers.value })
}

function isSelected(qIndex, optIndex) {
  return answers.value[qIndex] === optIndex
}

function isCorrect(qIndex, optIndex) {
  if (!submitted.value) return false
  return letterToIndex(listening?.questions?.[qIndex]?.answer) === optIndex
}

function isWrong(qIndex, optIndex) {
  if (!submitted.value) return false
  return isSelected(qIndex, optIndex) && !isCorrect(qIndex, optIndex)
}

const allAnswered = ref(false)

function handleSelect(qIndex, optIndex) {
  selectOption(qIndex, optIndex)
  if (listening?.questions) {
    allAnswered.value = listening.questions.every((_, i) => answers.value[i] !== undefined)
  }
}
</script>

<template>
  <div>
    <!-- 播放错误提示 -->
    <div v-if="audioError" class="audio-tip error">
      ⚠️ {{ audioError }}
    </div>

    <div
      v-for="(q, qi) in listening?.questions || []"
      :key="qi"
      class="listening-block"
    >
      <!-- 音频播放器 -->
      <div class="audio-player">
        <button class="play-btn" @click="togglePlay(qi)">
          {{ playing[qi] ? '⏸' : '▶' }}
        </button>
        <div class="audio-info">
          <div class="audio-title">🎧 听力第{{ qi + 1 }}题</div>
          <div class="audio-scenario">{{ q.scenario }}</div>
          <div v-if="playing[qi]" class="audio-wave">
            <span v-for="n in 5" :key="n" class="wave-bar"></span>
          </div>
        </div>
      </div>

      <!-- 题干翻译 -->
      <button class="trans-btn" @click="toggleTranslation(qi)">
        🌐 {{ showTranslation[qi] ? '隐藏翻译' : '查看中文翻译' }}
      </button>
      <div v-if="showTranslation[qi]" class="trans-text">
        {{ q.translation }}
      </div>

      <!-- 听力原文 -->
      <div v-if="showAudioText[qi]" class="trans-text" style="background: linear-gradient(135deg, #fff8e1, #fffde7); border-color: #ffe082; color: #f57f17;">
        📢 听力原文：{{ q.audio_text || q.scenario }}
      </div>
      <button
        class="trans-btn"
        @click="toggleAudioText(qi)"
        style="margin-left:8px; background: linear-gradient(135deg, #fff3e0, #ffe0b2); color: #e65100;"
      >
        📝 {{ showAudioText[qi] ? '隐藏原文' : '显示听力原文' }}
      </button>

      <!-- 选项 -->
      <div class="quiz-opts">
        <div
          v-for="(opt, oi) in q.options"
          :key="oi"
          class="quiz-opt"
          :class="{
            selected: isSelected(qi, oi),
            correct: isCorrect(qi, oi),
            wrong: isWrong(qi, oi)
          }"
          @click="handleSelect(qi, oi)"
        >
          <span class="opt-letter">{{ ['A','B','C','D'][oi] }}</span>
          <span class="opt-text">{{ opt.replace(/^[A-D]\.\s*/, '') }}</span>
          <span v-if="isCorrect(qi, oi)" class="opt-mark">✓</span>
          <span v-if="isWrong(qi, oi)" class="opt-mark">✗</span>
        </div>
      </div>
    </div>

    <!-- 提交按钮 -->
    <button
      v-if="!props.reviewMode"
      class="submit-btn"
      :disabled="!allAnswered"
      @click="submitAnswers"
    >
      {{ submitted ? '已完成 ✓' : '提交答案' }}
    </button>

    <!-- 得分 -->
    <div v-if="submitted" class="score-banner" :class="score === (listening?.questions?.length || 0) ? 'perfect' : ''">
      🎯 {{ props.reviewMode ? '上次' : '' }}得分：{{ score }} / {{ listening?.questions?.length || 0 }}
      <template v-if="score === (listening?.questions?.length || 0)"> 🎉 满分！</template>
    </div>
  </div>
</template>

<style scoped>
.listening-block {
  margin-bottom: 20px;
}

.audio-tip {
  background: #fff8e1;
  border: 1px solid #ffe082;
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 13px;
  color: #f57f17;
  margin-bottom: 16px;
  line-height: 1.6;
}

.audio-tip.error {
  background: #ffebee;
  border-color: #ef9a9a;
  color: #c62828;
}

.audio-player {
  background: linear-gradient(135deg, #e3f2fd, #bbdefb);
  border-radius: 14px;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 10px;
}

.play-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #42a5f5, #1e88e5);
  color: white;
  border: none;
  font-size: 18px;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 3px 10px rgba(66, 165, 245, 0.3);
}

.play-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 5px 15px rgba(66, 165, 245, 0.4);
}

.audio-info {
  flex: 1;
}

.audio-title {
  font-size: 11px;
  color: #1565c0;
  font-weight: 600;
  margin-bottom: 2px;
}

.audio-scenario {
  font-size: 14px;
  color: #1a237e;
  font-weight: 600;
}

.audio-wave {
  display: flex;
  gap: 3px;
  margin-top: 8px;
  height: 20px;
  align-items: center;
}

.wave-bar {
  width: 3px;
  border-radius: 2px;
  background: #42a5f5;
  animation: wave 0.8s ease-in-out infinite;
  animation-delay: calc(var(--i, 0) * 0.12s);
}

.wave-bar:nth-child(2) { height: 16px; }
.wave-bar:nth-child(3) { height: 20px; animation-delay: 0.15s; }
.wave-bar:nth-child(4) { height: 14px; animation-delay: 0.3s; }
.wave-bar:nth-child(5) { height: 18px; animation-delay: 0.45s; }
.wave-bar:nth-child(1) { height: 10px; }

@keyframes wave {
  0%, 100% { transform: scaleY(0.4); opacity: 0.4; }
  50% { transform: scaleY(1); opacity: 1; }
}

.trans-btn {
  background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
  color: #00695c;
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  margin-bottom: 10px;
}

.trans-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 105, 92, 0.15);
}

.trans-text {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border-radius: 10px;
  padding: 12px;
  font-size: 13px;
  color: #33691e;
  margin-bottom: 10px;
  line-height: 1.6;
  border: 1px solid #c8e6c9;
}

.quiz-opts {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quiz-opt {
  border: 2px solid #e8e8e8;
  border-radius: 14px;
  padding: 13px 16px;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all 0.2s;
  background: white;
  font-weight: 500;
}

.quiz-opt:hover {
  border-color: #b2dfdb;
  background: #f1f8e9;
}

.quiz-opt.selected {
  border-color: #4ECDC4;
  background: linear-gradient(135deg, #e0f7fa, #f1f8e9);
  box-shadow: 0 2px 8px rgba(78, 205, 196, 0.15);
}

.quiz-opt.correct {
  border-color: #66bb6a;
  background: #e8f5e9;
}

.quiz-opt.wrong {
  border-color: #ef5350;
  background: #ffebee;
}

.opt-letter {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: #90a4ae;
  flex-shrink: 0;
  transition: all 0.2s;
}

.quiz-opt.selected .opt-letter {
  background: #4ECDC4;
  color: white;
  box-shadow: 0 2px 6px rgba(78, 205, 196, 0.4);
}

.quiz-opt.correct .opt-letter {
  background: #66bb6a;
  color: white;
}

.quiz-opt.wrong .opt-letter {
  background: #ef5350;
  color: white;
}

.opt-mark {
  margin-left: auto;
  font-weight: 700;
  font-size: 16px;
}

.quiz-opt.correct .opt-mark { color: #66bb6a; }
.quiz-opt.wrong .opt-mark { color: #ef5350; }

.submit-btn {
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
  margin-top: 16px;
}

.submit-btn:disabled {
  background: linear-gradient(135deg, #e0e0e0, #bdbdbd);
  color: #b0bec5;
  cursor: default;
  box-shadow: none;
}

.score-banner {
  margin-top: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffe0b2;
}

.score-banner.perfect {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #c8e6c9;
}
</style>
