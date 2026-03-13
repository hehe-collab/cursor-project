<template>
  <el-card>
    <template #header><span>历史数据记录</span></template>
    <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="margin-bottom:16px" />
    <el-table :data="historyList" stripe>
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="play" label="播放量" />
      <el-table-column prop="user" label="新增用户" />
      <el-table-column prop="recharge" label="充值金额" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const dateRange = ref([])
const historyList = ref([])
</script>
