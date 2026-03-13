<template>
  <el-card>
    <template #header><span>已解锁剧集订单</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="order_no" label="订单号" />
      <el-table-column prop="user" label="用户" />
      <el-table-column prop="drama" label="剧名" />
      <el-table-column prop="episodes" label="解锁集数" />
      <el-table-column prop="bean" label="消耗金豆" width="100" />
      <el-table-column prop="create_time" label="解锁时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
