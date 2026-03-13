<template>
  <div class="delivery-links-page">
    <el-card>
      <template #header><span>投放链接配置</span></template>

      <!-- Tab: 推广 / 目标 / 设置 -->
      <div class="promo-tabs">
        <div :class="['promo-tab', { active: activeTab === 'promo' }]" @click="activeTab = 'promo'">推广</div>
        <div :class="['promo-tab', { active: activeTab === 'target' }]" @click="activeTab = 'target'">目标</div>
        <div :class="['promo-tab', { active: activeTab === 'settings' }]" @click="activeTab = 'settings'">设置</div>
      </div>

      <!-- 筛选 + 新增 -->
      <div class="filter-row">
        <el-form :inline="true" class="filter-form" @submit.prevent="onSearch">
          <el-form-item label="推广ID">
            <el-input v-model="query.promoteId" placeholder="请输入推广ID" clearable style="width:180px" @keyup.enter="onSearch" />
          </el-form-item>
          <el-form-item label="推广名称">
            <el-input v-model="query.promoteName" placeholder="请输入推广名称" clearable style="width:160px" />
          </el-form-item>
          <el-form-item label="剧ID">
            <el-input v-model="query.dramaId" placeholder="请输入剧ID" clearable style="width:120px" />
          </el-form-item>
          <el-form-item label="推广域名">
            <el-input v-model="query.domain" placeholder="例: chuhai1x3x4x5x8d" clearable style="width:160px" />
          </el-form-item>
          <el-form-item label="投放媒体">
            <el-select v-model="query.media" placeholder="请选择" clearable style="width:120px">
              <el-option label="Facebook" value="Facebook" />
              <el-option label="Google" value="Google" />
              <el-option label="TikTok" value="TikTok" />
            </el-select>
          </el-form-item>
          <el-form-item label="国家/地区">
            <el-select v-model="query.country" placeholder="请选择" clearable style="width:120px">
              <el-option label="美国" value="US" />
              <el-option label="印尼" value="ID" />
              <el-option label="泰国" value="TH" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="onSearch">搜索</el-button>
            <el-button link @click="onReset">重置</el-button>
          </el-form-item>
        </el-form>
        <el-button type="primary" class="btn-new" @click="showAdd">
          <el-icon><Plus /></el-icon>
          新增
        </el-button>
      </div>

      <div class="action-row">
        <el-button @click="showBatchEdit">批量修改</el-button>
        <el-button @click="onBatchDelete">删除</el-button>
        <el-button @click="onExport">导出</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe @selection-change="onSelectionChange">
        <template #empty>
          <el-empty description="暂无投放链接" />
        </template>
        <el-table-column type="selection" width="45" />
        <el-table-column label="" width="60" align="center">
          <template #default="{ row }">
            <el-switch v-model="row.status" active-value="active" inactive-value="stopped" @change="onToggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column prop="promote_id" label="推广ID" min-width="140" show-overflow-tooltip />
        <el-table-column prop="platform" label="广告平台" width="100">
          <template #default="{ row }">{{ (row.platform || '').toLowerCase() }}</template>
        </el-table-column>
        <el-table-column prop="promote_id" label="推广ID" width="100" />
        <el-table-column prop="promote_name" label="推广名称" min-width="160" show-overflow-tooltip />
        <el-table-column prop="image_id" label="图ID" width="80">
          <template #default>{{ '-' }}</template>
        </el-table-column>
        <el-table-column prop="target" label="目标" width="120" show-overflow-tooltip />
        <el-table-column prop="plan_group_id" label="方案ID" width="90" />
        <el-table-column prop="amount" label="金额" width="80" />
        <el-table-column prop="spend" label="花费" width="80" />
        <el-table-column prop="created_by" label="创建人" width="80" />
        <el-table-column prop="created_at" label="创建时间" width="175" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="copyPromo(row)">复制推广</el-button>
            <el-button link type="primary" @click="togglePromo(row)">
              {{ row.status === 'active' ? '停止推广' : '启用推广' }}
            </el-button>
            <el-button link type="primary" @click="showEdit(row)">修改</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span class="total">共 {{ total }} 条</span>
        <el-select v-model="query.pageSize" style="width:100px" @change="loadList">
          <el-option :value="10" label="10条/页" />
          <el-option :value="20" label="20条/页" />
          <el-option :value="50" label="50条/页" />
        </el-select>
        <el-pagination
          v-model:current-page="query.page"
          :total="total"
          :page-size="query.pageSize"
          layout="prev, pager, next"
          @current-change="loadList"
        />
        <span class="goto">前往</span>
        <el-input v-model.number="gotoPage" type="number" min="1" :max="Math.ceil(total / query.pageSize) || 1" style="width:60px" @keyup.enter="goToPage" />
        <span class="goto">页</span>
      </div>
    </el-card>

    <!-- 新增/修改弹窗 -->
    <el-dialog v-model="formVisible" :title="formId ? '修改投放链接' : '新增投放链接'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="推广ID" prop="promote_id"><el-input v-model="form.promote_id" placeholder="请输入推广ID" /></el-form-item>
        <el-form-item label="投放媒体" prop="platform">
          <el-select v-model="form.platform" placeholder="请选择投放媒体" style="width:100%">
            <el-option label="Facebook" value="Facebook" />
            <el-option label="Google" value="Google" />
            <el-option label="TikTok" value="TikTok" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家/地区">
          <el-select v-model="form.country" placeholder="请选择国家/地区" style="width:100%">
            <el-option label="美国" value="US" />
            <el-option label="印尼" value="ID" />
            <el-option label="泰国" value="TH" />
          </el-select>
        </el-form-item>
        <el-form-item label="推广名称"><el-input v-model="form.promote_name" placeholder="请输入推广名称" /></el-form-item>
        <el-form-item label="剧ID"><el-input v-model.number="form.drama_id" placeholder="请输入剧ID" /></el-form-item>
        <el-form-item label="方案组ID">
          <el-select v-model="form.plan_group_id" placeholder="请选择方案组" clearable style="width:100%">
            <el-option v-for="g in planGroups" :key="g.id" :label="g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="金豆数"><el-input-number v-model="form.bean_count" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="免费集数"><el-input-number v-model="form.free_episodes" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="推广域名"><el-input v-model="form.domain" placeholder="例: chuhai1x3x4x5x8d" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改弹窗 -->
    <el-dialog v-model="batchEditVisible" title="批量修改" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" style="margin-bottom:16px">已选择 {{ selectedRows.length }} 条记录</el-alert>
      <el-form :model="batchForm" label-width="120px">
        <el-form-item label="推广ID"><el-input v-model="batchForm.promote_id" placeholder="留空则不修改" clearable /></el-form-item>
        <el-form-item label="投放媒体">
          <el-select v-model="batchForm.platform" placeholder="留空则不修改" clearable style="width:100%">
            <el-option label="Facebook" value="Facebook" />
            <el-option label="Google" value="Google" />
            <el-option label="TikTok" value="TikTok" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家/地区">
          <el-select v-model="batchForm.country" placeholder="留空则不修改" clearable style="width:100%">
            <el-option label="美国" value="US" />
            <el-option label="印尼" value="ID" />
            <el-option label="泰国" value="TH" />
          </el-select>
        </el-form-item>
        <el-form-item label="剧ID"><el-input v-model.number="batchForm.drama_id" placeholder="留空则不修改" clearable /></el-form-item>
        <el-form-item label="方案组ID"><el-input v-model.number="batchForm.plan_group_id" placeholder="留空则不修改" clearable /></el-form-item>
        <el-form-item label="金豆数"><el-input-number v-model="batchForm.bean_count" :min="0" placeholder="留空则不修改" clearable style="width:100%" /></el-form-item>
        <el-form-item label="免费集数"><el-input-number v-model="batchForm.free_episodes" :min="0" placeholder="留空则不修改" clearable style="width:100%" /></el-form-item>
        <el-form-item label="推广域名"><el-input v-model="batchForm.domain" placeholder="留空则不修改" clearable /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchEditVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../api/request'
