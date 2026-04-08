<template>
  <div class="callback-config-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form :model="filterForm" class="filter-form" inline size="small" label-position="left">
        <el-form-item label="平台类型" label-width="70px">
          <div class="filter-item-s">
            <el-select v-model="filterForm.platform" placeholder="平台" clearable>
              <el-option label="TikTok" value="tiktok" />
              <el-option label="Facebook" value="facebook" />
              <el-option label="Google" value="google" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="推广链接" label-width="90px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.linkId" placeholder="链接ID / 关键词" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="button-group">
        <div class="button-group-left">
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="primary" :disabled="selectedRows.length !== 1" @click="handleEdit">
            <el-icon><Edit /></el-icon>
            修改
          </el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="handleBatchDelete">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </div>
        <div class="button-group-right"></div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="tableData"
          border
          stripe
          height="100%"
          size="small"
          @selection-change="handleSelectionChange"
        >
        <el-table-column type="selection" width="55" align="center" />

        <el-table-column label="推广链接 ID" min-width="200" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制推广链接ID"
                @click="
                  copyToClipboard(
                    row.promote_id ?? row.promoteId ?? row.link_id ?? row.linkId,
                    '推广链接ID',
                  )
                "
              />
              <span class="id-copy-row__text">{{ rowPromotionLinkId(row) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="platform" label="平台类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag>{{ platformLabel(row.platform) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="cold_start_count" label="冷启动数" width="120" align="center" />

        <el-table-column prop="min_price_limit" label="最低价格限制" width="150" align="center" />

        <el-table-column label="复充回传" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.replenish_callback_enabled === false ? 'info' : 'success'" size="small">
              {{ row.replenish_callback_enabled === false ? '关闭' : '开启' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="策略" width="100" align="center">
          <template #default="{ row }">
            {{ strategySummary(row) }}
          </template>
        </el-table-column>

        <el-table-column prop="created_at" label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>

        <el-table-column prop="creator" label="创建人" width="120" align="center" />

        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEditRow(row)">修改</el-button>
            <el-button type="danger" link @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination-container compact-pagination">
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

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="800px"
      destroy-on-close
      :close-on-click-modal="false"
      class="callback-config-dialog callback-dialog-compact"
    >
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="95px" class="callback-config-form">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="平台类型" prop="platform">
              <el-select v-model="formData.platform" placeholder="请选择平台类型" style="width: 100%" clearable>
                <el-option label="TikTok" value="tiktok" />
                <el-option label="Facebook" value="facebook" />
                <el-option label="Google" value="google" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推广链接" prop="link_id">
              <el-select
                v-model="formData.link_id"
                filterable
                remote
                reserve-keyword
                :remote-method="fetchDeliveryLinks"
                :loading="linkSearchLoading"
                placeholder="输入名称、推广ID 或数字ID 搜索"
                style="width: 100%"
                clearable
                @visible-change="onLinkDropdownVisible"
              >
                <el-option
                  v-for="opt in linkOptions"
                  :key="`${opt.id ?? ''}-${deliveryLinkOptionValue(opt)}`"
                  :label="opt.label"
                  :value="deliveryLinkOptionValue(opt)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="冷启动数" prop="cold_start_count">
              <el-input-number
                v-model="formData.cold_start_count"
                :min="0"
                controls-position="right"
                placeholder="请输入冷启动数"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="最低价格限制" prop="min_price_limit">
              <el-input-number
                v-model="formData.min_price_limit"
                :min="0"
                :precision="2"
                controls-position="right"
                placeholder="请输入最低价格限制"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="复充回传">
          <el-switch
            v-model="formData.replenish_callback_enabled"
            inline-prompt
            active-text="开"
            inactive-text="关"
          />
        </el-form-item>

        <el-form-item label="回传策略">
          <div class="strategy-list">
            <div
              v-for="(row, index) in strategyRows"
              :key="index"
              class="strategy-row-compact"
            >
              <span class="strategy-label">策略{{ index + 1 }}</span>
              <el-input-number
                v-model="row.amount_min"
                :min="0"
                :precision="2"
                controls-position="right"
                class="strategy-num-tight"
              />
              <span class="strategy-sep" v-text="'≤金额≤'" />
              <el-input-number
                v-model="row.amount_max"
                :min="0"
                :precision="2"
                controls-position="right"
                class="strategy-num-tight"
              />
              <span class="param-label-inline">传:</span>
              <el-input v-model="row.param_chuan" clearable class="param-input-tight" />
              <span class="param-label-inline">卡:</span>
              <el-input v-model="row.param_ka" clearable class="param-input-tight" />
              <el-button
                type="danger"
                text
                size="small"
                :disabled="strategyRows.length <= 1"
                @click="removeStrategyRow(index)"
              >
                删除
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label-width="0" class="strategy-add-form-item">
          <el-button type="primary" plain class="strategy-add-full" @click="addStrategyRow">
            <el-icon><Plus /></el-icon>
            添加策略
          </el-button>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, RefreshLeft, DocumentCopy } from '@element-plus/icons-vue'
import request from '../api/request'
import { copyToClipboard } from '@/utils/clipboard'

const PLATFORM_LABEL = { tiktok: 'TikTok', facebook: 'Facebook', google: 'Google' }
function platformLabel(p) {
  return PLATFORM_LABEL[p] || p || '-'
}

/** 列表展示：优先业务推广ID（promote_id），与投放链接列表「推广ID」列一致 */
function rowPromotionLinkId(row) {
  if (!row) return '—'
  const v = row.promote_id ?? row.promoteId ?? row.link_id ?? row.linkId
  if (v === null || v === undefined || v === '') return '—'
  return String(v)
}

/** 下拉取值：与库表 promote_id 一致；无则退回数字主键（兼容旧数据） */
function deliveryLinkOptionValue(opt) {
  if (!opt) return ''
  const p = opt.promo_id != null && String(opt.promo_id).trim() !== '' ? String(opt.promo_id).trim() : ''
  if (p) return p
  if (opt.id != null && opt.id !== '') return String(opt.id)
  return ''
}

function strategySummary(row) {
  if (!row?.config_json) return '—'
  try {
    const o = JSON.parse(row.config_json)
    const arr = o.strategies
    const n = Array.isArray(arr) ? arr.length : 0
    return n ? `${n} 条` : '—'
  } catch {
    return '—'
  }
}

function emptyStrategyRow() {
  return {
    amount_min: 0,
    amount_max: null,
    param_chuan: '',
    param_ka: '',
  }
}

function defaultStrategyRows() {
  return [emptyStrategyRow()]
}

function parseStrategiesFromRow(row) {
  if (!row?.config_json) return defaultStrategyRows()
  try {
    const o = JSON.parse(row.config_json)
    const arr = Array.isArray(o.strategies) ? o.strategies : Array.isArray(o) ? o : []
    if (!arr.length) return defaultStrategyRows()
    return arr.map((s) => {
      const rawP = s.params
      let param_chuan = ''
      let param_ka = ''
      if (rawP != null && typeof rawP === 'object' && !Array.isArray(rawP)) {
        param_chuan = rawP['传'] != null ? String(rawP['传']) : ''
        param_ka = rawP['卡'] != null ? String(rawP['卡']) : ''
      } else if (typeof rawP === 'string') {
        param_chuan = rawP
      } else if (s.send_params != null) {
        param_chuan = String(s.send_params)
      }
      const lo = s.amount_min != null ? Number(s.amount_min) : 0
      const hiRaw = s.amount_max
      const hi =
        hiRaw === null || hiRaw === '' || hiRaw === undefined ? null : Number(hiRaw)
      return {
        amount_min: Number.isFinite(lo) ? lo : 0,
        amount_max: hi != null && Number.isFinite(hi) ? hi : null,
        param_chuan,
        param_ka,
      }
    })
  } catch {
    return defaultStrategyRows()
  }
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

const filterForm = ref({
  platform: '',
  linkId: '',
})

const tableData = ref([])
const loading = ref(false)
const selectedRows = ref([])

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
})

const strategyRows = ref(defaultStrategyRows())

const formData = ref({
  id: null,
  link_id: '',
  platform: '',
  cold_start_count: 0,
  min_price_limit: 0,
  replenish_callback_enabled: false,
})

const dialogVisible = ref(false)
const dialogTitle = computed(() => (formData.value.id ? '修改回传配置' : '新增回传配置'))
const submitLoading = ref(false)
const formRef = ref(null)

function addStrategyRow() {
  strategyRows.value.push(emptyStrategyRow())
}

function removeStrategyRow(index) {
  if (strategyRows.value.length <= 1) return
  strategyRows.value.splice(index, 1)
}

function ensureLinkOptionForEdit() {
  const sid = formData.value.link_id
  if (sid === null || sid === undefined || String(sid).trim() === '') {
    linkOptions.value = []
    return
  }
  const s = String(sid).trim()
  linkOptions.value = [
    {
      id: s,
      promo_id: s,
      label: `推广ID ${s}`,
    },
  ]
}

const linkSearchLoading = ref(false)
const linkOptions = ref([])

const fetchDeliveryLinks = async (query) => {
  linkSearchLoading.value = true
  try {
    const res = await request.get('/delivery-links/search', {
      params: { keyword: query || undefined, limit: 40 },
    })
    if (res.code === 0) {
      linkOptions.value = Array.isArray(res.data) ? res.data : []
    }
  } catch {
    linkOptions.value = []
  } finally {
    linkSearchLoading.value = false
  }
}

function onLinkDropdownVisible(visible) {
  if (visible && (!linkOptions.value || linkOptions.value.length === 0)) {
    fetchDeliveryLinks('')
  }
}

const rules = {
  link_id: [{ required: true, message: '请选择推广链接', trigger: 'change' }],
  platform: [{ required: true, message: '请选择平台类型', trigger: 'change' }],
  cold_start_count: [{ required: true, message: '请输入冷启动数', trigger: 'blur' }],
  min_price_limit: [{ required: true, message: '请输入最低价格限制', trigger: 'blur' }],
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await request.get('/callback/config', {
      params: {
        platform: filterForm.value.platform,
        link_id: filterForm.value.linkId,
        page: pagination.value.page,
        pageSize: pagination.value.pageSize,
      },
    })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.value.total = res.data?.total ?? 0
    }
  } catch (error) {
    ElMessage.error(`查询失败：${error.message || ''}`)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = { platform: '', linkId: '' }
  pagination.value.page = 1
  handleQuery()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleAdd = () => {
  linkOptions.value = []
  formData.value = {
    id: null,
    link_id: '',
    platform: '',
    cold_start_count: 0,
    min_price_limit: 0,
    replenish_callback_enabled: false,
  }
  strategyRows.value = defaultStrategyRows()
  dialogVisible.value = true
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) return
  handleEditRow(selectedRows.value[0])
}

const handleEditRow = (row) => {
  const lid = rowPromotionLinkId(row)
  formData.value = {
    ...row,
    link_id: lid === '—' ? '' : lid,
    replenish_callback_enabled: row.replenish_callback_enabled !== false,
  }
  strategyRows.value = parseStrategiesFromRow(row)
  ensureLinkOptionForEdit()
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
    const url = formData.value.id ? `/callback/config/${formData.value.id}` : '/callback/config'
    const method = formData.value.id ? 'put' : 'post'
    const strategies = strategyRows.value.map((r) => {
      const lo = r.amount_min == null || r.amount_min === '' ? 0 : Number(r.amount_min)
      const hi =
        r.amount_max === null || r.amount_max === '' || r.amount_max === undefined
          ? null
          : Number(r.amount_max)
      const params = {}
      const a = (r.param_chuan || '').trim()
      const b = (r.param_ka || '').trim()
      if (a) params['传'] = a
      if (b) params['卡'] = b
      return {
        amount_min: Number.isFinite(lo) ? lo : 0,
        amount_max: hi != null && Number.isFinite(hi) ? hi : null,
        params,
      }
    })

    if (!strategies.length) {
      ElMessage.error('请至少保留一条策略')
      submitLoading.value = false
      return
    }
    for (let i = 0; i < strategies.length; i++) {
      const s = strategies[i]
      if (s.amount_max != null && s.amount_min > s.amount_max) {
        ElMessage.error(`策略 ${i + 1}：最低金额不能大于最高金额`)
        submitLoading.value = false
        return
      }
    }

    const payload = {
      link_id: formData.value.link_id,
      platform: formData.value.platform,
      cold_start_count: formData.value.cold_start_count,
      min_price_limit: formData.value.min_price_limit,
      replenish_callback_enabled: formData.value.replenish_callback_enabled,
      strategies,
    }

    const res = await request[method](url, payload)
    if (res.code === 0) {
      ElMessage.success(formData.value.id ? '修改成功' : '新增成功')
      dialogVisible.value = false
      handleQuery()
    }
  } catch (error) {
    ElMessage.error(`操作失败：${error.message || ''}`)
  } finally {
    submitLoading.value = false
  }
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这条配置吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/callback/config/${row.id}`)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      handleQuery()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`删除失败：${error.message || ''}`)
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条记录吗？`, '提示', {
      type: 'warning',
    })
    const ids = selectedRows.value.map((row) => row.id)
    const res = await request.post('/callback/config/batch-delete', { ids })
    if (res.code === 0) {
      ElMessage.success('删除成功')
      handleQuery()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`删除失败：${error.message || ''}`)
    }
  }
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.callback-config-container {
  padding: 0;
}

