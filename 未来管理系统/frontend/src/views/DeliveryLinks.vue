<template>
  <div class="delivery-links-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form
        :model="filterForm"
        class="filter-form delivery-filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
        @keyup.enter="handleQuery"
      >
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.promoId" placeholder="推广ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="剧的ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.dramaId" placeholder="剧的ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="投放媒体" label-width="70px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.media" placeholder="媒体" clearable>
              <el-option label="TikTok" value="tiktok" />
              <el-option label="Meta" value="meta" />
              <el-option label="Google" value="google" />
              <el-option label="Facebook" value="facebook" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="国家/地区" label-width="70px">
          <div class="filter-item-s">
            <el-select
              v-model="filterForm.country"
              :placeholder="countries.length ? '全部' : '请先在账户管理维护广告账户国家'"
              clearable
            >
              <el-option
                v-for="item in countryFilterOptions"
                :key="item.value === '' ? '_all' : item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="推广名称" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.promoName" placeholder="推广名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label="推广域名" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.promoDomain" placeholder="域名" clearable />
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
          </div>
        </el-form-item>
      </el-form>
      <div class="button-group">
        <div class="button-group-left">
          <el-button type="primary" @click="handleAdd">
            <el-icon class="el-icon--left"><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="primary" :disabled="selectedRows.length !== 1" @click="handleEdit">
            <el-icon class="el-icon--left"><Edit /></el-icon>
            修改
          </el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
            <el-icon class="el-icon--left"><Delete /></el-icon>
            删除
          </el-button>
          <el-button type="success" :disabled="selectedRows.length === 0" @click="handleBatchEnable(true)">
            批量启用
          </el-button>
          <el-button type="info" :disabled="selectedRows.length === 0" @click="handleBatchEnable(false)">
            批量禁用
          </el-button>
        </div>
        <div class="button-group-right">
          <el-button type="warning" :loading="exporting" @click="handleExport">
            <el-icon class="el-icon--left"><Download /></el-icon>
            导出
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        element-loading-text="加载中..."
        :data="tableData"
        border
        stripe
        style="width: 100%"
        height="calc(100vh - 340px)"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.enabled !== false ? 'success' : 'info'" size="small">
              {{ row.enabled !== false ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="promo_id" label="推广ID" width="150" align="center" />
        <el-table-column label="剧的ID" width="168" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.drama_public_id || row.drama_id }}
          </template>
        </el-table-column>
        <el-table-column prop="media" label="投放媒体" width="120" align="center" />
        <el-table-column prop="country" label="国家/地区" width="120" align="center" />
        <el-table-column prop="promo_name" label="推广名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="drama_name" label="剧名" width="150" show-overflow-tooltip />
        <el-table-column prop="plan_group_id" label="方案组ID" width="180" show-overflow-tooltip />
        <el-table-column prop="beans_per_episode" label="金豆数" width="100" align="center" />
        <el-table-column prop="free_episodes" label="免费集数" width="100" align="center" />
        <el-table-column prop="created_by" label="创建人" width="120" align="center" />
        <el-table-column prop="created_at" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="300" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleCopyLink(row)">复制链接</el-button>
            <el-button type="primary" link @click="handleCopy(row)">复制</el-button>
            <el-button type="primary" link @click="handleEditRow(row)">修改</el-button>
            <el-button type="danger" link @click="handleDeleteRow(row)">删除</el-button>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="90px">
        <el-form-item label="推广名称" prop="promo_name">
          <el-input v-model="formData.promo_name" placeholder="请输入推广名称" />
        </el-form-item>
        <el-form-item label="投放媒体" prop="media">
          <el-select v-model="formData.media" placeholder="请选择投放媒体" style="width: 100%">
            <el-option label="TikTok" value="tiktok" />
            <el-option label="Meta" value="meta" />
            <el-option label="Google" value="google" />
            <el-option label="Facebook" value="facebook" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家/地区" prop="country">
          <el-select
            v-model="formData.country"
            :placeholder="countryMultiOptions.length ? '请选择国家/地区' : '暂无账户国家数据，请先在账户管理添加账户'"
            style="width: 100%"
            filterable
          >
            <el-option
              v-for="item in countryMultiOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="剧" prop="drama_id">
          <el-select
            v-model="formData.drama_id"
            placeholder="请选择剧"
            filterable
            style="width: 100%"
            @change="onDramaChange"
            @focus="loadDramas"
          >
            <el-option
              v-for="drama in dramaOptions"
              :key="drama.id"
              :label="dramaLabel(drama)"
              :value="drama.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="充值方案组ID" prop="plan_group_id">
          <el-select
            v-model="formData.plan_group_id"
            placeholder="请选择充值方案组"
            style="width: 100%"
            @focus="loadPlanGroups"
          >
            <el-option
              v-for="group in planGroupOptions"
              :key="group.id"
              :label="`${group.id} - ${group.name || ''}`"
              :value="group.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="每集金豆数" prop="beans_per_episode">
          <el-input-number v-model="formData.beans_per_episode" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="免费集数" prop="free_episodes">
          <el-input-number v-model="formData.free_episodes" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预览集数" prop="preview_episodes">
          <el-input-number v-model="formData.preview_episodes" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="复制推广链接" v-model="copyDialogVisible" width="500px">
      <el-form label-width="140px">
        <el-form-item label="推广链接名称">
          <el-input v-model="copyForm.names" type="textarea" :rows="5" placeholder="输入推广链接名称，换行符相隔" />
          <div class="form-tip">提示：可输入多个名称，每行一个</div>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="copyForm.copyCallback">同时复制回传配置</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="copyLoading" @click="handleConfirmCopy">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import request from '../api/request'
