<template>
  <el-card>
    <template #header><span>昨日数据统计</span></template>
    <el-descriptions :column="2" border>
      <el-descriptions-item label="昨日播放量">0</el-descriptions-item>
      <el-descriptions-item label="昨日新增用户">0</el-descriptions-item>
      <el-descriptions-item label="昨日充值金额">¥0</el-descriptions-item>
      <el-descriptions-item label="昨日订单数">0</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>
