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
        <el-tab-pane name="drama">
          <template #label>
            <span class="tab-label">
              <el-icon><Film /></el-icon>
              短剧概览
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
        <!-- 日期筛选栏 -->
        <div class="overview-toolbar">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            size="small"
            class="overview-datepicker"
          />
          <el-button type="primary" size="small" :icon="Search" @click="handleOverviewSearch">查询</el-button>
          <el-button size="small" :icon="Refresh" @click="handleOverviewReset">重置</el-button>
        </div>

        <!-- KPI 指标卡片 -->
        <div class="kpi-cards">
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(64,158,255,0.1); color: #409eff;">
              <el-icon :size="22"><User /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">累计用户</div>
              <div class="kpi-value">{{ formatNum(statsData.total_users) }}</div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(103,194,58,0.1); color: #67c23a;">
              <el-icon :size="22"><UserFilled /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">今日新增</div>
              <div class="kpi-value">{{ formatNum(statsData.today_users) }}</div>
              <div class="kpi-change" :class="changeClass(statsData.today_users, statsData.yesterday_users)">
                {{ changeText(statsData.today_users, statsData.yesterday_users) }}
              </div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(230,162,60,0.1); color: #e6a23c;">
              <el-icon :size="22"><Money /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">累计充值额</div>
              <div class="kpi-value kpi-money">{{ formatMoney(statsData.total_amount) }}</div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(245,108,108,0.1); color: #f56c6c;">
              <el-icon :size="22"><Wallet /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">今日充值额</div>
              <div class="kpi-value kpi-money">{{ formatMoney(statsData.today_amount) }}</div>
              <div class="kpi-change" :class="changeClass(statsData.today_amount, statsData.yesterday_amount)">
                {{ changeText(statsData.today_amount, statsData.yesterday_amount) }}
              </div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(144,147,153,0.1); color: #909399;">
              <el-icon :size="22"><Tickets /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">充值笔数</div>
              <div class="kpi-value">{{ formatNum(statsData.total_recharge) }}</div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(103,194,58,0.1); color: #67c23a;">
              <el-icon :size="22"><Coin /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">充值用户 / 首充</div>
              <div class="kpi-value">{{ formatNum(statsData.recharge_users) }} <span class="kpi-sub">/ {{ formatNum(statsData.first_recharge_count) }}</span></div>
            </div>
          </div>
          <div class="kpi-card">
            <div class="kpi-icon" style="background: rgba(64,158,255,0.1); color: #409eff;">
              <el-icon :size="22"><Film /></el-icon>
            </div>
            <div class="kpi-info">
              <div class="kpi-label">短剧总数</div>
              <div class="kpi-value">{{ formatNum(statsData.total_dramas) }}</div>
            </div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="overview-charts">
          <!-- 第一行：收入趋势 + 用户增长 -->
          <el-row :gutter="12" class="chart-row">
            <el-col :span="12">
              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="chart-header">
                    <el-icon><TrendCharts /></el-icon>
                    <span>收入趋势</span>
                  </div>
                </template>
                <div ref="revenueTrendChartRef" class="chart-inner chart-inner--lg" />
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="chart-header">
                    <el-icon><Histogram /></el-icon>
                    <span>用户增长 vs 充值笔数</span>
                  </div>
                </template>
                <div ref="userGrowthChartRef" class="chart-inner chart-inner--lg" />
              </el-card>
            </el-col>
          </el-row>

          <!-- 第二行：充值状态 + 国家分布 + 渠道分布 -->
          <el-row :gutter="12" class="chart-row">
            <el-col :span="8">
              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="chart-header">
                    <el-icon><PieChart /></el-icon>
                    <span>充值状态分布</span>
                  </div>
                </template>
                <div ref="rechargeStatusChartRef" class="chart-inner chart-inner--sm" />
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="chart-header">
                    <el-icon><Location /></el-icon>
                    <span>国家/地区分布</span>
                  </div>
                </template>
                <div ref="countryChartRef" class="chart-inner chart-inner--sm" />
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card shadow="hover" class="chart-card">
                <template #header>
                  <div class="chart-header">
                    <el-icon><Platform /></el-icon>
                    <span>渠道平台分布</span>
                  </div>
                </template>
                <div ref="platformChartRef" class="chart-inner chart-inner--sm" />
              </el-card>
            </el-col>
          </el-row>
        </div>
      </div>

      <DramaOverview v-show="activeTab === 'drama'" :active="activeTab === 'drama'" />
    </div>
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
  Search,
  Refresh,
  User,
  UserFilled,
  Money,
  Wallet,
  Tickets,
  Coin,
  Film,
  Location,
  Platform,
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import PromotionDetails from './dashboard/PromotionDetails.vue'
import DramaOverview from './dashboard/DramaOverview.vue'

