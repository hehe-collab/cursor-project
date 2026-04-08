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
              placeholder="请选择"
              clearable
              @focus="loadAllAccounts"
            >
              <el-option
                v-for="item in accountOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
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
            placeholder="请选择"
            style="width: 100%"
            @focus="loadAllAccounts"
          >
            <el-option
              v-for="item in accountOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
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
            placeholder="请输入下载URL（换行相隔）"
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
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'success' ? 'success' : 'warning'">
              {{ row.status === 'success' ? '成功' : '进行中' }}
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

const filterForm = reactive({
  accountId: '',
  materialId: '',
  materialName: '',
})

const accountOptions = ref([])
/** 已成功拉取过全量账户后不再请求（点击筛选/上传下拉 @focus 时） */
const accountsLoaded = ref(false)
let accountsFetchPromise = null

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

function mapAccountsToOptions(list) {
  return (list || [])
    .map((a) => ({
      label: `${a.account_id || ''} - ${a.account_name || ''}`,
      value: a.account_id || '',
    }))
    .filter((x) => x.value)
}

/** 点击下拉时加载全部账户；本地 filterable 按 label 过滤（账户ID + 名称） */
async function loadAllAccounts() {
  if (accountsLoaded.value) return
  if (accountsFetchPromise) {
    await accountsFetchPromise
    return
  }
  accountsFetchPromise = (async () => {
    try {
      const first = await request.get('/accounts', { params: { page: 1, pageSize: 100 } })
      if (first.code !== 0) return
      let list = [...(first.data?.list || [])]
      const total = first.data?.total ?? list.length
      let page = 2
      while (list.length < total && page <= 50) {
        const r = await request.get('/accounts', { params: { page, pageSize: 100 } })
        if (r.code !== 0) break
        const chunk = r.data?.list || []
        list = list.concat(chunk)
        if (chunk.length < 100) break
        page += 1
      }
      accountOptions.value = mapAccountsToOptions(list)
      accountsLoaded.value = true
    } catch {
      ElMessage.error('加载账户列表失败')
    } finally {
      accountsFetchPromise = null
    }
  })()
  await accountsFetchPromise
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
      ElMessage.success('上传任务已提交')
      uploadDialogVisible.value = false
      handleQuery()
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
      handleQuery()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
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
