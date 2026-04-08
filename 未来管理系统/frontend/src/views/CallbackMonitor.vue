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
            <el-select
              v-model="filterForm.status"
              placeholder="全部"
              clearable
              @change="handleFilterImmediate"
            >
              <el-option label="全部" value="" />
              <el-option label="成功" value="success" />
              <el-option label="失败" value="failed" />
              <el-option label="待处理" value="pending" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="事件类型" label-width="70px">
          <div class="filter-item-s">
            <el-select
              v-model="filterForm.eventType"
              placeholder="全部"
              clearable
              @change="handleFilterImmediate"
            >
              <el-option label="全部" value="" />
              <el-option label="下单" value="下单" />
              <el-option label="完成支付" value="完成支付" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="订单号" label-width="60px">
          <div class="filter-item-m">
            <el-input
              v-model="filterForm.orderId"
              placeholder="订单号"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
          </div>
        </el-form-item>
        <el-form-item label="用户ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input
              v-model="filterForm.userId"
              placeholder="用户ID"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
          </div>
        </el-form-item>
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input
              v-model="filterForm.promotionId"
              placeholder="推广ID"
              clearable
              @input="handleSearchDebounced"
              @clear="handleReset"
            />
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
              @change="handleFilterImmediate"
            />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleSearchClick">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
            <el-button
              v-if="tableData.length > 50"
              size="small"
              @click="useVirtualScroll = !useVirtualScroll"
            >
              {{ useVirtualScroll ? '标准表格' : '虚拟滚动' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="table-card-header-row">
          <span>
            回传记录
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
          :data="tableData"
          border
          stripe
          height="100%"
          size="small"
        >
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

      <VirtualTable
        v-else
        :data="tableData"
        :columns="virtualColumns"
        :item-size="44"
        key-field="id"
      >
        <template #orderCol="{ row }">
          {{ row.order_id || row.order_no || '-' }}
        </template>
        <template #eventType="{ row }">
          <el-tag :type="row.event_type === '完成支付' ? 'success' : 'info'">
            {{ row.event_type || row.event || '-' }}
          </el-tag>
        </template>
        <template #statusTag="{ row }">
          <el-tag :type="statusTagType(row.status)">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
        <template #errMsg="{ row }">
          {{ row.error_message || '-' }}
        </template>
        <template #createdCol="{ row }">{{ formatTime(row.created_at) }}</template>
        <template #sentCol="{ row }">{{ formatTime(row.sent_at || row.send_time) }}</template>
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
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import request from '../api/request'
import VirtualTable from '@/components/VirtualTable.vue'
import Loading from '@/components/Loading.vue'
import { debounce } from '@/utils/performance'

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
  userId: '',
  promotionId: '',
  dateRange: [],
})

const tableData = ref([])
const loading = ref(false)
const searching = ref(false)

/** #093：虚拟滚动（>100 行自动；>50 行可切换） */
const useVirtualScroll = ref(false)
const virtualColumns = [
  { prop: 'id', label: 'ID', gridWidth: '72px' },
  { prop: '_order', label: '订单号', slot: 'orderCol', gridWidth: 'minmax(120px, 1fr)' },
  { prop: '_event', label: '事件类型', slot: 'eventType', gridWidth: 'minmax(100px, 0.9fr)' },
  { prop: 'pixel_id', label: 'Pixel ID', gridWidth: 'minmax(120px, 1fr)' },
  { prop: '_status', label: '状态', slot: 'statusTag', gridWidth: '88px' },
  { prop: '_err', label: '错误信息', slot: 'errMsg', gridWidth: 'minmax(120px, 1.2fr)' },
  { prop: 'retry_count', label: '重试', gridWidth: '64px' },
  { prop: '_created', label: '创建时间', slot: 'createdCol', gridWidth: 'minmax(130px, 0.95fr)' },
  { prop: '_sent', label: '发送时间', slot: 'sentCol', gridWidth: 'minmax(130px, 0.95fr)' },
]

watch(
  () => tableData.value.length,
  (n) => {
    if (n > 100) useVirtualScroll.value = true
  },
)

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
  searching.value = true
  loading.value = true
  try {
    const params = {
      page: pagination.value.page,
      pageSize: pagination.value.pageSize,
    }
    if (filterForm.value.status) params.status = filterForm.value.status
    if (filterForm.value.eventType) params.event_type = filterForm.value.eventType
    if (filterForm.value.orderId) params.order_id = filterForm.value.orderId
    if (filterForm.value.userId) params.user_id = filterForm.value.userId
    if (filterForm.value.promotionId) params.promotion_id = filterForm.value.promotionId
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
    status: '',
    eventType: '',
    orderId: '',
    userId: '',
    promotionId: '',
    dateRange: [],
  }
  pagination.value.page = 1
  void handleQuery()
}

onMounted(() => {
  loadKpiData()
  handleQuery()
})
</script>

<style scoped>
.callback-monitor-container {
  padding: 0;
}

.kpi-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--section-gap);
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
  margin-bottom: 0;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
}

.table-wrapper--virtual {
  position: relative;
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