.filter-card {
  margin-bottom: var(--section-gap);
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}

.strategy-list {
  width: 100%;
}

.strategy-row-compact {
  display: flex;
  align-items: center;
  flex-wrap: nowrap;
  gap: 6px;
  padding: 8px 10px;
  margin-bottom: 10px;
  background-color: #f9fafb;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  font-size: 13px;
  overflow-x: visible;
}

.strategy-label {
  font-weight: 600;
  font-size: 13px;
  color: #303133;
  min-width: 40px;
  flex-shrink: 0;
}

.strategy-num-tight {
  width: 75px;
  flex-shrink: 0;
}

.strategy-sep {
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
  padding: 0 1px;
  flex-shrink: 0;
  min-width: 0;
}

.param-label-inline {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  min-width: 20px;
  flex-shrink: 0;
}

.param-input-tight {
  width: 130px;
  flex-shrink: 0;
}

.strategy-row-compact :deep(.el-input-number) {
  font-size: 13px;
}

.strategy-row-compact :deep(.el-input__wrapper) {
  font-size: 13px;
  padding: 0 8px;
}

.strategy-row-compact :deep(.el-button) {
  font-size: 13px;
  padding: 4px 8px;
  flex-shrink: 0;
}

.strategy-add-form-item {
  margin-bottom: 0;
}

.strategy-add-form-item :deep(.el-form-item__content) {
  margin-left: 0 !important;
}

.strategy-add-full {
  width: 100%;
  margin-top: 4px;
}

</style>

<style>
/* 弹窗 teleport 到 body，单独块保证 .el-dialog__body 内边距生效 */
.callback-dialog-compact.el-dialog .el-dialog__body {
  padding: 15px 12px;
}
</style>
