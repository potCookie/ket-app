<script setup>
import { ref, computed } from 'vue'
import { useTaskStore } from '../stores/task.js'

const emit = defineEmits(['done'])
const taskStore = useTaskStore()
const words = taskStore.todayData.vocab.words

const currentIdx = ref(0)
const flipped = ref({})

function toggleFlip(idx) {
  flipped.value[idx] = !flipped.value[idx]
}

function prevWord() {
  if (currentIdx.value > 0) currentIdx.value--
}

function nextWord() {
  if (currentIdx.value < words.length - 1) {
    currentIdx.value++
  } else {
    emit('done', { score: null, total: null })
  }
}

const word = computed(() => words[currentIdx.value])
const show = computed(() => flipped.value[currentIdx.value])
</script>

<template>
  <div>
    <div class="flip-hint">👆 点击卡片查看中文释义</div>

    <div class="card" @click="toggleFlip(currentIdx)">
      <!-- 英文面 -->
      <div class="card-front" :class="{ hide: show }">
        <div class="card-en">{{ word.en }}</div>
      </div>
      <!-- 中文面 -->
      <div class="card-back" :class="{ show: show }">
        <div class="card-zh">{{ word.zh }}</div>
        <div class="card-eg">📝 {{ word.eg }}</div>
      </div>
    </div>

    <div class="nav">
      <button class="nav-btn" :disabled="currentIdx === 0" @click="prevWord">← 上一个</button>
      <span class="nav-count">{{ currentIdx + 1 }} / {{ words.length }}</span>
      <button class="nav-btn" @click="nextWord">
        {{ currentIdx === words.length - 1 ? '✔ 完成' : '下一个 →' }}
      </button>
    </div>

    <div class="dots">
      <span v-for="(_, i) in words" :key="i"
        class="dot" :class="{ active: i === currentIdx, seen: flipped[i] }"></span>
    </div>
  </div>
</template>

<style scoped>
.flip-hint {
  text-align: center; font-size: 11px; color: #b0bec5; margin-bottom: 12px;
}

.card {
  position: relative;
  min-height: 160px;
  border-radius: 16px;
  background: linear-gradient(180deg, #fafafa, white);
  border: 1px solid #f0f0f0;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05);
  cursor: pointer;
  margin-bottom: 16px;
  display: flex; align-items: center; justify-content: center;
  overflow: hidden;
}

.card-front,
.card-back {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: opacity 0.3s, transform 0.3s;
}

.card-front {
  opacity: 1;
  transform: scale(1);
}
.card-front.hide {
  opacity: 0;
  transform: scale(0.9);
  pointer-events: none;
}

.card-back {
  opacity: 0;
  transform: scale(1.1);
  pointer-events: none;
  background: linear-gradient(180deg, #e0f7fa, #f1f8e9);
}
.card-back.show {
  opacity: 1;
  transform: scale(1);
  pointer-events: auto;
}

.card-en {
  font-size: 32px; font-weight: 800; color: #1a237e;
  padding: 24px 18px; text-align: center;
}
.card-zh {
  font-size: 22px; color: #00897b; font-weight: 600;
  padding: 0 18px 6px; text-align: center;
}
.card-eg {
  font-size: 13px; color: #78909c;
  padding: 6px 18px 14px; text-align: center;
}

.nav {
  display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px;
}
.nav-btn {
  background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
  color: #00695c; border: none; border-radius: 10px;
  padding: 10px 18px; font-size: 13px; font-weight: 600; cursor: pointer;
}
.nav-btn:hover:not(:disabled) { transform: translateY(-1px); }
.nav-btn:disabled { opacity: 0.4; cursor: default; }
.nav-count { font-size: 13px; color: #78909c; font-weight: 600; }

.dots { display: flex; justify-content: center; gap: 8px; }
.dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #e8e8e8; transition: all 0.3s;
}
.dot.active { background: #4ECDC4; transform: scale(1.3); }
.dot.seen { background: #a5d6a7; }
</style>
