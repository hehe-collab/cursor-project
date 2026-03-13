<template>
  <div class="table-skeleton">
    <div v-for="i in rows" :key="i" class="skeleton-row">
      <div v-for="j in cols" :key="j" class="skeleton-cell" :style="{ width: colWidths[j - 1] || '100px' }" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 8 },
  cols: { type: Number, default: 6 },
  colWidths: { type: Array, default: () => [] },
})
</script>

<style scoped>
.table-skeleton { padding: 16px 0; }
.skeleton-row {
  display: flex;
  gap: 16px;
  padding: 14px 0;
  border-bottom: 1px solid #ebeef5;
}
.skeleton-cell {
  height: 20px;
  background: linear-gradient(90deg, #f0f2f5 25%, #e8eaed 50%, #f0f2f5 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}
@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
