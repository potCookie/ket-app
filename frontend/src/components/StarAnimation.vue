<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  stars: { type: Number, default: 1 },
})

const emit = defineEmits(['done'])

const show = ref(false)

watch(() => props.visible, (val) => {
  if (val) {
    show.value = true
    setTimeout(() => {
      show.value = false
      emit('done')
    }, 2000)
  }
})

// Burst positions for 8 stars (hardcoded circular)
const bursts = [
  { x: 0, y: -36 },
  { x: 26, y: -26 },
  { x: 36, y: 0 },
  { x: 26, y: 26 },
  { x: 0, y: 36 },
  { x: -26, y: 26 },
  { x: -36, y: 0 },
  { x: -26, y: -26 },
]
</script>

<template>
  <Transition name="star">
    <div v-if="show" class="star-overlay">
      <div class="star-container">
        <div class="big-star">⭐</div>
        <div class="star-text">+{{ stars }}</div>
        <div class="star-burst">
          <span
            v-for="(b, i) in bursts"
            :key="i"
            class="burst-star"
            :style="{ animationDelay: `${i * 0.06}s`, '--bx': `${b.x}px`, '--by': `${b.y}px` }"
          >🌟</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.star-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  pointer-events: none;
}

.star-container {
  position: relative;
  text-align: center;
}

.big-star {
  font-size: 72px;
  animation: star-scale 0.6s ease forwards;
}

@keyframes star-scale {
  0% { transform: scale(0) rotate(-20deg); opacity: 0; }
  40% { transform: scale(1.3) rotate(10deg); opacity: 1; }
  100% { transform: scale(1) rotate(0deg); opacity: 1; }
}

.star-text {
  font-size: 32px;
  font-weight: 900;
  color: #FF6B6B;
  margin-top: 8px;
  animation: text-pop 1.2s ease forwards;
}

@keyframes text-pop {
  0% { transform: translateY(20px) scale(0.5); opacity: 0; }
  30% { transform: translateY(0) scale(1.2); opacity: 1; }
  100% { transform: translateY(0) scale(1); opacity: 1; }
}

.star-burst {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.burst-star {
  position: absolute;
  top: 50%;
  left: 50%;
  font-size: 20px;
  animation: burst 0.8s ease-out forwards;
}

@keyframes burst {
  0% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(0);
  }
  40% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1.3);
  }
  100% {
    opacity: 0;
    transform: translate(
      calc(-50% + var(--bx, 0px)),
      calc(-50% + var(--by, 0px))
    ) scale(0.4);
  }
}

/* star transition */
.star-enter-active { transition: opacity 0.3s; }
.star-leave-active { transition: opacity 0.5s; }
.star-enter-from,
.star-leave-to { opacity: 0; }
</style>
