<template>
  <div class="drama-manage-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form
        :model="filterForm"
        class="filter-form drama-filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
        @keyup.enter="handleQuery"
      >
        <el-form-item label="剧ID" label-width="50px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.dramaId" clearable />
          </div>
        </el-form-item>
        <el-form-item label="剧名" label-width="50px">
          <div class="filter-item-s">
            <el-input
              v-model="filterForm.dramaName"
              placeholder="剧名"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
          </div>
        </el-form-item>
        <el-form-item label="状态" label-width="50px">
          <div class="filter-item-s">
            <el-select
              v-model="filterForm.status"
              placeholder="状态"
              clearable
              @change="handleFilterImmediate"
            >
              <el-option label="上架" value="online" />
              <el-option label="下架" value="offline" />
            </el-select>
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
          <el-dropdown :disabled="selectedRows.length === 0" @command="handleBatchStatus">
            <el-button type="warning" :disabled="selectedRows.length === 0">
              批量改状态
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="published">已发布</el-dropdown-item>
                <el-dropdown-item command="draft">草稿</el-dropdown-item>
                <el-dropdown-item command="archived">已下架</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="button-group-right">
          <el-button size="small" @click="useVirtualScroll = !useVirtualScroll">
            {{ useVirtualScroll ? '标准表格' : '虚拟滚动' }}
          </el-button>
          <el-button type="success" :loading="exporting" @click="handleExportExcel">
            <el-icon class="el-icon--left"><Download /></el-icon>
            导出 Excel
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-card-header-row">
          <span>
            短剧列表
            <el-tag v-if="searching" type="info" size="small" class="searching-tag">搜索中...</el-tag>
          </span>
        </div>
      </template>
      <div
        class="table-wrapper"
        :class="{ 'table-wrapper--virtual': useVirtualScroll }"
      >
      <SkeletonTable v-if="loading && tableData.length === 0 && !useVirtualScroll" :rows="10" :columns="8" />
      <template v-else-if="useVirtualScroll">
        <Loading :loading="loading" text="加载中..." />
        <VirtualTable
          :data="tableData"
          :columns="virtualColumns"
          :item-size="44"
          key-field="id"
        >
          <template #selection="{ row }">
            <el-checkbox
              :model-value="isRowSelected(row)"
              @change="(v) => toggleRowSelected(row, v)"
            />
          </template>
          <template #online="{ row }">
            <el-switch
              v-model="row.is_online"
              :active-value="1"
              :inactive-value="0"
              @change="() => handleToggleOnline(row)"
            />
          </template>
          <template #status="{ row }">
            <el-tag :type="row.is_online === 1 ? 'success' : 'info'">
              {{ row.is_online === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
          <template #dramaId="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制剧ID"
                @click="copyToClipboard(dramaListDisplayId(row), '剧ID')"
              />
              <span class="id-copy-row__text">{{ dramaListDisplayId(row) }}</span>
            </div>
          </template>
          <template #title="{ row }">
            {{ row.title || row.name }}
          </template>
          <template #cover="{ row }">
            <img
              v-if="dramaCoverUrl(row)"
              v-lazy="dramaCoverUrl(row)"
              class="drama-cover-thumb drama-cover-thumb--lazy"
              alt=""
            />
            <span v-else class="drama-cover-empty">—</span>
          </template>
          <template #totalEp="{ row }">{{ dramaTotalEpisodes(row) }}集</template>
          <template #freeEp="{ row }">{{ dramaFreeEpisodes(row) }}集</template>
          <template #task="{ row }">
            <el-tag :type="getTaskStatusType(row.task_status)" effect="plain">
              {{ getTaskStatusText(row.task_status) }}
            </el-tag>
          </template>
          <template #created="{ row }">{{ formatTime(row.created_at) }}</template>
          <template #actions="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">详细</el-button>
            <el-button type="primary" link size="small" @click="handleEditRow(row)">修改</el-button>
            <el-button type="danger" link size="small" @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </VirtualTable>
      </template>
      <el-table
        v-else
        v-loading="loading"
        element-loading-text="加载中..."
        :data="tableData"
        border
        stripe
        size="small"
        style="width: 100%"
        height="100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="投放" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.is_online"
              :active-value="1"
              :inactive-value="0"
              @change="() => handleToggleOnline(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_online === 1 ? 'success' : 'info'">
              {{ row.is_online === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="剧ID" width="200" align="left" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="id-copy-row">
              <el-button
                :icon="DocumentCopy"
                text
                size="small"
                title="复制剧ID"
                @click="copyToClipboard(dramaListDisplayId(row), '剧ID')"
              />
              <span class="id-copy-row__text">{{ dramaListDisplayId(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="剧名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.title || row.name }}</template>
        </el-table-column>
        <el-table-column label="封面" width="88" align="center">
          <template #default="{ row }">
            <img
              v-if="dramaCoverUrl(row)"
              v-lazy="dramaCoverUrl(row)"
              class="drama-cover-thumb drama-cover-thumb--lazy"
               alt=""
               @click="handleViewDetail(row)"
            />
            <span v-else class="drama-cover-empty">—</span>
          </template>
        </el-table-column>
        <el-table-column label="总集数" width="100" align="center">
          <template #default="{ row }">{{ dramaTotalEpisodes(row) }}集</template>
        </el-table-column>
        <el-table-column label="免费集数" width="100" align="center">
          <template #default="{ row }">{{ dramaFreeEpisodes(row) }}集</template>
        </el-table-column>
        <el-table-column label="任务状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getTaskStatusType(row.task_status)" effect="plain">
              {{ getTaskStatusText(row.task_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="created_at"
          label="创建时间"
          width="180"
          align="center"
        >
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">详细</el-button>
            <el-button type="primary" link @click="handleEditRow(row)">修改</el-button>
            <el-button type="danger" link @click="handleDeleteRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      </div>

      <el-pagination
        class="compact-pagination"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next, jumper"
        size="small"
        @size-change="handleQuery"
        @current-change="handleQuery"
      />
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" @closed="handleDialogClosed">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="120px">
        <el-form-item label="状态" prop="is_online">
          <el-radio-group v-model="formData.is_online">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="剧名" prop="name">
          <el-input v-model="formData.name" placeholder="请输入剧名" />
        </el-form-item>
        <el-form-item label="展示名" prop="display_name">
          <el-input v-model="formData.display_name" placeholder="请输入展示名" />
        </el-form-item>
        <el-form-item label="展示文本" prop="display_text">
          <el-input v-model="formData.display_text" placeholder="请输入展示文本" />
        </el-form-item>
        <el-form-item label="金豆数" prop="beans_per_episode">
          <el-input-number v-model="formData.beans_per_episode" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="总集数" prop="total_episodes">
          <el-input-number v-model="formData.total_episodes" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="免费集数" prop="free_episodes">
          <el-input-number v-model="formData.free_episodes" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="OSS路径" prop="oss_path">
          <el-input v-model="formData.oss_path" type="textarea" :rows="2" placeholder="请输入OSS路径" />
        </el-form-item>
        <el-form-item label="分类" prop="category_id">
          <el-select v-model="formData.category_id" placeholder="请选择分类" clearable filterable style="width: 100%">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :file-list="fileList"
          >
            <el-button type="primary">选择文件</el-button>
            <template #tip>
              <div class="upload-tip">备注：上传文件到阿里云VOD，最大10GB（当前为占位，未接真实上传）</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog title="剧详细" v-model="detailDialogVisible" width="600px">
      <el-descriptions :column="1" border>
        <el-descriptions-item v-if="detailCoverUrl" label="封面">
          <el-image
            :src="detailCoverUrl"
            lazy
            fit="contain"
            class="drama-cover-detail"
            :preview-src-list="[detailCoverUrl]"
            preview-teleported
          />
        </el-descriptions-item>
        <el-descriptions-item label="业务剧ID">{{ detailData.public_id || detailData.id || '—' }}</el-descriptions-item>
        <el-descriptions-item label="内部ID">{{ detailData.id != null ? detailData.id : '—' }}</el-descriptions-item>
        <el-descriptions-item label="剧名">{{ detailData.name }}</el-descriptions-item>
        <el-descriptions-item label="展示名">{{ detailData.display_name }}</el-descriptions-item>
        <el-descriptions-item label="展示文本">{{ detailData.display_text }}</el-descriptions-item>
        <el-descriptions-item label="金豆数">{{ detailData.beans_per_episode }}</el-descriptions-item>
        <el-descriptions-item label="总集数">{{ detailData.total_episodes }}</el-descriptions-item>
        <el-descriptions-item label="免费集数">{{ detailData.free_episodes }}</el-descriptions-item>
        <el-descriptions-item label="OSS路径">{{ detailData.oss_path }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detailData.category_name || detailData.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="任务状态">
          <el-tag :type="getTaskStatusType(detailData.task_status)">{{ getTaskStatusText(detailData.task_status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.created_at }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updated_at }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, RefreshLeft, Plus, Edit, Delete, ArrowDown, Download, DocumentCopy } from '@element-plus/icons-vue'
import request from '../api/request'
import { getCategories } from '@/api/category'
import { getDramaDetail, getDramaList, invalidateDramaCaches } from '@/api/drama'
import SkeletonTable from '@/components/SkeletonTable.vue'
import VirtualTable from '@/components/VirtualTable.vue'
import Loading from '@/components/Loading.vue'
import { exportJsonToXlsx } from '../utils/excelExport'
import { copyToClipboard } from '@/utils/clipboard'
import { debounce } from '@/utils/performance'

const filterForm = ref({
  dramaId: '',
  dramaName: '',
  status: '',
})

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)
const searching = ref(false)
const selectedRows = ref([])

/** #091：虚拟滚动（>100 行自动开启，可手动切回标准表格） */
const useVirtualScroll = ref(false)
const virtualColumns = [
  { prop: '_sel', label: '', slot: 'selection', gridWidth: '52px' },
  { prop: '_on', label: '投放', slot: 'online', gridWidth: '76px' },
  { prop: '_st', label: '状态', slot: 'status', gridWidth: '88px' },
  { prop: '_pid', label: '剧ID', slot: 'dramaId', gridWidth: 'minmax(140px, 1fr)' },
  { prop: '_title', label: '剧名', slot: 'title', gridWidth: 'minmax(160px, 1.3fr)' },
  { prop: '_cover', label: '封面', slot: 'cover', gridWidth: '72px' },
  { prop: '_te', label: '总集', slot: 'totalEp', gridWidth: '76px' },
  { prop: '_fe', label: '免费', slot: 'freeEp', gridWidth: '76px' },
  { prop: '_task', label: '任务', slot: 'task', gridWidth: '108px' },
  { prop: '_created', label: '创建时间', slot: 'created', gridWidth: 'minmax(140px, 1fr)' },
  { prop: '_op', label: '操作', slot: 'actions', gridWidth: 'minmax(220px, 1.2fr)' },
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
const dialogTitle = computed(() => (formData.value.id ? '修改剧' : '添加剧'))
const submitLoading = ref(false)
const formRef = ref(null)
const uploadRef = ref(null)
const fileList = ref([])

const categoryList = ref([])

const defaultForm = () => ({
  id: null,
  is_online: 1,
  name: '',
  display_name: '',
  display_text: '',
  beans_per_episode: 5,
  total_episodes: null,
  free_episodes: 11,
  oss_path: '',
  category_id: null,
  file: null,
})

const formData = ref(defaultForm())
const pendingFile = ref(null)

const rules = {
  is_online: [{ required: true, message: '请选择状态', trigger: 'change' }],
  name: [{ required: true, message: '请输入剧名', trigger: 'blur' }],
  beans_per_episode: [{ required: true, message: '请输入每集金豆数', trigger: 'blur' }],
  total_episodes: [{ required: true, message: '请输入总集数', trigger: 'blur' }],
  free_episodes: [{ required: true, message: '请输入免费集数', trigger: 'blur' }],
}

const detailDialogVisible = ref(false)
const detailData = ref({})

let pollingTimer = null

function dramaListDisplayId(row) {
  if (!row) return ''
  return row.public_id ?? row.publicId ?? row.id ?? ''
}

function dramaTotalEpisodes(row) {
  const n = Number(row?.total_episodes ?? row?.totalEpisodes ?? 0)
  return Number.isFinite(n) ? n : 0
}

function dramaFreeEpisodes(row) {
  const n = Number(row?.free_episodes ?? row?.freeEpisodes ?? 0)
  return Number.isFinite(n) ? n : 0
}

function dramaCoverUrl(row) {
  if (!row) return ''
  const u = row.cover_image || row.coverImage || row.cover || ''
  return typeof u === 'string' ? u.trim() : ''
}

const detailCoverUrl = computed(() => {
  const d = detailData.value || {}
  return (d.cover_image || d.cover || '').trim() || ''
})

function getTaskStatusText(status) {
  const map = {
    uploading: '上传中',
    transcoding: '解码中',
    callback: '回调中',
    completed: '完成',
    完成: '完成',
    进行中: '进行中',
    '': '未开始',
  }
  return map[status] || (status ? String(status) : '未开始')
}

function getTaskStatusType(status) {
  const map = {
    uploading: 'warning',
    transcoding: 'warning',
    callback: 'info',
    completed: 'success',
    完成: 'success',
    进行中: 'warning',
    '': 'info',
  }
  return map[status] || 'info'
}

/** Java：上架→published，下架→archived（库内 offline） */
function mapFilterStatus(s) {
  if (!s) return undefined
  if (s === 'online') return 'published'
  if (s === 'offline') return 'archived'
  return s
}

async function loadCategories() {
  try {
    const res = await getCategories()
    if (res.code === 0 && Array.isArray(res.data)) {
      categoryList.value = res.data
    }
  } catch (_) {
    categoryList.value = []
  }
}

function buildDramaFilterParams() {
  const params = {}
  const idStr = (filterForm.value.dramaId || '').trim()
  if (/^\d+$/.test(idStr)) {
    params.id = parseInt(idStr, 10)
  } else if (idStr) {
    params.public_id = idStr
  }
  const name = (filterForm.value.dramaName || '').trim()
  if (name) params.title = name
  const st = mapFilterStatus(filterForm.value.status)
  if (st) params.status = st
  return params
}

const handleQuery = async () => {
  searching.value = true
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
      ...buildDramaFilterParams(),
    }

    const res = await getDramaList(params)
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
  filterForm.value = { dramaId: '', dramaName: '', status: '' }
  pagination.value.page = 1
  void handleQuery()
}

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const handleToggleOnline = async (row) => {
  const prev = row.is_online === 1 ? 0 : 1
  try {
    const res = await request.put(`/dramas/${row.id}`, { is_online: row.is_online })
    if (res.code === 0) {
      invalidateDramaCaches()
      ElMessage.success(row.is_online === 1 ? '已上架' : '已下架')
      handleQuery()
    } else {
      ElMessage.error(res.message || '操作失败')
      row.is_online = prev
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '操作失败')
    row.is_online = prev
  }
}

const handleAdd = () => {
  formData.value = defaultForm()
  pendingFile.value = null
  fileList.value = []
  dialogVisible.value = true
}

const handleEdit = () => {
  if (selectedRows.value.length !== 1) {
    ElMessage.warning('请选择一条记录进行修改')
    return
  }
  handleEditRow(selectedRows.value[0])
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return String(t)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

const handleEditRow = (row) => {
  formData.value = {
    id: row.id,
    is_online: row.is_online === 1 || row.is_online === true ? 1 : 0,
    name: row.name || row.title || '',
    display_name: row.display_name || '',
    display_text: row.display_text || row.description || '',
    beans_per_episode: row.beans_per_episode ?? 5,
    total_episodes: row.total_episodes != null ? row.total_episodes : null,
    free_episodes: row.free_episodes != null ? row.free_episodes : 11,
    oss_path: row.oss_path || '',
    category_id: row.category_id ?? null,
    file: null,
  }
  pendingFile.value = null
  fileList.value = []
  dialogVisible.value = true
}

const handleFileChange = (file) => {
  pendingFile.value = file.raw
}

const buildSubmitPayload = () => {
  const {
    is_online,
    name,
    display_name,
    display_text,
    beans_per_episode,
    total_episodes,
    free_episodes,
    oss_path,
    category_id,
  } = formData.value
  const status = is_online === 1 ? 'published' : 'offline'
  return {
    title: name,
    name,
    display_name,
    display_text,
    description: display_text || '',
    beans_per_episode,
    total_episodes,
    free_episodes,
    oss_path,
    category_id: category_id ?? null,
    status,
    is_online,
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (pendingFile.value) {
    ElMessage.info('文件上传至阿里云 VOD 尚未接入，将仅保存表单字段')
  }
  submitLoading.value = true
  try {
    const payload = buildSubmitPayload()
    if (formData.value.id) {
      const res = await request.put(`/dramas/${formData.value.id}`, payload)
      if (res.code === 0) {
        invalidateDramaCaches()
        ElMessage.success('修改成功')
        dialogVisible.value = false
        handleQuery()
      }
    } else {
      const res = await request.post('/dramas', payload)
      if (res.code === 0) {
        invalidateDramaCaches()
        ElMessage.success('新增成功')
        dialogVisible.value = false
        handleQuery()
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
  fileList.value = []
  pendingFile.value = null
}

const handleViewDetail = async (row) => {
  try {
    const res = await getDramaDetail(row.id)
    if (res.code === 0) {
      const d = res.data || {}
      detailData.value = {
        id: d.id,
        public_id: d.public_id,
        name: d.name || d.title,
        cover_image: d.cover_image || d.cover || '',
        display_name: d.display_name,
        display_text: d.display_text || d.description,
        beans_per_episode: d.beans_per_episode,
        total_episodes: d.total_episodes,
        free_episodes: d.free_episodes,
        oss_path: d.oss_path,
        category: d.category_name || d.category,
        task_status: d.task_status,
        created_at: d.created_at,
        updated_at: d.updated_at,
      }
      detailDialogVisible.value = true
    } else {
      ElMessage.error(res.message || '获取详情失败')
    }
  } catch (error) {
    console.error(error)
    ElMessage.error(error?.message || '获取详情失败')
  }
}

const handleDeleteRow = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这部剧吗？', '提示', { type: 'warning' })
    const res = await request.delete(`/dramas/${row.id}`)
    if (res.code === 0) {
      invalidateDramaCaches()
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

function dramaStatusLabel(s) {
  if (s === 'published') return '已发布'
  if (s === 'draft') return '草稿'
  if (s === 'archived') return '已下架'
  return s ? String(s) : '-'
}

const handleBatchStatus = async (status) => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请至少选择一条记录')
    return
  }
  const label = dramaStatusLabel(status)
  try {
    await ElMessageBox.confirm(
      `将选中的 ${selectedRows.value.length} 条短剧设为「${label}」，是否继续？`,
      '批量改状态',
      { type: 'warning' },
    )
    let ok = 0
    for (const row of selectedRows.value) {
      try {
        const res = await request.put(`/dramas/${row.id}`, { status })
        if (res.code === 0) ok += 1
      } catch (e) {
        console.error(e)
      }
    }
    if (ok === selectedRows.value.length) ElMessage.success('批量更新成功')
    else if (ok > 0) ElMessage.warning(`部分成功：${ok}/${selectedRows.value.length}`)
    else ElMessage.error('批量更新失败')
    if (ok > 0) invalidateDramaCaches()
    handleQuery()
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
      ElMessage.error(e?.message || '操作失败')
    }
  }
}

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const pageSize = 100
    let page = 1
    let total = Infinity
    const all = []
    while (all.length < total) {
      /** 指令 #007：短剧导出为全库，不带列表筛选 */
      const res = await request.get('/dramas', { params: { page, pageSize } })
      if (res.code !== 0) {
        ElMessage.error(res.message || '导出失败')
        return
      }
      const list = res.data?.list || []
      total = res.data?.total ?? list.length
      all.push(...list)
      if (list.length < pageSize) break
      page += 1
      if (page > 500) break
    }
    if (!all.length) {
      ElMessage.warning('无数据可导出')
      return
    }
    const rows = all.map((row) => ({
      剧ID: row.public_id || row.id,
      剧名: row.title || row.name || '',
      总集数: dramaTotalEpisodes(row),
      免费集数: dramaFreeEpisodes(row),
      状态: dramaStatusLabel(row.status),
      观看次数: row.view_count ?? 0,
      创建时间: row.created_at || '',
    }))
    exportJsonToXlsx(rows, '短剧列表', `短剧列表_${new Date().toISOString().slice(0, 10)}`)
    ElMessage.success(`已导出 ${rows.length} 条`)
  } catch (e) {
    ElMessage.error(`导出失败：${e?.message || ''}`)
  } finally {
    exporting.value = false
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
    let ok = 0
    for (const id of ids) {
      try {
        const res = await request.delete(`/dramas/${id}`)
        if (res.code === 0) ok += 1
      } catch (e) {
        console.error(e)
      }
    }
    if (ok === ids.length) ElMessage.success('删除成功')
    else if (ok > 0) ElMessage.warning(`部分成功：已删除 ${ok}/${ids.length} 条`)
    else ElMessage.error('删除失败（Java 后端无批量接口时逐条删除）')
    if (ok > 0) invalidateDramaCaches()
    handleQuery()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.message || '删除失败')
    }
  }
}

const startPolling = () => {
  pollingTimer = setInterval(() => {
    handleQuery()
  }, 30000)
}

const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
}

onMounted(() => {
  handleQuery()
  startPolling()
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 0;
}
.filter-card :deep(.el-card__body) {
  padding-bottom: 10px;
}
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.drama-cover-thumb {
  width: 56px;
  height: 72px;
  border-radius: 4px;
}
.drama-cover-thumb--lazy {
  object-fit: cover;
  display: block;
  cursor: pointer;
}
.table-wrapper--virtual {
  position: relative;
}
.drama-cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 72px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.drama-cover-empty {
  color: var(--el-text-color-placeholder);
  font-size: 13px;
}
.drama-cover-detail {
  max-height: 140px;
  max-width: 100%;
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
