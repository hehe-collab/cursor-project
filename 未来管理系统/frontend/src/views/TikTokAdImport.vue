<template>
  <div class="ad-import-page">
    <!-- 页面标题 + 顶部操作栏 -->
    <div class="page-header">
      <div class="header-left">
        <h2>广告导入</h2>
        <p class="page-desc">一张 Excel 同时导入广告系列、广告组和广告，自动按层级顺序创建</p>
      </div>
      <div class="header-actions">
        <el-button :icon="Clock" @click="historyDrawer = true">导入记录</el-button>
      </div>
    </div>

    <!-- 顶部配置区 -->
    <div class="config-bar">
      <div class="config-item">
        <span class="config-label">导入模式</span>
        <el-radio-group v-model="form.importMode" size="default">
          <el-radio-button value="multiple">
            <el-icon style="margin-right: 4px;"><UserFilled /></el-icon>
            多账户
          </el-radio-button>
          <el-radio-button value="single">
            <el-icon style="margin-right: 4px;"><User /></el-icon>
            单账户
          </el-radio-button>
        </el-radio-group>
      </div>
      <div v-if="form.importMode === 'single'" class="config-item account-select">
        <span class="config-label">目标账户</span>
        <AccountSelector v-model="form.advertiserId" style="width: 320px;" />
      </div>
      <div class="config-item config-tip">
        <el-icon :color="form.importMode === 'single' ? '#409eff' : '#e6a23c'" :size="16">
          <InfoFilled />
        </el-icon>
        <span>{{
          form.importMode === 'single'
            ? 'Excel 中无需填写账户 ID，level 列区分层级'
            : 'Excel 第一列填写 advertiser_id，level 列区分层级'
        }}</span>
      </div>
    </div>

    <!-- 步骤进度条 -->
    <el-steps :active="currentStep" finish-status="success" class="import-steps" align-center>
      <el-step title="下载模板" description="获取统一三层级模板" />
      <el-step title="上传文件" description="上传填写好的 Excel" />
      <el-step title="查看结果" description="确认导入状态" />
    </el-steps>

    <!-- 步骤内容区 -->
    <div class="steps-content">

      <!-- 步骤 1：下载模板 -->
      <el-card class="step-card" :class="{ 'step-active': currentStep === 0, 'step-done': currentStep > 0 }">
        <div class="step-card-header">
          <span class="step-badge" :class="{ done: currentStep > 0 }">
            <el-icon v-if="currentStep > 0"><SuccessFilled /></el-icon>
            <span v-else>1</span>
          </span>
          <span class="step-title">下载模板并填写</span>
          <el-button link type="primary" @click="fieldDrawer = true" style="margin-left: auto;">
            <el-icon><Document /></el-icon>
            查看字段说明
          </el-button>
        </div>

        <div class="step-card-body">
          <div class="template-section">
            <el-button
              type="primary"
              :icon="Download"
              :disabled="!canDownload"
              :loading="downloadLoading"
              @click="handleDownloadTemplate"
            >
              下载统一模板（{{ form.importMode === 'single' ? '单账户' : '多账户' }}）
            </el-button>
            <span class="template-hint">按 level 列区分层级，参考示例行填写</span>
          </div>
        </div>
      </el-card>

      <!-- 步骤 2：上传 Excel -->
      <el-card class="step-card" :class="{ 'step-active': currentStep === 1, 'step-done': currentStep > 1 }">
        <div class="step-card-header">
          <span class="step-badge" :class="{ done: currentStep > 1 }">
            <el-icon v-if="currentStep > 1"><SuccessFilled /></el-icon>
            <span v-else>2</span>
          </span>
          <span class="step-title">上传 Excel 文件</span>
        </div>

        <div class="step-card-body">
          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :http-request="handleCustomUpload"
            :before-upload="beforeUpload"
            :show-file-list="false"
            :disabled="!canUpload"
            accept=".xlsx,.xls"
          >
            <div v-if="!uploading" class="upload-content">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <p class="upload-main">将 Excel 文件拖到此处，或<em>点击上传</em></p>
              <p class="upload-sub">支持 .xlsx / .xls，文件大小不超过 10MB</p>
            </div>
            <div v-else class="upload-progress">
              <el-progress type="circle" :percentage="uploadPercent" :status="progressStatus" :width="80" />
              <p class="progress-text">{{ uploadStatusText }}</p>
            </div>
          </el-upload>

          <div v-if="!canUpload && form.importMode === 'single'" class="upload-warning">
            <el-icon><WarningFilled /></el-icon>
            <span>请先在上方选择广告账户</span>
          </div>
        </div>
      </el-card>

      <!-- 步骤 3：导入结果 -->
      <el-card
        v-if="result"
        ref="resultCardRef"
        class="step-card step-active"
        :class="resultCardClass"
      >
        <div class="step-card-header">
          <span class="step-badge" :class="resultBadgeClass">3</span>
          <span class="step-title">导入结果</span>
          <el-button link type="primary" @click="handleReset" style="margin-left: auto;">
            继续导入
          </el-button>
        </div>

        <div class="step-card-body">
          <div class="result-summary">
            <div class="result-icon-wrap" :class="resultBadgeClass">
              <el-icon :size="48">
                <SuccessFilled v-if="result.status === 'success'" />
                <CircleCloseFilled v-else-if="result.status === 'failed'" />
                <WarningFilled v-else />
              </el-icon>
            </div>
            <div class="result-text">
              <div class="result-title">{{ resultTitle }}</div>
              <div class="result-subtitle">{{ resultSubTitle }}</div>
            </div>
          </div>

          <div class="result-stats">
            <div class="stat-card stat-success">
              <div class="stat-num">{{ result.successCount }}</div>
              <div class="stat-label">成功</div>
            </div>
            <div class="stat-card stat-fail">
              <div class="stat-num">{{ result.failedCount }}</div>
              <div class="stat-label">失败</div>
            </div>
            <div class="stat-card stat-total">
              <div class="stat-num">{{ result.totalCount }}</div>
              <div class="stat-label">总计</div>
            </div>
          </div>

          <div v-if="errorLogs.length > 0" class="error-log-block">
            <div class="error-log-header">
              <el-icon color="#f56c6c"><WarningFilled /></el-icon>
              <span>错误详情（共 {{ errorLogs.length }} 条）</span>
            </div>
            <div class="error-log-list">
              <div v-for="(err, idx) in errorLogs.slice(0, 20)" :key="idx" class="error-item">
                <el-tag type="danger" size="small" effect="dark">第{{ err.row_number }}行</el-tag>
                <code v-if="err.advertiser_id" class="err-adv">{{ err.advertiser_id }}</code>
                <span class="err-msg">{{ err.error_message }}</span>
              </div>
              <div v-if="errorLogs.length > 20" class="error-more">
                还有 {{ errorLogs.length - 20 }} 条错误，请在导入记录中查看完整日志
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 字段说明 Drawer -->
    <el-drawer v-model="fieldDrawer" title="Excel 字段说明" size="560px" :z-index="2000">
      <div class="field-drawer-body">
        <div class="field-block">
          <h4>通用字段（所有层级）</h4>
          <el-table :data="commonFields" border size="small" class="field-table">
            <el-table-column prop="col" label="列名" width="160">
              <template #default="{ row }"><code>{{ row.col }}</code></template>
            </el-table-column>
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="example" label="示例" width="200" />
          </el-table>
        </div>

        <div class="field-block">
          <h4>广告系列字段（level = campaign）</h4>
          <el-table :data="campaignFields" border size="small" class="field-table">
            <el-table-column prop="col" label="列名" width="160">
              <template #default="{ row }"><code>{{ row.col }}</code></template>
            </el-table-column>
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="example" label="示例" width="240" />
          </el-table>
        </div>

        <div class="field-block">
          <h4>广告组字段（level = adgroup）</h4>
          <el-table :data="adgroupFields" border size="small" class="field-table">
            <el-table-column prop="col" label="列名" width="160">
              <template #default="{ row }"><code>{{ row.col }}</code></template>
            </el-table-column>
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="example" label="示例" width="200" />
          </el-table>
        </div>

        <div class="field-block">
          <h4>广告字段（level = ad）</h4>
          <el-table :data="adFields" border size="small" class="field-table">
            <el-table-column prop="col" label="列名" width="160">
              <template #default="{ row }"><code>{{ row.col }}</code></template>
            </el-table-column>
            <el-table-column prop="desc" label="说明" />
            <el-table-column prop="example" label="示例" width="220" />
          </el-table>
        </div>

        <el-alert type="info" :closable="false" show-icon class="field-tips-alert">
          <template #title>使用提示</template>
          <template #default>
            <ul class="tips-list">
              <li>同一 Excel 可同时包含 campaign、adgroup、ad 三种层级</li>
              <li>系统按 <strong>campaign → adgroup → ad</strong> 顺序处理</li>
              <li>广告组通过 <code>campaign_name</code> 关联广告系列</li>
              <li>广告通过 <code>campaign_name</code> + <code>adgroup_name</code> 关联广告组</li>
            </ul>
          </template>
        </el-alert>
      </div>
    </el-drawer>

    <!-- 历史记录 Drawer -->
    <el-drawer v-model="historyDrawer" title="导入记录" size="820px" :z-index="2000" @open="fetchHistory">
      <el-table :data="historyList" border stripe size="small" v-loading="historyLoading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="importType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ typeLabelMap[row.importType] || row.importType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="advertiserId" label="账户 ID" min-width="180">
          <template #default="{ row }">
            <code>{{ row.advertiserId || '多账户' }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabelMap[row.status] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功" width="70" align="center" />
        <el-table-column prop="failedCount" label="失败" width="70" align="center" />
        <el-table-column prop="totalCount" label="总计" width="70" align="center" />
        <el-table-column prop="createdBy" label="操作人" width="90" />
        <el-table-column prop="createdAt" label="导入时间" min-width="160" />
        <el-table-column label="操作" width="70" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Download,
  UploadFilled,
  InfoFilled,
  WarningFilled,
  SuccessFilled,
  CircleCloseFilled,
  Document,
  User,
  UserFilled,
  Clock,
} from '@element-plus/icons-vue'
import AccountSelector from '@/components/TikTok/AccountSelector.vue'
import { uploadExcel, downloadTemplate, getImportList, deleteImport } from '@/api/tiktok/adImport'

