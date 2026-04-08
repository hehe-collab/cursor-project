<template>
  <div class="recharge-group-page friend-style page-list-layout">
    <el-card shadow="never" class="main-card filter-card">
      <el-form :model="filterForm" class="filter-form" inline size="small" label-position="left" @submit.prevent>
        <el-form-item label="分组ID" label-width="64px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.group_id" placeholder="分组ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="分组名" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.group_name" placeholder="分组名" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-actions filter-buttons">
            <el-button type="primary" :loading="loading" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </el-form-item>
        <div class="toolbar-row">
          <el-button type="primary" @click="handleAdd">
            <el-icon class="el-icon--left"><Plus /></el-icon>
            添加充值分组
          </el-button>
          <el-button type="danger" :disabled="!selectedRows.length" @click="handleBatchDelete">
            批量删除{{ selectedRows.length ? ` (${selectedRows.length})` : '' }}
          </el-button>
        </div>
      </el-form>
    </el-card>

    <el-card shadow="never" class="main-card table-card">
      <div class="table-wrapper">
        <el-table
          ref="tableRef"
          :data="tableData"
          v-loading="loading"
          border
          stripe
          class="data-table"
          height="100%"
          size="small"
          @selection-change="onSelectionChange"
        >
        <template #empty>
          <el-empty description="暂无方案组" />
        </template>
        <el-table-column type="selection" width="48" align="center" fixed />
        <el-table-column label="分组ID" width="200" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div v-if="row.group_id ?? row.groupId" class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制分组ID"
                @click="copyToClipboard(row.group_id ?? row.groupId, '分组ID')"
              />
              <span class="id-copy-row__text">{{ row.group_id ?? row.groupId }}</span>
            </div>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="group_name" label="分组名" min-width="130" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.group_name || row.name || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="充值选项" min-width="200">
          <template #default="{ row }">
            <template v-if="row.plan_items && row.plan_items.length">
              <el-tag
                v-for="p in row.plan_items"
                :key="p.id"
                type="success"
                size="small"
                class="plan-tag"
              >
                {{ p.name }}
              </el-tag>
            </template>
            <template v-else-if="(row.recharge_plan_ids || row.plan_ids || []).length">
              <el-tag
                v-for="planId in row.recharge_plan_ids || row.plan_ids || []"
                :key="planId"
                type="success"
                size="small"
                class="plan-tag"
              >
                {{ getPlanName(planId) }}
              </el-tag>
            </template>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="pixel_id" label="Pixel信号" width="130" show-overflow-tooltip />
        <el-table-column label="Pixel令牌" width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tooltip
              :content="row.pixel_token || '未设置'"
              placement="top"
              :disabled="!row.pixel_token"
            >
              <span class="token-mask-cell">{{ row.pixel_token_masked || '—' }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="创建人" width="100" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.creator || row.created_by_name || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatDateTime(row.created_at) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <div class="pagination-wrap compact-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          size="small"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      destroy-on-close
      @close="handleDialogClose"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="85px">
        <el-form-item label="分组名" prop="group_name">
          <el-input v-model="formData.group_name" placeholder="请输入分组名，例：首充优惠套餐" clearable />
        </el-form-item>
        <el-form-item label="投放媒体" prop="media_platform">
          <el-select v-model="formData.media_platform" placeholder="请选择投放媒体" clearable style="width: 100%">
            <el-option
              v-for="item in MEDIA_PLATFORMS"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="信号" prop="pixel_id">
          <el-input v-model="formData.pixel_id" placeholder="请输入 Pixel ID" clearable />
        </el-form-item>
        <el-form-item label="信号令牌" prop="pixel_token">
          <el-input
            v-model="formData.pixel_token"
            placeholder="请输入 Pixel Token"
            clearable
          />
        </el-form-item>
        <el-form-item label=" ">
          <el-button type="primary" :loading="testingPixel" @click="handleTestPixel">测试 Pixel</el-button>
          <span
            v-if="pixelTestResult"
            class="pixel-test-msg"
            :class="pixelTestResult.success ? 'is-ok' : 'is-fail'"
          >
            {{ pixelTestResult.message }}
          </span>
        </el-form-item>
        <el-form-item label="充值方案" required>
          <div class="plan-options-block">
            <div v-for="(_opt, index) in formData.plan_options" :key="index" class="plan-option-row">
              <span class="plan-option-label">选项{{ index + 1 }}：</span>
              <el-select
                v-model="formData.plan_options[index]"
                placeholder="请选择充值方案"
                clearable
                filterable
                class="plan-option-select"
              >
                <el-option
                  v-for="plan in rechargePlans"
                  :key="plan.id"
                  :label="plan.name"
                  :value="plan.id"
                />
              </el-select>
              <el-button
                v-if="formData.plan_options.length > 1"
                type="danger"
                link
                class="plan-option-del"
                @click="handleRemoveOption(index)"
              >
                删除
              </el-button>
            </div>
            <el-button type="primary" plain @click="handleAddOption">添加选项</el-button>
          </div>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, DocumentCopy } from '@element-plus/icons-vue'
