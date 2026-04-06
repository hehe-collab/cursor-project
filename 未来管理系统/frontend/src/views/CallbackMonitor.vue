<template>
  <div class="callback-monitor-container page-list-layout">
    <div class="kpi-cards">
      <el-card shadow="hover" class="kpi-card">
        <div class="kpi-value">{{ kpiData.total }}</div>
        <div class="kpi-label">总数</div>
      </el-card>

      <el-card shadow="hover" class="kpi-card success">
        <div class="kpi-value">{{ kpiData.success }}</div>
        <div class="kpi-label">成功</div>
      </el-card>

      <el-card shadow="hover" class="kpi-card error">
        <div class="kpi-value">{{ kpiData.failed }}</div>
        <div class="kpi-label">失败</div>
      </el-card>

      <el-card shadow="hover" class="kpi-card warning">
        <div class="kpi-value">{{ kpiData.pending }}</div>
        <div class="kpi-label">待处理</div>
      </el-card>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :model="filterForm" class="filter-form callback-monitor-filter-form" inline size="small" label-position="left">
        <el-form-item label="状态" label-width="50px">
          <div class="filter-item-s">
            <el-select v-model="filterForm.status" placeholder="全部" clearable>
              <el-option label="全部" value="" />
              <el-option label="成功" value="success" />
              <el-option label="失败" value="failed" />
              <el-option label="待处理" value="pending" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="事件类型" label-width="70px">
          <div class="filter-item-s">
            <el-select v-model="filterForm.eventType" placeholder="全部" clearable>
              <el-option label="全部" value="" />
              <el-option label="下单" value="下单" />
              <el-option label="完成支付" value="完成支付" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="订单号" label-width="60px">
          <div class="filter-item-m">
            <el-input v-model="filterForm.orderId" placeholder="订单号" clearable />
          </div>
        </el-form-item>
        <el-form-item label="时间范围" label-width="70px">
          <div class="filter-item-daterange">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
            />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table v-loading="loading" :data="tableData" border stripe height="calc(100vh - 360px)">
        <template #empty>
          <el-empty description="暂无回传记录" />
        </template>
        <el-table-column prop="id" label="ID" width="80" align="center" />

        <el-table-column prop="order_id" label="订单号" width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.order_id || row.order_no || '-' }}</template>
        </el-table-column>

        <el-table-column prop="event_type" label="事件类型" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.event_type === '完成支付' ? 'success' : 'info'">
              {{ row.event_type || row.event || '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="pixel_id" label="Pixel ID" width="200" show-overflow-tooltip />

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="error_message" label="错误信息" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.error_message || '-' }}</template>
        </el-table-column>

        <el-table-column prop="retry_count" label="重试次数" width="100" align="center" />

        <el-table-column prop="created_at" label="创建时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.created_at) }}</template>
        </el-table-column>

        <el-table-column label="发送时间" width="180" align="center">
          <template #default="{ row }">{{ formatTime(row.sent_at || row.send_time) }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import request from '../api/request'

const kpiData = ref({
  total: 0,
  success: 0,
  failed: 0,
  pending: 0,
})

const filterForm = ref({
  status: '',
  eventType: '',
  orderId: '',
  dateRange: [],
})

const tableData = ref([])
const loading = ref(false)

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
})

function statusTagType(s) {
  if (s === '成功' || s === 'success') return 'success'
  if (s === '失败' || s === 'failed') return 'danger'
  if (s === '待处理' || s === 'pending') return 'warning'
  return 'info'
}

function statusText(s) {
  if (s === '成功' || s === 'success') return '成功'
  if (s === '失败' || s === 'failed') return '失败'
  if (s === '待处理' || s === 'pending') return '待处理'
  return s || '-'
}

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

const loadKpiData = async () => {
  try {
    const res = await request.get('/callback/stats')
    if (res.code === 0 && res.data) {
      kpiData.value = {
        total: res.data.total ?? 0,
        success: res.data.success ?? 0,
        failed: res.data.failed ?? 0,
        pending: res.data.pending ?? 0,
      }
    }
  } catch (e) {
    console.error('加载 KPI 失败:', e)
  }
}

const handleQuery = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
    }
    if (filterForm.value.status) params.status = filterForm.value.status
    if (filterForm.value.eventType) params.event_type = filterForm.value.eventType
    if (filterForm.value.orderId) params.order_id = filterForm.value.orderId
    if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
      params.start_date = filterForm.value.dateRange[0]
      params.end_date = filterForm.value.dateRange[1]
    }
    const res = await request.get('/callback/logs', { params })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.value.total = res.data?.total ?? 0
    }
  } catch (error) {
    ElMessage.error(`查询失败：${error.message || ''}`)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = {
    status: '',
    eventType: '',
    orderId: '',
    dateRange: [],
  }
  pagination.value.page = 1
  handleQuery()
}

onMounted(() => {
  loadKpiData()
  handleQuery()
})
</script>

<style scoped>
.callback-monitor-container {
  padding: 20px;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.kpi-card {
  text-align: center;
  padding: 20px;
}

.kpi-value {
  font-size: 36px;
  font-weight: bold;
  color: #409eff;
  margin-bottom: 10px;
}

.kpi-card.success .kpi-value {
  color: #67c23a;
}

.kpi-card.error .kpi-value {
  color: #f56c6c;
}

.kpi-card.warning .kpi-value {
  color: #e6a23c;
}

.kpi-label {
  font-size: 14px;
  color: #909399;
}

.filter-card {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