const form = reactive({
  importMode: 'multiple',
  advertiserId: '',
})

const currentStep = computed(() => {
  if (result.value) return 2
  if (uploading.value) return 1
  return 0
})

// ====== 字段说明数据 ======
const commonFields = computed(() => {
  const rows = []
  if (form.importMode === 'multiple') {
    rows.push({ col: 'advertiser_id', desc: '广告账户 ID（必填）', example: '7123456789012345678' })
  }
  rows.push(
    { col: 'level', desc: '层级类型（必填）', example: 'campaign / adgroup / ad' },
    { col: 'status', desc: '状态', example: 'ENABLE / DISABLE' },
  )
  return rows
})
const campaignFields = [
  { col: 'campaign_name', desc: '广告系列名称（必填）', example: '春季促销活动' },
  { col: 'objective', desc: '目标（必填）', example: 'TRAFFIC / CONVERSIONS / REACH' },
  { col: 'budget', desc: '预算金额', example: '1000.00' },
  { col: 'budget_mode', desc: '预算模式', example: 'BUDGET_MODE_DAY / BUDGET_MODE_TOTAL' },
]
const adgroupFields = [
  { col: 'campaign_name', desc: '关联的广告系列名称（必填）', example: '春季促销活动' },
  { col: 'adgroup_name', desc: '广告组名称（必填）', example: '18-24岁女性-北京' },
  { col: 'placements', desc: '投放位置', example: 'PLACEMENT_TIKTOK' },
  { col: 'bid', desc: '出价金额', example: '1.50' },
  { col: 'budget', desc: '预算金额', example: '500.00' },
]
const adFields = [
  { col: 'campaign_name', desc: '关联的广告系列名称（必填）', example: '春季促销活动' },
  { col: 'adgroup_name', desc: '关联的广告组名称（必填）', example: '18-24岁女性-北京' },
  { col: 'ad_name', desc: '广告名称（必填）', example: '视频1' },
  { col: 'ad_text', desc: '广告文案', example: '限时优惠！立即购买' },
  { col: 'call_to_action', desc: '行动号召', example: 'SHOP_NOW' },
  { col: 'landing_page_url', desc: '落地页 URL', example: 'https://example.com/product' },
  { col: 'video_id', desc: '视频素材 ID', example: 'v1234567890' },
]