const COUNTRY_NAME = { ID: '印尼', TH: '泰国', US: '美国', VN: '越南', PH: '菲律宾', MY: '马来西亚' }
const PLATFORM_NAME = { tiktok: 'TikTok', facebook: 'Facebook', google: 'Google', snapchat: 'Snapchat' }

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
  today_recharge: 0,
  today_amount: 0,
  yesterday_users: 0,
  yesterday_amount: 0,
  total_dramas: 0,
  recharge_users: 0,
  first_recharge_count: 0,
  recharge_status_dist: { success: 0, pending: 0, failed: 0 },
  chart_data: { dates: [], users: [], recharge: [], amount: [] },
  country_dist: [],
  platform_dist: [],
})

const revenueTrendChartRef = ref(null)
const userGrowthChartRef = ref(null)
const rechargeStatusChartRef = ref(null)
const countryChartRef = ref(null)
const platformChartRef = ref(null)

let revenueTrendChart = null
let userGrowthChart = null
let rechargeStatusChart = null
let countryChart = null
let platformChart = null

const dateShortcuts = [
  { text: '今天', value: () => { const t = todayStr(); return [t, t] } },
  { text: '昨天', value: () => { const y = daysAgoStr(1); return [y, y] } },
  { text: '近7天', value: () => [daysAgoStr(6), todayStr()] },
  { text: '近30天', value: () => [daysAgoStr(29), todayStr()] },
]

function todayStr() {
  return new Date().toISOString().slice(0, 10)
}

function daysAgoStr(n) {
  const d = new Date()
  d.setDate(d.getDate() - n)
  return d.toISOString().slice(0, 10)
}

function formatNum(v) {
  if (v === null || v === undefined) return '0'
  const n = Number(v)
  if (!Number.isFinite(n)) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString('zh-CN')
}

function formatMoney(v) {
  if (v === null || v === undefined) return '0.00'
  const n = Number(v)
  if (!Number.isFinite(n)) return '0.00'
  if (n >= 10000) return (n / 10000).toFixed(2) + 'w'
  return n.toFixed(2)
}

function changeClass(current, previous) {
  const c = Number(current) || 0
  const p = Number(previous) || 0
  if (p === 0) return c > 0 ? 'kpi-up' : 'kpi-flat'
  return c >= p ? 'kpi-up' : 'kpi-down'
}

function changeText(current, previous) {
  const c = Number(current) || 0
  const p = Number(previous) || 0
  if (p === 0) return c > 0 ? '↑ 新增' : '— 持平'
  const pct = (((c - p) / p) * 100).toFixed(1)
  if (c >= p) return `↑ ${pct}%`
  return `↓ ${Math.abs(pct)}%`
}

async function fetchStats() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  statsLoading.value = true
  try {
    const res = await request.get('/dashboard/stats', {
      params: { start_date: dateRange.value[0], end_date: dateRange.value[1] },
    })
    const d = res.data || {}
    const rsd = d.recharge_status_dist || {}
    const ch = d.chart_data || {}
    statsData.value = {
      total_users: d.total_users ?? 0,
      total_recharge: d.total_recharge ?? 0,
      total_amount: Number(d.total_amount ?? 0),
      today_users: d.today_users ?? 0,
      today_recharge: d.today_recharge ?? 0,
      today_amount: Number(d.today_amount ?? 0),
      yesterday_users: d.yesterday_users ?? 0,
      yesterday_amount: Number(d.yesterday_amount ?? 0),
      total_dramas: d.total_dramas ?? 0,
      recharge_users: d.recharge_users ?? 0,
      first_recharge_count: d.first_recharge_count ?? 0,
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
      country_dist: d.country_dist || [],
      platform_dist: d.platform_dist || [],
    }
  } catch (e) {
    console.error(e)
  } finally {
    statsLoading.value = false
  }
}

