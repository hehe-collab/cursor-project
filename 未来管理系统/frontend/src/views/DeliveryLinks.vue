<template>
  <div class="delivery-links-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form
        :model="filterForm"
        class="filter-form delivery-filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleSearchClick"
        @keyup.enter="handleSearchClick"
      >
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input
              v-model="filterForm.promoId"
              placeholder="推广ID"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
          </div>
        </el-form-item>
        <el-form-item label="剧的ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.dramaId" placeholder="剧的ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="投放媒体" label-width="70px">
          <div class="filter-item-select">
            <el-select
              v-model="filterForm.media"
              placeholder="媒体"
              clearable
              @change="handleFilterImmediate"
            >
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
              :placeholder="countryMultiOptions.length ? '全部' : '请先在账户管理维护广告账户国家'"
              clearable
              @change="handleFilterImmediate"
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
            <el-input
              v-model="filterForm.promoName"
              placeholder="推广名称"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
          </div>
        </el-form-item>
        <el-form-item label="推广域名" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.promoDomain" placeholder="域名" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" :loading="loading" @click="handleSearchClick">
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
          <el-button
            v-if="tableData.length > 50"
            size="small"
            @click="useVirtualScroll = !useVirtualScroll"
          >
            {{ useVirtualScroll ? '标准表格' : '虚拟滚动' }}
          </el-button>
          <el-button type="warning" :loading="exporting" @click="handleExport">
            <el-icon class="el-icon--left"><Download /></el-icon>
            导出
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-card-header-row">
          <span>
            投放链接列表
            <el-tag v-if="searching" type="info" size="small" class="searching-tag">搜索中...</el-tag>
          </span>
        </div>
      </template>
      <div
        class="table-wrapper"
        :class="{ 'table-wrapper--virtual': useVirtualScroll }"
      >
        <Loading v-if="useVirtualScroll" :loading="loading" text="加载中..." />
        <el-table
          v-if="!useVirtualScroll"
          v-loading="loading"
          element-loading-text="加载中..."
          :data="tableData"
          border
          stripe
          style="width: 100%"
          height="100%"
          size="small"
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
        <el-table-column label="推广ID" width="190" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制推广ID"
                @click="copyToClipboard(row.promo_id ?? row.promoId, '推广ID')"
              />
              <span class="id-copy-row__text">{{ (row.promo_id ?? row.promoId) || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="剧的ID" width="200" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制剧ID"
                @click="copyToClipboard(row.drama_public_id ?? row.dramaPublicId ?? row.drama_id ?? row.dramaId, '剧ID')"
              />
              <span class="id-copy-row__text">{{ row.drama_public_id || row.drama_id || '—' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="media" label="投放媒体" width="120" align="center" />
        <el-table-column label="国家/地区" width="120" align="center" show-overflow-tooltip>
          <template #default="{ row }">
            {{ formatAccountCountryLabel(row.country) }}
          </template>
        </el-table-column>
        <el-table-column prop="promo_name" label="推广名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="drama_name" label="剧名" width="150" show-overflow-tooltip />
        <el-table-column prop="plan_group_id" label="方案组ID" width="180" show-overflow-tooltip />
        <el-table-column prop="beans_per_episode" label="金豆数" width="100" align="center" />
        <el-table-column prop="free_episodes" label="免费集数" width="100" align="center" />
        <el-table-column prop="created_by" label="创建人" width="120" align="center" />
        <el-table-column prop="created_at" label="创建时间" width="180" align="center" />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <div class="delivery-link-row-actions">
              <el-button size="small" @click="handleCopyLink(row)">复制链接</el-button>
              <el-button size="small" @click="handleCopy(row)">复制</el-button>
              <el-button size="small" @click="handleEditRow(row)">修改</el-button>
              <el-button size="small" type="danger" @click="handleDeleteRow(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <VirtualTable
        v-else
        :data="tableData"
        :columns="virtualColumns"
        :item-size="52"
        key-field="id"
      >
        <template #selection="{ row }">
          <el-checkbox
            :model-value="isRowSelected(row)"
            @change="(v) => toggleRowSelected(row, v)"
          />
        </template>
        <template #enabledTag="{ row }">
          <el-tag :type="row.enabled !== false ? 'success' : 'info'" size="small">
            {{ row.enabled !== false ? '启用' : '禁用' }}
          </el-tag>
        </template>
        <template #promoId="{ row }">
          <div class="id-copy-row">
            <el-button
              :icon="DocumentCopy"
              text
              size="small"
              title="复制推广ID"
              @click="copyToClipboard(row.promo_id ?? row.promoId, '推广ID')"
            />
            <span class="id-copy-row__text">{{ (row.promo_id ?? row.promoId) || '—' }}</span>
          </div>
        </template>
        <template #dramaId="{ row }">
          <div class="id-copy-row">
            <el-button
              :icon="DocumentCopy"
              text
              size="small"
              title="复制剧ID"
              @click="
                copyToClipboard(
                  row.drama_public_id ?? row.dramaPublicId ?? row.drama_id ?? row.dramaId,
                  '剧ID',
                )
              "
            />
            <span class="id-copy-row__text">{{ row.drama_public_id || row.drama_id || '—' }}</span>
          </div>
        </template>
        <template #countryCell="{ row }">
          {{ formatAccountCountryLabel(row.country) }}
        </template>
        <template #createdAt="{ row }">{{ row.created_at }}</template>
        <template #actions="{ row }">
          <div class="delivery-link-row-actions">
            <el-button size="small" @click="handleCopyLink(row)">复制链接</el-button>
            <el-button size="small" @click="handleCopy(row)">复制</el-button>
            <el-button size="small" @click="handleEditRow(row)">修改</el-button>
            <el-button size="small" type="danger" @click="handleDeleteRow(row)">删除</el-button>
          </div>
        </template>
      </VirtualTable>
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

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @closed="handleDialogClosed">
      <el-form
        ref="formRef"
        class="delivery-link-dialog-form"
        :model="formData"
        :rules="rules"
        label-width="118px"
      >
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
          <div class="form-tip">提示：可输入多个名称，每行一个；复制后将自动带上原链接的回传配置</div>
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, Download, DocumentCopy } from '@element-plus/icons-vue'
import request from '../api/request'
import { copyToClipboard } from '@/utils/clipboard'
import { useCountries } from '@/composables/useCountries'
import VirtualTable from '@/components/VirtualTable.vue'
import Loading from '@/components/Loading.vue'
import { debounce } from '@/utils/performance'

const { countryFilterOptions, countryMultiOptions, formatAccountCountryLabel } = useCountries()

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
const searching = ref(false)
const selectedRows = ref([])

/** #093：虚拟滚动 */
const useVirtualScroll = ref(false)
const virtualColumns = [
  { prop: '_sel', label: '', slot: 'selection', gridWidth: '48px' },
  { prop: '_en', label: '状态', slot: 'enabledTag', gridWidth: '76px' },
  { prop: '_promo', label: '推广ID', slot: 'promoId', gridWidth: 'minmax(120px, 1fr)' },
  { prop: '_drama', label: '剧的ID', slot: 'dramaId', gridWidth: 'minmax(120px, 1fr)' },
  { prop: 'media', label: '投放媒体', gridWidth: '88px' },
  { prop: '_country', label: '国家/地区', slot: 'countryCell', gridWidth: '100px' },
  { prop: 'promo_name', label: '推广名称', gridWidth: 'minmax(120px, 1.15fr)' },
  { prop: 'drama_name', label: '剧名', gridWidth: 'minmax(88px, 0.95fr)' },
  { prop: 'plan_group_id', label: '方案组ID', gridWidth: 'minmax(100px, 1fr)' },
  { prop: 'beans_per_episode', label: '金豆', gridWidth: '64px' },
  { prop: 'free_episodes', label: '免费', gridWidth: '64px' },
  { prop: 'created_by', label: '创建人', gridWidth: '88px' },
  { prop: '_created', label: '创建时间', slot: 'createdAt', gridWidth: 'minmax(130px, 0.9fr)' },
  { prop: '_op', label: '操作', slot: 'actions', gridWidth: 'minmax(200px, 1.5fr)' },
]

watch(
  () => tableData.value.length,
  (n) => {
    if (n > 100) useVirtualScroll.value = true
  },
)

function isRowSelected(row) {
  return selectedRows.value.some((r) => r.id === row.id)
}

function toggleRowSelected(row, checked) {
  if (checked) {
    if (!isRowSelected(row)) selectedRows.value = [...selectedRows.value, row]
  } else {
    selectedRows.value = selectedRows.value.filter((r) => r.id !== row.id)
  }
}

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
})
const currentCopyRow = ref(null)

function dramaLabel(drama) {
  const t = drama.title || drama.name || ''
  const pid = drama.public_id || drama.id
  return t ? `${t}（${pid}）` : String(pid ?? '')
}

function onDramaChange(id) {
  const d = dramaOptions.value.find((x) => x.id === id || String(x.id) === String(id))
  if (d) {
    formData.value.drama_name = d.title || d.name || ''
  }
}

function normalizeFormId(v) {
  if (v == null || v === '') return ''
  const n = Number(v)
  return Number.isFinite(n) && String(n) === String(v).trim() ? n : v
}

/** 编辑弹窗打开前拉满下拉数据，并补一条与当前行一致的 option，避免只显示数字 ID */
function ensureDramaOptionForRow(row, dramaId) {
  if (dramaId === '' || dramaId == null) return
  const hit = dramaOptions.value.some((d) => d.id === dramaId || String(d.id) === String(dramaId))
  if (hit) return
  const title = row.drama_name || ''
  dramaOptions.value = [
    {
      id: dramaId,
      title,
      name: title,
      public_id: row.drama_public_id,
    },
    ...dramaOptions.value,
  ]
}

function ensurePlanGroupOptionForRow(row, planGroupId) {
  if (planGroupId === '' || planGroupId == null) return
  const hit = planGroupOptions.value.some(
    (g) => g.id === planGroupId || String(g.id) === String(planGroupId),
  )
  if (hit) return
  const name = row.plan_group_name || row.group_name || ''
  planGroupOptions.value = [
    { id: planGroupId, name: name || `方案组 #${planGroupId}` },
    ...planGroupOptions.value,
  ]
}

/** 编辑/新增弹窗内下拉需展示名称：打开编辑前强制拉取最新列表（与 @focus 缓存策略独立） */
async function ensureFormSelectOptions() {
  try {
    const [dRes, gRes] = await Promise.all([
      request.get('/dramas', { params: { page: 1, pageSize: 1000 } }),
      request.get('/recharge-groups', { params: { page: 1, pageSize: 1000 } }),
    ])
    if (dRes.code === 0) {
      dramaOptions.value = dRes.data?.list || []
    }
    if (gRes.code === 0) {
      planGroupOptions.value = gRes.data?.list || []
    }
  } catch (e) {
    console.error(e)
  }
}

const handleQuery = async () => {
  searching.value = true
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
    searching.value = false
  }
}

