<template>
  <div class="promotion-details">
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" :model="filters" class="filter-form" size="small" @submit.prevent="handleSearch">
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD"
            :shortcuts="dateShortcuts"
            class="filter-daterange"
          />
        </el-form-item>
        <el-form-item label="推广ID">
          <el-input v-model="filters.promotionId" placeholder="推广ID" clearable class="filter-w-sm" />
        </el-form-item>
        <el-form-item label="推广名称">
          <el-input v-model="filters.promotionName" placeholder="推广名称" clearable class="filter-w-sm" />
        </el-form-item>
        <el-form-item label="投放媒体">
          <el-select v-model="filters.platform" placeholder="全部" clearable class="filter-w-sm">
            <el-option
              v-for="p in platformList"
              :key="p"
              :label="getPlatformLabel(p)"
              :value="p"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="短剧ID">
          <el-input v-model="filters.dramaId" placeholder="短剧ID" clearable class="filter-w-xs" />
        </el-form-item>
        <el-form-item label="短剧">
          <el-select
            v-model="filters.dramaName"
            placeholder="选择短剧"
            clearable
            filterable
            class="filter-w-sm"
          >
            <el-option v-for="d in dramaList" :key="d.id" :label="d.title" :value="d.title" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="filters.country" placeholder="请选择" clearable class="filter-w-xs">
            <el-option
              v-for="c in countryList"
              :key="c.code"
              :label="countryOptionLabel(c)"
              :value="c.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账户">
          <el-input v-model="filters.accountId" placeholder="账户ID" clearable class="filter-w-xs" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button :icon="Download" @click="handleExport">导出</el-button>
          <el-button :icon="RefreshRight" :loading="syncLoading" @click="handleSync">同步数据</el-button>
        </el-form-item>
      </el-form>
      <div class="local-time">
        <el-icon><Clock /></el-icon>
        <span>{{ localTimeText }}</span>
      </div>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table
        v-loading="loading"
        :data="displayTableData"
        border
        stripe
        size="small"
        :height="tableHeight"
        :row-class-name="tableRowClassName"
        :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold' }"
      >
        <el-table-column prop="date" label="日期" width="108" fixed="left">
          <template #default="{ row, $index }">
            <span v-if="isSummaryRow($index)" class="summary-label">汇总</span>
            <span v-else>{{ row.date }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="promotionId" label="推广ID" width="120" show-overflow-tooltip>
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.promotionId }}
          </template>
        </el-table-column>
        <el-table-column prop="promotionName" label="推广名称" min-width="160" show-overflow-tooltip>
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.promotionName }}
          </template>
        </el-table-column>
        <el-table-column prop="dramaName" label="剧" min-width="120" show-overflow-tooltip>
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.dramaName }}
          </template>
        </el-table-column>
        <el-table-column prop="accountId" label="账户" width="100">
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.accountId }}
          </template>
        </el-table-column>
        <el-table-column prop="accountName" label="名称" min-width="120" show-overflow-tooltip>
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.accountName }}
          </template>
        </el-table-column>
        <el-table-column prop="balance" label="余额" width="88" align="right">
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : formatNumber(row.balance, 2) }}
          </template>
        </el-table-column>
        <el-table-column prop="campaignName" label="系列/广告组" min-width="120" show-overflow-tooltip>
          <template #default="{ row, $index }">
            {{ isSummaryRow($index) ? '-' : row.campaignName }}
          </template>
        </el-table-column>
        <el-table-column prop="cost" label="消耗" width="88" align="right">
          <template #default="{ row }">{{ formatNumber(row.cost, 2) }}</template>
        </el-table-column>
        <el-table-column prop="speed" label="时速" width="88" align="right">
          <template #default="{ row }">{{ formatNumber(row.speed, 2) }}</template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="80" align="right">
          <template #default="{ row }">{{ formatNumber(row.roi, 4) }}</template>
        </el-table-column>
        <el-table-column prop="userCount" label="用户数" width="80" align="right">
          <template #default="{ row }">{{ row.userCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="rechargeAmount" label="充值金额" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.rechargeAmount, 2) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" width="100" align="right">
          <template #default="{ row }">
            <span :class="Number(row.profit) >= 0 ? 'profit-positive' : 'profit-negative'">
              {{ formatNumber(row.profit, 2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="orderCount" label="订单数" width="80" align="right">
          <template #default="{ row }">{{ row.orderCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="firstRechargeCount" label="首充数" width="80" align="right">
          <template #default="{ row }">{{ row.firstRechargeCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="firstRechargeRate" label="首充率" width="88" align="right">
          <template #default="{ row }">{{ formatPercent(row.firstRechargeRate) }}</template>
        </el-table-column>
        <el-table-column prop="repeatRechargeCount" label="复充数" width="80" align="right">
          <template #default="{ row }">{{ row.repeatRechargeCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="cpm" label="千次曝光" width="88" align="right">
          <template #default="{ row }">{{ formatNumber(row.cpm, 2) }}</template>
        </el-table-column>
        <el-table-column prop="avgRechargePerUser" label="人均消耗" width="100" align="right">
          <template #default="{ row }">{{ formatNumber(row.avgRechargePerUser, 2) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="88" fixed="right" align="center">
          <template #default="{ row, $index }">
            <el-button type="primary" link @click="showProfitChart(row, $index)">利润图</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="chartDialogVisible"
      :title="chartTitle"
      width="900px"
      destroy-on-close
      :close-on-click-modal="false"
      @closed="onChartDialogClosed"
    >
      <div class="chart-header">
        <div class="chart-info">
          <span class="time-range">时间范围：{{ dateRange[0] }} 至 {{ dateRange[1] }}</span>
        </div>
        <div class="chart-controls-inline">
          <el-radio-group v-model="chartGranularity" @change="onChartGranularityChange">
            <el-radio-button value="hour">按小时</el-radio-button>
            <el-radio-button value="day">按天</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      <div class="chart-tip">
        <span v-if="chartGranularity === 'hour'">
          按小时：X 轴为时间范围「结束日」当天 00:00～23:00；多天时已自动按天展示。
        </span>
        <span v-else>按天：X 轴为区间内各日（如 04-01、04-02），Y 轴为当日利润合计。</span>
      </div>
      <div ref="chartContainer" class="chart-box"></div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, RefreshRight, Clock } from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import {
  getPromotionDetails,
  getProfitChart,
  getProfitChartAll,
  syncTikTokData,
  getPromotionPlatforms,
  getPromotionCountries,
} from '@/api/promotionDetails'
import { getDramaList } from '@/api/drama'

const loading = ref(false)
const syncLoading = ref(false)
const tableData = ref([])
const summary = ref(null)
const dramaList = ref([])

/** #083：首屏即有选项，避免 el-select 在异步返回前只显示 value（ID） */
const DEFAULT_COUNTRY_OPTIONS = [
  { code: 'ID', name: '印尼' },
  { code: 'TH', name: '泰国' },
  { code: 'US', name: '美国' },
  { code: 'VN', name: '越南' },
  { code: 'PH', name: '菲律宾' },
  { code: 'MY', name: '马来西亚' },
]
const DEFAULT_PLATFORMS = ['tiktok', 'facebook', 'google']

const platformList = ref([...DEFAULT_PLATFORMS])
const countryList = ref(DEFAULT_COUNTRY_OPTIONS.map((c) => ({ ...c })))

const dateRange = ref([])
const filters = reactive({
  promotionId: '',
  promotionName: '',
  platform: '',
  dramaId: '',
  dramaName: '',
  country: 'ID',
  accountId: '',
})

const localTimeTick = ref(Date.now())
const localTimeText = computed(() => {
  const code = filters.country && String(filters.country).trim() ? filters.country : 'ID'
  const name = getCountryName(code)
  void localTimeTick.value
  return `${name}当地时间：${formatZonedDateTime(code, localTimeTick.value)}`
})

const pagination = reactive({
  page: 1,
  pageSize: 20,
  total: 0,
})

const tableHeight = computed(() => Math.max(320, window.innerHeight - 420))

const displayTableData = computed(() => {
  const rows = tableData.value || []
  if (pagination.page !== 1 || !summary.value) return rows
  return [summary.value, ...rows]
})

function isSummaryRow(index) {
  return pagination.page === 1 && summary.value && index === 0
}

const dateShortcuts = [
  {
    text: '今天',
    value: () => {
      const t = new Date()
      return [t, t]
    },
  },
  {
    text: '昨天',
    value: () => {
      const t = new Date()
      t.setDate(t.getDate() - 1)
      return [t, t]
    },
  },
  {
    text: '最近7天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    },
  },
  {
    text: '最近30天',
    value: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 29)
      return [start, end]
    },
  },
]