/* ---- 图表 ---- */

function initRevenueTrendChart() {
  if (!revenueTrendChartRef.value) return
  const cd = statsData.value.chart_data || {}
  const dates = cd.dates || []
  const amount = (cd.amount || []).map((x) => Number(x))
  if (!revenueTrendChart) revenueTrendChart = echarts.init(revenueTrendChartRef.value)
  revenueTrendChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['充值金额'], top: 4, textStyle: { fontSize: 11 } },
    grid: { left: 52, right: 20, bottom: 36, top: 40 },
    xAxis: { type: 'category', boundaryGap: false, data: dates, axisLabel: { fontSize: 10, rotate: dates.length > 14 ? 30 : 0 } },
    yAxis: { type: 'value', name: '金额', axisLabel: { fontSize: 10 }, nameTextStyle: { fontSize: 10 } },
    series: [
      {
        name: '充值金额', type: 'line', smooth: true, data: amount,
        itemStyle: { color: '#e6a23c' },
        areaStyle: {
          color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [{ offset: 0, color: 'rgba(230,162,60,0.35)' }, { offset: 1, color: 'rgba(230,162,60,0.05)' }] },
        },
      },
    ],
  })
}

function initUserGrowthChart() {
  if (!userGrowthChartRef.value) return
  const cd = statsData.value.chart_data || {}
  const dates = cd.dates || []
  const users = cd.users || []
  const recharge = cd.recharge || []
  if (!userGrowthChart) userGrowthChart = echarts.init(userGrowthChartRef.value)
  userGrowthChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['新增用户', '充值笔数'], top: 4, textStyle: { fontSize: 11 } },
    grid: { left: 48, right: 20, bottom: 36, top: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { rotate: dates.length > 14 ? 30 : 0, fontSize: 10 } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 10 } },
    series: [
      { name: '新增用户', type: 'bar', data: users, itemStyle: { color: '#409eff' }, barMaxWidth: 20 },
      { name: '充值笔数', type: 'bar', data: recharge, itemStyle: { color: '#67c23a' }, barMaxWidth: 20 },
    ],
  })
}

function initRechargeStatusChart() {
  if (!rechargeStatusChartRef.value) return
  const rs = statsData.value.recharge_status_dist || {}
  if (!rechargeStatusChart) rechargeStatusChart = echarts.init(rechargeStatusChartRef.value)
  rechargeStatusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 4, top: 'center', textStyle: { fontSize: 11 } },
    series: [
      {
        name: '充值状态', type: 'pie', radius: ['40%', '65%'], center: ['60%', '50%'],
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: [
          { value: rs.success || 0, name: '成功', itemStyle: { color: '#67c23a' } },
          { value: rs.pending || 0, name: '待支付', itemStyle: { color: '#e6a23c' } },
          { value: rs.failed || 0, name: '失败', itemStyle: { color: '#f56c6c' } },
        ],
      },
    ],
  })
}

function initCountryChart() {
  if (!countryChartRef.value) return
  const dist = statsData.value.country_dist || []
  if (!countryChart) countryChart = echarts.init(countryChartRef.value)
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9']
  const data = dist.map((item, i) => ({
    value: Number(item.amount || 0),
    name: COUNTRY_NAME[item.name] || item.name || '未知',
    itemStyle: { color: colors[i % colors.length] },
  }))
  countryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 4, top: 'center', textStyle: { fontSize: 11 } },
    series: [
      {
        name: '国家分布', type: 'pie', radius: ['40%', '65%'], center: ['60%', '50%'],
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
        data: data.length ? data : [{ value: 0, name: '暂无数据', itemStyle: { color: '#dcdfe6' } }],
      },
    ],
  })
}

