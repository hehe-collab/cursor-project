<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>资讯管理</span>
        <el-button type="primary">新增资讯</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column prop="create_time" label="发布时间" width="180" />
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary">编辑</el-button>
          <el-button link type="danger">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
