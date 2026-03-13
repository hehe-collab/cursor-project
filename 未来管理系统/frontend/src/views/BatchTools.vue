<template>
  <div class="batch-tools-page">
    <el-card>
      <template #header><span>批量工具</span></template>
    <div class="page-toolbar">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="主体">
          <el-select v-model="filter.subject" placeholder="请选择主体" clearable style="width:160px">
            <el-option label="启量" value="启量" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户">
          <el-select
            v-model="filter.accounts"
            multiple
            filterable
            placeholder="可多选账户，支持名称/ID搜索"
            style="width:320px"
          >
            <el-option v-for="a in accountOptions" :key="a.id" :label="`${a.account_name || a.account_id || '-'} (${a.account_id || '-'})`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSubmit">提交</el-button>
          <el-button type="primary" @click="$router.push('/ad-task')">查看任务</el-button>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="$router.push('/delivery-links')">投放链接配置</el-button>
    </div>

    <el-row :gutter="20" class="cards-row">
      <el-col :span="8">
        <el-card shadow="hover" class="workflow-card">
          <template #header>
            <span>项目</span>
            <div class="card-actions">
              <el-button link type="primary" size="small" @click="onProjectUseExisting">使用已有</el-button>
              <el-button link type="primary" size="small" @click="onProjectAdd">新增</el-button>
              <el-button link type="primary" size="small" @click="onProjectSet">设置</el-button>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="64" class="cloud-icon"><Upload /></el-icon>
            <p class="card-hint">{{ projectName || (hasAccounts ? '点击设置选择或创建项目' : '请先选择账户') }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="workflow-card">
          <template #header>
            <span>广告组</span>
            <div class="card-actions">
              <el-button link type="primary" size="small" @click="onAdGroupUseExisting">使用已有</el-button>
              <el-button link type="primary" size="small" @click="onAdGroupAdd">新增</el-button>
              <el-button link type="primary" size="small" @click="onAdGroupSet">设置</el-button>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="64" class="cloud-icon"><Upload /></el-icon>
            <p class="card-hint">{{ adGroupName || '点击设置配置出价与预算' }}</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="workflow-card">
          <template #header>
            <span>广告</span>
            <div class="card-actions">
              <el-button link type="primary" size="small" @click="onAdSet">设置</el-button>
              <el-button link type="primary" size="small" @click="onAdClear">清空</el-button>
            </div>
          </template>
          <div class="card-content">
            <el-icon :size="64" class="cloud-icon"><Upload /></el-icon>
            <p class="card-hint">{{ adName || '点击设置关联素材与标题' }}</p>
          </div>
        </el-card>
      </el-col>
    </el-row>
    </el-card>

    <!-- 项目选择/设置弹窗 -->
    <el-dialog v-model="projectDialogVisible" title="选择项目" width="480px" destroy-on-close>
      <el-select v-model="selectedProjectId" placeholder="请选择项目" filterable style="width:100%">
        <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <template #footer>
        <el-button @click="projectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmProject">确定</el-button>
      </template>
    </el-dialog>
    <!-- 广告组选择弹窗 -->
    <el-dialog v-model="adGroupDialogVisible" title="选择广告组" width="480px" destroy-on-close>
      <el-select v-model="selectedAdGroupId" placeholder="请选择广告组" filterable style="width:100%">
        <el-option v-for="g in adGroupOptions" :key="g.id" :label="g.name" :value="g.id" />
      </el-select>
      <template #footer>
        <el-button @click="adGroupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAdGroup">确定</el-button>
      </template>
    </el-dialog>
    <!-- 广告设置弹窗 -->
    <el-dialog v-model="adDialogVisible" title="广告设置" width="480px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="素材"><el-select v-model="adForm.materialId" placeholder="请选择素材" clearable style="width:100%" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="adForm.title" placeholder="请输入标题" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const filter = reactive({ subject: '', accounts: [] })
const accountOptions = ref([])
const projectName = ref('')
const adGroupName = ref('')
const adName = ref('')
const projectDialogVisible = ref(false)
const adGroupDialogVisible = ref(false)
const adDialogVisible = ref(false)
const selectedProjectId = ref(null)
const selectedAdGroupId = ref(null)
const projectOptions = ref([{ id: 1, name: '项目A' }, { id: 2, name: '项目B' }])
const adGroupOptions = ref([{ id: 1, name: '广告组1' }, { id: 2, name: '广告组2' }])
const adForm = reactive({ materialId: null, title: '' })

const hasAccounts = computed(() => filter.accounts && filter.accounts.length > 0)

async function loadAccounts() {
  try {
    const res = await request.get('/accounts', { params: { pageSize: 100 } }).catch(() => ({ data: { list: [] } }))
    accountOptions.value = (res.data?.list || []).filter(a => a && a.id != null)
  } catch {
    accountOptions.value = []
  }
}

function onSubmit() {
  if (!filter.accounts?.length) {
    ElMessage.warning('请先选择账户')
    return
  }
  ElMessage.info('任务已提交（功能开发中）')
}

function onProjectUseExisting() {
  if (!hasAccounts.value) return ElMessage.warning('请先选择账户')
  projectDialogVisible.value = true
}
function onProjectAdd() {
  if (!hasAccounts.value) return ElMessage.warning('请先选择账户')
  ElMessage.info('新增项目（需对接广告平台 API）')
}
function onProjectSet() {
  if (!hasAccounts.value) return ElMessage.warning('请先选择账户')
  projectDialogVisible.value = true
}
function confirmProject() {
  const p = projectOptions.value.find(x => x.id === selectedProjectId.value)
  projectName.value = p?.name || ''
  projectDialogVisible.value = false
  if (p) ElMessage.success('已选择项目')
}

function onAdGroupUseExisting() {
  adGroupDialogVisible.value = true
}
function onAdGroupAdd() {
  ElMessage.info('新增广告组（需对接广告平台 API）')
}
function onAdGroupSet() {
  adGroupDialogVisible.value = true
}
function confirmAdGroup() {
  const g = adGroupOptions.value.find(x => x.id === selectedAdGroupId.value)
  adGroupName.value = g?.name || ''
  adGroupDialogVisible.value = false
  if (g) ElMessage.success('已选择广告组')
}

function onAdSet() {
  adDialogVisible.value = true
}
function onAdClear() {
  adName.value = ''
  adForm.materialId = null
  adForm.title = ''
  ElMessage.success('已清空')
}
function confirmAd() {
  adName.value = adForm.title || '广告'
  adDialogVisible.value = false
  ElMessage.success('已设置')
}

onMounted(loadAccounts)
</script>

<style scoped>
.batch-tools-page { padding: 0; }
.page-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}
.filter-form { margin: 0; }
.cards-row { margin-top: 0; }
.workflow-card {
  min-height: 240px;
  margin-bottom: 20px;
  border-radius: 10px;
  transition: all 0.2s ease;
  overflow: hidden;
}
.workflow-card:hover {
  box-shadow: 0 8px 24px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.workflow-card :deep(.el-card__header) {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  font-weight: 600;
}
.card-actions { display: flex; gap: 6px; }
.card-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  min-height: 160px;
  background: linear-gradient(180deg, #fafbfc 0%, #f5f7fa 100%);
}
.cloud-icon {
  color: #c0c4cc;
  margin-bottom: 16px;
  opacity: 0.8;
}
.card-hint { font-size: 14px; color: #909399; margin: 0; line-height: 1.6; }
</style>
