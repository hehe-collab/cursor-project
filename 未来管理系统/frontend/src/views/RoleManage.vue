<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>角色管理</span>
        <el-button type="primary">新增角色</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column prop="name" label="角色名称" />
      <el-table-column prop="desc" label="描述" />
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
