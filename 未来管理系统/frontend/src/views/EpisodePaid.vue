<template>
  <el-card>
    <template #header><span>已支付剧集</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="user" label="用户" />
      <el-table-column prop="drama" label="剧名" />
      <el-table-column prop="episode" label="集数" width="80" />
      <el-table-column prop="amount" label="支付金额" />
      <el-table-column prop="pay_time" label="支付时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
