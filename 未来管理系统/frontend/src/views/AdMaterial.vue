<template>
  <div class="ad-material page-list-layout">
    <el-card class="filter-card" shadow="never">
      <el-form
        :model="filterForm"
        class="filter-form ad-material-filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
        @keyup.enter="handleQuery"
      >
        <el-form-item label="账户ID" label-width="60px">
          <div class="filter-item-m">
            <el-select
              v-model="filterForm.accountId"
              filterable
              placeholder="素材历史账户"
              clearable
            >
              <el-option
                v-for="item in materialAccountOptions"
                :key="item.accountId"
                :label="item.label"
                :value="item.accountId"
              />
              <template #empty>
                <div class="select-empty-tip">{{ materialHistoryEmptyText }}</div>
              </template>
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="素材ID" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.materialId" placeholder="素材ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="素材名称" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.materialName" placeholder="模糊搜索" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
      <div class="button-group">
        <div class="button-group-left">
          <el-button type="primary" @click="handleUploadMaterial">上传素材</el-button>
          <el-button @click="handleBatchSync">批量同步</el-button>
        </div>
        <div class="button-group-right">
          <el-button type="warning" @click="handleViewRecords">同步/上传记录</el-button>
        </div>
      </div>
      <div class="history-filter-tip">顶部账户ID筛选只看素材库历史账户；上传素材弹窗里显示的是当前可执行的 TikTok 账户。</div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-wrapper">
        <el-table :data="tableData" border stripe v-loading="loading" height="100%" size="small">
        <template #empty>
          <el-empty description="暂无广告素材" />
        </template>
        <el-table-column prop="materialId" label="素材ID" width="180" show-overflow-tooltip />
        <el-table-column prop="materialName" label="素材名称" min-width="250" show-overflow-tooltip />
        <el-table-column prop="videoId" label="视频ID" width="200" show-overflow-tooltip />
        <el-table-column label="封面" width="100">
          <template #default="{ row }">
            <img
              v-if="row.coverUrl"
              v-lazy="row.coverUrl"
              class="cover-thumb"
              alt=""
            />
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleView(row)">查看</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination compact-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          size="small"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <el-dialog v-model="uploadDialogVisible" title="上传素材" width="600px" destroy-on-close @close="resetUploadForm">
      <el-form ref="uploadFormRef" :model="uploadForm" label-width="100px">
        <el-form-item label="账户ID" prop="accountId">
          <el-select
            v-model="uploadForm.accountId"
            filterable
            placeholder="请选择可执行TikTok账户"
            style="width: 100%"
          >
            <el-option
              v-for="item in uploadAccountOptions"
              :key="item.accountId"
              :label="item.label"
              :value="item.accountId"
            />
            <template #empty>
              <div class="select-empty-tip">{{ executableUploadEmptyText }}</div>
            </template>
          </el-select>
        </el-form-item>
        <el-form-item label="文件夹">
          <el-input v-model="uploadForm.folder" placeholder="请输入" />
        </el-form-item>
        <el-form-item label="文件">
          <el-input
            v-model="uploadForm.files"
            type="textarea"
            :rows="5"
            placeholder="请输入 TikTok 可拉取的下载 URL（每行一个，需带图片/视频后缀）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleUploadSubmit">确定</el-button>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="syncDialogVisible" title="素材同步" width="600px" destroy-on-close @close="resetSyncForm">
      <el-form ref="syncFormRef" :model="syncForm" label-width="100px">
        <el-form-item label="账户ID" prop="accountIds">
          <el-input
            v-model="syncForm.accountIds"
            type="textarea"
            :rows="5"
            placeholder="支持多个账户，每行一个账户"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="handleSyncSubmit">确定</el-button>
        <el-button @click="syncDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordDialogVisible" title="广告素材同步记录" width="800px">
      <el-table :data="recordData" border stripe v-loading="recordLoading">
        <el-table-column prop="accountId" label="账户ID" width="200" show-overflow-tooltip />
        <el-table-column prop="accountName" label="账户名称" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            {{ row.taskType === 'upload' ? '上传' : row.taskType === 'sync' ? '同步' : row.taskType || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="recordStatusType(row.status)">
              {{ recordStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="180" />
      </el-table>
    </el-dialog>

    <el-dialog v-model="viewDialogVisible" title="素材详情" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="账户ID">{{ viewData.accountId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="素材ID">{{ viewData.materialId }}</el-descriptions-item>
        <el-descriptions-item label="素材名称">{{ viewData.materialName }}</el-descriptions-item>
        <el-descriptions-item label="视频ID">{{ viewData.videoId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createdAt }}</el-descriptions-item>
        <el-descriptions-item label="封面">
          <el-image
            v-if="viewData.coverUrl"
            :src="viewData.coverUrl"
            lazy
            :preview-src-list="[viewData.coverUrl]"
            fit="cover"
            style="width: 200px; height: 200px"
          />
          <span v-else>—</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import { formatHistoryAccountOptions, buildHistoryAccountEmptyText } from '@/utils/accountOptionDisplay'

const filterForm = reactive({
  accountId: '',
  materialId: '',
  materialName: '',
})

const materialHistoryEmptyText = buildHistoryAccountEmptyText('素材库历史')
const executableUploadEmptyText = '这里只显示已在账户管理录入且 TikTok OAuth 为 active 的可执行账户。'

const materialAccountOptions = ref([])
const uploadAccountOptions = ref([])

const tableData = ref([])
const loading = ref(false)

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const uploadDialogVisible = ref(false)
const uploadFormRef = ref(null)
const uploadForm = reactive({
  accountId: '',
  folder: '',
  files: '',
})

const syncDialogVisible = ref(false)
const syncFormRef = ref(null)
const syncForm = reactive({
  accountIds: '',
})

const recordDialogVisible = ref(false)
const recordData = ref([])
const recordLoading = ref(false)

const viewDialogVisible = ref(false)
const viewData = ref({})

async function loadMaterialAccountOptions() {
  try {
    const res = await request.get('/ad-material/account-options')
    if (res.code === 0) {
      materialAccountOptions.value = formatHistoryAccountOptions(res.data || [], {
        countKey: 'materialCount',
        countLabel: '条素材',
      })
    }
  } catch {
    ElMessage.error('加载素材账户列表失败')
  }
}

async function refreshMaterialHistoryState() {
  const currentAccountId = filterForm.accountId
  await loadMaterialAccountOptions()
  if (currentAccountId && !materialAccountOptions.value.some((item) => item.accountId === currentAccountId)) {
    filterForm.accountId = ''
  }
}

async function loadUploadAccountOptions() {
  try {
    const res = await request.get('/accounts/executable-options', {
      params: { media: 'tiktok', oauthStatus: 'active' },
    })
    if (res.code === 0) {
      uploadAccountOptions.value = (res.data || [])
        .map((item) => ({
          accountId: item.accountId || '',
          label: item.accountName
            ? `${item.accountId} - ${item.accountName}`
            : `${item.accountId}`,
        }))
        .filter((item) => item.accountId)
    }
  } catch {
    ElMessage.error('加载可执行账户列表失败')
  }
}

const handleQuery = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (filterForm.accountId) params.accountId = filterForm.accountId
    if (filterForm.materialId) params.materialId = filterForm.materialId
    if (filterForm.materialName) params.materialName = filterForm.materialName
    const res = await request.get('/ad-material', { params })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.total = res.data?.total ?? 0
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(filterForm, {
    accountId: '',
    materialId: '',
    materialName: '',
  })
  pagination.page = 1
  handleQuery()
}

function resetUploadForm() {
  Object.assign(uploadForm, { accountId: '', folder: '', files: '' })
  uploadFormRef.value?.resetFields()
}

function resetSyncForm() {
  syncForm.accountIds = ''
  syncFormRef.value?.resetFields()
}

const handleUploadMaterial = () => {
  uploadDialogVisible.value = true
}

const handleUploadSubmit = async () => {
  if (!uploadForm.accountId) {
    ElMessage.warning('请选择账户ID')
    return
  }
  try {
    const res = await request.post('/ad-material/upload', { ...uploadForm })
    if (res.code === 0) {
      const successCount = res.data?.successCount ?? 0
      const failedCount = res.data?.failedCount ?? 0
      if (failedCount > 0 && successCount > 0) {
        ElMessage.warning(`上传部分成功：成功 ${successCount} 条，失败 ${failedCount} 条`)
      } else if (failedCount > 0) {
        ElMessage.error(`上传失败：共 ${failedCount} 条失败`)
      } else {
        ElMessage.success(`上传完成：成功 ${successCount} 条`)
      }
      uploadDialogVisible.value = false
      await refreshMaterialHistoryState()
      await handleQuery()
    }
  } catch {
    ElMessage.error('上传失败')
  }
}

const handleBatchSync = () => {
  syncDialogVisible.value = true
}

const handleSyncSubmit = async () => {
  if (!syncForm.accountIds.trim()) {
    ElMessage.warning('请输入账户ID')
    return
  }
  try {
    const res = await request.post('/ad-material/sync', { accountIds: syncForm.accountIds })
    if (res.code === 0) {
      ElMessage.success('同步任务已提交')
      syncDialogVisible.value = false
    }
  } catch {
    ElMessage.error('同步失败')
  }
}

const handleViewRecords = async () => {
  recordDialogVisible.value = true
  recordLoading.value = true
  try {
    const res = await request.get('/ad-material/records')
    if (res.code === 0) {
      recordData.value = Array.isArray(res.data) ? res.data : []
    }
  } catch {
    ElMessage.error('获取记录失败')
  } finally {
    recordLoading.value = false
  }
}

const handleView = (row) => {
  viewData.value = { ...row }
  viewDialogVisible.value = true
}

function recordStatusType(status) {
  return {
    success: 'success',
    partial: 'warning',
    failed: 'danger',
    pending: 'info',
    processing: 'warning',
  }[status] || 'info'
}

function recordStatusLabel(status) {
  return {
    success: '成功',
    partial: '部分成功',
    failed: '失败',
    pending: '等待中',
    processing: '处理中',
  }[status] || status || '未知'
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该素材吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    const res = await request.delete(`/ad-material/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      await refreshMaterialHistoryState()
      await handleQuery()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  refreshMaterialHistoryState()
  loadUploadAccountOptions()
  handleQuery()
})
</script>

<style scoped>
.ad-material {
  padding: 0;
}

.filter-card {
  margin-bottom: var(--section-gap);
}

.table-card {
  position: relative;
}

.history-filter-tip,
.select-empty-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}

.history-filter-tip {
  margin-top: 8px;
}


.cover-thumb {
  width: 50px;
  height: 50px;
  cursor: pointer;
  border-radius: 4px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
