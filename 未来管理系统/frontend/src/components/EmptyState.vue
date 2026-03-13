<template>
  <el-empty :description="description" :image-size="80" />
</template>

<script setup>
defineProps({
  description: { type: String, default: '暂无数据' },
})
</script>