// ====== Drawer 控制 ======
const fieldDrawer = ref(false)
const historyDrawer = ref(false)

// ====== 模板下载 ======
const downloadLoading = ref(false)

const canDownload = computed(() => {
  if (form.importMode === 'single') return !!form.advertiserId
  return true
})

async function handleDownloadTemplate() {
  if (!canDownload.value) {
    ElMessage.warning('单账户模式请先选择广告账户')
    return
  }
  downloadLoading.value = true
  try {
    const blob = await downloadTemplate(form.importMode)
    const name = `tiktok_ad_import_${form.importMode === 'single' ? '单账户' : '多账户'}.xlsx`
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success('模板下载成功')
  } catch (e) {
    console.error(e)
    ElMessage.error('模板下载失败')
  } finally {
    downloadLoading.value = false
  }
}

// ====== 上传 ======
const uploadRef = ref(null)
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadStatus = ref('')

const canUpload = computed(() => {
  if (form.importMode === 'single') return !!form.advertiserId
  return true
})
const progressStatus = computed(() => {
  if (uploadStatus.value === 'exception') return 'exception'
  if (uploadPercent.value === 100) return 'success'
  return ''
})
const uploadStatusText = computed(() => {
  if (uploadPercent.value < 100) return `上传中… ${uploadPercent.value}%`
  return '处理中…'
})

