<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>素材管理</span>
        <el-button type="primary">上传素材</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column label="预览" width="80">
        <template #default="{ row }">
          <el-image v-if="row.url" :src="row.url" style="width:50px;height:50px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="文件名" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="size" label="大小" width="100" />
      <el-table-column prop="create_time" label="上传时间" width="180" />
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
