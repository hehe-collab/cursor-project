<template>
  <el-card>
    <template #header><span>财务对账</span></template>
    <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="margin-bottom:16px" />
    <el-descriptions :column="2" border>
      <el-descriptions-item label="充值总额">¥0</el-descriptions-item>
      <el-descriptions-item label="订单总额">¥0</el-descriptions-item>
      <el-descriptions-item label="平台收入">¥0</el-descriptions-item>
      <el-descriptions-item label="代理商分成">¥0</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const dateRange = ref([])
</script>
