<template>
  <el-card>
    <template #header><span>投放面板</span></template>
    <el-row :gutter="20">
      <el-col :span="6"><el-statistic title="今日投放" :value="0" /></el-col>
      <el-col :span="6"><el-statistic title="今日转化" :value="0" /></el-col>
      <el-col :span="6"><el-statistic title="总投放" :value="0" /></el-col>
      <el-col :span="6"><el-statistic title="总转化" :value="0" /></el-col>
    </el-row>
    <el-divider />
    <el-table :data="list" stripe>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="click" label="点击量" />
      <el-table-column prop="convert" label="转化数" />
      <el-table-column prop="cost" label="投放成本" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
