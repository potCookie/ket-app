<script setup>
import { computed } from 'vue'
import { useTaskStore } from '../stores/task.js'

const taskStore = useTaskStore()

const steps = computed(() =>
  taskStore.stepOrder.map((step, i) => ({
    key: step,
    label: taskStore.stepLabels[step],
    icon: taskStore.stepIcons[step],
    index: i,
    isDone: taskStore.isStepDone(step),
    isActive: taskStore.currentStep === step,
  }))
)
</script>

<template>
  <div class="progress-bar card">
    <template v-for="(step, i) in steps" :key="step.key">
      <div
        class="progress-step"
        :class="{ active: step.isActive, done: step.isDone }"
      >
        <div class="step-dot">
          <template v-if="step.isDone">✓</template>
          <template v-else>{{ i + 1 }}</template>
        </div>
        <div class="step-label">{{ step.label }}</div>
      </div>
      <div
        v-if="i < steps.length - 1"
        class="progress-line"
        :class="{ filled: step.isDone }"
      ></div>
    </template>
  </div>
</template>

<style scoped>
.progress-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 10px;
  border-radius: 14px;
  margin-bottom: 12px;
  background: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.progress-step {
  text-align: center;
  font-size: 10px;
  color: #cfd8dc;
  transition: all 0.3s;
  flex-shrink: 0;
}

.progress-step.active {
  color: #4ECDC4;
  font-weight: 700;
}

.progress-step.done {
  color: #66bb6a;
  font-weight: 600;
}

.step-dot {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 5px;
  font-size: 12px;
  border: 2px solid #e0e0e0;
  transition: all 0.3s;
  font-weight: 600;
}

.progress-step.active .step-dot {
  background: linear-gradient(135deg, #4ECDC4, #26a69a);
  color: white;
  border-color: #4ECDC4;
  box-shadow: 0 2px 8px rgba(78, 205, 196, 0.4);
  transform: scale(1.15);
}

.progress-step.done .step-dot {
  background: linear-gradient(135deg, #a5d6a7, #66bb6a);
  color: white;
  border-color: #a5d6a7;
}

.step-label {
  font-size: 10px;
  white-space: nowrap;
}

.progress-line {
  flex: 1;
  height: 3px;
  background: #e8e8e8;
  margin: 0 2px;
  border-radius: 2px;
  position: relative;
  top: -11px;
  transition: all 0.3s;
  min-width: 8px;
}

.progress-line.filled {
  background: linear-gradient(90deg, #a5d6a7, #66bb6a);
}
</style>
