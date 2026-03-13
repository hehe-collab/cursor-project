<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>代理商等级</span>
        <el-button type="primary">新增等级</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column prop="name" label="等级名称" />
      <el-table-column prop="rate" label="分成比例(%)" width="120" />
      <el-table-column prop="min_recharge" label="最低充值" width="100" />
      <el-table-column prop="sort" label="排序" width="80" />
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
