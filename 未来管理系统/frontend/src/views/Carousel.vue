<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>首页轮播图配置</span>
        <el-button type="primary">新增轮播图</el-button>
      </div>
    </template>
    <el-table :data="list" stripe>
      <el-table-column label="图片" width="100">
        <template #default="{ row }">
          <el-image v-if="row.image" :src="row.image" style="width:80px;height:45px" fit="cover" />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="link" label="跳转链接" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column prop="status" label="状态" width="80" />
      <el-table-column label="操作" width="150">
        <template #default>
          <el-button link type="primary">编辑</el-button>
          <el-button link type="danger">删除</el-button>
        </template>
      </el-table-column>
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
