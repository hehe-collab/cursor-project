<template>
  <div class="recharge-plan-page friend-style page-list-layout">
    <el-card shadow="never" class="main-card filter-card">
      <el-form :model="query" class="filter-form" inline size="small" label-position="left" @submit.prevent>
        <el-form-item label="方案名称" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="query.name" placeholder="名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label="支付平台" label-width="70px">
          <div class="filter-item-select">
            <el-select v-model="query.payment_platform" placeholder="全部" clearable>
              <el-option
                v-for="item in PAYMENT_PLATFORMS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="ID" class="filter-id-item" label-width="40px">
          <div class="filter-item-m">
            <el-input v-model="query.id" placeholder="ID / UUID" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-actions filter-buttons">
            <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
            <el-button @click="onReset">重置</el-button>
          </div>
        </el-form-item>
        <div class="toolbar-row">
          <el-button type="success" @click="showAdd">
            <el-icon class="el-icon--left"><Plus /></el-icon>
            新增
          </el-button>
          <el-button type="warning" :disabled="selectedRows.length !== 1" @click="onToolbarEdit">修改</el-button>
          <el-button type="danger" :disabled="!selectedRows.length" @click="onBatchDelete">删除</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="main-card table-card">
      <div class="table-wrapper">
        <el-table
          ref="tableRef"
          :data="list"
          v-loading="loading"
          border
          stripe
          class="data-table"
          height="100%"
          size="small"
          @selection-change="onSelectionChange"
        >
        <template #empty>
          <el-empty description="暂无充值方案" />
        </template>
        <el-table-column type="selection" width="48" align="center" fixed />
        <el-table-column label="ID" min-width="220" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制方案ID"
                @click="copyToClipboard(row.display_id || row.uuid || row.id, '方案ID')"
              />
              <span class="id-copy-row__text id-text">{{ row.display_id || row.uuid || row.id }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
        <el-table-column label="解锁全集" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.unlock_full_series ? 'success' : 'info'" size="small">
              {{ row.unlock_full_series ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="actual_coins" label="实际到账金豆" width="120" align="center" />
        <el-table-column label="赠送金豆数" width="110" align="center">
          <template #default="{ row }">
            {{ row.bonus_coins !== 0 ? row.bonus_coins : row.extra_bean !== 0 ? row.extra_bean : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="支付平台" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.payment_platform || row.pay_platform || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="center">
          <template #default="{ row }">
            {{ formatAmount(row.amount, row.currency) }}
          </template>
        </el-table-column>
        <el-table-column prop="currency" label="货币" width="72" align="center" />
        <el-table-column prop="recharge_info" label="充值信息" min-width="160" show-overflow-tooltip />
        <el-table-column label="创建时间" width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEdit(row)">修改</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination-wrap compact-pagination">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[20, 50, 100, 200]"
          size="small"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadList"
          @size-change="loadList"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="formVisible"
      :title="formId ? '修改充值方案' : '新增充值方案'"
      width="500px"
      class="recharge-plan-dialog"
      destroy-on-close
      @closed="onDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="85px" class="recharge-plan-form">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="如：首充优惠" clearable />
        </el-form-item>
        <el-form-item label="解锁全集" prop="unlock_full_series">
          <el-radio-group v-model="form.unlock_full_series" class="unlock-radio-group">
            <el-radio :label="true">是</el-radio>
            <el-radio :label="false">否（金豆数必填）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="实际到账金豆" prop="actual_coins">
          <el-input
            v-model="form.actual_coins"
            type="text"
            inputmode="numeric"
            placeholder="例：300"
            clearable
            @input="handleCoinsInput('actual_coins')"
          />
        </el-form-item>
        <el-form-item label="赠送金豆数" prop="bonus_coins">
          <el-input
            v-model="form.bonus_coins"
            type="text"
            inputmode="numeric"
            placeholder="可选，仅展示"
            clearable
            @input="handleCoinsInput('bonus_coins')"
          />
        </el-form-item>
        <el-form-item label="支付平台" prop="payment_platform">
          <el-select v-model="form.payment_platform" placeholder="请选择" style="width: 100%" clearable>
            <el-option
              v-for="item in PAYMENT_PLATFORMS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input
            v-model="form.amount"
            type="text"
            inputmode="decimal"
            placeholder="例：9.9"
            clearable
            @input="handleAmountInput"
          />
        </el-form-item>
        <el-form-item label="货币" prop="currency">
          <el-select v-model="form.currency" placeholder="请选择货币" style="width: 100%" clearable>
            <el-option
              v-for="item in currencyOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="充值信息" prop="recharge_info">
          <el-input
            v-model="form.recharge_info"
            placeholder="展示给用户，例：300金豆（当地语言）"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, DocumentCopy } from '@element-plus/icons-vue'
import { copyToClipboard } from '@/utils/clipboard'
import {
  getRechargePlans,
  createRechargePlan,
  updateRechargePlan,
  deleteRechargePlan,
} from '@/api/recharge'
import { useCountries } from '@/composables/useCountries'
import { PAYMENT_PLATFORMS, getCurrencyOptionsForAccounts, getCurrencyInfo } from '@/constants/payment'
import { useConfirmDelete } from '@/composables/useConfirmDelete'
import { formatDateTimeDisplay as formatDateTime } from '@/utils/dateDisplay'

const { countries } = useCountries()

const currencyOptions = computed(() => getCurrencyOptionsForAccounts(countries.value))

function formatAmount(amount, currency) {
  const n = Number(amount)
  const formatted = Number.isFinite(n) ? n.toFixed(1) : '0.0'
  const info = getCurrencyInfo(currency)
  if (info && info.symbol) return `${info.symbol}${formatted}`
  if (info) return `${formatted} ${info.code}`
  return formatted
}

const loading = ref(false)
const submitLoading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formId = ref(null)
const tableRef = ref()
const selectedRows = ref([])
const query = reactive({
  page: 1,
  pageSize: 20,
  id: '',
  name: '',
  payment_platform: '',
})
const formRef = ref()

const form = reactive({
  name: '',
  unlock_full_series: false,
  actual_coins: '',
  bonus_coins: '',
  payment_platform: '',
  amount: '',
  currency: 'USD',
  recharge_info: '',
  is_recommended: false,
  is_hot: false,
  status: 'active',
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  unlock_full_series: [
    {
      validator: (_rule, v, cb) => {
        if (v !== true && v !== false) cb(new Error('请选择是否解锁全集'))
        else cb()
      },
      trigger: 'change',
    },
  ],
  actual_coins: [
    {
      validator: (_rule, value, cb) => {
        if (form.unlock_full_series) {
          cb()
          return
        }
        const n = parseInt(String(value ?? ''), 10)
        if (value === '' || value == null || Number.isNaN(n) || n < 1) {
          cb(new Error('请输入大于0的整数'))
        } else cb()
      },
      trigger: 'blur',
    },
  ],
  payment_platform: [{ required: true, message: '请选择支付平台', trigger: 'change' }],
  amount: [
    { required: true, message: '请输入金额', trigger: 'blur' },
    {
      validator: (_rule, value, cb) => {
        const n = parseFloat(String(value ?? ''))
        if (value === '' || value == null || Number.isNaN(n) || n <= 0) {
          cb(new Error('请输入大于0的金额'))
        } else cb()
      },
      trigger: 'blur',
    },
  ],
  currency: [{ required: true, message: '请选择货币', trigger: 'change' }],
  recharge_info: [{ required: true, message: '请输入充值信息', trigger: 'blur' }],
}

function handleCoinsInput(field) {
  let v = form[field]
  if (v === '' || v == null) {
    form[field] = ''
    return
  }
  v = String(v).replace(/[^\d]/g, '')
  form[field] = v
}

function handleAmountInput() {
  let value = form.amount
  if (value === '' || value == null) return
  value = String(value).replace(/[^\d.]/g, '')
  const parts = value.split('.')
  if (parts.length > 2) {
    value = parts[0] + '.' + parts.slice(1).join('')
  }
  const p2 = value.split('.')
  if (p2.length === 2 && p2[1].length > 1) {
    value = p2[0] + '.' + p2[1].substring(0, 1)
  }
  form.amount = value
}

function formatAmountOneDecimal(n) {
  const x = Number(n)
  if (!Number.isFinite(x)) return ''
  return x.toFixed(1)
}

function resetForm() {
  Object.assign(form, {
    name: '',
    unlock_full_series: false,
    actual_coins: '',
    bonus_coins: '',
    payment_platform: '',
    amount: '',
    currency: currencyOptions.value[0]?.value || 'USD',
    recharge_info: '',
    is_recommended: false,
    is_hot: false,
    status: 'active',
  })
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

async function loadList() {
  loading.value = true
  try {
    const params = {
      page: query.page,
      pageSize: query.pageSize,
    }
    if (query.id) params.id = query.id
    if (query.name) params.name = query.name
    if (query.payment_platform) params.payment_platform = query.payment_platform
    const body = await getRechargePlans(params)
    list.value = body.data?.list || []
    total.value = body.data?.total || 0
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  query.id = ''
  query.name = ''
  query.payment_platform = ''
  query.page = 1
  loadList()
}

function showAdd() {
  formId.value = null
  resetForm()
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  const bean = row.actual_coins ?? row.bean_count ?? 0
  const extra = row.bonus_coins ?? row.extra_bean ?? 0
  Object.assign(form, {
    name: row.name || '',
    unlock_full_series: row.unlock_full_series === true,
    actual_coins: bean ? String(parseInt(String(bean), 10)) : '',
    bonus_coins: extra ? String(parseInt(String(extra), 10)) : '',
    payment_platform: row.payment_platform || row.pay_platform || '',
    amount: formatAmountOneDecimal(row.amount ?? 0),
    currency: row.currency || 'USD',
    recharge_info: row.recharge_info || row.description || '',
    is_recommended: !!row.is_recommended,
    is_hot: !!row.is_hot,
    status: row.status || 'active',
  })
  formVisible.value = true
}

function onToolbarEdit() {
  if (selectedRows.value.length === 1) showEdit(selectedRows.value[0])
}

function onDialogClosed() {
  formRef.value?.resetFields?.()
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  submitLoading.value = true
  try {
    let actualCoins = parseInt(String(form.actual_coins).replace(/[^\d]/g, ''), 10)
    if (Number.isNaN(actualCoins)) actualCoins = 0
    const bonusCoins = form.bonus_coins === '' || form.bonus_coins == null ? 0 : parseInt(String(form.bonus_coins), 10) || 0
    const amountNum = parseFloat(String(form.amount))
    const amountOne = Number.isFinite(amountNum) ? Math.round(amountNum * 10) / 10 : 0
    const payload = {
      name: form.name,
      unlock_full_series: form.unlock_full_series,
      actual_coins: actualCoins,
      bonus_coins: bonusCoins,
      payment_platform: form.payment_platform,
      pay_platform: form.payment_platform,
      bean_count: actualCoins,
      extra_bean: bonusCoins,
      amount: amountOne,
      currency: form.currency,
      recharge_info: form.recharge_info,
      is_recommended: form.is_recommended,
      is_hot: form.is_hot,
      status: form.status,
    }
    if (formId.value) {
      payload.id = formId.value
      await updateRechargePlan(payload)
      ElMessage.success('修改成功')
    } else {
      await createRechargePlan(payload)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    submitLoading.value = false
  }
}

const { confirmDelete } = useConfirmDelete({ onSuccess: loadList })
function onDelete(row) {
  confirmDelete(async (r) => deleteRechargePlan(r.id), row)
}

async function onBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${rows.length} 条充值方案？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  loading.value = true
  try {
    for (const r of rows) {
      await deleteRechargePlan(r.id)
    }
    ElMessage.success('删除成功')
    selectedRows.value = []
    tableRef.value?.clearSelection?.()
    loadList()
  } catch {
    ElMessage.error('部分删除失败，请重试')
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.friend-style .main-card {
  border-radius: 4px;
}
.filter-form {
  margin-bottom: 12px;
}
.filter-id-item {
  margin-bottom: 0;
}
.filter-id-item :deep(.el-form-item__label) {
  font-weight: 500;
}
.filter-actions {
  display: flex;
  gap: 8px;
}
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 0;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.data-table {
  width: 100%;
}
.data-table :deep(.el-table__cell) {
  white-space: nowrap;
}
.data-table :deep(.el-table__cell .cell) {
  overflow: hidden;
  text-overflow: ellipsis;
}
.id-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.id-icon {
  flex-shrink: 0;
}
.id-text {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}

/* 弹窗紧凑（指令 #027 样式优化） */
.recharge-plan-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
  padding-bottom: 4px;
}
.recharge-plan-form :deep(.el-form-item) {
  margin-bottom: 14px;
}
.unlock-radio-group {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.unlock-radio-group :deep(.el-radio) {
  margin-right: 0;
  height: auto;
  line-height: 1.4;
}

/* 若仍使用数字输入框，隐藏步进箭头 */
:deep(.el-input-number .el-input-number__decrease),
:deep(.el-input-number .el-input-number__increase) {
  display: none !important;
}
:deep(.el-input-number .el-input__wrapper) {
  padding-left: 11px;
  padding-right: 11px;
}
</style>