const chartDialogVisible = ref(false)
const chartContainer = ref(null)
const chartGranularity = ref('hour')
const chartTitle = ref('利润趋势图')
const currentPromotionId = ref('')
let chartInstance = null
let refreshTimer = null
let clockTimer = null

const COUNTRY_TIMEZONE = {
  ID: 'Asia/Jakarta',
  TH: 'Asia/Bangkok',
  US: 'America/New_York',
  VN: 'Asia/Ho_Chi_Minh',
  PH: 'Asia/Manila',
  MY: 'Asia/Kuala_Lumpur',
}

/** 国家中文名（下拉标签、当地时间前缀） */
function getCountryName(code) {
  if (code == null || code === '') return ''
  const key = String(code).toUpperCase()
  const row = countryList.value.find((c) => c.code === key)
  if (row?.name) return row.name
  const fb = { ID: '印尼', TH: '泰国', US: '美国', VN: '越南', PH: '菲律宾', MY: '马来西亚' }
  return fb[key] || code
}

function countryOptionLabel(c) {
  if (!c) return ''
  return c.name || getCountryName(c.code)
}

function formatZonedDateTime(countryCode, atMs) {
  const tz = COUNTRY_TIMEZONE[countryCode] || COUNTRY_TIMEZONE.ID
  try {
    return new Intl.DateTimeFormat('zh-CN', {
      timeZone: tz,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    }).format(new Date(atMs))
  } catch (e) {
    console.error(e)
    return formatDateTime(new Date(atMs))
  }
}

