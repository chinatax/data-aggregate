<template>
  <slot v-if="visible" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'

const props = withDefaults(defineProps<{
  author?: string
  role?: string
}>(), {})

const authStore = useAuthStore()

const visible = computed(() => {
  if (!authStore.isLoggedIn) return false
  if (props.role && authStore.role !== props.role) return false
  if (props.author && authStore.username !== props.author) return false
  return true
})
</script>
