<template>
  <div class="recharge-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form
        :model="filterForm"
        class="filter-form recharge-filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleQuery"
      >
        <el-form-item label="用户ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.userId" placeholder="用户ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.promotionId" placeholder="推广ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="订单号" label-width="60px">
          <div class="filter-item-m">
            <el-input v-model="filterForm.orderId" placeholder="订单号" clearable />
          </div>
        </el-form-item>
        <el-form-item label="外订单号" label-width="70px">
          <div class="filter-item-m">
            <el-input v-model="filterForm.externalOrderId" placeholder="外订单号" clearable />
          </div>
        </el-form-item>
        <el-form-item label="媒体" label-width="50px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.platform" placeholder="全部" clearable>
              <el-option label="TikTok" value="tiktok" />
              <el-option label="Meta" value="meta" />
              <el-option label="Facebook" value="facebook" />
              <el-option label="Google" value="google" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="账户ID" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.accountId" placeholder="广告账户" clearable />
          </div>
        </el-form-item>
        <el-form-item v-if="countries.length > 0" label="国家" label-width="50px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.country" placeholder="全部" clearable>
              <el-option
                v-for="item in countryFilterOptions"
                :key="item.value === '' ? '_all' : item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="注册时间" label-width="70px">
          <div class="filter-item-daterange">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
          </div>
        </el-form-item>
        <el-form-item v-show="activeTab === 'all'" label="支付结果" label-width="70px">
          <div class="filter-item-select">
            <el-select v-model="filterForm.paymentStatus" placeholder="全部" clearable>
              <el-option label="成功" value="paid" />
              <el-option label="待支付" value="pending" />
              <el-option label="失败" value="failed" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="handleQuery">
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
            <el-button type="success" :loading="exporting" @click="handleExportExcel">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-tabs v-model="activeTab" class="recharge-tabs" @tab-change="handleTabChange">
        <el-tab-pane :label="`全部 (${stats.total})`" name="all" />
        <el-tab-pane :label="`待支付订单 (${stats.pending})`" name="pending" />
      </el-tabs>

      <el-table
        v-loading="loading"
        element-loading-text="加载中..."
        :data="tableData"
        border
        stripe
        class="recharge-table"
        height="calc(100vh - 280px)"
      >
        <template #empty>
          <el-empty description="暂无充值记录" />
        </template>
        <!-- §13 #058 任务 3 表头顺序：用户ID→剧名→金额→支付结果→账户ID/名称→首充→是否回传→推广ID→当地时间→操作；其余列置后 -->
        <el-table-column prop="user_id" label="用户ID" width="200" show-overflow-tooltip />
        <el-table-column prop="drama_name" label="剧名" width="180" show-overflow-tooltip />
        <el-table-column prop="amount" label="充值金额" width="100" align="center" />
        <el-table-column label="支付结果" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="payTagType(row.payment_status)">
              {{ payStatusText(row.payment_status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ad_account_id" label="账户ID" width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.ad_account_id || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="ad_account_name" label="账户名称" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.ad_account_name || '—' }}
          </template>
        </el-table-column>
        <el-table-column label="是否首充" width="100" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.is_first_recharge" color="#67c23a" :size="20"><CircleCheck /></el-icon>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="是否回传" width="90" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.callback_sent" type="success" size="small">已回传</el-tag>
            <span v-else class="sub-muted">否</span>
          </template>
        </el-table-column>
        <el-table-column prop="promotion_id" label="推广ID" width="180" show-overflow-tooltip />
        <el-table-column
          prop="local_register_time"
          label="当地注册时间"
          width="180"
          align="center"
        />
        <el-table-column prop="local_order_time" label="当地订单时间" width="180" align="center" />
        <el-table-column prop="order_id" label="订单ID" width="120" align="center" />
        <el-table-column prop="platform" label="媒体" width="100" align="center" show-overflow-tooltip />
        <el-table-column prop="new_user_id" label="新用户ID" width="200" show-overflow-tooltip />
        <el-table-column label="是否新用户" width="120" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.is_new_user" color="#67c23a" :size="20"><CircleCheck /></el-icon>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">查看详情</el-button>
          </template>
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

    <el-dialog v-model="detailVisible" title="充值详情" width="600px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单ID">{{ currentOrder.order_id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentOrder.user_id }}</el-descriptions-item>
        <el-descriptions-item label="媒体">{{ currentOrder.platform || '—' }}</el-descriptions-item>
        <el-descriptions-item label="广告账户">
          {{ currentOrder.ad_account_name || '—' }}
          <template v-if="currentOrder.ad_account_id"><br >{{ currentOrder.ad_account_id }}</template>
        </el-descriptions-item>
        <el-descriptions-item label="剧名">{{ currentOrder.drama_name }}</el-descriptions-item>
        <el-descriptions-item label="充值金额">{{ currentOrder.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付结果">
          <el-tag :type="payTagType(currentOrder.payment_status)">
            {{ payStatusText(currentOrder.payment_status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否回传">{{ currentOrder.callback_sent ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="推广ID">{{ currentOrder.promotion_id }}</el-descriptions-item>
        <el-descriptions-item label="新用户ID">{{ currentOrder.new_user_id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="是否首充">{{ currentOrder.is_first_recharge ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="是否新用户">{{ currentOrder.is_new_user ? '是' : '否' }}</el-descriptions-item>
        <el-descriptions-item label="当地注册时间">{{ currentOrder.local_register_time || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当地订单时间">{{ currentOrder.local_order_time || '-' }}</el-descriptions-item>
        <el-descriptions-item label="国家">{{ currentOrder.country || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, CircleCheck, Download } from '@element-plus/icons-vue'
import request from '../api/request'
import { exportJsonToXlsx } from '../utils/excelExport'
import { useCountries } from '@/composables/useCountries'

const { countries, countryFilterOptions } = useCountries()

const filterForm = ref({
  userId: '',
  promotionId: '',
  orderId: '',
  externalOrderId: '',
  dateRange: [],
  country: '',
  platform: '',
  accountId: '',
  paymentStatus: '',
})

const activeTab = ref('all')

const stats = ref({
  total: 0,
  pending: 0,
})

const tableData = ref([])
const loading = ref(false)
const exporting = ref(false)

const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
})

const detailVisible = ref(false)
const currentOrder = ref({})

const loadStats = async () => {
  try {
    const p = buildRechargeParams(1, 1)
    delete p.page
    delete p.pageSize
    const res = await request.get('/recharge/stats', { params: p })
    if (res.code === 0 && res.data) {
      const d = res.data
      stats.value = {
        total: d.total_count ?? d.total ?? 0,
        pending: d.pending_count ?? d.pending ?? 0,
      }
    }
  } catch (e) {
    console.error('加载统计失败:', e)
  }
}

const handleTabChange = () => {
  pagination.value.page = 1
  handleQuery()
}

const handleQuery = async () => {
  loading.value = true
  try {
    const params = buildRechargeParams(pagination.value.page, pagination.value.pageSize)
    const res = await request.get('/recharge', { params })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.value.total = res.data?.total ?? 0
    }
    await loadStats()
  } catch (error) {
    ElMessage.error(`查询失败：${error.message || ''}`)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = {
    userId: '',
    promotionId: '',
    orderId: '',
    externalOrderId: '',
    dateRange: [],
    country: '',
    platform: '',
    accountId: '',
    paymentStatus: '',
  }
  activeTab.value = 'all'
  pagination.value.page = 1
  handleQuery()
}

const handleViewDetail = (row) => {
  currentOrder.value = { ...row }
  detailVisible.value = true
}

function payStatusText(ps) {
  if (ps === 'paid') return '成功'
  if (ps === 'pending') return '待支付'
  if (ps === 'failed') return '失败'
  return '未知'
}

function payTagType(ps) {
  if (ps === 'paid') return 'success'
  if (ps === 'pending') return 'warning'
  if (ps === 'failed') return 'danger'
  return 'info'
}

function buildRechargeParams(page, pageSize) {
  const params = { page, pageSize }
  if (filterForm.value.userId) params.user_id = filterForm.value.userId
  if (filterForm.value.promotionId) params.promotion_id = filterForm.value.promotionId
  if (filterForm.value.orderId) params.order_id = filterForm.value.orderId
  if (filterForm.value.externalOrderId) params.external_order_id = filterForm.value.externalOrderId
  if (filterForm.value.country) params.country = filterForm.value.country
  if (filterForm.value.platform) params.platform = filterForm.value.platform
  if (filterForm.value.accountId) params.account_id = filterForm.value.accountId
  if (filterForm.value.dateRange && filterForm.value.dateRange.length === 2) {
    params.start_date = filterForm.value.dateRange[0]
    params.end_date = filterForm.value.dateRange[1]
  }
  if (activeTab.value === 'pending') params.status = 'pending'
  else if (filterForm.value.paymentStatus) params.status = filterForm.value.paymentStatus
  return params
}

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const pageSize = 100
    let page = 1
    let total = Infinity
    const all = []
    while (all.length < total) {
      const res = await request.get('/recharge', { params: buildRechargeParams(page, pageSize) })
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
      ElMessage.warning('当前筛选条件下无数据可导出')
      return
    }
    const rows = all.map((row) => {
      const paid = row.payment_status === 'paid'
      const amt = row.amount != null ? Number(row.amount) : 0
      const actual =
        row.actual_amount != null ? Number(row.actual_amount) : paid ? amt : 0
      return {
        订单号: row.order_no || row.order_id || '',
        用户ID: row.user_id ?? '',
        媒体: row.platform ?? '',
        广告账户: row.ad_account_id ? `${row.ad_account_name || ''} ${row.ad_account_id}`.trim() : '',
        充值金额: amt,
        实付金额: actual,
        支付状态: payStatusText(row.payment_status),
        是否回传: row.callback_sent ? '是' : '否',
        创建时间: row.created_at || row.local_order_time || '',
      }
    })
    exportJsonToXlsx(rows, '充值记录', `充值记录_${new Date().toISOString().slice(0, 10)}`)
    ElMessage.success(`已导出 ${rows.length} 条`)
  } catch (e) {
    ElMessage.error(`导出失败：${e?.message || ''}`)
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  handleQuery()
})
</script>

<style scoped>
.recharge-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}
.recharge-tabs {
  margin-bottom: 0;
}

.recharge-table {
  margin-top: 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.sub-muted {
  color: #909399;
  font-size: 12px;
}

</style>
