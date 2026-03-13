<template>
  <el-card>
    <template #header><span>金豆收支明细</span></template>
    <el-table :data="list" stripe>
      <el-table-column prop="user" label="用户" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="amount" label="金豆数量" width="100" />
      <el-table-column prop="balance" label="余额" width="100" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column prop="create_time" label="时间" width="180" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
const list = ref([])
</script>