function getPlatformLabel(p) {
  const m = { tiktok: 'TikTok', facebook: 'Facebook', google: 'Google' }
  return m[p] || p
}

onMounted(() => {
  const today = new Date()
  dateRange.value = [formatDate(today), formatDate(today)]
  loadPlatformAndCountries()
  loadDramaList()
  loadData()
  refreshTimer = window.setInterval(() => loadData(false), 10 * 60 * 1000)
  clockTimer = window.setInterval(() => {
    localTimeTick.value = Date.now()
  }, 1000)
  window.addEventListener('resize', resizeChart)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  if (clockTimer) clearInterval(clockTimer)
  window.removeEventListener('resize', resizeChart)
  disposeChart()
})

async function loadPlatformAndCountries() {
  try {
    const [pr, cr] = await Promise.all([getPromotionPlatforms(), getPromotionCountries()])
    if (pr.code === 0 && Array.isArray(pr.data) && pr.data.length) {
      platformList.value = [...new Set(pr.data)].sort()
    } else if (!platformList.value.length) {
      platformList.value = [...DEFAULT_PLATFORMS]
    }
    if (cr.code === 0 && Array.isArray(cr.data) && cr.data.length) {
      const byCode = new Map(
        DEFAULT_COUNTRY_OPTIONS.map((c) => [c.code, { ...c }]),
      )
      for (const row of cr.data) {
        const code = row?.code != null ? String(row.code).toUpperCase() : ''
        if (!code) continue
        const name = row?.name || getCountryName(code)
        byCode.set(code, { code, name })
      }
      countryList.value = [...byCode.values()].sort((a, b) => a.code.localeCompare(b.code))
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadDramaList() {
  try {
    const res = await getDramaList({ page: 1, pageSize: 100 })
    if (res.code === 0 && res.data) {
      dramaList.value = res.data.list || []
    }
  } catch (e) {
    console.error(e)
  }
}

async function loadData(showLoading = true) {
  if (!dateRange.value?.length || dateRange.value.length !== 2) {
    if (showLoading) ElMessage.warning('请选择日期范围')
    return
  }
  if (showLoading) loading.value = true
  try {
    const params = {
      start_date: dateRange.value[0],
      end_date: dateRange.value[1],
      promotion_id: filters.promotionId || undefined,
      promotion_name: filters.promotionName || undefined,
      platform: filters.platform || undefined,
      drama_id: filters.dramaId || undefined,
      drama_name: filters.dramaName || undefined,
      country: filters.country || undefined,
      account_id: filters.accountId || undefined,
      page: pagination.page,
      page_size: pagination.pageSize,
    }
    const res = await getPromotionDetails(params)
    if (res.code === 0 && res.data) {
      tableData.value = res.data.list || []
      summary.value = res.data.summary ?? null
      pagination.total = Number(res.data.total ?? 0)
    } else if (res?.message) {
      ElMessage.error(res.message)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.page = 1
  loadData()
}

function handleReset() {
  const today = new Date()
  dateRange.value = [formatDate(today), formatDate(today)]
  filters.promotionId = ''
  filters.promotionName = ''
  filters.platform = ''
  filters.dramaId = ''
  filters.dramaName = ''
  filters.country = 'ID'
  filters.accountId = ''
  pagination.page = 1
  loadData()
}

function handleExport() {
  ElMessage.info('导出功能开发中…')
}

async function handleSync() {
  syncLoading.value = true
  try {
    const res = await syncTikTokData()
    if (res.code === 0) {
      ElMessage.success('同步已触发')
      await loadData()
    } else if (res?.message) {
      ElMessage.error(res.message)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('同步失败')
  } finally {
    syncLoading.value = false
  }
}

function handleSizeChange() {
  pagination.page = 1
  loadData()
}

function handlePageChange() {
  loadData()
}

function tableRowClassName({ rowIndex }) {
  if (isSummaryRow(rowIndex)) return 'summary-row'
  return ''
}

function chartDateRangeLabel() {
  if (!dateRange.value?.length || dateRange.value.length !== 2) return ''
  return `${dateRange.value[0]} 至 ${dateRange.value[1]}`
}

async function showProfitChart(row, rowIndex) {
  if (!dateRange.value?.length || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  const rangeLabel = chartDateRangeLabel()
  if (isSummaryRow(rowIndex)) {
    currentPromotionId.value = 'all'
    chartTitle.value = `所有推广利润趋势图（${rangeLabel}）`
  } else {
    if (!row?.promotionId) {
      ElMessage.warning('无推广ID')
      return
    }
    currentPromotionId.value = row.promotionId
    const name = row.promotionName || row.promotionId
    chartTitle.value = `${name} — 利润趋势（${rangeLabel}）`
  }
  const singleDay = dateRange.value[0] === dateRange.value[1]
  chartGranularity.value = singleDay ? 'hour' : 'day'
  chartDialogVisible.value = true
  await nextTick()
  await loadChartData()
}

function onChartGranularityChange() {
  if (chartGranularity.value === 'hour' && dateRange.value?.length === 2 && dateRange.value[0] !== dateRange.value[1]) {
    chartGranularity.value = 'day'
    ElMessage.info('多日期间请使用按天查看')
  }
  loadChartData()
}

async function loadChartData() {
  if (!dateRange.value?.length || dateRange.value.length !== 2) return
  let granularity = chartGranularity.value
  if (granularity === 'hour' && dateRange.value[0] !== dateRange.value[1]) {
    granularity = 'day'
  }
  try {
    const params = {
      start_date: dateRange.value[0],
      end_date: dateRange.value[1],
      granularity,
      promotion_id: filters.promotionId || undefined,
      promotion_name: filters.promotionName || undefined,
      platform: filters.platform || undefined,
      drama_id: filters.dramaId || undefined,
      drama_name: filters.dramaName || undefined,
      country: filters.country || undefined,
      account_id: filters.accountId || undefined,
    }
    let res
    if (currentPromotionId.value === 'all') {
      res = await getProfitChartAll(params)
    } else {
      if (!currentPromotionId.value) return
      res = await getProfitChart(currentPromotionId.value, params)
    }
    if (res.code === 0 && res.data) {
      renderChart(res.data.chartData || [])
    } else if (res?.message) {
      ElMessage.error(res.message)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载图表失败')
  }
}

function renderChart(data) {
  if (!chartContainer.value) return
  disposeChart()
  chartInstance = echarts.init(chartContainer.value)
  const times = data.map((item) => item.time)
  const profits = data.map((item) => Number(item.profit ?? 0))
  const isHour = chartGranularity.value === 'hour'
  chartInstance.setOption({
    title: {
      text: isHour ? '每小时利润趋势' : '每日利润趋势',
      left: 'center',
      top: 0,
      textStyle: { fontSize: 14 },
    },
    tooltip: {
      trigger: 'axis',
      formatter: (ps) => {
        const p = ps[0]
        return `${p.axisValue}<br/>利润: ${formatNumber(p.data, 2)}`
      },
    },
    grid: { left: 48, right: 24, bottom: times.length > 12 ? 56 : 40, top: 40 },
    xAxis: {
      type: 'category',
      data: times,
      axisLabel: { rotate: isHour || times.length > 10 ? 40 : 0 },
    },
    yAxis: { type: 'value', name: '利润' },
    series: [
      {
        name: '利润',
        type: 'line',
        data: profits,
        smooth: true,
        itemStyle: { color: '#409EFF' },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(64, 158, 255, 0.35)' },
              { offset: 1, color: 'rgba(64, 158, 255, 0.06)' },
            ],
          },
        },
      },
    ],
  })
}

function resizeChart() {
  chartInstance?.resize()
}

function disposeChart() {
  chartInstance?.dispose()
  chartInstance = null
}

function onChartDialogClosed() {
  disposeChart()
}

function formatNumber(value, decimals = 2) {
  if (value === null || value === undefined) return '-'
  const n = Number(value)
  if (!Number.isFinite(n)) return '-'
  return n.toFixed(decimals)
}

function formatPercent(value) {
  if (value === null || value === undefined) return '-'
  const n = Number(value)
  if (!Number.isFinite(n)) return '-'
  return (n * 100).toFixed(2) + '%'
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function formatDateTime(date) {
  return (
    formatDate(date) +
    ' ' +
    String(date.getHours()).padStart(2, '0') +
    ':' +
    String(date.getMinutes()).padStart(2, '0') +
    ':' +
    String(date.getSeconds()).padStart(2, '0')
  )
}
</script>

<style scoped>
.promotion-details {
  padding: 0 4px 8px;
}
.filter-card {
  margin-bottom: 10px;
  border-radius: 8px;
}
.filter-form {
  margin-bottom: 0;
}
.filter-daterange {
  width: 260px;
}
.filter-w-sm {
  width: 140px;
}
.filter-w-xs {
  width: 120px;
}
.table-card {
  border-radius: 8px;
}
.summary-label {
  font-weight: 700;
  color: #409eff;
}
:deep(.summary-row) {
  background-color: #f5f7fa !important;
  font-weight: 600;
}
:deep(.summary-row td) {
  background-color: #f5f7fa !important;
}
.profit-positive {
  color: #67c23a;
  font-weight: 600;
}
.profit-negative {
  color: #f56c6c;
  font-weight: 600;
}
.pagination-bar {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  flex-wrap: wrap;
  gap: 8px;
}
.chart-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.time-range {
  font-size: 13px;
  color: #606266;
}
.chart-controls-inline {
  flex-shrink: 0;
}
.chart-controls {
  margin-bottom: 12px;
  text-align: center;
}
.chart-tip {
  margin-bottom: 16px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  text-align: center;
}
.chart-box {
  width: 100%;
  height: 400px;
}
.local-time {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
  color: #606266;
}
.local-time .el-icon {
  font-size: 16px;
  color: #409eff;
}
</style>