function initPlatformChart() {
  if (!platformChartRef.value) return
  const dist = statsData.value.platform_dist || []
  if (!platformChart) platformChart = echarts.init(platformChartRef.value)
  const colors = ['#1890ff', '#52c41a', '#fa8c16', '#f5222d', '#722ed1', '#13c2c2']
  const names = dist.map((item) => PLATFORM_NAME[item.name] || item.name || '未知')
  const values = dist.map((item) => Number(item.amount || 0))
  platformChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 70, right: 20, bottom: 12, top: 12 },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 11 }, inverse: true },
    series: [
      {
        type: 'bar', data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i % colors.length] } })),
        barMaxWidth: 22,
        label: { show: true, position: 'right', fontSize: 10, formatter: '{c}' },
      },
    ],
  })
}

function initAllCharts() {
  initRevenueTrendChart()
  initUserGrowthChart()
  initRechargeStatusChart()
  initCountryChart()
  initPlatformChart()
}

function disposeCharts() {
  ;[revenueTrendChart, userGrowthChart, rechargeStatusChart, countryChart, platformChart].forEach((c) => c?.dispose())
  revenueTrendChart = null
  userGrowthChart = null
  rechargeStatusChart = null
  countryChart = null
  platformChart = null
}

function resizeCharts() {
  ;[revenueTrendChart, userGrowthChart, rechargeStatusChart, countryChart, platformChart].forEach((c) => c?.resize())
}

async function refreshOverview() {
  await fetchStats()
  await nextTick()
  await nextTick()
  disposeCharts()
  initAllCharts()
  resizeCharts()
}

function handleOverviewSearch() {
  refreshOverview()
}

function handleOverviewReset() {
  dateRange.value = [daysAgoStr(6), todayStr()]
  refreshOverview()
}

async function handleTabChange(name) {
  if (name === 'overview') {
    await refreshOverview()
  } else if (name !== 'drama') {
    disposeCharts()
  } else {
    disposeCharts()
  }
}

function updateLocalTime() {
  const now = new Date()
  localTimeText.value = now.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
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

.main-tabs { flex: 1; min-height: 40px; }
.main-tabs :deep(.el-tabs__header) { margin-bottom: 0; }
.main-tabs :deep(.el-tabs__nav-wrap::after) { display: none; }
.main-tabs :deep(.el-tabs__item) { font-size: 13px; font-weight: 500; padding: 0 16px; height: 40px; }
.main-tabs :deep(.el-tabs__active-bar) { height: 2px; }

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
  bottom: 0;
  overflow: hidden;
  background: transparent;
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

/* ========== 数据概览 ========== */
.overview-wrap {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow-y: auto;
  padding: 0 2px 12px;
}

.overview-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px;
}

.overview-datepicker {
  width: 280px;
}

/* KPI 卡片 */
.kpi-cards {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 10px;
}

.kpi-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
}
.kpi-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.kpi-icon {
  flex-shrink: 0;
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.kpi-info {
  min-width: 0;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
  white-space: nowrap;
}

.kpi-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
  white-space: nowrap;
}
.kpi-value.kpi-money {
  color: #e6a23c;
}

.kpi-sub {
  font-size: 14px;
  font-weight: 500;
  color: #909399;
}

.kpi-change {
  font-size: 11px;
  margin-top: 2px;
}
.kpi-up { color: #67c23a; }
.kpi-down { color: #f56c6c; }
.kpi-flat { color: #909399; }

/* 图表 */
.overview-charts {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chart-row {
  flex-shrink: 0;
}

.chart-card {
  border-radius: 10px;
}
.chart-card :deep(.el-card__body) { padding: 8px 10px !important; }
.chart-card :deep(.el-card__header) { padding: 8px 12px !important; }

.chart-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  font-size: 13px;
}

.chart-inner--lg {
  width: 100%;
  height: 260px;
}
.chart-inner--sm {
  width: 100%;
  height: 220px;
}

/* ========== 推广明细通用 ========== */
.profit-pos { color: #67c23a; font-weight: 700; }
.profit-neg { color: #f56c6c; font-weight: 700; }
:deep(.el-table) { border-radius: 8px; overflow: hidden; }
:deep(.el-table__body tr:hover > td) { background-color: #f5f7fa !important; }
:deep(.el-table__row) { transition: background-color 0.2s; }
</style>
