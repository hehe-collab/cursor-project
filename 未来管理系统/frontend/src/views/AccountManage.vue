<template>
  <div class="account-manage page-list-layout">
    <el-card class="filter-card" shadow="never">
      <el-form
        :model="filterForm"
        class="filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
        @keyup.enter="handleQuery"
      >
        <el-form-item label="账户媒体" label-width="70px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.platform" placeholder="媒体" clearable>
              <el-option label="TikTok" value="tiktok" />
              <el-option label="Facebook" value="facebook" />
              <el-option label="Google" value="google" />
              <el-option label="Meta" value="meta" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item v-if="countries.length > 0" label="国家" label-width="50px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.country" placeholder="全部" clearable>
              <el-option
                v-for="item in countryFilterOptions"
                :key="item.value === '' ? '_all' : item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="账户主体" label-width="70px">
          <div class="filter-item-m">
            <el-select v-model="filterForm.entityName" placeholder="主体" clearable>
              <el-option
                v-for="item in entityOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="账户ID" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.spid" placeholder="账户ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="账户名称" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.accountName" placeholder="账户名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleQuery">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="button-group account-toolbar">
        <div class="button-group-left">
          <el-button type="primary" @click="handleAdd">新增</el-button>
          <el-button type="primary" :disabled="selectedRows.length !== 1" @click="handleBatchEdit">修改</el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">删除</el-button>
        </div>
        <div class="button-group-right">
          <el-button type="success" @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        :data="tableData"
        border
        stripe
        v-loading="loading"
        height="calc(100vh - 300px)"
        @selection-change="handleSelectionChange"
      >
        <template #empty>
          <el-empty description="暂无账户数据" />
        </template>
        <el-table-column type="selection" width="55" />
        <el-table-column label="账户媒体" width="120">
          <template #default="{ row }">
            {{ formatMediaDisplay(row.media) }}
          </template>
        </el-table-column>
        <el-table-column label="国家" width="100">
          <template #default="{ row }">
            {{ formatAccountCountryLabel(row.country) }}
          </template>
        </el-table-column>
        <el-table-column prop="subject_name" label="账户主体" min-width="180" show-overflow-tooltip />
        <el-table-column prop="account_id" label="账户ID" min-width="180" show-overflow-tooltip />
        <el-table-column prop="account_name" label="账户名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="账户代理" width="120" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatAccountAgent(row.account_agent) }}
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="120">
          <template #default="{ row }">{{ row.created_by_name || formatCreator(row.created_by) }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="账户媒体" prop="platform">
          <el-select v-model="formData.platform" placeholder="请选择" style="width: 100%">
            <el-option label="TikTok" value="tiktok" />
            <el-option label="Facebook" value="facebook" />
            <el-option label="Google" value="google" />
            <el-option label="Meta" value="meta" />
          </el-select>
        </el-form-item>

        <el-form-item label="国家" prop="country">
          <el-select v-model="formData.country" placeholder="请选择国家" style="width: 100%">
            <el-option label="泰国" value="TH" />
            <el-option label="印尼" value="ID" />
            <el-option label="越南" value="VN" />
            <el-option label="美国" value="US" />
          </el-select>
        </el-form-item>

        <el-form-item label="账户主体" prop="entityName">
          <el-select v-model="formData.entityName" placeholder="请选择账户主体" style="width: 100%">
            <el-option
              v-for="item in entityOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="账户ID" prop="spid">
          <el-input
            v-model="formData.spid"
            type="textarea"
            :rows="3"
            placeholder="请输入账户ID"
          />
          <div class="batch-tip">
            如需批量添加账户，请在每个ID之间用逗号或换行符，进行分隔
          </div>
        </el-form-item>

        <el-form-item label="账户代理" prop="accountAgent">
          <el-select v-model="formData.accountAgent" placeholder="请选择" clearable style="width: 100%">
            <el-option label="代理商A" value="agent_a" />
            <el-option label="代理商B" value="agent_b" />
            <el-option label="代理商C" value="agent_c" />
            <el-option label="直营" value="direct" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
        <el-button @click="handleDialogClose">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import { useCountries, formatAccountCountryLabel } from '@/composables/useCountries'

