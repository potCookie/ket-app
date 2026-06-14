<script setup>
import { ref, onMounted } from 'vue'
import { useTaskStore } from '../stores/task.js'

const props = defineProps({
  reviewMode: { type: Boolean, default: false },
  prevAnswers: { type: Object, default: null }
})

const emit = defineEmits(['done'])
const taskStore = useTaskStore()
const reading = taskStore.todayData.reading

const showTranslation = ref(false)
const answers = ref({})
const submitted = ref(props.reviewMode)
const allCorrect = ref(false)

// In review mode, pre-fill answers from previous attempt
onMounted(() => {
  if (props.reviewMode && props.prevAnswers) {
    Object.entries(props.prevAnswers).forEach(([key, val]) => {
      answers.value[parseInt(key)] = parseInt(val)
    })
    // Calculate score from prev answers
    const score = reading.questions.reduce((acc, q, i) =>
      acc + (answers.value[i] === letterToIndex(q.answer) ? 1 : 0), 0)
    allCorrect.value = score === reading.questions.length
  }
})

function toggleTranslation() {
  showTranslation.value = !showTranslation.value
}

function selectOption(qIndex, optIndex) {
  if (submitted.value || props.reviewMode) return
  answers.value[qIndex] = optIndex
}

function letterToIndex(letter) {
  return letter ? letter.charCodeAt(0) - 65 : -1  // 'A' → 0, 'B' → 1, 'C' → 2
}

function submitAnswers() {
  submitted.value = true
  const score = reading.questions.reduce((acc, q, i) =>
    acc + (answers.value[i] === letterToIndex(q.answer) ? 1 : 0), 0)
  allCorrect.value = score === reading.questions.length
  emit('done', { score, total: reading.questions.length, correct: allCorrect.value, answers: answers.value })
}

function isSelected(qIndex, optIndex) {
  return answers.value[qIndex] === optIndex
}

function isCorrect(qIndex, optIndex) {
  if (!submitted.value) return false
  return letterToIndex(reading.questions[qIndex].answer) === optIndex
}

function isWrong(qIndex, optIndex) {
  if (!submitted.value) return false
  return isSelected(qIndex, optIndex) && !isCorrect(qIndex, optIndex)
}

const allAnswered = ref(false)
function checkAllAnswered() {
  allAnswered.value = reading.questions.every((_, i) => answers.value[i] !== undefined)
}

function handleSelect(qIndex, optIndex) {
  selectOption(qIndex, optIndex)
  checkAllAnswered()
}
</script>

<template>
  <div>
    <!-- 阅读短文 -->
    <div class="reading-passage">
      <div class="passage-title">{{ reading.passage.title }}</div>
      <div class="passage-text">{{ reading.passage.text }}</div>
    </div>

    <!-- 翻译按钮 -->
    <button class="trans-btn" @click="toggleTranslation">
      🌐 {{ showTranslation ? '隐藏中文翻译' : '查看中文翻译' }}
    </button>
    <div v-if="showTranslation" class="trans-text">
      {{ reading.passage.translation }}
    </div>

    <!-- 理解题 -->
    <div class="questions-section">
      <div class="quiz-header">📝 理解题</div>
      <div
        v-for="(q, qi) in reading.questions"
        :key="qi"
        class="quiz-block"
      >
        <div class="quiz-question">{{ qi + 1 }}. {{ q.q }}</div>
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
    </div>

    <!-- Review mode badge -->
    <div v-if="props.reviewMode" class="review-badge">
      📋 历史作答记录
    </div>

    <!-- 提交按钮 (隐藏于回顾模式) -->
    <button
      v-if="!props.reviewMode"
      class="submit-btn"
      :disabled="!allAnswered"
      @click="submitAnswers"
    >
      {{ submitted ? '已完成 ✓' : '提交答案' }}
    </button>

    <!-- 结果提示 -->
    <div v-if="submitted" class="result-banner" :class="{ success: allCorrect, partial: !allCorrect }">
      <template v-if="allCorrect">
        🎉 全部正确！太厉害了！
      </template>
      <template v-else>
        {{ props.reviewMode ? '📝 上次作答结果' : '📝 继续加油！看看正确答案吧~' }}
      </template>
    </div>
  </div>
</template>

<style scoped>
.reading-passage {
  background: white;
  border-radius: 14px;
  padding: 18px;
  font-size: 14px;
  line-height: 2;
  color: #37474f;
  margin-bottom: 12px;
  border: 1px solid #e8e8e8;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.03);
}

.passage-title {
  font-size: 16px;
  font-weight: 700;
  color: #1a237e;
  margin-bottom: 8px;
}

.trans-btn {
  background: linear-gradient(135deg, #e0f7fa, #b2ebf2);
  color: #00695c;
  border: none;
  border-radius: 10px;
  padding: 9px 16px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  font-weight: 500;
  box-shadow: 0 1px 4px rgba(0, 105, 92, 0.1);
}

.trans-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(0, 105, 92, 0.15);
}

.trans-text {
  background: linear-gradient(135deg, #e8f5e9, #f1f8e9);
  border-radius: 10px;
  padding: 14px;
  font-size: 13px;
  color: #33691e;
  margin-top: 10px;
  line-height: 1.8;
  border: 1px solid #c8e6c9;
}

.questions-section {
  margin-top: 18px;
}

.quiz-header {
  font-size: 14px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 12px;
}

.quiz-block {
  margin-bottom: 16px;
}

.quiz-question {
  font-size: 13px;
  color: #546e7a;
  margin-bottom: 8px;
  font-weight: 500;
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

.quiz-opt:hover:not(.no-hover) {
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

.quiz-opt.correct .opt-mark {
  color: #66bb6a;
}

.quiz-opt.wrong .opt-mark {
  color: #ef5350;
}

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
  margin-top: 8px;
}

.submit-btn:disabled {
  background: linear-gradient(135deg, #e0e0e0, #bdbdbd);
  color: #b0bec5;
  cursor: default;
  box-shadow: none;
}

.result-banner {
  margin-top: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
}

.result-banner.success {
  background: #e8f5e9;
  color: #2e7d32;
  border: 1px solid #c8e6c9;
}

.result-banner.partial {
  background: #fff3e0;
  color: #e65100;
  border: 1px solid #ffe0b2;
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
