<template>
  <div class="drama-manage-list">
    <el-card>
      <template #header>
        <span>短剧管理</span>
      </template>
      <el-form :model="query" label-position="top" class="drama-manage-list-filter-form" @submit.prevent="loadList">
        <el-row :gutter="16">
          <el-col :span="6">
            <el-form-item label="剧ID">
              <el-input v-model="query.dramaId" placeholder="请输入" clearable @keyup.enter="loadList" />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="剧名">
              <el-input v-model="query.title" placeholder="请输入" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="类型">
              <el-select v-model="query.categoryId" placeholder="请选择" clearable style="width: 100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="状态">
              <el-select v-model="query.status" placeholder="请选择" clearable style="width: 100%">
                <el-option label="草稿" value="draft" />
                <el-option label="上线" value="published" />
                <el-option label="下线" value="offline" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <div class="button-group">
          <div class="button-group-left">
            <el-button type="primary" @click="loadList">搜索</el-button>
            <el-button @click="onReset">重置</el-button>
          </div>
          <div class="button-group-right"></div>
        </div>
        <div class="button-group">
          <div class="button-group-left">
            <el-button type="primary" @click="$router.push('/dramas/add')">
              <el-icon><Plus /></el-icon>
              新增
            </el-button>
            <el-button type="success" :disabled="selectedRows.length !== 1" @click="onBatchEdit">
              <el-icon><Edit /></el-icon>
              批量修改
            </el-button>
            <el-button type="danger" :disabled="!selectedRows.length" @click="onBatchDelete">
              <el-icon><Delete /></el-icon>
              批量删除
            </el-button>
          </div>
          <div class="button-group-right"></div>
        </div>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe size="small" @selection-change="handleSelectionChange">
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
        <el-table-column type="selection" width="50" />
        <el-table-column label="剧集" width="80" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.enabled" @change="toggleEnabled(row)" />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'published'" type="success">上线</el-tag>
            <el-tag v-else-if="row.status === 'offline'" type="info">下线</el-tag>
            <el-tag v-else type="info">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="id" label="剧ID" width="90" />
        <el-table-column prop="title" label="剧名" min-width="150" />
        <el-table-column prop="episode_count" label="集数" width="80" />
        <el-table-column label="任务状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.task_status === '完成'" type="success">完成</el-tag>
            <el-tag v-else type="info">{{ row.task_status || '进行中' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="onView(row)">查看</el-button>
            <el-button link type="primary" @click="onCopy(row)">复制</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :page-sizes="[20, 50, 100, 200]"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '../api/request'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const categories = ref([])
const selectedRows = ref([])
const query = reactive({
  page: 1,
  pageSize: 20,
  dramaId: '',
  title: '',
  categoryId: '',
  status: '',
})

async function loadCategories() {
  try {
    const res = await request.get('/categories').catch(() => ({ data: [] }))
    categories.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
  } catch {
    categories.value = []
  }
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.dramaId) params.dramaId = query.dramaId
    if (query.title) params.title = query.title
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.status) params.status = query.status
    const res = await request.get('/dramas/manage-list', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    const data = res.data || {}
    list.value = (data.list || []).map(d => ({
      ...d,
      enabled: d.status === 'published',
    }))
    total.value = data.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  const s = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${h}:${min}:${s}`
}

function onReset() {
  query.dramaId = ''
  query.title = ''
  query.categoryId = ''
  query.status = ''
  query.page = 1
  loadList()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

async function toggleEnabled(row) {
  try {
    const detailRes = await request.get(`/dramas/${row.id}`)
    const drama = detailRes.data
    const newStatus = row.enabled ? 'published' : 'offline'
    await request.put(`/dramas/${row.id}`, {
      title: drama.title,
      cover: drama.cover,
      description: drama.description,
      category_id: drama.category_id,
      status: newStatus,
      sort: drama.sort,
      tag_ids: drama.tag_ids || [],
      episodes: (drama.episodes || []).map(e => ({
        episode_num: e.episode_num,
        title: e.title,
        video_id: e.video_id,
        video_url: e.video_url,
        duration: e.duration,
      })),
    })
    row.status = newStatus
    ElMessage.success(row.enabled ? '已上线' : '已下线')
  } catch (e) {
    row.enabled = !row.enabled
    ElMessage.error('操作失败')
  }
}

function onBatchEdit() {
  if (selectedRows.value.length === 1) router.push(`/dramas/edit/${selectedRows.value[0].id}`)
}

async function onBatchDelete() {
  if (!selectedRows.value.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedRows.value.length} 个短剧吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    for (const row of selectedRows.value) {
      await request.delete(`/dramas/${row.id}`)
    }
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function onView(row) {
  router.push(`/dramas/edit/${row.id}`)
}

async function onCopy(row) {
  try {
    const detailRes = await request.get(`/dramas/${row.id}`)
    const drama = detailRes.data
    await request.post('/dramas', {
      title: (drama.title || '') + ' (副本)',
      cover: drama.cover,
      description: drama.description,
      category_id: drama.category_id,
      status: 'draft',
      sort: drama.sort,
      tag_ids: drama.tag_ids || [],
      episodes: (drama.episodes || []).map(e => ({
        episode_num: e.episode_num,
        title: e.title,
        video_id: e.video_id,
        video_url: e.video_url,
        duration: e.duration,
      })),
    })
    ElMessage.success('复制成功')
    loadList()
  } catch (e) {
    ElMessage.error('复制失败')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm('确定删除该短剧吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.delete(`/dramas/${row.id}`)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadCategories()
  loadList()
})
</script>

<style scoped>
.drama-manage-list :deep(.el-card__body) { padding: 20px 24px; }
.drama-manage-list-filter-form :deep(.el-input),
.drama-manage-list-filter-form :deep(.el-select) {
  width: 100%;
}
.drama-manage-list-filter-form :deep(.el-form-item) {
  margin-bottom: 16px;
}
.drama-manage-list .button-group .el-button {
  min-width: 90px;
}
.drama-manage-list :deep(.el-table) { border-radius: 6px; overflow: hidden; }
.drama-manage-list :deep(.el-table th) { background: #fafbfc !important; font-weight: 600; }
.drama-manage-list :deep(.el-pagination) { margin-top: 20px; }
</style>
