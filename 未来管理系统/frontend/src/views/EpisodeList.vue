<template>
  <el-card>
    <template #header><span>剧集列表</span></template>
    <p class="tip">展示所有剧集的集数列表，支持按剧名、集数筛选。</p>
    <el-table :data="list" stripe>
      <el-table-column prop="drama_title" label="剧名" />
      <el-table-column prop="episode_num" label="集数" width="80" />
      <el-table-column prop="title" label="集标题" />
      <el-table-column prop="status" label="状态" width="100" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>

<style scoped>
.tip { color: #909399; margin-bottom: 16px; }
</style>
