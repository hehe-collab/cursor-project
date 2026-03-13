<template>
  <div class="callback-monitor-page">
    <el-card class="page-card">
      <template #header><span>回传监控</span></template>

      <!-- KPI 卡片 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6">
          <div class="kpi-card kpi-total">
            <div class="kpi-value">{{ stats.total }}</div>
            <div class="kpi-label">总数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-success">
            <div class="kpi-value">{{ stats.success }}</div>
            <div class="kpi-label">成功</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-failure">
            <div class="kpi-value">{{ stats.failure }}</div>
            <div class="kpi-label">失败</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="kpi-card kpi-pending">
            <div class="kpi-value">{{ stats.pending }}</div>
            <div class="kpi-label">待处理</div>
          </div>
        </el-col>
      </el-row>

      <el-form :inline="true" class="filter-form">
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width:120px">
            <el-option label="全部" value="" />
            <el-option label="成功" value="成功" />
            <el-option label="失败" value="失败" />
            <el-option label="待处理" value="待处理" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="query.eventType" placeholder="全部" clearable style="width:120px">
            <el-option label="全部" value="" />
            <el-option label="完成支付" value="完成支付" />
            <el-option label="下单" value="下单" />
          </el-select>
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="query.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">
            <el-icon><Search /></el-icon>
            查询
          </el-button>
          <el-button @click="onReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无回传记录" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="order_no" label="订单号" min-width="140">
          <template #default="{ row }">{{ row.order_no || row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column label="事件类型" width="110">
          <template #default="{ row }">
            <el-tag :type="eventTagType(row.event_type || row.event)" size="small">
              {{ row.event_type || row.event || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pixel_id" label="Pixel ID" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.pixel_id || row.pixelId || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ row.status || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="error_message" label="错误信息" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.error_message || row.errorMessage || '-' }}</template>
        </el-table-column>
        <el-table-column prop="retry_count" label="重试次数" width="90">
          <template #default="{ row }">{{ row.retry_count ?? row.retryCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>
        <el-table-column label="发送时间" width="180">
          <template #default="{ row }">{{ formatTime(row.send_time || row.sendTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import request from '../api/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const stats = reactive({ total: 0, success: 0, failure: 0, pending: 0 })
const query = reactive({ page: 1, pageSize: 10, status: '', eventType: '', orderNo: '', dateRange: null })

function eventTagType(ev) {
  if (!ev) return 'info'
  if (ev.includes('支付') || ev.includes('完成支付')) return 'success'
  if (ev.includes('下单')) return 'primary'
  return 'info'
}

function statusTagType(s) {
  if (!s) return 'info'
  if (s === '成功') return 'success'
  if (s === '失败') return 'danger'
  if (s === '待处理') return 'info'
  return 'info'
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.status) params.status = query.status
    if (query.eventType) params.eventType = query.eventType
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.dateRange && query.dateRange.length === 2) {
      params.dateStart = query.dateRange[0]
      params.dateEnd = query.dateRange[1]
    }
    const res = await request.get('/callback/logs', { params }).catch(() => ({ data: { list: [], total: 0, stats: {} } }))
    list.value = res.data?.list || []
    total.value = res.data?.total ?? 0
    Object.assign(stats, res.data?.stats || { total: 0, success: 0, failure: 0, pending: 0 })
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.status = ''
  query.eventType = ''
  query.orderNo = ''
  query.dateRange = null
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.kpi-row { margin-bottom: 20px; }
.kpi-card {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
}
.kpi-value { font-size: 28px; font-weight: 600; line-height: 1.2; }
.kpi-total .kpi-value { color: #409eff; }
.kpi-success .kpi-value { color: #67c23a; }
.kpi-failure .kpi-value { color: #f56c6c; }
.kpi-pending .kpi-value { color: #909399; }
.kpi-label { font-size: 14px; color: #909399; margin-top: 4px; }
.filter-form { margin-bottom: 16px; }
</style>
