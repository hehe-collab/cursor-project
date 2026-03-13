<template>
  <el-card>
    <template #header><span>电视剧分集管理</span></template>
    <el-select v-model="dramaId" placeholder="选择电视剧" filterable clearable style="width:200px;margin-right:12px" @change="loadEpisodes">
      <el-option v-for="d in dramas" :key="d.id" :label="d.title" :value="d.id" />
    </el-select>
    <el-table v-if="dramaId" :data="episodes" stripe>
      <el-table-column prop="episode_num" label="集数" width="80" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="video_id" label="VideoId" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" @click="$router.push(`/dramas/edit/${row.drama_id}`)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-else description="请先选择电视剧" />
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../api/request'
const dramaId = ref(null)
const dramas = ref([])
const episodes = ref([])
async function loadDramas() {
  const res = await request.get('/dramas', { params: { pageSize: 999 } })
  dramas.value = res.data.list || []
}
async function loadEpisodes() {
  if (!dramaId.value) return
  const res = await request.get(`/dramas/${dramaId.value}`)
  episodes.value = res.data.episodes || []
}
onMounted(loadDramas)
</script>