import { copyToClipboard } from '@/utils/clipboard'
import { MEDIA_PLATFORMS } from '@/constants/media'
import { formatDateTimeDisplay as formatDateTime } from '@/utils/dateDisplay'
import {
  getRechargePlanGroups,
  getRechargePlanGroup,
  createRechargePlanGroup,
  updateRechargePlanGroup,
  deleteRechargePlanGroup,
  batchDeleteRechargePlanGroups,
  testRechargeGroupPixel,
  getRechargePlans,
} from '@/api/recharge'

const loading = ref(false)
const submitLoading = ref(false)
const tableData = ref([])
const tableRef = ref(null)
const selectedRows = ref([])
const rechargePlans = ref([])
const filterForm = reactive({
  group_id: '',
  group_name: '',
})
const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const dialogVisible = ref(false)
const dialogTitle = computed(() => (formData.id ? '编辑充值分组' : '添加充值分组'))

const formRef = ref()
const formData = reactive({
  id: null,
  group_name: '',
  media_platform: '',
  pixel_id: '',
  pixel_token: '',
  plan_options: [null],
  sort_order: 999,
  description: '',
  status: 'active',
  item_no: '',
})

const formRules = {
  group_name: [{ required: true, message: '请输入分组名', trigger: 'blur' }],
  media_platform: [{ required: true, message: '请选择投放媒体', trigger: 'change' }],
  pixel_id: [{ required: true, message: '请输入 Pixel ID', trigger: 'blur' }],
  pixel_token: [{ required: true, message: '请输入 Pixel Token', trigger: 'blur' }],
}

const testingPixel = ref(false)
const pixelTestResult = ref(null)

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function getPlanName(planId) {
  const plan = rechargePlans.value.find((p) => p.id === planId)
  return plan ? plan.name : `方案 ${planId}`
}

async function fetchRechargePlans() {
  try {
    const res = await getRechargePlans({ page: 1, pageSize: 500 })
    rechargePlans.value = res.data?.list || []
  } catch (e) {
    console.error(e)
  }
}

async function fetchData() {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      pageSize: pagination.pageSize,
    }
    if (filterForm.group_id) params.group_id = filterForm.group_id
    if (filterForm.group_name) params.group_name = filterForm.group_name
    const res = await getRechargePlanGroups(params)
    tableData.value = res.data?.list || []
    pagination.total = res.data?.total ?? 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  fetchData()
}

function handleReset() {
  filterForm.group_id = ''
  filterForm.group_name = ''
  pagination.page = 1
  fetchData()
}

function handleSizeChange() {
  pagination.page = 1
  fetchData()
}

function handlePageChange() {
  fetchData()
}

function handleAdd() {
  formData.id = null
  formData.group_name = ''
  formData.media_platform = ''
  formData.pixel_id = ''
  formData.pixel_token = ''
  formData.plan_options = [null]
  formData.sort_order = 999
  formData.description = ''
  formData.status = 'active'
  formData.item_no = ''
  pixelTestResult.value = null
  dialogVisible.value = true
}

