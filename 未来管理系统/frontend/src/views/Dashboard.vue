<template>
  <div class="dashboard-container">
    <el-card class="filter-card" shadow="never">
      <el-form
        :model="filterForm"
        class="filter-form"
        inline
        size="small"
        label-position="left"
        @submit.prevent="handleFilter"
      >
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.promotion_id" placeholder="推广ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="推广名称" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.promotion_name" placeholder="推广名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label="剧ID" label-width="50px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.drama_id" placeholder="剧ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="剧名称" label-width="50px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.drama_name" placeholder="剧名称" clearable />
          </div>
        </el-form-item>
        <el-form-item label="账户" label-width="50px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.account" placeholder="账户" clearable />
          </div>
        </el-form-item>
        <el-form-item label="投放媒体" label-width="70px">
          <div class="filter-item-m">
            <el-select
              v-model="filterForm.media"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="媒体"
            >
              <el-option label="TikTok" value="tiktok" />
              <el-option label="Facebook" value="facebook" />
              <el-option label="Google" value="google" />
              <el-option label="Snapchat" value="snapchat" />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item v-if="countries.length > 0" label="国家" label-width="50px">
          <div class="filter-item-m">
            <el-select
              v-model="filterForm.country"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="国家"
            >
              <el-option
                v-for="opt in countryMultiOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </div>
        </el-form-item>
        <el-form-item label="日期范围" label-width="60px">
          <div class="filter-item-daterange">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始"
              end-placeholder="结束"
              value-format="YYYY-MM-DD"
              @change="handleDateChange"
            />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" :loading="filterSubmitting" @click="handleFilter">
              <el-icon><Search /></el-icon>
              查询
            </el-button>
            <el-button :disabled="filterSubmitting" @click="handleReset">
              <el-icon><RefreshLeft /></el-icon>
              重置
            </el-button>
            <el-button type="success" @click="handleExport">
              <el-icon><Download /></el-icon>
              导出
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="tabs-card" shadow="never">
      <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">
        <el-tab-pane name="promotion">
          <template #label>
            <span class="tab-label">
              <el-icon><DataLine /></el-icon>
              推广明细
            </span>
          </template>

          <el-table
            :data="promotionData"
            v-loading="promotionLoading"
            element-loading-text="加载中..."
            border
            stripe
            height="calc(100vh - 288px)"
            style="width: 100%"
            :default-sort="{ prop: 'date', order: 'descending' }"
            :header-cell-style="tableHeaderStyle"
          >
            <el-table-column type="index" label="序号" width="58" fixed="left" align="center" />
            <el-table-column prop="date" label="日期" width="118" sortable fixed="left" align="center">
              <template #default="{ row }">
                <el-tag type="info" size="small">{{ row.date }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="promotion_name"
              label="推广名称"
              min-width="200"
              show-overflow-tooltip
              fixed="left"
            />
            <el-table-column prop="drama_id" label="剧ID" width="100" align="center">
              <template #default="{ row }">
                <el-tag type="primary" size="small">{{ row.drama_id }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="drama_name"
              label="剧名"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="account" label="账户" width="100" align="center" />
            <el-table-column prop="country" label="国家" width="88" align="center">
              <template #default="{ row }">
                <el-tag size="small">{{ formatAccountCountryLabel(row.country) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="promotion_id" label="推广ID" width="126" align="center" />
            <el-table-column prop="cost" label="消耗" width="108" sortable align="right">
              <template #default="{ row }">
                <span class="num-cost">¥{{ formatMoney(row.cost) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="cpm" label="千次曝光" width="100" sortable align="right">
              <template #default="{ row }">
                <span class="num-muted">{{ formatMoney(row.cpm) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="clicks" label="点击" width="88" sortable align="right">
              <template #default="{ row }">
                <span class="num-blue">{{ row.clicks ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="registrations" label="注册" width="88" sortable align="right">
              <template #default="{ row }">
                <span class="num-warn">{{ row.registrations ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="recharge_users" label="充值人数" width="100" sortable align="right">
              <template #default="{ row }">
                <span class="num-ok-strong">{{ row.recharge_users ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="recharge_amount" label="充值金额" width="118" sortable align="right">
              <template #default="{ row }">
                <span class="num-ok-strong">¥{{ formatMoney(row.recharge_amount) }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="roi" label="ROI" width="100" sortable align="center">
              <template #default="{ row }">
                <el-tag :type="Number(row.roi) >= 1 ? 'success' : 'danger'" size="small" effect="dark">
                  {{ formatMoney(row.roi) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="user_count" label="用户数" width="90" sortable align="right">
              <template #default="{ row }">
                <span class="num-plain">{{ row.user_count ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="order_count" label="订单数" width="90" sortable align="right">
              <template #default="{ row }">
                <span class="num-plain">{{ row.order_count ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="paid_users" label="有充数" width="90" sortable align="right">
              <template #default="{ row }">
                <span class="num-ok">{{ row.paid_users ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="profit" label="利润" width="108" sortable align="right">
              <template #default="{ row }">
                <span
                  :class="Number(row.profit) >= 0 ? 'profit-pos' : 'profit-neg'"
                >
                  {{ Number(row.profit) >= 0 ? '+' : '' }}¥{{ formatMoney(row.profit) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="avg_cost_per_user" label="人均消耗" width="100" sortable align="right">
              <template #default="{ row }">
                <span class="num-muted">¥{{ formatMoney(row.avg_cost_per_user) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="viewDetail(row)">
                  <el-icon class="el-icon--left"><View /></el-icon>
                  详情
                </el-button>
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
              background
              @size-change="handlePageSizeChange"
              @current-change="handlePageChange"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane name="overview" lazy>
          <template #label>
            <span class="tab-label">
              <el-icon><PieChart /></el-icon>
              数据概览
            </span>
          </template>

          <div v-loading="statsLoading" element-loading-text="加载中..." class="overview-charts">
            <el-row :gutter="20">
              <el-col :span="8">
                <el-card shadow="hover" class="chart-card">
                  <template #header>
                    <div class="chart-header">
                      <el-icon><PieChart /></el-icon>
                      <span>充值状态分布</span>
                    </div>
                  </template>
                  <div ref="rechargeStatusChartRef" class="chart-inner" />
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" class="chart-card">
                  <template #header>
                    <div class="chart-header">
                      <el-icon><Histogram /></el-icon>
                      <span>每日新增用户 vs 充值笔数</span>
                    </div>
                  </template>
                  <div ref="dailyCompareChartRef" class="chart-inner" />
                </el-card>
              </el-col>
              <el-col :span="8">
                <el-card shadow="hover" class="chart-card">
                  <template #header>
                    <div class="chart-header">
                      <el-icon><TrendCharts /></el-icon>
                      <span>趋势分析</span>
                    </div>
                  </template>
                  <div ref="trendChartRef" class="chart-inner chart-inner--trend" />
                </el-card>
              </el-col>
            </el-row>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <p class="hint hint--footer">
      推广明细为 Java 演示数据（<code>drama_id</code> 如 <code>D10000</code>）；数据概览与
      <code>/api/dashboard/stats</code> 一致。
    </p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  Search,
  RefreshLeft,
  Download,
  DataLine,
  PieChart,
  Histogram,
  TrendCharts,
  View,
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useCountries } from '@/composables/useCountries'

const { countries, countryMultiOptions, formatAccountCountryLabel } = useCountries()

const tableHeaderStyle = {
  background: '#f5f7fa',
  color: '#606266',
  fontWeight: 'bold',
  fontSize: '14px',
}

const activeTab = ref('promotion')
const dateRange = ref([])
const statsLoading = ref(false)
const promotionLoading = ref(false)
const filterSubmitting = ref(false)

const filterForm = ref({
  promotion_id: '',
  promotion_name: '',
  drama_id: '',
  drama_name: '',
  account: '',
  media: [],
  country: [],
})

const statsData = ref({
  total_users: 0,
  total_recharge: 0,
  total_amount: 0,
  today_users: 0,
  recharge_status_dist: { success: 0, pending: 0, failed: 0 },
  chart_data: { dates: [], users: [], recharge: [], amount: [] },
})

const promotionData = ref([])
const pagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
})

const rechargeStatusChartRef = ref(null)
const dailyCompareChartRef = ref(null)
const trendChartRef = ref(null)
let rechargeStatusChart = null
let dailyCompareChart = null
let trendChart = null

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  const n = Number(v)
  if (!Number.isFinite(n)) return '0.00'
  return n.toFixed(2)
}

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoStr(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toISOString().slice(0, 10)
}

function buildPromotionParams() {
  const params = {
    page: pagination.value.page,
    pageSize: pagination.value.pageSize,
    promotion_id: filterForm.value.promotion_id || undefined,
    promotion_name: filterForm.value.promotion_name || undefined,
    drama_id: filterForm.value.drama_id || undefined,
    drama_name: filterForm.value.drama_name || undefined,
    account: filterForm.value.account || undefined,
  }
  if (dateRange.value && dateRange.value.length === 2) {
    params.start_date = dateRange.value[0]
    params.end_date = dateRange.value[1]
  }
  if (filterForm.value.media && filterForm.value.media.length > 0) {
    params.media = filterForm.value.media.join(',')
  }
  if (filterForm.value.country && filterForm.value.country.length > 0) {
    params.country = filterForm.value.country.join(',')
  }
  return params
}

async function fetchStats() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  statsLoading.value = true
  try {
    const res = await request.get('/dashboard/stats', {
      params: {
        start_date: dateRange.value[0],
        end_date: dateRange.value[1],
      },
    })
    const d = res.data || {}
    const rsd = d.recharge_status_dist || {}
    const ch = d.chart_data || {}
    statsData.value = {
      total_users: d.total_users ?? 0,
      total_recharge: d.total_recharge ?? 0,
      total_amount: Number(d.total_amount ?? 0),
      today_users: d.today_users ?? 0,
      recharge_status_dist: {
        success: Number(rsd.success ?? 0),
        pending: Number(rsd.pending ?? 0),
        failed: Number(rsd.failed ?? 0),
      },
      chart_data: {
        dates: ch.dates || [],
        users: ch.users || [],
        recharge: ch.recharge || [],
        amount: (ch.amount || []).map((x) => Number(x)),
      },
    }
  } catch (e) {
    console.error(e)
  } finally {
    statsLoading.value = false
  }
}

async function fetchPromotionData() {
  if (!dateRange.value || dateRange.value.length !== 2) return
  promotionLoading.value = true
  try {
    const res = await request.get('/dashboard/promotion-details', {
      params: buildPromotionParams(),
    })
    const d = res.data || {}
    promotionData.value = d.list || []
    pagination.value.total = Number(d.total ?? 0)
  } catch (e) {
    console.error(e)
  } finally {
    promotionLoading.value = false
  }
}

function initRechargeStatusChart() {
  if (!rechargeStatusChartRef.value) return
  const rs = statsData.value.recharge_status_dist || {}
  if (!rechargeStatusChart) rechargeStatusChart = echarts.init(rechargeStatusChartRef.value)
  rechargeStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '充值状态',
        type: 'pie',
        radius: '60%',
        data: [
          { value: rs.success || 0, name: '成功', itemStyle: { color: '#67c23a' } },
          { value: rs.pending || 0, name: '待支付', itemStyle: { color: '#e6a23c' } },
          { value: rs.failed || 0, name: '失败', itemStyle: { color: '#f56c6c' } },
        ],
      },
    ],
  })
}

function initDailyCompareChart() {
  if (!dailyCompareChartRef.value) return
  const cd = statsData.value.chart_data || {}
  const dates = cd.dates || []
  const users = cd.users || []
  const recharge = cd.recharge || []
  if (!dailyCompareChart) dailyCompareChart = echarts.init(dailyCompareChartRef.value)
  dailyCompareChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['新增用户', '充值笔数'] },
    grid: { left: 44, right: 20, bottom: 48, top: 32 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 8 ? 28 : 0 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '新增用户', type: 'bar', data: users, itemStyle: { color: '#409eff' } },
      { name: '充值笔数', type: 'bar', data: recharge, itemStyle: { color: '#67c23a' } },
    ],
  })
}

function initTrendChart() {
  if (!trendChartRef.value) return
  const cd = statsData.value.chart_data || {}
  const dates = cd.dates || []
  const users = (cd.users || []).map((x) => Number(x))
  const recharge = (cd.recharge || []).map((x) => Number(x))
  const amount = (cd.amount || []).map((x) => Number(x))
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['新增用户', '充值笔数', '实付金额'] },
    grid: { left: 52, right: 52, bottom: 48, top: 56 },
    xAxis: { type: 'category', boundaryGap: false, data: dates },
    yAxis: [
      { type: 'value', name: '数量', minInterval: 1 },
      { type: 'value', name: '金额(元)', splitLine: { show: false } },
    ],
    series: [
      { name: '新增用户', type: 'line', smooth: true, data: users, itemStyle: { color: '#409eff' } },
      { name: '充值笔数', type: 'line', smooth: true, data: recharge, itemStyle: { color: '#67c23a' } },
      { name: '实付金额', type: 'line', yAxisIndex: 1, smooth: true, data: amount, itemStyle: { color: '#e6a23c' } },
    ],
  })
}

function initAllCharts() {
  initRechargeStatusChart()
  initDailyCompareChart()
  initTrendChart()
}

function disposeCharts() {
  rechargeStatusChart?.dispose()
  dailyCompareChart?.dispose()
  trendChart?.dispose()
  rechargeStatusChart = null
  dailyCompareChart = null
  trendChart = null
}

function resizeCharts() {
  rechargeStatusChart?.resize()
  dailyCompareChart?.resize()
  trendChart?.resize()
}

async function handleTabChange(name) {
  if (name === 'overview') {
    await fetchStats()
    await nextTick()
    await nextTick()
    disposeCharts()
    initAllCharts()
    resizeCharts()
  } else {
    disposeCharts()
    if (name === 'promotion') {
      pagination.value.page = 1
      await fetchPromotionData()
    }
  }
}

async function handleDateChange() {
  await fetchStats()
  pagination.value.page = 1
  if (activeTab.value === 'promotion') {
    await fetchPromotionData()
  } else {
    disposeCharts()
    await nextTick()
    initAllCharts()
  }
}

async function handleFilter() {
  filterSubmitting.value = true
  try {
    pagination.value.page = 1
    await fetchStats()
    if (activeTab.value === 'promotion') {
      await fetchPromotionData()
    } else {
      disposeCharts()
      await nextTick()
      initAllCharts()
    }
  } finally {
    filterSubmitting.value = false
  }
}

async function handleReset() {
  filterForm.value = {
    promotion_id: '',
    promotion_name: '',
    drama_id: '',
    drama_name: '',
    account: '',
    media: [],
    country: [],
  }
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 6)
  dateRange.value = [start.toISOString().split('T')[0], end.toISOString().split('T')[0]]
  pagination.value.page = 1
  await handleFilter()
}

function handleExport() {
  ElMessage.success('导出功能开发中…')
}

function handlePageSizeChange() {
  fetchPromotionData()
}

function handlePageChange() {
  fetchPromotionData()
}

function viewDetail(row) {
  ElMessage.info(`查看推广详情：${row.promotion_name || ''}`)
}

onMounted(async () => {
  dateRange.value = [daysAgoStr(6), todayStr()]
  await fetchStats()
  await fetchPromotionData()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.dashboard-container {
  padding: 8px;
  background: #f0f2f5;
  height: calc(100vh - 48px - 36px - 24px);
  min-height: 320px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.filter-card {
  flex-shrink: 0;
  border-radius: 8px;
}

.filter-form {
  margin-top: 0;
}

.filter-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

.tabs-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
}

.custom-tabs {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.custom-tabs :deep(.el-tabs__header) {
  margin-bottom: 8px;
  flex-shrink: 0;
}

.custom-tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

.custom-tabs :deep(.el-tab-pane) {
  height: 100%;
}

.custom-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 500;
  padding: 0 16px;
  height: 40px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.chart-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.chart-inner {
  width: 100%;
  height: 300px;
}
.chart-inner--trend {
  height: 320px;
}
.overview-charts {
  min-height: 320px;
}

.pagination-container {
  margin-top: 22px;
  display: flex;
  justify-content: flex-end;
}

.hint {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.num-cost {
  color: #f56c6c;
  font-weight: 700;
}
.num-ok {
  color: #67c23a;
}
.num-ok-strong {
  color: #67c23a;
  font-weight: 700;
}
.num-blue {
  color: #409eff;
}
.num-warn {
  color: #e6a23c;
}
.num-muted {
  color: #909399;
}
.num-plain {
  color: #606266;
}
.profit-pos {
  color: #67c23a;
  font-weight: 700;
}
.profit-neg {
  color: #f56c6c;
  font-weight: 700;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}
:deep(.el-table__body tr:hover > td) {
  background-color: #f5f7fa !important;
}
:deep(.el-table__row) {
  transition: background-color 0.2s;
}

</style>
