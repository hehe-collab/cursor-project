<template>
  <el-card>
    <template #header>
      <span>{{ title }}</span>
    </template>
    <el-empty :description="desc">
      <template #image>
        <el-icon :size="80" color="#c0c4cc"><Folder /></el-icon>
      </template>
    </el-empty>
  </el-card>
</template>

<script setup>
defineProps({
  title: { type: String, default: '功能' },
  desc: { type: String, default: '该功能正在开发中，敬请期待' },
})
</script>