function beforeUpload(file) {
  if (!canUpload.value) {
    ElMessage.warning('请先选择广告账户')
    return false
  }
  const ext = file.name.toLowerCase()
  if (!ext.endsWith('.xlsx') && !ext.endsWith('.xls')) {
    ElMessage.error('只支持 .xlsx / .xls 格式')
    return false
  }
  if (file.size / 1024 / 1024 > 10) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  uploading.value = true
  uploadPercent.value = 0
  uploadStatus.value = ''
  return true
}

async function handleCustomUpload({ file }) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('importMode', form.importMode)
  if (form.importMode === 'single' && form.advertiserId) {
    formData.append('advertiserId', form.advertiserId)
  }
  try {
    const res = await uploadExcel(formData, (event) => {
      if (event.total > 0) {
        uploadPercent.value = Math.floor((event.loaded / event.total) * 100)
      }
    })
    uploading.value = false
    uploadPercent.value = 100
    uploadStatus.value = 'success'
    if (res?.code === 0) {
      result.value = res.data
      ElMessage.success('导入完成')
      await nextTick()
      resultCardRef.value?.$el?.scrollIntoView({ behavior: 'smooth', block: 'center' })
    } else {
      ElMessage.error(res?.message || '上传失败')
    }
  } catch (err) {
    uploading.value = false
    uploadStatus.value = 'exception'
    console.error(err)
    ElMessage.error('上传失败，请重试')
  }
}

// ====== 结果 ======
const result = ref(null)
const resultCardRef = ref(null)

const errorLogs = computed(() => {
  try {
    if (!result.value?.errorLogs) return []
    const parsed = typeof result.value.errorLogs === 'string'
      ? JSON.parse(result.value.errorLogs)
      : result.value.errorLogs
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
})

const resultTitle = computed(() => {
  if (result.value?.status === 'success') return '导入成功'
  if (result.value?.status === 'failed') return '导入失败'
  return '部分成功'
})
const resultSubTitle = computed(() => {
  if (!result.value) return ''
  return `总计 ${result.value.totalCount} 条，成功 ${result.value.successCount} 条，失败 ${result.value.failedCount} 条`
})
const resultCardClass = computed(() => {
  if (!result.value) return ''
  return {
    'result-success': result.value.status === 'success',
    'result-fail': result.value.status === 'failed',
    'result-partial': result.value.status !== 'success' && result.value.status !== 'failed',
  }
})
const resultBadgeClass = computed(() => {
  if (!result.value) return ''
  if (result.value.status === 'success') return 'done'
  if (result.value.status === 'failed') return 'fail'
  return 'warn'
})

// ====== 历史记录 ======
const historyList = ref([])
const historyLoading = ref(false)

const typeLabelMap = { unified: '三层级统一', campaigns: '广告系列', adgroups: '广告组', ads: '广告' }
const statusLabelMap = { success: '成功', partial: '部分成功', failed: '失败', pending: '等待中', processing: '处理中' }

function statusTagType(status) {
  return { success: 'success', partial: 'warning', failed: 'danger', pending: 'info', processing: 'warning' }[status] || 'info'
}

async function fetchHistory() {
  historyLoading.value = true
  try {
    const res = await getImportList({ page: 1, pageSize: 50 })
    historyList.value = res?.data?.list || []
  } catch (e) {
    console.error(e)
  } finally {
    historyLoading.value = false
  }
}

async function handleDelete(id) {
  try {
    await deleteImport(id)
    ElMessage.success('删除成功')
    historyList.value = historyList.value.filter(h => h.id !== id)
  } catch (e) {
    console.error(e)
  }
}

function handleReset() {
  result.value = null
  uploading.value = false
  uploadPercent.value = 0
  uploadStatus.value = ''
  if (uploadRef.value) uploadRef.value.clearFiles()
}
</script>

<style scoped>
.ad-import-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px 20px;
}

/* 页面标题栏 */
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}
.header-left h2 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1d2129;
}
.page-desc {
  margin: 0;
  font-size: 13px;
  color: #86909c;
}

/* 顶部配置区 */
.config-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  margin-bottom: 24px;
}
.config-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.config-label {
  font-size: 13px;
  font-weight: 600;
  color: #4e5969;
  white-space: nowrap;
}
.config-tip {
  font-size: 12px;
  color: #86909c;
  margin-left: auto;
}