import { useCountries } from '@/composables/useCountries'

const { countries, countryFilterOptions, countryMultiOptions } = useCountries()

const filterForm = ref({
  promoId: '',
  dramaId: '',
  media: '',
  country: '',
  promoName: '',
  promoDomain: '',
})

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)
const selectedRows = ref([])

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
})

const dialogVisible = ref(false)
const dialogTitle = computed(() => (formData.value.id ? '修改推广信息' : '添加推广信息'))
const submitLoading = ref(false)
const formRef = ref(null)

const defaultForm = () => ({
  id: null,
  promo_name: '',
  media: 'tiktok',
  country: '',
  drama_id: '',
  plan_group_id: '',
  beans_per_episode: 5,
  free_episodes: 11,
  preview_episodes: 11,
  drama_name: '',
})

const formData = ref(defaultForm())

const rules = {
  promo_name: [{ required: true, message: '请输入推广名称', trigger: 'blur' }],
  media: [{ required: true, message: '请选择投放媒体', trigger: 'change' }],
  country: [{ required: true, message: '请选择国家/地区', trigger: 'change' }],
  drama_id: [{ required: true, message: '请选择剧', trigger: 'change' }],
  plan_group_id: [{ required: true, message: '请选择充值方案组', trigger: 'change' }],
  beans_per_episode: [{ required: true, message: '请输入每集金豆数', trigger: 'blur' }],
  free_episodes: [{ required: true, message: '请输入免费集数', trigger: 'blur' }],
  preview_episodes: [{ required: true, message: '请输入预览集数', trigger: 'blur' }],
}

const dramaOptions = ref([])
const planGroupOptions = ref([])

const copyDialogVisible = ref(false)
const copyLoading = ref(false)
const copyForm = ref({
  names: '',
  copyCallback: false,
})
const currentCopyRow = ref(null)

function dramaLabel(drama) {
  const t = drama.title || drama.name || ''
  const pid = drama.public_id || drama.id
  return t ? `${t}（${pid}）` : String(pid ?? '')
}

function onDramaChange(id) {
  const d = dramaOptions.value.find((x) => x.id === id)
  if (d) {
    formData.value.drama_name = d.title || d.name || ''
  }
}

