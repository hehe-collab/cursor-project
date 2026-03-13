<template>
  <el-card>
    <template #header><span>菜单管理</span></template>
    <p class="tip">管理前台展示的导航菜单，支持排序、启用/禁用。</p>
    <el-table :data="list" stripe row-key="id" default-expand-all>
      <el-table-column prop="name" label="菜单名称" />
      <el-table-column prop="path" label="路径" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80" />
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
