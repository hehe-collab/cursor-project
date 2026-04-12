<template>
  <div class="dashboard-page-layout dashboard-root">
    <div class="tabs-header">
      <el-tabs v-model="activeTab" class="main-tabs" @tab-change="handleTabChange">
        <el-tab-pane name="promotion">
          <template #label>
            <span class="tab-label">
              <el-icon><DataLine /></el-icon>
              推广明细
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="overview">
          <template #label>
            <span class="tab-label">
              <el-icon><PieChart /></el-icon>
              数据概览
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
      <div class="header-time">
        <el-icon><Clock /></el-icon>
        <span>{{ localTimeText }}</span>
      </div>
    </div>

    <div class="tab-content-area">
      <PromotionDetails v-show="activeTab === 'promotion'" />
      <div v-show="activeTab === 'overview'" v-loading="statsLoading" element-loading-text="加载中..." class="overview-wrap">
        <div class="overview-charts">
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
      </div>
    </div>

    <p class="hint hint--footer">
      推广明细 Tab 对接 <code>/api/promotion-details</code>（<code>schema-promotion.sql</code>）；数据概览与
      <code>/api/dashboard/stats</code> 一致。
    </p>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import {
  DataLine,
  PieChart,
  Histogram,
  TrendCharts,
  Clock,
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import PromotionDetails from './dashboard/PromotionDetails.vue'

const activeTab = ref('promotion')
const dateRange = ref([])
const statsLoading = ref(false)

const localTimeText = ref('')
let localTimeTimer = null

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
    legend: { orient: 'vertical', left: 'left', textStyle: { fontSize: 11 } },
    series: [
      {
        name: '充值状态',
        type: 'pie',
        radius: '55%',
        center: ['50%', '50%'],
        label: { fontSize: 11 },
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
    legend: { data: ['新增用户', '充值笔数'], textStyle: { fontSize: 11 } },
    grid: { left: 48, right: 20, bottom: 52, top: 36 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 8 ? 28 : 0, fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
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
    legend: { data: ['新增用户', '充值笔数', '实付金额'], textStyle: { fontSize: 11 } },
    grid: { left: 52, right: 52, bottom: 52, top: 56 },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLabel: { fontSize: 11 } },
    yAxis: [
      { type: 'value', name: '数量', minInterval: 1, axisLabel: { fontSize: 11 }, nameTextStyle: { fontSize: 11 } },
      { type: 'value', name: '金额(元)', splitLine: { show: false }, axisLabel: { fontSize: 11 }, nameTextStyle: { fontSize: 11 } },
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

function updateLocalTime() {
  const now = new Date()
  localTimeText.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  })
}

onMounted(async () => {
  updateLocalTime()
  localTimeTimer = setInterval(updateLocalTime, 1000)
  dateRange.value = [daysAgoStr(6), todayStr()]
  await fetchStats()
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
  clearInterval(localTimeTimer)
})
</script>

<style scoped>
/* #090：高度由 Layout + .dashboard-page-layout（全局 flex）承担 */
.dashboard-root {
  position: relative;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.tabs-header {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 8px;
  padding: 0 12px;
  margin-bottom: 8px;
}

.main-tabs {
  flex: 1;
  min-height: 40px;
}

.main-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.main-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.main-tabs :deep(.el-tabs__item) {
  font-size: 13px;
  font-weight: 500;
  padding: 0 16px;
  height: 40px;
}

.main-tabs :deep(.el-tabs__active-bar) {
  height: 2px;
}

.header-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
  padding-left: 12px;
}

.tab-content-area {
  position: absolute;
  top: 52px;
  left: 0;
  right: 0;
  bottom: 20px;
  overflow: hidden;
  background: #fff;
  border-radius: 8px;
}

.tab-content-area > * {
  height: 100%;
  border-radius: 8px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.chart-card {
  margin-bottom: 8px;
  border-radius: 12px;
}

.chart-card :deep(.el-card__body) {
  padding: 8px 10px !important;
}

.chart-card :deep(.el-card__header) {
  padding: 6px 10px !important;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 4px;
  font-weight: 600;
  font-size: 13px;
}

.chart-inner {
  width: 100%;
  height: 220px;
}
.chart-inner--trend {
  height: 240px;
}
.overview-charts {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
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