async function handleEdit(row) {
  pixelTestResult.value = null
  try {
    const res = await getRechargePlanGroup(row.id)
    const full = res.data
    if (!full) return
    const ids = full.recharge_plan_ids || full.plan_ids || []
    formData.id = full.id
    formData.group_name = full.group_name || full.name || ''
    formData.media_platform = full.media_platform || ''
    formData.pixel_id = full.pixel_id || ''
    formData.pixel_token = full.pixel_token || ''
    formData.plan_options = ids.length ? [...ids] : [null]
    formData.sort_order = full.sort_order != null ? full.sort_order : 999
    formData.description = full.description || ''
    formData.status = full.status || 'active'
    formData.item_no = full.item_no || ''
    dialogVisible.value = true
  } catch (e) {
    console.error(e)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该充值分组吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteRechargePlanGroup(row.id)
    ElMessage.success('删除成功')
    tableRef.value?.clearSelection()
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

async function handleBatchDelete() {
  const rows = selectedRows.value
  if (!rows.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${rows.length} 个充值分组？`, '批量删除', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    const ids = rows.map((r) => r.id)
    const res = await batchDeleteRechargePlanGroups(ids)
    ElMessage.success(res.message || '删除成功')
    tableRef.value?.clearSelection()
    selectedRows.value = []
    fetchData()
  } catch (e) {
    console.error(e)
  }
}

function handleAddOption() {
  formData.plan_options.push(null)
}

function handleRemoveOption(index) {
  formData.plan_options.splice(index, 1)
}

async function handleTestPixel() {
  if (!formData.pixel_id || !formData.pixel_token) {
    ElMessage.warning('请先输入 Pixel ID 和 Token')
    return
  }
  testingPixel.value = true
  pixelTestResult.value = null
  try {
    await testRechargeGroupPixel({
      pixel_id: formData.pixel_id,
      pixel_token: formData.pixel_token,
      media_platform: formData.media_platform,
    })
    pixelTestResult.value = { success: true, message: 'Pixel 验证成功' }
    ElMessage.success('Pixel 验证成功')
  } catch {
    pixelTestResult.value = {
      success: false,
      message: 'Pixel 验证失败，请检查 ID 和令牌',
    }
  } finally {
    testingPixel.value = false
  }
}

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  const validPlanIds = formData.plan_options.filter((id) => id !== null && id !== undefined && id !== '')
  if (!validPlanIds.length) {
    ElMessage.warning('请至少选择一个充值方案')
    return
  }
  const payload = {
    id: formData.id,
    group_name: formData.group_name,
    name: formData.group_name,
    media_platform: formData.media_platform,
    pixel_id: formData.pixel_id,
    pixel_token: formData.pixel_token,
    plan_ids: validPlanIds,
    recharge_plan_ids: validPlanIds,
    sort_order: formData.sort_order,
    description: formData.description,
    status: formData.status,
    item_no: formData.item_no,
  }
  submitLoading.value = true
  try {
    if (formData.id) {
      await updateRechargePlanGroup(payload)
      ElMessage.success('更新成功')
    } else {
      await createRechargePlanGroup(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e) {
    console.error(e)
  } finally {
    submitLoading.value = false
  }
}

function handleDialogClose() {
  formRef.value?.resetFields()
  pixelTestResult.value = null
  formData.plan_options = [null]
}

onMounted(() => {
  fetchRechargePlans()
  fetchData()
})
</script>

<style scoped>
.recharge-group-page .main-card {
  border-radius: 4px;
}
.recharge-group-page .main-card.filter-card {
  margin-bottom: var(--section-gap);
}
.filter-form {
  margin-bottom: 0;
}
.filter-item :deep(.el-form-item__label) {
  font-weight: 500;
}
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 4px;
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
.plan-tag {
  margin-right: 5px;
  margin-bottom: 4px;
}
.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}
.pixel-test-msg {
  margin-left: 10px;
  font-size: 13px;
}
.pixel-test-msg.is-ok {
  color: var(--el-color-success);
}
.pixel-test-msg.is-fail {
  color: var(--el-color-danger);
}
.plan-options-block {
  width: 100%;
}
.plan-option-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}
.plan-option-label {
  flex: 0 0 auto;
  width: 56px;
  font-size: 13px;
  color: var(--el-text-color-regular);
}
.plan-option-select {
  flex: 1;
  min-width: 0;
}
.plan-option-del {
  flex-shrink: 0;
}
.token-mask-cell {
  font-family: ui-monospace, monospace;
  font-size: 12px;
  cursor: default;
}
</style>
