<template>
  <div class="dashboard-page-layout dashboard-root">
    <el-card v-show="activeTab === 'overview'" class="filter-card" shadow="never">
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
      <el-tabs v-model="activeTab" class="custom-tabs compact-tabs" @tab-change="handleTabChange">
        <el-tab-pane name="promotion">
          <template #label>
            <span class="tab-label">
              <el-icon><DataLine /></el-icon>
              推广明细
            </span>
          </template>
          <PromotionDetails />
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
      推广明细 Tab 对接 <code>/api/promotion-details</code>（<code>schema-promotion.sql</code>）；数据概览与
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
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useCountries } from '@/composables/useCountries'
import PromotionDetails from './dashboard/PromotionDetails.vue'

const { countries, countryMultiOptions, formatAccountCountryLabel } = useCountries()

const activeTab = ref('promotion')
const dateRange = ref([])
const statsLoading = ref(false)
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
  }
}

async function handleDateChange() {
  await fetchStats()
  if (activeTab.value === 'overview') {
    disposeCharts()
    await nextTick()
    initAllCharts()
  }
}

async function handleFilter() {
  filterSubmitting.value = true
  try {
    await fetchStats()
    disposeCharts()
    await nextTick()
    initAllCharts()
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
  await handleFilter()
}

function handleExport() {
  ElMessage.success('导出功能开发中…')
}

onMounted(async () => {
  dateRange.value = [daysAgoStr(6), todayStr()]
  await fetchStats()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
/* #090：高度由 Layout + .dashboard-page-layout（全局 flex）承担 */
.dashboard-root {
  background: #f0f2f5;
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
