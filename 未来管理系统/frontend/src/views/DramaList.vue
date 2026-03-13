<template>
  <div class="drama-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>剧集信息</span>
          <el-button type="primary" @click="$router.push('/dramas/add')">新增</el-button>
        </div>
      </template>
      <el-form :inline="true" class="filter-form" @submit.prevent="loadList">
        <el-form-item label="剧ID">
          <el-input v-model="query.dramaId" placeholder="请输入剧ID" clearable style="width:160px" @keyup.enter="loadList" />
        </el-form-item>
        <el-form-item label="集数">
          <el-input v-model="query.episodeNum" placeholder="请输入集数" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="外部ID">
          <el-input v-model="query.externalId" placeholder="请输入外部ID" clearable style="width:180px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无剧集数据" />
        </template>
        <el-table-column prop="id" label="剧集ID" width="80" />
        <el-table-column prop="drama_id" label="剧ID" width="80" />
        <el-table-column prop="title" label="剧名" min-width="150" />
        <el-table-column prop="episode_num" label="集数" width="70" />
        <el-table-column prop="external_id" label="外部ID" width="180" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEdit(row)">修改</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>

    <!-- 修改剧集弹窗 -->
    <el-dialog v-model="editVisible" title="修改剧集" width="560px" destroy-on-close>
      <el-form v-if="editRow" :model="editForm" label-width="100px">
        <el-form-item label="剧集ID"><el-input v-model="editRow.id" disabled /></el-form-item>
        <el-form-item label="剧ID"><el-input v-model="editRow.drama_id" disabled /></el-form-item>
        <el-form-item label="剧名"><el-input v-model="editRow.title" disabled /></el-form-item>
        <el-form-item label="集数"><el-input v-model="editRow.episode_num" disabled /></el-form-item>
        <el-form-item label="外部ID"><el-input v-model="editForm.external_id" placeholder="请输入外部ID" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const editVisible = ref(false)
const editRow = ref(null)
const editForm = reactive({ external_id: '' })
const query = reactive({
  page: 1,
  pageSize: 10,
  dramaId: '',
  episodeNum: '',
  externalId: '',
})

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.dramaId) params.dramaId = query.dramaId
    if (query.episodeNum) params.episodeNum = query.episodeNum
    if (query.externalId) params.externalId = query.externalId
    const res = await request.get('/dramas/episodes', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.dramaId = ''
  query.episodeNum = ''
  query.externalId = ''
  loadList()
}

function showEdit(row) {
  if (row.episode_num === '-') return ElMessage.warning('该剧暂无集数信息')
  editRow.value = row
  editForm.external_id = row.external_id || ''
  editVisible.value = true
}

async function submitEdit() {
  if (!editRow.value) return
  try {
    const dramaRes = await request.get(`/dramas/${editRow.value.drama_id}`)
    const drama = dramaRes.data
    const episodes = (drama.episodes || []).map(ep => {
      if (String(ep.episode_num) === String(editRow.value.episode_num)) {
        return { ...ep, video_id: editForm.external_id }
      }
      return ep
    })
    await request.put(`/dramas/${editRow.value.drama_id}`, {
      title: drama.title,
      cover: drama.cover,
      description: drama.description,
      category_id: drama.category_id,
      status: drama.status,
      sort: drama.sort,
      tag_ids: drama.tag_ids || [],
      episodes: episodes.map(e => ({
        episode_num: e.episode_num,
        title: e.title || '',
        video_id: e.video_id || '',
        video_url: e.video_url || '',
        duration: e.duration || 0,
      })),
    })
    ElMessage.success('修改成功')
    editVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('修改失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filter-form { margin-bottom: 16px; }
</style>
