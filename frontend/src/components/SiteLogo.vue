<template>
  <div
    :style="{ width: size + 'px', height: size + 'px' }"
    :class="['site-logo', { pulse }]"
  ></div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  size?: number
  color?: string
  pulse?: boolean
}>(), {
  size: 28,
  color: '#38BDF8',
  pulse: true,
})
</script>

<style scoped>
.site-logo {
  position: relative;
  flex-shrink: 0;
}

.site-logo::before,
.site-logo::after {
  content: '';
  position: absolute;
}

/* Outer square */
.site-logo::before {
  width: 58%;
  height: 58%;
  top: 0;
  left: 0;
  border: 2.5px solid v-bind(color);
  border-radius: 5px;
}

/* Inner square — 固定在右下角，与边框不重叠 */
.site-logo::after {
  width: 30%;
  height: 30%;
  bottom: 0;
  right: 0;
  background: v-bind(color);
  border-radius: 2.5px;
  opacity: 0.6;
}

/* Pulse animation */
.site-logo.pulse::after {
  animation: logo-pulse 1.6s ease-in-out infinite;
}

@keyframes logo-pulse {
  0%, 100% { opacity: 0.35; transform: scale(1); }
  50% { opacity: 0.85; transform: scale(1.25); }
}
</style>