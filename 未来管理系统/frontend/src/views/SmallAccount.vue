<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>小号管理</span>
        <el-button type="primary">新增小号</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column prop="nickname" label="昵称" />
      <el-table-column prop="avatar" label="头像" width="80">
        <template #default="{ row }">
          <el-avatar v-if="row.avatar" :src="row.avatar" :size="36" />
        </template>
      </el-table-column>
      <el-table-column prop="platform" label="平台" width="100" />
      <el-table-column prop="status" label="状态" width="80" />
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