import { exportToCSV } from '../utils/export'
import { useConfirmDelete } from '../composables/useConfirmDelete'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const batchEditVisible = ref(false)
const formId = ref(null)
const formRef = ref()
const activeTab = ref('promo')
const gotoPage = ref(1)
const formRules = {
  promote_id: [{ required: true, message: '请输入推广ID', trigger: 'blur' }],
  platform: [{ required: true, message: '请选择投放媒体', trigger: 'change' }],
}
const selectedRows = ref([])
const planGroups = ref([])
const query = reactive({ page: 1, pageSize: 10, promoteId: '', media: '', country: '', promoteName: '', dramaId: '', domain: '' })
const form = reactive({ promote_id: '', platform: '', country: '', promote_name: '', drama_id: null, plan_group_id: null, bean_count: 0, free_episodes: 0, domain: '' })
const batchForm = reactive({ promote_id: '', platform: '', country: '', drama_id: null, plan_group_id: null, bean_count: null, free_episodes: null, domain: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.promoteId) params.promoteId = query.promoteId
    if (query.media) params.media = query.media
    if (query.promoteName) params.promoteName = query.promoteName
    if (query.country) params.country = query.country
    if (query.dramaId) params.dramaId = query.dramaId
    if (query.domain) params.domain = query.domain
    const res = await request.get('/delivery-links', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = (res.data?.list || []).map(r => ({
      ...r,
      drama_name: r.drama_name || `剧${r.drama_id || '-'}`,
      target: r.target || r.drama_name || `剧${r.drama_id || '-'}`,
      status: r.status || 'active',
    }))
    total.value = res.data?.total || 0
    gotoPage.value = query.page
  } finally {
    loading.value = false
  }
}

