<template>
  <div class="admin-log page-container">
    <el-card shadow="never">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-select v-model="filters.operationType" placeholder="操作类型" clearable size="small" style="width:130px" @change="loadLogs">
            <el-option v-for="t in operationTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <el-select v-model="filters.targetType" placeholder="对象类型" clearable size="small" style="width:130px" @change="loadLogs">
            <el-option v-for="t in targetTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
            style="width:240px"
            value-format="YYYY-MM-DD"
            @change="onDateChange"
          />
        </div>
        <div class="filter-right">
          <el-button size="small" @click="resetFilters">重置</el-button>
          <el-button size="small" type="primary" @click="toggleStats">
            {{ showStats ? '收起' : '统计' }}
          </el-button>
        </div>
      </div>

      <!-- 统计图 -->
      <div v-if="showStats" class="stats-bar">
        <el-row :gutter="12">
          <el-col v-for="s in statsData" :key="s.type" :span="6">
            <div class="stat-card">
              <div class="stat-label">{{ s.type }}</div>
              <div class="stat-value">{{ s.count }}</div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 数据表格 -->
      <el-table :data="logs" size="small" stripe v-loading="loading" style="margin-top:12px">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="adminUsername" label="操作人" width="120" />
        <el-table-column label="操作类型" width="120">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.operationType)" size="small">{{ row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="targetType" label="对象类型" width="110" />
        <el-table-column prop="operationDesc" label="操作描述" min-width="180" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="130" />
        <el-table-column prop="executionTime" label="耗时(ms)" width="90" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.responseStatus === 200" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="160" />
        <el-table-column label="详情" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-descriptions v-if="currentLog" :column="2" border size="small">
        <el-descriptions-item label="操作人">{{ currentLog.adminUsername }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <el-tag :type="typeTag(currentLog.operationType)" size="small">{{ currentLog.operationType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="目标类型">{{ currentLog.targetType }}</el-descriptions-item>
        <el-descriptions-item label="目标ID">{{ currentLog.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentLog.operationDesc }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.requestMethod }}</el-descriptions-item>
        <el-descriptions-item label="响应状态">
          <el-tag v-if="currentLog.responseStatus === 200" type="success" size="small">成功</el-tag>
          <el-tag v-else type="danger" size="small">{{ currentLog.responseStatus }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ currentLog.executionTime }} ms</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ipAddress }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">
          <code style="font-size:12px">{{ currentLog.requestUrl }}</code>
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">
          <span style="font-size:11px;color:#909399;word-break:break-all">{{ currentLog.userAgent }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="params-pre">{{ currentLog.requestParams }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="currentLog.errorMsg" label="错误信息" :span="2">
          <span style="color:#f56c6c">{{ currentLog.errorMsg }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ currentLog.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminLogs, getAdminLogStats } from '@/api/adminLog'

const logs = ref([])
const statsData = ref([])
const loading = ref(false)
const total = ref(0)
const showStats = ref(false)
const detailVisible = ref(false)
const currentLog = ref(null)
const dateRange = ref([])

const operationTypes = ['CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT', 'ASSIGN_PERM', 'RESET_PWD']
const targetTypes = ['admin', 'role', 'drama', 'category', 'user', 'recharge', 'promotion', 'account', 'tiktok']

const query = reactive({ page: 1, size: 20 })
const filters = reactive({ operationType: '', targetType: '', startDate: '', endDate: '' })

function typeTag(type) {
  const map = { CREATE: 'success', UPDATE: 'warning', DELETE: 'danger', LOGIN: 'info', LOGOUT: 'info', ASSIGN_PERM: 'primary', RESET_PWD: 'warning' }
  return map[type] || ''
}

function onDateChange(val) {
  filters.startDate = val ? val[0] : ''
  filters.endDate = val ? val[1] : ''
  loadLogs()
}

function resetFilters() {
  Object.assign(filters, { operationType: '', targetType: '', startDate: '', endDate: '' })
  dateRange.value = []
  query.page = 1
  loadLogs()
}

async function loadLogs() {
  loading.value = true
  try {
    const res = await getAdminLogs({ ...filters, page: query.page, size: query.size })
    const d = res.data || {}
    logs.value = d.list || []
    total.value = d.total || 0
  } catch (e) {
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getAdminLogStats({ startDate: filters.startDate, endDate: filters.endDate })
    statsData.value = (res.data || []).map(s => ({ type: s.operationType, count: s.count }))
  } catch (e) { /* ignore */ }
}

function toggleStats() {
  showStats.value = !showStats.value
  if (showStats.value) {
    loadStats()
  }
}

function viewDetail(row) {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(() => { loadLogs() })
</script>

<style scoped>
.admin-log .filter-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.admin-log .filter-left { display: flex; gap: 8px; flex-wrap: wrap; }
.admin-log .filter-right { display: flex; gap: 8px; }
.stats-bar { margin-bottom: 12px; }
.stat-card { background: #f5f7fa; border-radius: 6px; padding: 12px 16px; text-align: center; }
.stat-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.stat-value { font-size: 22px; font-weight: 600; color: #409EFF; }
.pagination-container { display: flex; justify-content: flex-end; margin-top: 12px; }
.params-pre { background: #f5f7fa; padding: 8px; border-radius: 4px; font-size: 11px; max-height: 200px; overflow: auto; white-space: pre-wrap; word-break: break-all; margin: 0; }
</style>
