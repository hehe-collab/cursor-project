<template>
  <div class="skeleton-table" role="status" aria-label="加载中">
    <div v-for="i in rows" :key="i" class="skeleton-row">
      <div v-for="j in columns" :key="j" class="skeleton-cell">
        <div class="skeleton-block" />
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  rows: { type: Number, default: 6 },
  columns: { type: Number, default: 6 },
})
</script>

<style scoped>
.skeleton-table {
  width: 100%;
  padding: 8px 0;
}

.skeleton-row {
  display: flex;
  padding: 12px 0;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.skeleton-cell {
  flex: 1;
  padding: 0 10px;
}

.skeleton-block {
  height: 20px;
  background: linear-gradient(90deg, #f2f2f2 25%, #e8e8e8 37%, #f2f2f2 63%);
  background-size: 400% 100%;
  animation: skeleton-shine 1.2s ease-in-out infinite;
  border-radius: 4px;
}

@keyframes skeleton-shine {
  0% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0 50%;
  }
}
</style>
