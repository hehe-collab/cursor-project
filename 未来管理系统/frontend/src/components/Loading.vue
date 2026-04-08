<template>
  <div v-if="loading" class="loading-overlay" :class="{ fullscreen }">
    <div class="loading-spinner">
      <div class="spinner" />
      <p v-if="text" class="loading-text">{{ text }}</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  text: { type: String, default: '' },
  fullscreen: { type: Boolean, default: false },
})
</script>

<style scoped>
.loading-overlay {
  position: absolute;
  inset: 0;
  background: rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.loading-overlay.fullscreen {
  position: fixed;
  z-index: 3000;
}

.loading-spinner {
  text-align: center;
}

.spinner {
  width: 36px;
  height: 36px;
  margin: 0 auto 10px;
  border: 3px solid var(--el-border-color-lighter);
  border-top-color: var(--el-color-primary);
  border-radius: 50%;
  animation: spin 0.9s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-text {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
</style>
