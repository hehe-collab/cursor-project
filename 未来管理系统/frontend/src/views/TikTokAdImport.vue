<template>
  <div class="ad-import-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>广告导入</h2>
      <p class="page-desc">一张 Excel 同时导入广告系列、广告组和广告，自动按层级顺序创建</p>
    </div>

    <!-- ====== 左右分栏主区域 ====== -->
    <div class="main-grid">

      <!-- ====== 左侧：控制面板 ====== -->
      <aside class="control-panel">
        <div class="panel-section">
          <div class="section-label">
            <el-icon><Setting /></el-icon>
            导入模式
          </div>
          <el-radio-group v-model="form.importMode" class="mode-group">
            <el-radio-button value="multiple">
              <div class="mode-inner">
                <el-icon class="mode-icon"><UserFilled /></el-icon>
                <div class="mode-text">
                  <div class="mode-name">多账户</div>
                  <div class="mode-desc">Excel 含 advertiser_id</div>
                </div>
              </div>
            </el-radio-button>
            <el-radio-button value="single">
              <div class="mode-inner">
                <el-icon class="mode-icon"><User /></el-icon>
                <div class="mode-text">
                  <div class="mode-name">单账户</div>
                  <div class="mode-desc">页面选择账户</div>
                </div>
              </div>
            </el-radio-button>
          </el-radio-group>

          <el-alert
            :title="form.importMode === 'single'
              ? 'Excel 中无需填写账户 ID，level 列区分层级'
              : 'Excel 第一列填写 advertiser_id，level 列区分层级'"
            :type="form.importMode === 'single' ? 'info' : 'warning'"
            :closable="false"
            show-icon
            class="mode-tip"
          />
        </div>

        <el-divider />

        <div v-if="form.importMode === 'single'" class="panel-section">
          <div class="section-label">
            <el-icon><OfficeBuilding /></el-icon>
            目标账户
          </div>
          <AccountSelector v-model="form.advertiserId" />
          <div class="form-tip">仅显示已在账户管理录入且 TikTok OAuth 为 active 的可执行账户</div>
        </div>

        <el-divider />

        <div class="panel-section">
          <div class="section-label">
            <el-icon><Document /></el-icon>
            历史记录
          </div>
          <el-button size="small" :icon="Document" @click="handleViewHistory">
            查看导入记录
          </el-button>
          <el-button size="small" link type="primary" @click="handleReset" style="margin-left: 8px;">
            继续导入
          </el-button>
        </div>
      </aside>

      <!-- ====== 右侧：操作区 ====== -->
      <main class="action-area">

        <!-- ====== 步骤 1：下载统一模板 ====== -->
        <el-card class="action-card">
          <template #header>
            <div class="card-header">
              <span class="card-step">1</span>
              <span class="card-title">下载模板并填写</span>
            </div>
          </template>

          <el-alert
            title="统一三层级模板：一张表包含广告系列 + 广告组 + 广告，level 列区分层级"
            type="info"
            :closable="false"
            show-icon
          />

          <div class="template-row">
            <el-button
              type="primary"
              :icon="Download"
              :disabled="!canDownload"
              :loading="downloadLoading"
              @click="handleDownloadTemplate"
            >
              下载统一模板（{{ form.importMode === 'single' ? '单账户' : '多账户' }}）
            </el-button>
            <div class="template-hint">
              <el-icon><InfoFilled /></el-icon>
              <span>按 level 列区分层级，参考示例行填写</span>
            </div>
          </div>

          <!-- 字段说明折叠 -->
          <el-collapse v-model="collapseActive" class="field-collapse">
            <el-collapse-item title="查看字段说明" name="fields">
              <div class="field-table">
                <!-- 通用字段 -->
                <div class="field-block">
                  <h4>通用字段（所有层级）</h4>
                  <table class="field-spec">
                    <thead><tr><th>列名</th><th>说明</th><th>示例</th></tr></thead>
                    <tbody>
                      <tr v-if="form.importMode === 'multiple'">
                        <td><code>advertiser_id</code></td>
                        <td>广告账户 ID（必填）</td>
                        <td>7123456789012345678</td>
                      </tr>
                      <tr>
                        <td><code>level</code></td>
                        <td>层级类型（必填）</td>
                        <td>campaign / adgroup / ad</td>
                      </tr>
                      <tr>
                        <td><code>status</code></td>
                        <td>状态</td>
                        <td>ENABLE / DISABLE</td>
                      </tr>
                    </tbody>
                  </table>
                </div>

                <!-- 广告系列字段 -->
                <div class="field-block">
                  <h4>广告系列字段（level = campaign）</h4>
                  <table class="field-spec">
                    <thead><tr><th>列名</th><th>说明</th><th>示例</th></tr></thead>
                    <tbody>
                      <tr><td><code>campaign_name</code></td><td>广告系列名称（必填）</td><td>春季促销活动</td></tr>
                      <tr><td><code>objective</code></td><td>目标（必填）</td><td>TRAFFIC / CONVERSIONS / REACH</td></tr>
                      <tr><td><code>budget</code></td><td>预算金额</td><td>1000.00</td></tr>
                      <tr><td><code>budget_mode</code></td><td>预算模式</td><td>BUDGET_MODE_DAY / BUDGET_MODE_TOTAL</td></tr>
                    </tbody>
                  </table>
                </div>

                <!-- 广告组字段 -->
                <div class="field-block">
                  <h4>广告组字段（level = adgroup）</h4>
                  <table class="field-spec">
                    <thead><tr><th>列名</th><th>说明</th><th>示例</th></tr></thead>
                    <tbody>
                      <tr><td><code>campaign_name</code></td><td>关联的广告系列名称（必填）</td><td>春季促销活动</td></tr>
                      <tr><td><code>adgroup_name</code></td><td>广告组名称（必填）</td><td>18-24岁女性-北京</td></tr>
                      <tr><td><code>placements</code></td><td>投放位置</td><td>PLACEMENT_TIKTOK</td></tr>
                      <tr><td><code>bid</code></td><td>出价金额</td><td>1.50</td></tr>
                      <tr><td><code>budget</code></td><td>预算金额</td><td>500.00</td></tr>
                    </tbody>
                  </table>
                </div>

                <!-- 广告字段 -->
                <div class="field-block">
                  <h4>广告字段（level = ad）</h4>
                  <table class="field-spec">
                    <thead><tr><th>列名</th><th>说明</th><th>示例</th></tr></thead>
                    <tbody>
                      <tr><td><code>campaign_name</code></td><td>关联的广告系列名称（必填）</td><td>春季促销活动</td></tr>
                      <tr><td><code>adgroup_name</code></td><td>关联的广告组名称（必填）</td><td>18-24岁女性-北京</td></tr>
                      <tr><td><code>ad_name</code></td><td>广告名称（必填）</td><td>视频1</td></tr>
                      <tr><td><code>ad_text</code></td><td>广告文案</td><td>限时优惠！立即购买</td></tr>
                      <tr><td><code>call_to_action</code></td><td>行动号召</td><td>SHOP_NOW</td></tr>
                      <tr><td><code>landing_page_url</code></td><td>落地页 URL</td><td>https://example.com/product</td></tr>
                      <tr><td><code>video_id</code></td><td>视频素材 ID</td><td>v1234567890</td></tr>
                    </tbody>
                  </table>
                </div>

                <div class="field-block">
                  <h4>使用提示</h4>
                  <ul class="tips-list">
                    <li>同一 Excel 可同时包含 <code>campaign</code>、<code>adgroup</code>、<code>ad</code> 三种层级</li>
                    <li>系统按 <strong>campaign → adgroup → ad</strong> 顺序处理</li>
                    <li>广告组通过 <code>campaign_name</code> 关联广告系列</li>
                    <li>广告通过 <code>campaign_name</code> + <code>adgroup_name</code> 关联广告组</li>
                  </ul>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>

        <!-- ====== 步骤 2：上传 ====== -->
        <el-card class="action-card">
          <template #header>
            <div class="card-header">
              <span class="card-step">2</span>
              <span class="card-title">上传 Excel 文件</span>
            </div>
          </template>

          <el-upload
            ref="uploadRef"
            class="upload-area"
            drag
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="uploadExtraData"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-progress="handleUploadProgress"
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
              <el-progress type="circle" :percentage="uploadPercent" :status="progressStatus" />
              <p class="progress-text">{{ uploadStatusText }}</p>
            </div>
          </el-upload>

          <div v-if="!canUpload" class="upload-warning">
            <el-icon><WarningFilled /></el-icon>
            <span>{{ uploadHintText }}</span>
          </div>
        </el-card>

        <!-- ====== 步骤 3：结果 ====== -->
        <el-card v-if="result" class="action-card result-card">
          <template #header>
            <div class="card-header">
              <span class="card-step">3</span>
              <span class="card-title">导入结果</span>
            </div>
          </template>

          <el-result
            :icon="result.status === 'success' ? 'success' : result.status === 'failed' ? 'error' : 'warning'"
            :title="resultTitle"
            :sub-title="resultSubTitle"
          >
            <template #extra>
              <div class="result-stats">
                <div class="stat-item">
                  <el-icon class="stat-icon success"><SuccessFilled /></el-icon>
                  <div class="stat-body">
                    <div class="stat-num">{{ result.successCount }}</div>
                    <div class="stat-label">成功</div>
                  </div>
                </div>
                <div class="stat-item">
                  <el-icon class="stat-icon danger"><CircleCloseFilled /></el-icon>
                  <div class="stat-body">
                    <div class="stat-num">{{ result.failedCount }}</div>
                    <div class="stat-label">失败</div>
                  </div>
                </div>
                <div class="stat-item">
                  <el-icon class="stat-icon info"><Document /></el-icon>
                  <div class="stat-body">
                    <div class="stat-num">{{ result.totalCount }}</div>
                    <div class="stat-label">总计</div>
                  </div>
                </div>
              </div>

              <div v-if="errorLogs.length > 0" class="error-log-block">
                <el-alert title="错误详情" type="error" :closable="false" />
                <div class="error-log-list">
                  <div v-for="(err, idx) in errorLogs.slice(0, 20)" :key="idx" class="error-item">
                    <el-tag type="danger" size="small">第{{ err.row_number }}行</el-tag>
                    <span v-if="err.advertiser_id" class="err-adv">{{ err.advertiser_id }}</span>
                    <span class="err-msg">{{ err.error_message }}</span>
                  </div>
                  <div v-if="errorLogs.length > 20" class="error-more">
                    还有 {{ errorLogs.length - 20 }} 条错误…（请下载导入记录查看完整日志）
                  </div>
                </div>
              </div>

              <div class="result-actions">
                <el-button type="primary" @click="handleViewHistory">查看导入记录</el-button>
                <el-button @click="handleReset">继续导入</el-button>
              </div>
            </template>
          </el-result>
        </el-card>

        <!-- 历史记录 -->
        <el-card v-if="showHistory" class="action-card">
          <template #header>
            <div class="card-header">
              <span class="card-step">📋</span>
              <span class="card-title">导入记录</span>
              <el-button size="small" style="margin-left: auto;" @click="showHistory = false">收起</el-button>
            </div>
          </template>
          <el-table :data="historyList" border stripe size="small">
            <el-table-column prop="id" label="ID" width="80" />
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
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabelMap[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="successCount" label="成功" width="80" />
            <el-table-column prop="failedCount" label="失败" width="80" />
            <el-table-column prop="totalCount" label="总计" width="80" />
            <el-table-column prop="createdBy" label="操作人" width="100" />
            <el-table-column prop="createdAt" label="导入时间" min-width="160" />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
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
  Setting,
  OfficeBuilding,
} from '@element-plus/icons-vue'
import AccountSelector from '@/components/TikTok/AccountSelector.vue'
import { uploadExcel, downloadTemplate, getImportList, deleteImport } from '@/api/tiktok/adImport'