/* 步骤条 */
.import-steps {
  margin-bottom: 24px;
}

/* 步骤卡片 */
.steps-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.step-card {
  border-radius: 10px;
  transition: box-shadow 0.25s, border-color 0.25s;
  border: 1px solid #e5e6eb;
}
.step-card.step-active {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.08);
}
.step-card.step-done {
  opacity: 0.7;
}
.step-card :deep(.el-card__body) {
  padding: 0;
}

.step-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 20px;
  border-bottom: 1px solid #f2f3f5;
}
.step-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  background: #c0c4cc;
  color: #fff;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
  transition: background 0.25s;
}
.step-active .step-badge {
  background: #409eff;
}
.step-badge.done {
  background: #67c23a;
}
.step-badge.fail {
  background: #f56c6c;
}
.step-badge.warn {
  background: #e6a23c;
}
.step-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d2129;
}

.step-card-body {
  padding: 20px;
}

/* 步骤 1：模板下载 */
.template-section {
  display: flex;
  align-items: center;
  gap: 16px;
}
.template-hint {
  font-size: 13px;
  color: #86909c;
}

/* 步骤 2：上传 */
.upload-area :deep(.el-upload) {
  width: 100%;
}
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}
.upload-content {
  text-align: center;
}
.upload-icon {
  font-size: 44px;
  color: #c0c4cc;
  margin-bottom: 10px;
}
.upload-main {
  font-size: 14px;
  color: #4e5969;
  margin: 0 0 4px;
}
.upload-main em {
  color: #409eff;
  font-style: normal;
}
.upload-sub {
  font-size: 12px;
  color: #86909c;
  margin: 0;
}
.upload-progress {
  text-align: center;
}
.progress-text {
  margin-top: 10px;
  font-size: 13px;
  color: #4e5969;
}
.upload-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #fff7e6;
  border: 1px solid #ffe58f;
  border-radius: 6px;
  color: #d46b08;
  font-size: 13px;
}

/* 步骤 3：结果 */
.result-success { border-color: #67c23a; }
.result-fail { border-color: #f56c6c; }
.result-partial { border-color: #e6a23c; }

.result-summary {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.result-icon-wrap {
  flex-shrink: 0;
}
.result-icon-wrap.done { color: #67c23a; }
.result-icon-wrap.fail { color: #f56c6c; }
.result-icon-wrap.warn { color: #e6a23c; }
.result-title {
  font-size: 18px;
  font-weight: 700;
  color: #1d2129;
  margin-bottom: 4px;
}
.result-subtitle {
  font-size: 13px;
  color: #86909c;
}

.result-stats {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  flex: 1;
  text-align: center;
  padding: 16px 12px;
  border-radius: 8px;
}
.stat-success { background: #f0f9eb; }
.stat-fail { background: #fef0f0; }
.stat-total { background: #ecf5ff; }
.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: 4px;
}
.stat-success .stat-num { color: #67c23a; }
.stat-fail .stat-num { color: #f56c6c; }
.stat-total .stat-num { color: #409eff; }
.stat-label {
  font-size: 12px;
  color: #86909c;
}

/* 错误日志 */
.error-log-block {
  border: 1px solid #fde2e2;
  border-radius: 8px;
  overflow: hidden;
}
.error-log-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: #fef0f0;
  font-size: 13px;
  font-weight: 600;
  color: #f56c6c;
}
.error-log-list {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px 14px;
}
.error-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 5px 0;
  font-size: 12px;
  border-bottom: 1px solid #f5f5f5;
}
.error-item:last-child { border-bottom: none; }
.err-adv {
  font-size: 11px;
  padding: 1px 6px;
  background: #f5f5f5;
  border-radius: 3px;
  color: #86909c;
}
.err-msg {
  color: #f56c6c;
  flex: 1;
}
.error-more {
  padding: 8px 0;
  font-size: 12px;
  color: #86909c;
  text-align: center;
}

/* 字段说明 Drawer */
.field-drawer-body {
  padding: 0 4px;
}
.field-block {
  margin-bottom: 20px;
}
.field-block h4 {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #1d2129;
}
.field-table code {
  padding: 1px 6px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
.field-tips-alert {
  margin-top: 8px;
}
.tips-list {
  margin: 6px 0 0;
  padding-left: 18px;
  font-size: 13px;
  color: #4e5969;
  line-height: 1.8;
}
.tips-list code {
  padding: 1px 5px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 3px;
  font-size: 12px;
}
</style>