const handleSearchDebounced = debounce(() => {
  pagination.value.page = 1
  void handleQuery()
}, 300)

const handleSearchClick = () => {
  handleSearchDebounced.cancel()
  pagination.value.page = 1
  void handleQuery()
}

const handleFilterImmediate = () => {
  handleSearchDebounced.cancel()
  pagination.value.page = 1
  void handleQuery()
}

const handleReset = () => {
  handleSearchDebounced.cancel()
  filterForm.value = {
    promoId: '',
    dramaId: '',
    media: '',
    country: '',
    promoName: '',
    promoDomain: '',
  }
  pagination.value.page = 1
  void handleQuery()
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
  void handleEditRow(selectedRows.value[0])
}

const handleEditRow = async (row) => {
  await ensureFormSelectOptions()
  const dramaId = normalizeFormId(row.drama_id)
  const planGroupId = normalizeFormId(row.plan_group_id)
  ensureDramaOptionForRow(row, dramaId)
  ensurePlanGroupOptionForRow(row, planGroupId)
  formData.value = {
    id: row.id,
    promo_name: row.promo_name || '',
    media: row.media || 'tiktok',
    country: row.country || '',
    drama_id: dramaId,
    plan_group_id: planGroupId,
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
  copyForm.value = { names: '' }
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
  copyLoading.value = true
  try {
    let successCount = 0
    for (const name of names) {
      const res = await request.post('/delivery-links/copy', {
        source_id: currentCopyRow.value.id,
        new_name: name,
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
  padding: 0;
}
.filter-card {
  margin-bottom: var(--section-gap);
}
.filter-card :deep(.el-card__body) {
  padding-bottom: 10px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
}
.table-wrapper--virtual {
  position: relative;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}
.delivery-link-dialog-form :deep(.el-form-item__label) {
  white-space: nowrap;
}
/* 操作列：单行排列，避免缩列后换行 */
.delivery-link-row-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 4px;
  justify-content: center;
  align-items: center;
  white-space: nowrap;
}
.delivery-link-row-actions :deep(.el-button) {
  flex-shrink: 0;
  margin: 0;
}
.table-card-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.searching-tag {
  margin-left: 8px;
  vertical-align: middle;
}
</style>