const { countries, countryFilterOptions, refreshCountries } = useCountries()

const ACCOUNT_AGENT_LABEL = {
  agent_a: '代理商A',
  agent_b: '代理商B',
  agent_c: '代理商C',
  direct: '直营',
}

function formatAccountAgent(agent) {
  if (agent == null || agent === '') return '—'
  return ACCOUNT_AGENT_LABEL[agent] || agent
}

function formatMediaDisplay(media) {
  if (media == null || media === '') return '—'
  const m = String(media).trim().toLowerCase()
  if (m === 'tiktok') return 'TikTok'
  if (m === 'facebook') return 'Facebook'
  if (m === 'google') return 'Google'
  if (m === 'meta') return 'Meta'
  return String(media).trim()
}

function formatTime(iso) {
  if (!iso) return '—'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatCreator(uid) {
  if (uid == null || uid === '') return '—'
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    if (u.id === uid) return u.nickname || u.username || String(uid)
  } catch (_) {}
  return String(uid)
}

const MEDIA_TO_KEY = {
  TikTok: 'tiktok',
  tiktok: 'tiktok',
  Facebook: 'facebook',
  facebook: 'facebook',
  Google: 'google',
  google: 'google',
  Meta: 'meta',
  meta: 'meta',
}

/** 账户主体选项（默认；优先由 GET /accounts/entities 覆盖） */
const DEFAULT_ENTITY_OPTIONS = [
  { label: '测试一', value: '测试一' },
  { label: '测试二', value: '测试二' },
]

const entityOptions = ref([...DEFAULT_ENTITY_OPTIONS])

async function fetchEntityOptions() {
  try {
    const res = await request.get('/accounts/entities')
    if (res.code === 0 && Array.isArray(res.data) && res.data.length) {
      entityOptions.value = res.data
    }
  } catch {
    entityOptions.value = [...DEFAULT_ENTITY_OPTIONS]
  }
}

const filterForm = reactive({
  platform: '',
  country: '',
  entityName: '',
  spid: '',
  accountName: '',
})

const tableData = ref([])
const loading = ref(false)
const tableRef = ref(null)
const selectedRows = ref([])

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const dialogVisible = ref(false)
const dialogTitle = ref('添加账户管理')
const formRef = ref(null)
const formData = reactive({
  id: null,
  platform: '',
  country: '',
  entityName: '',
  spid: '',
  accountAgent: '',
})

const formRules = {
  platform: [{ required: true, message: '请选择账户媒体', trigger: 'change' }],
  country: [{ required: true, message: '请选择国家', trigger: 'change' }],
  entityName: [{ required: true, message: '请选择账户主体', trigger: 'change' }],
  spid: [
    { required: true, message: '请输入账户ID', trigger: 'blur' },
    {
      validator: (_, v, cb) => {
        const ids = String(v ?? '')
          .split(/[,\n]+/)
          .map((s) => s.trim())
          .filter((s) => s.length > 0)
        if (!ids.length) cb(new Error('请输入至少一个账户ID'))
        else cb()
      },
      trigger: 'blur',
    },
  ],
}

function emptyForm() {
  Object.assign(formData, {
    id: null,
    platform: '',
    country: '',
    entityName: '',
    spid: '',
    accountAgent: '',
  })
}

function buildAccountPayload(spidSingle) {
  return {
    platform: formData.platform,
    country: formData.country,
    entityName: formData.entityName,
    spid: spidSingle,
    accountAgent: formData.accountAgent,
  }
}

function filterQueryParams() {
  const params = {}
  if (filterForm.platform) params.platform = filterForm.platform
  if (filterForm.country) params.country = filterForm.country
  if (filterForm.entityName) params.entityName = filterForm.entityName
  if (filterForm.spid) params.spid = filterForm.spid
  if (filterForm.accountName) params.accountName = filterForm.accountName
  return params
}

