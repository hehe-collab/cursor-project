<template>
  <div class="batch-tools-container">
    <!-- 筛选区域 -->
    <el-card class="filter-card" shadow="never">
      <el-form
        :model="filterForm"
        label-position="left"
        label-width="48px"
        class="filter-form batch-filter-form"
        inline
        size="small"
      >
        <el-form-item label="主体">
          <div class="filter-item-m">
            <el-select
              v-model="filterForm.entity"
              placeholder="请选择主体"
              clearable
              filterable
              @change="onEntityChange"
            >
              <el-option
                v-for="item in entityOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="账户" label-width="40px">
          <div class="filter-item-wide">
            <el-select
              v-model="filterForm.accounts"
              placeholder="可多选账户，支持名称搜索"
              multiple
              filterable
              clearable
              collapse-tags
              collapse-tags-tooltip
            >
              <el-option
                v-for="item in accountOptionsFiltered"
                :key="item.id"
                :label="item.label"
                :value="item.id"
              />
            </el-select>
          </div>
        </el-form-item>
      </el-form>
      <div class="button-group">
        <div class="button-group-left">
          <el-button type="primary" @click="handleSubmitTask">提交任务</el-button>
          <el-button type="primary" @click="handleViewTask">查看任务</el-button>
        </div>
        <div class="button-group-right"></div>
      </div>
    </el-card>

    <!-- 三列：第 N 行一一对应（无复选框，仅序号） -->
    <div class="cards-container">
      <el-card class="tool-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">项目</span>
            <div class="card-actions">
              <el-button type="primary" link @click="handleUseExisting('project')">使用已有</el-button>
              <el-button type="primary" link @click="handleAdd('project')">新增</el-button>
              <el-button type="primary" link @click="handleSettings('project')">设置</el-button>
            </div>
          </div>
        </template>
        <div class="card-content card-content--scroll">
          <el-scrollbar height="350px">
            <div v-if="projectList.length > 0" class="data-list">
              <div v-for="(item, index) in projectList" :key="item.id" class="data-item">
                <div class="item-index">{{ index + 1 }}</div>
                <div class="item-info">
                  <div class="item-name">{{ item.campaignName || '未命名广告系列' }}</div>
                  <div class="item-meta"><span>账户ID: {{ item.accountId || '—' }}</span></div>
                  <div class="item-meta"><span>账户: {{ item.accountName || '—' }}</span></div>
                  <div class="item-meta"><span>项目名称: {{ item.projectName || '—' }}</span></div>
                </div>
              </div>
            </div>
            <el-empty v-else description="请先选择账户" />
          </el-scrollbar>
        </div>
      </el-card>

      <el-card class="tool-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">广告组</span>
            <div class="card-actions">
              <el-button type="primary" link @click="handleUseExisting('adGroup')">使用已有</el-button>
              <el-button type="primary" link @click="handleAdd('adGroup')">新增</el-button>
              <el-button type="primary" link @click="handleClear('adGroup')">清空</el-button>
              <el-button type="primary" link @click="handleSettings('adGroup')">设置</el-button>
            </div>
          </div>
        </template>
        <div class="card-content card-content--scroll">
          <el-scrollbar height="350px">
            <div v-if="adGroupList.length > 0" class="data-list">
              <div v-for="(item, index) in adGroupList" :key="item.id" class="data-item">
                <div class="item-index">{{ index + 1 }}</div>
                <div class="item-info">
                  <div class="item-name">{{ item.adGroupName || 'Smart2.0+' }}</div>
                  <div class="item-meta"><span>账户ID: {{ item.accountId || '—' }}</span></div>
                  <div class="item-meta"><span>账户: {{ item.accountName || '—' }}</span></div>
                  <div class="item-meta"><span>项目: {{ item.projectName || '—' }}</span></div>
                  <div v-if="item.price != null && item.price !== ''" class="item-meta">
                    <span>出价: {{ item.price }}</span>
                  </div>
                  <div v-if="item.age" class="item-meta"><span>年龄: {{ item.age }}</span></div>
                </div>
              </div>
            </div>
            <el-empty v-else description="与项目行数一致；请先选择账户或点击设置" />
          </el-scrollbar>
        </div>
      </el-card>

      <el-card class="tool-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="card-title">广告</span>
            <div class="card-actions">
              <el-button type="primary" link @click="handleClear('ad')">清空</el-button>
              <el-button type="primary" link @click="handleSettings('ad')">设置</el-button>
            </div>
          </div>
        </template>
        <div class="card-content card-content--scroll">
          <el-scrollbar height="350px">
            <div v-if="adList.length > 0" class="data-list">
              <div v-for="(item, index) in adList" :key="item.id" class="data-item">
                <div class="item-index">{{ index + 1 }}</div>
                <div class="item-info">
                  <div class="item-name">广告 {{ index + 1 }}</div>
                  <div class="item-meta"><span>账户ID: {{ item.accountId || '—' }}</span></div>
                  <div class="item-meta"><span>项目: {{ item.projectName || '—' }}</span></div>
                  <div v-if="item.materialId != null && item.materialId !== ''" class="item-meta">
                    <span>素材: {{ materialLabel(item.materialId) }}</span>
                  </div>
                  <div v-if="item.titlePackId != null && item.titlePackId !== ''" class="item-meta">
                    <span>标题: {{ titleLabel(item.titlePackId) }}</span>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="与项目行数一致；请点击设置关联素材与标题" />
          </el-scrollbar>
        </div>
      </el-card>
    </div>

    <!-- 选择广告系列（项目设置） -->
    <el-dialog
      v-model="projectDialogVisible"
      title="选择广告系列"
      width="90%"
      top="5vh"
      class="batch-dialog batch-dialog--wide"
      destroy-on-close
    >
      <div class="table-wrap">
        <el-table :data="projectTableData" border stripe>
          <el-table-column prop="accountId" label="账户ID" min-width="160" show-overflow-tooltip />
          <el-table-column prop="account" label="账户" min-width="140" show-overflow-tooltip />
          <el-table-column label="已有项目(空则新增)" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.existingProject"
                placeholder="空则新增"
                clearable
                filterable
                size="small"
                style="width: 100%"
                :loading="existingProjectsLoading"
              >
                <el-option
                  v-for="item in projectOptionsForRow(row)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
                <template #empty>
                  <div class="select-empty-tip">{{ existingProjectEmptyText(row) }}</div>
                </template>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="项目名称" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.projectName" placeholder="请输入项目名称" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="每日预算" width="120">
            <template #default="{ row }">
              <el-input-number v-model="row.dailyBudget" :min="0" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="基于目标增加预算" width="150">
            <template #default="{ row }">
              <el-switch v-model="row.targetBasedBudget" />
            </template>
          </el-table-column>
          <el-table-column label="商品库" width="100">
            <template #default="{ row }">
              <el-switch v-model="row.productLibrary" />
            </template>
          </el-table-column>
          <el-table-column label="启用" width="88" fixed="right">
            <template #default="{ row }">
              <el-switch v-model="row.enabled" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="projectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmCampaign">确定</el-button>
      </template>
    </el-dialog>

    <!-- 广告组设置（友商 10 列） -->
    <el-dialog
      v-model="adGroupDialogVisible"
      title="广告组设置"
      width="95%"
      top="5vh"
      class="batch-dialog batch-dialog--wide"
      destroy-on-close
    >
      <div class="table-wrap">
        <el-table :data="adGroupTableData" border stripe>
          <el-table-column prop="account" label="账户" min-width="130" show-overflow-tooltip />
          <el-table-column prop="project" label="项目" min-width="100" show-overflow-tooltip />
          <el-table-column label="广告组(空则新增)" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.existingAdGroups"
                placeholder="可多选"
                multiple
                filterable
                clearable
                collapse-tags
                collapse-tags-tooltip
                size="small"
                style="width: 100%"
                :loading="existingAdGroupsLoading"
              >
                <el-option
                  v-for="item in adGroupOptionsForRow(row)"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
                <template #empty>
                  <div class="select-empty-tip">{{ existingAdGroupEmptyText(row) }}</div>
                </template>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="名称" min-width="130">
            <template #default="{ row }">
              <el-input v-model="row.adGroupName" placeholder="请输入名称" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="Pixel" min-width="140">
            <template #default="{ row }">
              <el-select
                v-model="row.pixel"
                placeholder="请选择"
                filterable
                clearable
                size="small"
                style="width: 100%"
                :loading="pixelsLoading"
              >
                <el-option v-for="item in pixelOptionsForRow(row)" :key="item.value" :label="item.label" :value="item.value" />
                <template #empty>
                  <div class="select-empty-tip">{{ pixelEmptyText(row) }}</div>
                </template>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="优化目标" width="120">
            <template #default="{ row }">
              <el-select v-model="row.optimizationGoal" placeholder="请选择" clearable size="small" style="width: 100%">
                <el-option label="转化" value="conversion" />
                <el-option label="点击" value="click" />
                <el-option label="展示" value="impression" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="出价" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.price" :min="0" :step="0.01" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="开始时间" min-width="190">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.startTime"
                type="datetime"
                placeholder="北京时间"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="年龄" width="120">
            <template #default="{ row }">
              <el-select v-model="row.age" placeholder="请选择" clearable size="small" style="width: 100%">
                <el-option label="18+" value="18+" />
                <el-option label="25+" value="25+" />
                <el-option label="35+" value="35+" />
                <el-option label="45+" value="45+" />
                <el-option label="不限" value="all" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="商品库" min-width="160">
            <template #default="{ row }">
              <el-select
                v-model="row.adGroupCatalog"
                placeholder="请选择商品库"
                filterable
                clearable
                size="small"
                style="width: 100%"
                :disabled="adGroupCatalogOptions.length === 0"
              >
                <el-option
                  v-for="item in adGroupCatalogOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
                <template #empty>
                  <div class="select-empty-tip">商品库接口暂未接入，当前先保留为空。</div>
                </template>
              </el-select>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="adGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAdGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 广告设置 -->
    <el-dialog
      v-model="adSettingsDialogVisible"
      title="广告设置"
      width="90%"
      top="5vh"
      class="batch-dialog batch-dialog--wide"
      destroy-on-close
    >
      <div class="dialog-tag-row">
        <el-tag type="primary">Smart-2.0</el-tag>
      </div>
      <div class="table-wrap">
        <el-table :data="adTableData" border stripe>
          <el-table-column label="基本信息" min-width="200">
            <template #default="{ row }">
              <div class="basic-cell">账户ID: {{ row.accountId || '—' }}</div>
              <div class="basic-cell">项目: {{ row.project || '—' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="弹窗落地页" min-width="160">
            <template #default="{ row }">
              <el-input v-model="row.landingPage" placeholder="落地页链接" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="素材" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.materialId"
                placeholder="请选择素材"
                filterable
                clearable
                size="small"
                style="width: 100%"
                :loading="materialsLoading"
              >
                <el-option
                  v-for="item in materialOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="标题" min-width="180">
            <template #default="{ row }">
              <el-select
                v-model="row.titlePackId"
                placeholder="请选择标题包"
                filterable
                clearable
                size="small"
                style="width: 100%"
                :loading="titlesLoading"
              >
                <el-option
                  v-for="item in titleOptions"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="试看" width="110">
            <template #default="{ row }">
              <el-input-number v-model="row.preview" :min="0" :max="999" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="认证页面" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.authPage" placeholder="认证页面" size="small" />
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="adSettingsDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAdSettings">确定</el-button>
      </template>
    </el-dialog>

    <!-- 使用已有（广告组 / 广告） -->
    <el-dialog
      v-model="useExistingDialogVisible"
      :title="useExistingTitle"
      width="600px"
      destroy-on-close
    >
      <el-table :data="existingList" @selection-change="handleExistingSelectionChange">
        <el-table-column type="selection" width="55" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
      <template #footer>
        <el-button @click="useExistingDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmUseExisting">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增 -->
    <el-dialog v-model="addDialogVisible" :title="addDialogTitle" width="520px" destroy-on-close>
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="名称">
          <el-input v-model="addForm.name" placeholder="请输入名称" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../api/request'

const router = useRouter()

const filterForm = reactive({
  entity: '',
  accounts: [],
})

const allAccounts = ref([])
const entityOptions = computed(() => {
  const dedup = new Set()
  return (allAccounts.value || [])
    .map((item) => (item.subjectName || '').trim())
    .filter((name) => {
      if (!name || dedup.has(name)) return false
      dedup.add(name)
      return true
    })
    .map((name) => ({ label: name, value: name }))
})

const accountOptionsFiltered = computed(() => {
  const rows = allAccounts.value || []
  const entity = (filterForm.entity || '').trim()
  const filtered = entity
    ? rows.filter((a) => (a.subjectName || '').trim() === entity)
    : rows
  return filtered.map((a) => ({
    id: a.id,
    label: `${a.accountName || '-'} (${a.accountId || '-'})`,
  }))
})

watch([() => filterForm.entity, allAccounts], () => {
  const allowed = new Set(accountOptionsFiltered.value.map((x) => x.id))
  filterForm.accounts = (filterForm.accounts || []).filter((id) => allowed.has(id))
})

function accountById(id) {
  return allAccounts.value.find((a) => a.id === id)
}

const projectList = ref([])
const adGroupList = ref([])
const adList = ref([])

const projectDialogVisible = ref(false)
const adGroupDialogVisible = ref(false)
const adSettingsDialogVisible = ref(false)
const useExistingDialogVisible = ref(false)
const addDialogVisible = ref(false)
const currentCardType = ref('')

const projectTableData = ref([])
const adGroupTableData = ref([])
const adTableData = ref([])

const existingList = ref([])
const selectedExisting = ref([])

const addForm = reactive({
  name: '',
})

const materialOptions = ref([])
const titleOptions = ref([])
const materialsLoading = ref(false)
const titlesLoading = ref(false)

/** 已有项目下拉（真实 TikTok 广告系列） */
const existingProjectOptions = ref([])
/** 广告组弹窗：已有广告组 / Pixel / 商品库 */
const existingAdGroupOptions = ref([])
const pixelOptions = ref([])
const adGroupCatalogOptions = ref([])
const existingProjectsLoading = ref(false)
const existingAdGroupsLoading = ref(false)
const pixelsLoading = ref(false)

const useExistingTitle = computed(() => {
  if (currentCardType.value === 'adGroup') return '选择已有广告组'
  return '选择已有广告'
})

const addDialogTitle = computed(() => {
  if (currentCardType.value === 'project') return '新增项目'
  if (currentCardType.value === 'adGroup') return '新增广告组'
  return '新增广告'
})

function projectOptionsForRow(row) {
  const advertiserId = String(row?.accountId || '').trim()
  return existingProjectOptions.value.filter((item) => !advertiserId || item.advertiserId === advertiserId)
}

function adGroupOptionsForRow(row) {
  const advertiserId = String(projectAccountId(row) || '').trim()
  return existingAdGroupOptions.value.filter((item) => !advertiserId || item.advertiserId === advertiserId)
}

function pixelOptionsForRow(row) {
  const advertiserId = String(projectAccountId(row) || '').trim()
  return pixelOptions.value.filter((item) => !advertiserId || item.advertiserId === advertiserId)
}

function existingProjectEmptyText(row) {
  return projectOptionsForRow(row).length
    ? '暂无匹配项目'
    : '当前账户下暂无已同步的广告系列，可手动填写新项目名称。'
}

function existingAdGroupEmptyText(row) {
  return adGroupOptionsForRow(row).length
    ? '暂无匹配广告组'
    : '当前账户下暂无已同步的广告组，可手动填写新广告组名称。'
}

function pixelEmptyText(row) {
  return pixelOptionsForRow(row).length
    ? '暂无匹配 Pixel'
    : '当前账户下暂无已同步的 Pixel。'
}

function projectAccountId(row) {
  return row?.accountId || row?.account || ''
}

let accountsFetchPromise = null
async function loadAllAccounts() {
  if (accountsFetchPromise) {
    await accountsFetchPromise
    return
  }
  accountsFetchPromise = (async () => {
    try {
      const res = await request.get('/accounts/executable-options', {
        params: { media: 'tiktok', oauthStatus: 'active' },
      })
      if (res.code === 0) {
        allAccounts.value = Array.isArray(res.data) ? res.data : []
      }
    } catch {
      allAccounts.value = []
      ElMessage.error('加载账户列表失败')
    } finally {
      accountsFetchPromise = null
    }
  })()
  await accountsFetchPromise
}

async function loadMaterialOptions(force = false) {
  if (materialOptions.value.length && !force) return
  materialsLoading.value = true
  try {
    const res = await request.get('/ad-material', { params: { page: 1, pageSize: 200 } })
    if (res.code === 0) {
      const list = res.data?.list || []
      materialOptions.value = list.map((m) => {
        const mid = m.materialId || m.material_id || (m.id != null ? `MAT${m.id}` : '')
        const name = m.materialName || m.material_name || m.name || mid
        const accountId = m.accountId || m.account_id || ''
        const label = accountId ? `${name} (${accountId} / ${mid})` : `${name} (${mid})`
        return { label: String(label), value: m.id != null ? m.id : mid }
      })
    }
  } catch {
    materialOptions.value = []
  } finally {
    materialsLoading.value = false
  }
}

async function loadTitleOptions() {
  if (titleOptions.value.length) return
  titlesLoading.value = true
  try {
    const res = await request.get('/title-pack', { params: { page: 1, pageSize: 200 } })
    if (res.code === 0) {
      const list = res.data?.list || []
      titleOptions.value = list.map((t) => ({
        label: t.name || `标题包 ${t.id}`,
        value: t.id,
      }))
    }
  } catch {
    titleOptions.value = []
  } finally {
    titlesLoading.value = false
  }
}

function projectNameForAccount(a) {
  const e = (filterForm.entity || '').trim()
  if (e) return e
  return (a?.subjectName || '').trim() || '—'
}

/** 弹窗与保存：无默认值，空则为 null */
function parseDailyBudget(v) {
  if (v === '' || v == null) return null
  const n = Number(v)
  return Number.isFinite(n) ? n : null
}

function existingProjectLabel(value) {
  if (value == null || value === '') return ''
  const o = existingProjectOptions.value.find((x) => x.value === value)
  return o?.name || o?.label || String(value)
}

async function loadExistingProjects() {
  existingProjectsLoading.value = true
  try {
    const res = await request.get('/tiktok/campaigns')
    if (res.code === 0) {
      existingProjectOptions.value = (Array.isArray(res.data) ? res.data : [])
        .filter((item) => item?.campaignId)
        .map((item) => {
          const advertiserId = String(item.advertiserId || '').trim()
          const campaignId = String(item.campaignId || '').trim()
          const campaignName = String(item.campaignName || '').trim() || campaignId
          const status = String(item.operationStatus || '').trim()
          return {
            advertiserId,
            value: campaignId,
            name: campaignName,
            label: `${campaignName}${status ? ` (${campaignId} / ${status})` : ` (${campaignId})`}`,
          }
        })
        .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
    } else {
      existingProjectOptions.value = []
    }
  } catch {
    existingProjectOptions.value = []
  } finally {
    existingProjectsLoading.value = false
  }
}

async function loadExistingAdGroups() {
  existingAdGroupsLoading.value = true
  try {
    const campaignNameById = new Map(existingProjectOptions.value.map((item) => [item.value, item.label]))
    const res = await request.get('/tiktok/adgroups')
    if (res.code === 0) {
      existingAdGroupOptions.value = (Array.isArray(res.data) ? res.data : [])
        .filter((item) => item?.adgroupId)
        .map((item) => {
          const advertiserId = String(item.advertiserId || '').trim()
          const adgroupId = String(item.adgroupId || '').trim()
          const adgroupName = String(item.adgroupName || '').trim() || adgroupId
          const campaignLabel = campaignNameById.get(String(item.campaignId || '').trim())
          return {
            advertiserId,
            campaignId: String(item.campaignId || '').trim(),
            value: adgroupId,
            label: campaignLabel
              ? `${adgroupName} (${adgroupId}) / ${campaignLabel}`
              : `${adgroupName} (${adgroupId})`,
          }
        })
        .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
    } else {
      existingAdGroupOptions.value = []
    }
  } catch {
    existingAdGroupOptions.value = []
  } finally {
    existingAdGroupsLoading.value = false
  }
}

async function loadPixelOptions() {
  pixelsLoading.value = true
  try {
    const res = await request.get('/tiktok/pixels')
    if (res.code === 0) {
      pixelOptions.value = (Array.isArray(res.data) ? res.data : [])
        .filter((item) => item?.pixelId)
        .map((item) => {
          const advertiserId = String(item.advertiserId || '').trim()
          const pixelId = String(item.pixelId || '').trim()
          const pixelName = String(item.pixelName || '').trim() || pixelId
          const pixelCode = String(item.pixelCode || '').trim()
          return {
            advertiserId,
            value: pixelId,
            label: pixelCode ? `${pixelName} (${pixelId} / ${pixelCode})` : `${pixelName} (${pixelId})`,
          }
        })
        .sort((a, b) => a.label.localeCompare(b.label, 'zh-CN'))
    } else {
      pixelOptions.value = []
    }
  } catch {
    pixelOptions.value = []
  } finally {
    pixelsLoading.value = false
  }
}

async function loadAdGroupCatalogOptions() {
  try {
    adGroupCatalogOptions.value = []
  } catch {
    adGroupCatalogOptions.value = []
  }
}

/** 按当前所选账户生成项目行，再按行生成同长度的广告组、广告（第 N 行一一对应） */
function syncProjectListFromAccounts() {
  const ids = filterForm.accounts || []
  if (!ids.length) {
    projectList.value = []
    adGroupList.value = []
    adList.value = []
    return
  }
  projectList.value = ids.map((internalId) => {
    const a = accountById(internalId)
    const pid = `cmp_${internalId}_0`
    const prev = projectList.value.find((p) => p.id === pid)
    return {
      id: pid,
      internalAccountId: internalId,
      accountId: a?.accountId || '',
      accountName: a?.accountName || '',
      campaignName: prev?.campaignName ?? '待配置',
      projectName: prev?.projectName ?? '',
      existingProject: prev?.existingProject ?? '',
      dailyBudget: parseDailyBudget(prev?.dailyBudget),
      targetBasedBudget: prev?.targetBasedBudget ?? false,
      productLibrary: prev?.productLibrary ?? false,
      enabled: prev?.enabled !== false,
    }
  })
  rebuildDerivedChains()
}

function onEntityChange() {
  filterForm.accounts = []
}

watch(
  () => filterForm.accounts,
  () => syncProjectListFromAccounts(),
  { deep: true }
)

/** 与 projectList 等长：第 i 行项目 ↔ 第 i 行广告组 ↔ 第 i 行广告 */
function rebuildDerivedChains() {
  const projects = projectList.value
  if (!projects.length) {
    adGroupList.value = []
    adList.value = []
    return
  }
  const prevAg = [...adGroupList.value]
  const prevAd = [...adList.value]
  adGroupList.value = projects.map((p, i) => {
    const agid = `ag_${p.id}`
    const old = prevAg.find((x) => x.projectId === p.id) || prevAg[i]
    return {
      id: agid,
      projectId: p.id,
      accountId: p.accountId,
      accountName: p.accountName,
      projectName: p.projectName,
      campaignName: p.campaignName,
      adGroupName: old?.adGroupName ?? `${p.campaignName || '广告系列'}-广告组`,
      existingAdGroups: Array.isArray(old?.existingAdGroups) ? [...old.existingAdGroups] : [],
      pixel: old?.pixel ?? '',
      optimizationGoal: old?.optimizationGoal ?? '',
      price: old?.price ?? null,
      startTime: old?.startTime ?? null,
      age: old?.age ?? '',
      adGroupCatalog: old?.adGroupCatalog ?? '',
    }
  })
  adList.value = projects.map((p, i) => {
    const adid = `ad_${p.id}`
    const old = prevAd.find((x) => x.projectId === p.id) || prevAd[i]
    return {
      id: adid,
      projectId: p.id,
      adGroupId: `ag_${p.id}`,
      accountId: p.accountId,
      projectName: p.projectName,
      adName: old?.adName ?? `广告 ${i + 1}`,
      materialId: old?.materialId ?? '',
      titlePackId: old?.titlePackId ?? '',
      landingPage: old?.landingPage ?? '',
      preview: old?.preview ?? 0,
      authPage: old?.authPage ?? '',
    }
  })
}

function materialLabel(val) {
  const m = materialOptions.value.find((x) => x.value === val)
  return m?.label || String(val ?? '—')
}

function titleLabel(val) {
  const t = titleOptions.value.find((x) => x.value === val)
  return t?.label || String(val ?? '—')
}

function parseInternalAccountId(row) {
  if (row.internalAccountId != null) return row.internalAccountId
  const m = String(row.id).match(/^cmp_(\d+)_/)
  return m ? parseInt(m[1], 10) : undefined
}

function campaignRowToProject(row) {
  const internal = parseInternalAccountId(row)
  const a = internal != null ? accountById(internal) : undefined
  const pn = (row.projectName != null ? String(row.projectName) : '').trim()
  const ep = row.existingProject != null && row.existingProject !== '' ? String(row.existingProject) : ''
  const resolvedName = pn || existingProjectLabel(ep)
  return {
    id: row.id,
    internalAccountId: internal,
    accountId: row.accountId || a?.accountId || '',
    accountName: row.account || row.accountName || a?.accountName || '',
    campaignName: resolvedName || row.campaignName || '待配置',
    projectName: resolvedName,
    existingProject: ep,
    dailyBudget: parseDailyBudget(row.dailyBudget),
    targetBasedBudget: !!row.targetBasedBudget,
    productLibrary: !!row.productLibrary,
    enabled: row.enabled !== false,
  }
}

/** 打开项目弹窗时：仅保留账户维度，输入项一律空白（不预填已保存配置） */
function buildCampaignTableFromAccounts() {
  if (projectList.value.length > 0) {
    projectTableData.value = projectList.value.map((p) => ({
      id: p.id,
      internalAccountId: p.internalAccountId,
      accountId: p.accountId,
      account: p.accountName,
      existingProject: '',
      projectName: '',
      dailyBudget: null,
      targetBasedBudget: false,
      productLibrary: false,
      enabled: true,
    }))
    return true
  }
  const ids = filterForm.accounts || []
  if (!ids.length) {
    ElMessage.warning('请先选择账户')
    return false
  }
  projectTableData.value = ids.map((internalId, i) => {
    const a = accountById(internalId)
    const pid = `cmp_${internalId}_${i}`
    const existing = projectList.value.find((p) => p.id === pid) || projectList.value.find((p) => p.internalAccountId === internalId)
    return {
      id: pid,
      internalAccountId: internalId,
      accountId: existing?.accountId || a?.accountId || '',
      account: existing?.accountName || a?.accountName || '',
      existingProject: '',
      projectName: '',
      dailyBudget: null,
      targetBasedBudget: false,
      productLibrary: false,
      enabled: true,
    }
  })
  return true
}

function handleConfirmCampaign() {
  const rows = projectTableData.value || []
  if (!rows.length) {
    ElMessage.warning('暂无项目行')
    return
  }
  for (const row of rows) {
    const hasExisting = row.existingProject != null && row.existingProject !== ''
    const hasName = row.projectName != null && String(row.projectName).trim() !== ''
    if (!hasExisting && !hasName) {
      ElMessage.warning('请选择已有项目或填写项目名称')
      return
    }
  }
  for (const row of rows) {
    const merged = campaignRowToProject(row)
    const idx = projectList.value.findIndex((p) => p.id === merged.id)
    if (idx >= 0) projectList.value[idx] = merged
    else projectList.value.push(merged)
  }
  rebuildDerivedChains()
  projectDialogVisible.value = false
  ElMessage.success('已同步到项目列表')
}

function ensureAdGroupRows() {
  if (!projectList.value.length) {
    adGroupTableData.value = []
    return false
  }
  if (adGroupList.value.length !== projectList.value.length) {
    rebuildDerivedChains()
  }
  /** 打开弹窗：仅保留账户/项目列，其余输入一律空白（与项目弹窗一致） */
  adGroupTableData.value = adGroupList.value.map((g) => ({
    id: g.id,
    accountId: g.accountId,
    account: g.accountName || g.accountId || '',
    project: g.projectName || '—',
    existingAdGroups: [],
    adGroupName: '',
    pixel: '',
    optimizationGoal: '',
    price: null,
    startTime: null,
    age: '',
    adGroupCatalog: '',
  }))
  return true
}

function handleConfirmAdGroup() {
  for (const row of adGroupTableData.value) {
    const idx = adGroupList.value.findIndex((x) => x.id === row.id)
    if (idx < 0) continue
    const prev = adGroupList.value[idx]
    adGroupList.value[idx] = {
      ...prev,
      existingAdGroups: Array.isArray(row.existingAdGroups) ? [...row.existingAdGroups] : [],
      adGroupName: row.adGroupName,
      pixel: row.pixel,
      optimizationGoal: row.optimizationGoal,
      price: row.price,
      startTime: row.startTime,
      age: row.age,
      adGroupCatalog: row.adGroupCatalog,
    }
  }
  ElMessage.success('广告组设置已保存')
  adGroupDialogVisible.value = false
}

async function ensureAdTableRows() {
  await Promise.all([loadMaterialOptions(true), loadTitleOptions()])
  if (!projectList.value.length) {
    adTableData.value = []
    return false
  }
  if (adList.value.length !== projectList.value.length) {
    rebuildDerivedChains()
  }
  adTableData.value = adList.value.map((ad) => ({
    id: ad.id,
    accountId: ad.accountId,
    project: ad.projectName || '—',
    landingPage: ad.landingPage || '',
    materialId: ad.materialId ?? '',
    titlePackId: ad.titlePackId ?? '',
    preview: ad.preview ?? 0,
    authPage: ad.authPage || '',
  }))
  return true
}

function handleConfirmAdSettings() {
  for (const row of adTableData.value) {
    const idx = adList.value.findIndex((x) => x.id === row.id)
    if (idx < 0) continue
    const prev = adList.value[idx]
    adList.value[idx] = {
      ...prev,
      landingPage: row.landingPage,
      materialId: row.materialId,
      titlePackId: row.titlePackId,
      preview: row.preview,
      authPage: row.authPage,
    }
  }
  ElMessage.success('广告设置已保存')
  adSettingsDialogVisible.value = false
}

async function handleSubmitTask() {
  if (!filterForm.entity) {
    ElMessage.warning('请选择主体')
    return
  }
  if (!filterForm.accounts.length) {
    ElMessage.warning('请选择账户')
    return
  }
  if (!projectList.value.length) {
    ElMessage.warning('请至少配置一个项目（选择账户并完善项目行）')
    return
  }
  const ids = filterForm.accounts || []
  const accountIds = []
  const accountNames = []
  for (const internalId of ids) {
    const a = accountById(internalId)
    if (a) {
      accountIds.push(a.accountId || '')
      accountNames.push(a.accountName || '')
    }
  }
  let createdBy = 'admin'
  try {
    const u = JSON.parse(localStorage.getItem('user') || '{}')
    createdBy = u.username || u.nickname || createdBy
  } catch (_) {
    /* ignore */
  }
  try {
    const res = await request.post('/ad-task', {
      account_ids: accountIds.join(','),
      account_names: accountNames.join(','),
      promotion_type: 'B-GF-zzz',
      created_by: createdBy,
      config: {
        entity: filterForm.entity,
        accountInternalIds: ids,
        projects: projectList.value,
        adGroups: adGroupList.value,
        ads: adList.value,
      },
    })
    const tid = res?.data?.task_id
      const execution = res?.data?.config?.execution
      if (execution) {
        const successCount = Number(execution.successCount || 0)
        const failedCount = Number(execution.failedCount || 0)
        const skippedCount = Number(execution.skippedCount || 0)
        const detail = [`成功 ${successCount}`]
        if (failedCount > 0) detail.push(`失败 ${failedCount}`)
        if (skippedCount > 0) detail.push(`跳过 ${skippedCount}`)
        const message = tid ? `任务ID: ${tid}，${detail.join('，')}` : detail.join('，')
        if (execution.status === 'partial') {
          ElMessage.warning(`任务已执行，部分完成。${message}`)
        } else if (execution.status === 'failed') {
          ElMessage.error(`任务执行失败。${message}`)
        } else {
          ElMessage.success(`任务已执行完成。${message}`)
        }
      } else {
        ElMessage.success(tid ? `任务提交成功！任务ID: ${tid}` : '任务提交成功')
      }
  } catch (e) {
    console.error(e)
  }
}

function handleViewTask() {
  router.push('/ad-task')
}

async function openProjectCampaignDialog() {
  await loadExistingProjects()
  if (!buildCampaignTableFromAccounts()) return
  projectDialogVisible.value = true
}

async function handleUseExisting(type) {
  currentCardType.value = type
  if (type === 'project') {
    await openProjectCampaignDialog()
    return
  }
  const label = type === 'adGroup' ? '广告组' : '广告'
  existingList.value = [
    { id: 91001, name: `已有${label} A`, createTime: '2024-01-01 10:00:00' },
    { id: 91002, name: `已有${label} B`, createTime: '2024-01-02 10:00:00' },
  ]
  selectedExisting.value = []
  useExistingDialogVisible.value = true
}

function handleExistingSelectionChange(selection) {
  selectedExisting.value = selection
}

function handleConfirmUseExisting() {
  if (selectedExisting.value.length === 0) {
    ElMessage.warning('请选择数据')
    return
  }
  const internalId = filterForm.accounts[0]
  if (!internalId) {
    ElMessage.warning('请先选择账户')
    return
  }
  const a = accountById(internalId)
  for (const r of selectedExisting.value) {
    projectList.value.push({
      id: `cmp_${internalId}_exist_${r.id}_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      internalAccountId: internalId,
      accountId: a?.accountId || '',
      accountName: a?.accountName || '',
      campaignName: r.name,
      projectName: projectNameForAccount(a),
      existingProject: '',
      dailyBudget: null,
      targetBasedBudget: false,
      productLibrary: false,
      enabled: true,
    })
  }
  rebuildDerivedChains()
  useExistingDialogVisible.value = false
  ElMessage.success('已按行追加项目并同步广告组/广告')
}

function handleAdd(type) {
  currentCardType.value = type
  addForm.name = ''
  addDialogVisible.value = true
}

function handleConfirmAdd() {
  if (!addForm.name.trim()) {
    ElMessage.warning('请输入名称')
    return
  }
  const name = addForm.name.trim()
  const internalId = filterForm.accounts[0]
  if (!internalId) {
    ElMessage.warning('请先选择账户')
    return
  }
  const a = accountById(internalId)
  projectList.value.push({
    id: `cmp_${internalId}_line_${Date.now()}`,
    internalAccountId: internalId,
    accountId: a?.accountId || '',
    accountName: a?.accountName || '',
    campaignName: name,
    projectName: name,
    existingProject: '',
    dailyBudget: null,
    targetBasedBudget: false,
    productLibrary: false,
    enabled: true,
  })
  rebuildDerivedChains()
  addDialogVisible.value = false
  ElMessage.success('新增成功')
}

function handleClear(type) {
  if (type === 'adGroup') {
    rebuildDerivedChains()
    ElMessage.success('已重置广告组为与项目行对应的默认')
    return
  }
  if (!projectList.value.length) {
    adList.value = []
    ElMessage.warning('暂无项目行')
    return
  }
  adList.value = projectList.value.map((p, i) => ({
    id: `ad_${p.id}`,
    projectId: p.id,
    adGroupId: `ag_${p.id}`,
    accountId: p.accountId,
    projectName: p.projectName,
    adName: `广告 ${i + 1}`,
    materialId: '',
    titlePackId: '',
    landingPage: '',
    preview: 0,
    authPage: '',
  }))
  ElMessage.success('已清空广告层素材与标题')
}

async function handleSettings(type) {
  currentCardType.value = type
  if (type === 'project') {
    await openProjectCampaignDialog()
    return
  }
  if (!projectList.value.length) {
    ElMessage.warning('请先选择账户生成项目行')
    return
  }
  if (type === 'adGroup') {
    await Promise.all([loadExistingAdGroups(), loadPixelOptions()])
    if (!ensureAdGroupRows()) return
    adGroupDialogVisible.value = true
  } else {
    if (!(await ensureAdTableRows())) return
    adSettingsDialogVisible.value = true
  }
}

onMounted(async () => {
  await loadAllAccounts()
  await loadExistingProjects()
  await Promise.all([
    loadExistingAdGroups(),
    loadPixelOptions(),
    loadAdGroupCatalogOptions(),
    loadMaterialOptions(),
    loadTitleOptions(),
  ])
})
</script>

<style scoped>
.batch-tools-container {
  padding: 0;
}

.filter-card {
  margin-bottom: 20px;
}

.cards-container {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.tool-card {
  min-height: 400px;
  border-radius: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  align-items: center;
}

.card-content {
  min-height: 280px;
  padding: 8px 0 0;
}

.card-content--scroll {
  min-height: 360px;
}

.data-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-right: 4px;
}

.data-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.item-index {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  background: var(--el-color-primary);
  color: #fff;
  border-radius: 50%;
  font-size: 14px;
  font-weight: 500;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  margin-bottom: 6px;
}

.item-meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.item-meta span {
  margin-right: 12px;
}

.dialog-tag-row {
  margin-bottom: 12px;
}

.table-wrap {
  width: 100%;
  overflow-x: auto;
}

.basic-cell {
  font-size: 13px;
  line-height: 1.5;
  color: var(--el-text-color-regular);
}

.batch-dialog--wide :deep(.el-dialog__body) {
  padding-top: 12px;
}

.select-empty-tip {
  padding: 10px 12px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