async function loadPlanGroups() {
  const res = await request.get('/recharge-groups', {}).catch(() => ({ data: { list: [] } }))
  planGroups.value = res.data?.list || []
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  Object.assign(query, { page: 1, promoteId: '', media: '', country: '', promoteName: '', dramaId: '', domain: '' })
  loadList()
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function showAdd() {
  formId.value = null
  Object.assign(form, { promote_id: '', platform: '', country: '', promote_name: '', drama_id: null, plan_group_id: null, bean_count: 0, free_episodes: 0, domain: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { promote_id: row.promote_id, platform: row.platform, country: row.country || '', promote_name: row.promote_name, drama_id: row.drama_id, plan_group_id: row.plan_group_id, bean_count: row.bean_count || 0, free_episodes: row.free_episodes || 0, domain: row.domain })
  formVisible.value = true
}

function showBatchEdit() {
  if (!selectedRows.value.length) return ElMessage.warning('请先选择要修改的项')
  Object.assign(batchForm, { promote_id: '', platform: '', country: '', drama_id: null, plan_group_id: null, bean_count: null, free_episodes: null, domain: '' })
  batchEditVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch { return }
  try {
    if (formId.value) {
      await request.put(`/delivery-links/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/delivery-links', form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function submitBatchEdit() {
  try {
    const payload = {}
    if (batchForm.promote_id) payload.promote_id = batchForm.promote_id
    if (batchForm.platform) payload.platform = batchForm.platform
    if (batchForm.country) payload.country = batchForm.country
    if (batchForm.drama_id != null && batchForm.drama_id !== '') payload.drama_id = batchForm.drama_id
    if (batchForm.plan_group_id != null && batchForm.plan_group_id !== '') payload.plan_group_id = batchForm.plan_group_id
    if (batchForm.bean_count != null && batchForm.bean_count !== '') payload.bean_count = batchForm.bean_count
    if (batchForm.free_episodes != null && batchForm.free_episodes !== '') payload.free_episodes = batchForm.free_episodes
    if (batchForm.domain) payload.domain = batchForm.domain
    if (!Object.keys(payload).length) return ElMessage.warning('请至少填写一项要修改的内容')
    for (const row of selectedRows.value) {
      await request.put(`/delivery-links/${row.id}`, { ...row, ...payload })
    }
    ElMessage.success('批量修改成功')
    batchEditVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('批量修改失败')
  }
}

const { confirmDelete } = useConfirmDelete({ onSuccess: loadList })
function onDelete(row) {
  confirmDelete(async (r) => request.delete(`/delivery-links/${r.id}`), row)
}

function copyPromo(row) {
  const url = `https://${row.domain || 'example.com'}/?promote=${row.promote_id}&drama=${row.drama_id}`
  navigator.clipboard.writeText(url).then(() => ElMessage.success('链接已复制')).catch(() => ElMessage.error('复制失败'))
}

async function onToggleStatus(row) {
  try {
    await request.put(`/delivery-links/${row.id}`, { ...row, status: row.status })
    ElMessage.success(row.status === 'active' ? '已启用' : '已停止')
  } catch {
    row.status = row.status === 'active' ? 'stopped' : 'active'
    ElMessage.error('操作失败')
  }
}

async function togglePromo(row) {
  const newStatus = row.status === 'active' ? 'stopped' : 'active'
  try {
    await request.put(`/delivery-links/${row.id}`, { ...row, status: newStatus })
    row.status = newStatus
    ElMessage.success(newStatus === 'active' ? '已启用推广' : '已停止推广')
  } catch {
    ElMessage.error('操作失败')
  }
}

async function onBatchDelete() {
  if (!selectedRows.value.length) return ElMessage.warning('请先选择要删除的项')
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedRows.value.length} 条记录吗？`, '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    for (const row of selectedRows.value) {
      await request.delete(`/delivery-links/${row.id}`)
    }
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

async function onExport() {
  try {
    const params = { pageSize: 2000 }
    if (query.promoteId) params.promoteId = query.promoteId
    if (query.media) params.media = query.media
    if (query.promoteName) params.promoteName = query.promoteName
    const res = await request.get('/delivery-links', { params }).catch(() => ({ data: { list: [] } }))
    const data = res.data?.list || []
    if (!data.length) return ElMessage.warning('暂无数据可导出')
    exportToCSV(data, [
      { prop: 'promote_id', label: '推广ID' },
      { prop: 'platform', label: '广告平台' },
      { prop: 'promote_name', label: '推广名称' },
      { prop: 'drama_id', label: '剧ID' },
      { prop: 'plan_group_id', label: '方案ID' },
      { prop: 'amount', label: '金额' },
      { prop: 'spend', label: '花费' },
      { prop: 'created_at', label: '创建时间' },
    ], `投放链接_${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

function goToPage() {
  const p = Math.max(1, Math.min(Math.ceil(total.value / query.pageSize) || 1, parseInt(gotoPage.value) || 1))
  query.page = p
  gotoPage.value = p
  loadList()
}

onMounted(() => {
  loadList()
  loadPlanGroups()
})
</script>

<style scoped>
.promo-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #ebeef5;
}
.promo-tab {
  padding: 10px 24px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  transition: all 0.2s;
}
.promo-tab:hover { color: #409EFF; }
.promo-tab.active {
  color: #409EFF;
  font-weight: 600;
  border-bottom-color: #409EFF;
}
.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 16px;
}
.filter-form { margin: 0; }
.btn-new {
  display: flex;
  align-items: center;
  gap: 6px;
}
.action-row { margin-bottom: 16px; }
.pagination-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}
.pagination-row .total { color: #606266; font-size: 14px; }
.pagination-row .goto { color: #606266; font-size: 14px; margin-left: 4px; }
</style>
