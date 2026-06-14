<script setup>
import { ref, onMounted } from 'vue'
import { useTaskStore } from '../stores/task.js'

const props = defineProps({
  reviewMode: { type: Boolean, default: false },
  prevAnswers: { type: Object, default: null }
})

const emit = defineEmits(['done'])
const taskStore = useTaskStore()
const grammar = taskStore.todayData.grammar

const answers = ref({})
const submitted = ref(props.reviewMode)
const score = ref(0)

// In review mode, pre-fill answers from previous attempt
onMounted(() => {
  if (props.reviewMode && props.prevAnswers) {
    Object.entries(props.prevAnswers).forEach(([key, val]) => {
      answers.value[parseInt(key)] = String(val)  // grammar answers are strings
    })
    score.value = grammar.exercises.reduce((acc, _, i) =>
      acc + (String(answers.value[i] || '') === String(grammar.answers[i]) ? 1 : 0), 0)
  }
})

function selectAnswer(qIndex, answer) {
  if (submitted.value || props.reviewMode) return
  answers.value[qIndex] = answer
}

function submitAnswers() {
  submitted.value = true
  score.value = grammar.exercises.reduce((acc, _, i) =>
    acc + (answers.value[i] === grammar.answers[i] ? 1 : 0), 0)
  emit('done', { score: score.value, total: grammar.exercises.length, correct: score.value === grammar.exercises.length, answers: answers.value })
}

function isCorrect(qIndex) {
  if (!submitted.value) return null
  return answers.value[qIndex] === grammar.answers[qIndex]
}

const allAnswered = ref(false)
function checkAll() {
  allAnswered.value = grammar.exercises.every((_, i) => answers.value[i] !== undefined)
}
</script>

<template>
  <div>
    <!-- 语法讲解 -->
    <div class="grammar-card">
      <div class="grammar-point">📖 {{ grammar.point }}</div>
      <div class="grammar-explanation">{{ grammar.explanation }}</div>
    </div>

    <!-- 练习题 -->
    <div class="exercises-section">
      <div class="section-label">✏️ 练习题</div>
      <div
        v-for="(ex, ei) in grammar.exercises"
        :key="ei"
        class="exercise-item"
      >
        <div class="exercise-sentence">
          <span class="ex-num">{{ ei + 1 }}.</span>
          {{ ex }}
        </div>

        <!-- 填空题输入 -->
        <div class="fill-input-wrap">
          <input
            v-model="answers[ei]"
            type="text"
            class="fill-input"
            :class="{
              correct: submitted && answers[ei] === grammar.answers[ei],
              wrong: submitted && answers[ei] && answers[ei] !== grammar.answers[ei]
            }"
            placeholder="请输入答案..."
            :disabled="submitted"
            @input="checkAll"
          />
          <span
            v-if="submitted && answers[ei] && answers[ei] !== grammar.answers[ei]"
            class="correct-answer"
          >
            正确答案：{{ grammar.answers[ei] }}
          </span>
          <span
            v-if="submitted && answers[ei] === grammar.answers[ei]"
            class="check-mark"
          >
            ✓ 正确
          </span>
        </div>
      </div>
    </div>

    <!-- Review mode badge -->
    <div v-if="props.reviewMode" class="review-badge">
      📋 历史作答记录
    </div>

    <!-- 提交按钮 -->
    <button
      v-if="!props.reviewMode"
      class="submit-btn"
      :disabled="!allAnswered"
      @click="submitAnswers"
    >
      {{ submitted ? '已完成 ✓' : '提交检查' }}
    </button>

    <!-- 得分 -->
    <div v-if="submitted" class="score-banner" :class="score === grammar.exercises.length ? 'perfect' : ''">
      🎯 {{ props.reviewMode ? '上次' : '' }}得分：{{ score }} / {{ grammar.exercises.length }}
      <template v-if="score === grammar.exercises.length"> 🎉 全对！</template>
    </div>
  </div>
</template>

<style scoped>
.grammar-card {
  background: linear-gradient(135deg, #e8eaf6, #c5cae9);
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 18px;
  border: 2px solid #b0bec5;
}

.grammar-point {
  font-size: 16px;
  font-weight: 700;
  color: #1a237e;
  margin-bottom: 12px;
}

.grammar-explanation {
  font-size: 14px;
  color: #37474f;
  line-height: 1.8;
  white-space: pre-line;
}

.exercises-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 700;
  color: #37474f;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.exercise-item {
  background: white;
  border-radius: 14px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f0f0f0;
}

.exercise-sentence {
  font-size: 16px;
  color: #1a237e;
  font-weight: 600;
  margin-bottom: 10px;
  line-height: 1.6;
}

.ex-num {
  color: #4ECDC4;
  font-weight: 800;
  margin-right: 4px;
}

.fill-input-wrap {
  position: relative;
}

.fill-input {
  width: 100%;
  padding: 12px 16px;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  font-size: 15px;
  color: #37474f;
  font-weight: 600;
  outline: none;
  transition: all 0.2s;
  background: #fafafa;
}

.fill-input:focus {
  border-color: #4ECDC4;
  background: white;
}

.fill-input.correct {
  border-color: #66bb6a;
  background: #e8f5e9;
}

.fill-input.wrong {
  border-color: #ef5350;
  background: #ffebee;
}

.correct-answer {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #ef5350;
  font-weight: 500;
}

.check-mark {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  color: #66bb6a;
  font-weight: 600;
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