const form = reactive({
  importMode: 'multiple',
  advertiserId: '',
})

// ====== 模板下载 ======
const downloadLoading = ref(false)
const collapseActive = ref([])

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

const uploadAction = computed(() => '/api/tiktok/excel-imports')
const uploadHeaders = computed(() => ({ Authorization: `Bearer ${localStorage.getItem('token') || ''}` }))
const uploadExtraData = computed(() => ({
  importMode: form.importMode,
  ...(form.importMode === 'single' && form.advertiserId ? { advertiserId: form.advertiserId } : {}),
}))
const canUpload = computed(() => {
  if (form.importMode === 'single') return !!form.advertiserId
  return true
})
const uploadHintText = computed(() =>
  form.importMode === 'single' ? '请先选择广告账户' : ''
)
const progressStatus = computed(() => {
  if (uploadStatus.value === 'exception') return 'exception'
  if (uploadPercent.value === 100) return 'success'
  return ''
})
const uploadStatusText = computed(() => {
  if (uploadPercent.value < 100) return `上传中… ${uploadPercent.value}%`
  return '处理中…'
})

const beforeUpload = (file) => {
  if (!canUpload.value) {
    ElMessage.warning(uploadHintText.value)
    return false
  }
  if (!file.name.toLowerCase().endsWith('.xlsx') && !file.name.toLowerCase().endsWith('.xls')) {
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

const handleUploadProgress = (event) => {
  uploadPercent.value = Math.floor(event.percent || 0)
}

const handleUploadSuccess = (res) => {
  uploading.value = false
  uploadPercent.value = 100
  uploadStatus.value = 'success'
  if (res?.code === 0) {
    Object.assign(result, res.data)
    ElMessage.success('文件上传成功，正在处理…')
    fetchHistory()
  } else {
    ElMessage.error(res?.message || '上传失败')
  }
}

const handleUploadError = (err) => {
  uploading.value = false
  uploadStatus.value = 'exception'
  console.error(err)
  ElMessage.error('上传失败，请重试')
}

// ====== 结果 ======
const result = ref(null)
const errorLogs = computed(() => {
  try {
    if (!result.value?.errorLogs) return []
    const parsed = JSON.parse(result.value.errorLogs)
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

// ====== 历史记录 ======
const showHistory = ref(false)
const historyList = ref([])

const typeLabelMap = { unified: '三层级统一', campaigns: '广告系列', adgroups: '广告组', ads: '广告' }
const statusLabelMap = { success: '成功', partial: '部分成功', failed: '失败', pending: '等待中', processing: '处理中' }

function statusTagType(status) {
  return { success: 'success', partial: 'warning', failed: 'danger', pending: 'info', processing: 'warning' }[status] || 'info'
}

async function fetchHistory() {
  try {
    const res = await getImportList({ page: 1, pageSize: 50 })
    historyList.value = res?.data?.list || []
    showHistory.value = true
  } catch (e) {
    console.error(e)
  }
}

function handleViewHistory() {
  fetchHistory()
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
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px;
}

.page-header {
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.page-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* 左右分栏 */
.main-grid {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

/* 左侧控制面板 */
.control-panel {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.panel-section {
  margin-bottom: 4px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 10px;
}

.mode-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 10px;
}
.mode-group :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 12px 14px;
  border-radius: 8px !important;
  text-align: left;
}
.mode-inner {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mode-icon {
  font-size: 24px;
  color: #409eff;
  flex-shrink: 0;
}
.mode-text {
  flex: 1;
}
.mode-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 1px;
}
.mode-desc {
  font-size: 11px;
  color: #909399;
}

.mode-tip {
  font-size: 12px;
}

.form-tip {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

/* 右侧操作区 */
.action-area {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.card-step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}
.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.template-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin: 14px 0;
}
.template-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #909399;
}

.field-collapse {
  margin-top: 10px;
}
.field-block h4 {
  margin: 12px 0 8px;
  font-size: 14px;
  font-weight: 600;
}
.field-spec {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.field-spec th {
  background: #f5f7fa;
  text-align: left;
  padding: 6px 8px;
  border: 1px solid #ebeef5;
  font-weight: 600;
}
.field-spec td {
  padding: 5px 8px;
  border: 1px solid #ebeef5;
  color: #606266;
}
.field-spec code {
  padding: 1px 5px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}
.tips-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
}

/* 上传 */
.upload-area :deep(.el-upload) {
  width: 100%;
}
.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.upload-content {
  text-align: center;
}
.upload-icon {
  font-size: 52px;
  color: #c0c4cc;
  margin-bottom: 12px;
}
.upload-main {
  font-size: 15px;
  color: #606266;
  margin: 0 0 6px;
}
.upload-main em {
  color: #409eff;
  font-style: normal;
}
.upload-sub {
  font-size: 13px;
  color: #909399;
  margin: 0;
}
.upload-progress {
  text-align: center;
}
.progress-text {
  margin-top: 14px;
  font-size: 13px;
  color: #606266;
}
.upload-warning {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 12px;
  padding: 10px;
  background: #fef0f0;
  border: 1px solid #fde2e2;
  border-radius: 4px;
  color: #f56c6c;
  font-size: 13px;
}

/* 结果 */
.result-card {
  border: 2px solid #67c23a;
}
.result-stats {
  display: flex;
  gap: 40px;
  justify-content: center;
  margin: 20px 0;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.stat-icon {
  font-size: 32px;
}
.stat-icon.success { color: #67c23a; }
.stat-icon.danger { color: #f56c6c; }
.stat-icon.info { color: #409eff; }
.stat-body { text-align: center; }
.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.error-log-block {
  max-width: 700px;
  margin: 16px auto 0;
}
.error-log-list {
  margin-top: 8px;
  max-height: 200px;
  overflow-y: auto;
}
.error-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12px;
  border-bottom: 1px solid #f5f5f5;
}
.err-adv {
  font-family: 'Courier New', monospace;
  color: #909399;
}
.err-msg {
  color: #f56c6c;
}
.error-more {
  padding: 8px 0;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
.result-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 16px;
}
</style>
