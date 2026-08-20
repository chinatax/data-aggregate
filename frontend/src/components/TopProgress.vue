<template>
  <div v-if="active" class="top-progress" :class="{ instant }">
    <div class="tp-bar"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'

const props = withDefaults(defineProps<{
  loading: boolean
  instant?: boolean
}>(), {
  instant: false,
})

const active = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

function clearTimer() {
  if (timer) { clearTimeout(timer); timer = null }
}

watch(() => props.loading, (v) => {
  clearTimer()
  if (v) {
    active.value = true
  } else {
    // Small delay to let the bar finish its animation
    timer = setTimeout(() => {
      active.value = false
    }, 400)
  }
})

onUnmounted(() => {
  clearTimer()
})
</script>

<style scoped>
.top-progress {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 3px;
  z-index: 99998;
  pointer-events: none;
  overflow: hidden;

  &.instant .tp-bar {
    transition: none;
  }
}

.tp-bar {
  height: 100%;
  width: 0%;
  background: linear-gradient(90deg, #7dd3fc, #38BDF8, #0284C7);
  background-size: 200% 100%;
  border-radius: 0 2px 2px 0;
  animation:
    tp-fill 1.6s ease-in-out infinite,
    tp-shimmer 2s linear infinite;
}

@keyframes tp-fill {
  0%   { width: 0%; margin-left: 0; }
  30%  { width: 40%; margin-left: 0; }
  70%  { width: 60%; margin-left: 30%; }
  100% { width: 100%; margin-left: 100%; }
}

@keyframes tp-shimmer {
  0%   { background-position: 0% 0%; }
  100% { background-position: 200% 0%; }
}
</style>