const handleQuery = async () => {
  if (loading.value) return
  loading.value = true
  try {
    const res = await request.get('/delivery-links', {
      params: {
        promo_id: filterForm.value.promoId || undefined,
        drama_id: filterForm.value.dramaId || undefined,
        media: filterForm.value.media || undefined,
        country: filterForm.value.country || undefined,
        promo_name: filterForm.value.promoName || undefined,
        promo_domain: filterForm.value.promoDomain || undefined,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
      },
    })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.value.total = res.data?.total ?? 0
    } else {
      ElMessage.error(res.message || '查询失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = {
    promoId: '',
    dramaId: '',
    media: '',
    country: '',
    promoName: '',
    promoDomain: '',
  }
  pagination.value.page = 1
  handleQuery()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleAdd = () => {
  formData.value = defaultForm()
  dialogVisible.value = true
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行修改')
    return
  }
  handleEditRow(selectedRows.value[0])
}

const handleEditRow = (row) => {
  formData.value = {
    id: row.id,
    promo_name: row.promo_name || '',
    media: row.media || 'tiktok',
    country: row.country || '',
    drama_id: row.drama_id,
    plan_group_id: row.plan_group_id,
    beans_per_episode: row.beans_per_episode ?? 5,
    free_episodes: row.free_episodes ?? 11,
    preview_episodes: row.preview_episodes ?? 11,
    drama_name: row.drama_name || '',
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    const payload = {
      promo_name: formData.value.promo_name,
      media: formData.value.media,
      country: formData.value.country,
      drama_id: formData.value.drama_id,
      plan_group_id: formData.value.plan_group_id,
      beans_per_episode: formData.value.beans_per_episode,
      free_episodes: formData.value.free_episodes,
      preview_episodes: formData.value.preview_episodes,
      drama_name: formData.value.drama_name,
    }
    if (formData.value.id) {
      const res = await request.put(`/delivery-links/${formData.value.id}`, payload)
      if (res.code === 0) {
        ElMessage.success('修改成功')
        dialogVisible.value = false
        handleQuery()
      } else {
        ElMessage.error(res.message || '修改失败')
      }
    } else {
      const res = await request.post('/delivery-links', payload)
      if (res.code === 0) {
        ElMessage.success('新增成功')
        dialogVisible.value = false
        handleQuery()
      } else {
        ElMessage.error(res.message || '新增失败')
      }
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '提交失败')
  } finally {
    submitLoading.value = false
  }
}

const handleDialogClosed = () => {
  formRef.value?.resetFields()
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条推广链接吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/delivery-links/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      handleQuery()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条记录吗？`, '提示', { type: 'warning' })
    const ids = selectedRows.value.map((row) => row.id)
    const res = await request.post('/delivery-links/batch-delete', { ids })
    if (res.code === 0) {
      ElMessage.success('删除成功')
      handleQuery()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const handleBatchEnable = async (enabled) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  const verb = enabled ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定要${verb}选中的 ${selectedRows.value.length} 条投放链接吗？`, '批量操作', {
      type: 'warning',
    })
    const ids = selectedRows.value.map((row) => row.id)
    const res = await request.post('/delivery-links/batch-status', { ids, enabled })
    if (res.code === 0) {
      ElMessage.success(`${verb}成功`)
      handleQuery()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.message || '操作失败')
    }
  }
}

const handleExport = async () => {
  exporting.value = true
  try {
    ElMessage.info('正在导出，请稍候…')
    const axiosRes = await request.get('/delivery-links/export', {
      params: {
        promo_id: filterForm.value.promoId || undefined,
        drama_id: filterForm.value.dramaId || undefined,
        media: filterForm.value.media || undefined,
        country: filterForm.value.country || undefined,
        promo_name: filterForm.value.promoName || undefined,
        promo_domain: filterForm.value.promoDomain || undefined,
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
    link.download = `投放链接_${timestamp}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const handleCopyLink = (row) => {
  const url = `https://aody3m.dramabagus.com/play?promo_id=${row.promo_id}`
  navigator.clipboard.writeText(url).then(
    () => {
      ElMessage.success('链接已复制到剪贴板')
    },
    () => {
      const textarea = document.createElement('textarea')
      textarea.value = url
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      ElMessage.success('链接已复制到剪贴板')
    }
  )
}

const handleCopy = (row) => {
  currentCopyRow.value = row
  copyForm.value = { names: '', copyCallback: false }
  copyDialogVisible.value = true
}

const handleConfirmCopy = async () => {
  if (!copyForm.value.names.trim()) {
    ElMessage.warning('请输入推广链接名称')
    return
  }
  const names = copyForm.value.names.split('\n').map((n) => n.trim()).filter(Boolean)
  if (names.length === 0) {
    ElMessage.warning('请输入至少一个推广链接名称')
    return
  }
  if (copyForm.value.copyCallback) {
    ElMessage.info('回传配置复制尚未接入，已仅复制推广链接配置')
  }
  copyLoading.value = true
  try {
    let successCount = 0
    for (const name of names) {
      const res = await request.post('/delivery-links/copy', {
        source_id: currentCopyRow.value.id,
        new_name: name,
        copy_callback: copyForm.value.copyCallback,
      })
      if (res.code === 0) successCount += 1
    }
    ElMessage.success(`成功复制 ${successCount} 条推广链接`)
    copyDialogVisible.value = false
    handleQuery()
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '复制失败')
  } finally {
    copyLoading.value = false
  }
}

const loadDramas = async () => {
  if (dramaOptions.value.length > 0) return
  try {
    const res = await request.get('/dramas', { params: { page: 1, pageSize: 1000 } })
    if (res.code === 0) {
      dramaOptions.value = res.data?.list || []
    }
  } catch (error) {
    console.error(error)
  }
}

const loadPlanGroups = async () => {
  if (planGroupOptions.value.length > 0) return
  try {
    const res = await request.get('/recharge-groups', { params: { page: 1, pageSize: 1000 } })
    if (res.code === 0) {
      planGroupOptions.value = res.data?.list || []
    }
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.delivery-links-container {
  padding: 20px;
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
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
</style>
