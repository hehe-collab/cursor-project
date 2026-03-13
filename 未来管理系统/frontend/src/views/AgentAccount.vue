<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>代理商账号</span>
        <el-button type="primary">新增代理商</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column prop="username" label="账号" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="level" label="等级" width="80" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column prop="create_time" label="创建时间" width="180" />
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
