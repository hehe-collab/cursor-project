<template>
  <el-card>
    <template #header><span>抖音投放链接</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="name" label="链接名称" />
      <el-table-column prop="url" label="投放链接" />
      <el-table-column prop="agent" label="所属代理商" width="120" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary">编辑</el-button>
          <el-button link type="primary">复制链接</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