const handleQuery = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
      ...filterQueryParams(),
    }
    const res = await request.get('/accounts', { params })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.total = res.data?.total ?? 0
      selectedRows.value = []
      tableRef.value?.clearSelection()
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(filterForm, {
    platform: '',
    country: '',
    entityName: '',
    spid: '',
    accountName: '',
  })
  pagination.page = 1
  handleQuery()
}

function handleSelectionChange(rows) {
  selectedRows.value = rows
}

const handleAdd = () => {
  dialogTitle.value = '添加账户管理'
  emptyForm()
  dialogVisible.value = true
}

const handleBatchEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行修改')
    return
  }
  handleEdit(selectedRows.value[0])
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑账户'
  Object.assign(formData, {
    id: row.id,
    platform: MEDIA_TO_KEY[row.media] || '',
    country: row.country || '',
    entityName: row.subject_name || '',
    spid: row.account_id || '',
    accountAgent: row.account_agent || '',
  })
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该账户吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
    const res = await request.delete(`/accounts/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      await refreshCountries()
      handleQuery()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedRows.value.length} 条记录吗？`,
      '提示',
      { type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消' },
    )
    const ids = selectedRows.value.map((row) => row.id)
    const res = await request.post('/accounts/batch-delete', { ids })
    if (res.code === 0) {
      ElMessage.success('删除成功')
      selectedRows.value = []
      tableRef.value?.clearSelection()
      await refreshCountries()
      handleQuery()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

const handleExport = async () => {
  loading.value = true
  try {
    const response = await request.get('/accounts/export', {
      params: filterQueryParams(),
      responseType: 'blob',
    })
    const blob = response.data
    let filename = `账户管理_${new Date().toISOString().slice(0, 10)}.xlsx`
    const disposition = response.headers['content-disposition']
    if (disposition) {
      const m = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i)
      if (m && m[1]) {
        try {
          filename = decodeURIComponent(m[1].trim())
        } catch {
          filename = m[1].trim()
        }
      }
    }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // 失败提示由 request 拦截器处理（含 JSON 包装的 blob 错误体）
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  const spids = String(formData.spid)
    .split(/[,\n]+/)
    .map((id) => id.trim())
    .filter((id) => id.length > 0)

  if (spids.length === 0) {
    ElMessage.warning('请输入至少一个账户ID')
    return
  }

  try {
    if (formData.id) {
      const res = await request.put(`/accounts/${formData.id}`, buildAccountPayload(spids[0]))
      if (res.code === 0) {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        await refreshCountries()
        handleQuery()
      } else {
        ElMessage.error(res.message || '修改失败')
      }
      return
    }

    let successCount = 0
    for (const spid of spids) {
      try {
        const res = await request.post('/accounts', buildAccountPayload(spid))
        if (res.code === 0) {
          successCount += 1
        } else {
          ElMessage.error(res.message || `账户 ${spid} 添加失败`)
        }
      } catch {
        /* 网络错误等由 request 拦截器提示 */
      }
    }

    if (spids.length > 1) {
      ElMessage.success(`批量添加成功 ${successCount}/${spids.length} 条`)
    } else if (successCount === 1) {
      ElMessage.success('新增成功')
    }

    if (successCount > 0) {
      dialogVisible.value = false
      await refreshCountries()
      handleQuery()
    }
  } catch {
    ElMessage.error('操作失败')
  }
}

const handleDialogClose = () => {
  dialogVisible.value = false
  formRef.value?.resetFields()
  emptyForm()
}

onMounted(async () => {
  await fetchEntityOptions()
  handleQuery()
})
</script>

<style scoped>
.account-manage {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.table-card :deep(.el-card__body) {
  padding-top: 16px;
}

.table-card .button-group {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.batch-tip {
  color: #ff4d4f;
  font-size: 12px;
  margin-top: 4px;
  line-height: 1.5;
}
</style>
