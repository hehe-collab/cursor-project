<template>
  <div class="ad-task-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form
        :model="filterForm"
        class="filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
        @keyup.enter="handleQuery"
      >
        <el-form-item label="任务ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.taskId" placeholder="任务ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="账户id" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.accountId" placeholder="账户id" clearable />
          </div>
        </el-form-item>
        <el-form-item label="账户名称" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.accountName" placeholder="账户名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label="状态" label-width="50px">
          <div class="filter-item-s">
            <el-select v-model="filterForm.status" placeholder="状态" clearable>
              <el-option label="全部" value="" />
              <el-option label="成功" value="success" />
              <el-option label="失败" value="failed" />
              <el-option label="进行中" value="running" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" :loading="loading" @click="handleQuery">
              <el-icon class="el-icon--left"><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon class="el-icon--left"><RefreshLeft /></el-icon>
              重置
            </el-button>
            <el-button :loading="exporting" @click="handleExport">
              <el-icon class="el-icon--left"><Download /></el-icon>
              导出
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe style="width: 100%" height="calc(100vh - 240px)">
        <el-table-column prop="task_id" label="任务ID" width="120" align="center" />
        <el-table-column prop="account_ids" label="账户ID" min-width="300" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="text-overflow">{{ row.account_ids }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="account_names" label="账户名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="text-overflow">{{ row.account_names }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="promotion_type" label="推广类型" width="150" align="center" />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="created_by" label="创建人" width="120" align="center" />
        <el-table-column prop="created_at" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleView(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="任务详情" width="70%" destroy-on-close class="ad-task-detail-dialog">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID" label-align="right">
          {{ detailData.task_id }}
        </el-descriptions-item>

        <el-descriptions-item label="状态" label-align="right">
          <el-tag :type="getStatusType(detailData.status)">
            {{ getStatusText(detailData.status) }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="推广类型" label-align="right">
          {{ detailData.promotion_type }}
        </el-descriptions-item>

        <el-descriptions-item label="创建人" label-align="right">
          {{ detailData.created_by }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间" label-align="right" :span="2">
          {{ detailData.created_at }}
        </el-descriptions-item>

        <el-descriptions-item label="账户ID" label-align="right" :span="2">
          <div class="detail-scroll">{{ detailData.account_ids }}</div>
        </el-descriptions-item>

        <el-descriptions-item label="账户名称" label-align="right" :span="2">
          <div class="detail-scroll">{{ detailData.account_names }}</div>
        </el-descriptions-item>

        <el-descriptions-item v-if="detailData.config != null" label="任务配置" label-align="right" :span="2">
          <el-scrollbar max-height="400px">
            <pre class="config-json">{{ formatConfig(detailData.config) }}</pre>
          </el-scrollbar>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Download, RefreshLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const filterForm = reactive({
  taskId: '',
  accountId: '',
  accountName: '',
  status: '',
})

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const detailVisible = ref(false)
const detailData = ref({})

function getStatusType(status) {
  const map = {
    success: 'success',
    failed: 'danger',
    running: 'warning',
  }
  return map[status] || 'info'
}

function getStatusText(status) {
  const map = {
    success: '成功',
    failed: '失败',
    running: '进行中',
  }
  return map[status] || '未知'
}

function formatConfig(config) {
  try {
    return JSON.stringify(config, null, 2)
  } catch (e) {
    return String(config)
  }
}

function handleReset() {
  filterForm.taskId = ''
  filterForm.accountId = ''
  filterForm.accountName = ''
  filterForm.status = ''
  pagination.page = 1
  handleQuery()
}

async function handleQuery() {
  if (loading.value) return
  loading.value = true
  try {
    const res = await request.get('/ad-task', {
      params: {
        task_id: filterForm.taskId,
        account_id: filterForm.accountId,
        account_name: filterForm.accountName,
        status: filterForm.status,
        page: pagination.page,
        pageSize: pagination.pageSize,
      },
    })
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total ?? 0
  } catch (error) {
    console.error('查询失败:', error)
  } finally {
    loading.value = false
  }
}

async function handleExport() {
  exporting.value = true
  try {
    ElMessage.info('正在导出，请稍候…')
    const axiosRes = await request.get('/ad-task/export', {
      params: {
        task_id: filterForm.taskId,
        account_id: filterForm.accountId,
        account_name: filterForm.accountName,
        status: filterForm.status,
      },
      responseType: 'blob',
    })
    const blob = axiosRes.data
    if (!(blob instanceof Blob)) {
      ElMessage.error('导出失败')
      return
    }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toLocaleDateString('zh-CN').replace(/\//g, '-')
    link.download = `广告任务_${timestamp}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  } finally {
    exporting.value = false
  }
}

function handleView(row) {
  detailData.value = { ...row }
  detailVisible.value = true
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.ad-task-container {
  padding: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-card :deep(.el-card__body) {
  padding-bottom: 10px;
}

.table-card {
  margin-bottom: 20px;
}

.text-overflow {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.detail-scroll {
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.config-json {
  background-color: var(--el-fill-color-light);
  padding: 15px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  color: var(--el-text-color-primary);
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 600;
}
</style>
