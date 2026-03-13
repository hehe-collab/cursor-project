<template>
  <el-card>
    <template #header><span>用户数据</span></template>
    <el-row :gutter="20" class="stats">
      <el-col :span="6"><el-statistic title="总用户数" :value="stats.total" /></el-col>
      <el-col :span="6"><el-statistic title="今日新增" :value="stats.today" /></el-col>
      <el-col :span="6"><el-statistic title="活跃用户" :value="stats.active" /></el-col>
      <el-col :span="6"><el-statistic title="付费用户" :value="stats.paid" /></el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
const stats = reactive({ total: 0, today: 0, active: 0, paid: 0 })
</script>
