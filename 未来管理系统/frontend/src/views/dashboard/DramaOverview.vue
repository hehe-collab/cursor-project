<template>
  <div class="drama-overview">
    <!-- 筛选栏 -->
    <el-card class="filter-card" :body-style="{ padding: '12px' }" shadow="never">
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
        <el-form-item label="短剧名称">
          <el-input v-model="filters.dramaName" placeholder="搜索短剧" clearable class="filter-w-sm" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="filters.categoryId" placeholder="全部" clearable class="filter-w-xs">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Download" :loading="exportLoading" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- KPI 卡片 -->
    <div class="kpi-cards">
      <div class="kpi-card">
        <div class="kpi-icon" style="background: rgba(64,158,255,0.1); color: #409eff;">
          <el-icon :size="20"><Film /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">上架剧数</div>
          <div class="kpi-value">{{ formatNum(kpi.online_dramas) }}</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background: rgba(103,194,58,0.1); color: #67c23a;">
          <el-icon :size="20"><VideoCamera /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">总集数</div>
          <div class="kpi-value">{{ formatNum(kpi.total_episodes) }}</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background: rgba(230,162,60,0.1); color: #e6a23c;">
          <el-icon :size="20"><Money /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">充值总额</div>
          <div class="kpi-value kpi-money">{{ formatMoney(kpi.total_recharge_amount) }}</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background: rgba(245,108,108,0.1); color: #f56c6c;">
          <el-icon :size="20"><User /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">充值用户</div>
          <div class="kpi-value">{{ formatNum(kpi.recharge_users) }}</div>
        </div>
      </div>
      <div class="kpi-card">
        <div class="kpi-icon" style="background: rgba(144,147,153,0.1); color: #909399;">
          <el-icon :size="20"><TrendCharts /></el-icon>
        </div>
        <div class="kpi-info">
          <div class="kpi-label">均单剧收入</div>
          <div class="kpi-value kpi-money">{{ formatMoney(kpi.avg_per_drama) }}</div>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <div class="table-wrapper">
        <el-table
          v-loading="loading"
          :data="displayTableData"
          border
          stripe
          size="small"
          :row-class-name="tableRowClassName"
          :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold' }"
          class="table-flex"
        >
          <el-table-column label="排名" width="50" align="center" fixed="left">
            <template #default="{ $index }">
              <span v-if="isSummaryRow($index)" class="summary-label">汇总</span>
              <span v-else>{{ getRank($index) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="drama_name" label="短剧名称" min-width="130" show-overflow-tooltip fixed="left">
            <template #default="{ row, $index }">
              {{ isSummaryRow($index) ? '-' : row.drama_name }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="65" align="center">
            <template #default="{ row, $index }">
              <template v-if="!isSummaryRow($index)">
                <el-tag v-if="row.status === 'published'" type="success" size="small">上架</el-tag>
                <el-tag v-else-if="row.status === 'draft'" type="info" size="small">草稿</el-tag>
                <el-tag v-else type="danger" size="small">下架</el-tag>
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="category_name" label="分类" width="70" align="center" show-overflow-tooltip>
            <template #default="{ row, $index }">
              {{ isSummaryRow($index) ? '-' : (row.category_name || '-') }}
            </template>
          </el-table-column>
          <el-table-column prop="total_episodes" label="总集数" width="60" align="center">
            <template #default="{ row, $index }">
              {{ isSummaryRow($index) ? '-' : row.total_episodes }}
            </template>
          </el-table-column>
          <el-table-column prop="recharge_amount" label="充值金额" width="90" align="center" sortable>
            <template #default="{ row }">
              <span class="num-money">{{ formatNumber(row.recharge_amount, 2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="recharge_count" label="充值笔数" width="75" align="center">
            <template #default="{ row }">{{ row.recharge_count ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="recharge_users" label="充值用户" width="75" align="center">
            <template #default="{ row }">{{ row.recharge_users ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="first_recharge_count" label="首充数" width="60" align="center">
            <template #default="{ row }">{{ row.first_recharge_count ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="first_recharge_rate" label="首充率" width="65" align="center">
            <template #default="{ row }">{{ formatPercent(row.first_recharge_rate) }}</template>
          </el-table-column>
          <el-table-column prop="avg_recharge_per_user" label="人均充值" width="80" align="center">
            <template #default="{ row }">{{ formatNumber(row.avg_recharge_per_user, 2) }}</template>
          </el-table-column>
          <el-table-column prop="link_count" label="推广链接" width="70" align="center">
            <template #default="{ row, $index }">
              {{ isSummaryRow($index) ? '-' : (row.link_count ?? 0) }}
            </template>
          </el-table-column>
          <el-table-column prop="view_count" label="播放量" width="70" align="center">
            <template #default="{ row, $index }">
              {{ isSummaryRow($index) ? '-' : formatNum(row.view_count) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70" fixed="right" align="center">
            <template #default="{ row, $index }">
              <el-button v-if="!isSummaryRow($index)" type="primary" link size="small" @click="showTrendChart(row)">趋势图</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <el-pagination
        class="compact-pagination"
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        background
        size="small"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 图表区 -->
    <el-row :gutter="12" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <el-icon><Histogram /></el-icon>
              <span>充值 TOP10 排行</span>
            </div>
          </template>
          <div ref="top10ChartRef" class="chart-inner" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <el-icon><PieChart /></el-icon>
              <span>分类充值分布</span>
            </div>
          </template>
          <div ref="categoryChartRef" class="chart-inner" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 单剧趋势弹窗 -->
    <el-dialog
      v-model="trendDialogVisible"
      :title="trendTitle"
      width="800px"
      destroy-on-close
      @closed="onTrendDialogClosed"
    >
      <div ref="trendChartRef" class="trend-chart-box" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Search, Refresh, Download,
  Film, VideoCamera, Money, User, TrendCharts,
  Histogram, PieChart,
} from '@element-plus/icons-vue'
import echarts from '@/utils/echarts'
import { getDramaStats, getDramaDailyRecharge } from '@/api/dashboard'
import { getCategories } from '@/api/category'

const props = defineProps({ active: { type: Boolean, default: false } })

const loading = ref(false)
const exportLoading = ref(false)
const tableData = ref([])
const summary = ref(null)
const categoryList = ref([])

const kpi = ref({
  online_dramas: 0, total_episodes: 0,
  total_recharge_amount: 0, recharge_users: 0, avg_per_drama: 0,
})

const dateRange = ref([])
const filters = reactive({ dramaName: '', categoryId: null })
const pagination = reactive({ page: 1, pageSize: 20, total: 0 })

const dateShortcuts = [
  { text: '今天', value: () => { const t = todayStr(); return [t, t] } },
  { text: '昨天', value: () => { const y = daysAgoStr(1); return [y, y] } },
  { text: '近7天', value: () => [daysAgoStr(6), todayStr()] },
  { text: '近30天', value: () => [daysAgoStr(29), todayStr()] },
]

const displayTableData = computed(() => {
  const rows = tableData.value || []
  if (pagination.page !== 1 || !summary.value) return rows
  return [summary.value, ...rows]
})

function isSummaryRow(index) {
  return pagination.page === 1 && summary.value && index === 0
}

function getRank(index) {
  const offset = (pagination.page - 1) * pagination.pageSize
  const idx = summary.value && pagination.page === 1 ? index - 1 : index
  return offset + idx + 1
}

/* ---- 格式化 ---- */

function todayStr() { return new Date().toISOString().slice(0, 10) }
function daysAgoStr(n) { const d = new Date(); d.setDate(d.getDate() - n); return d.toISOString().slice(0, 10) }

function formatNum(v) {
  if (v == null) return '0'
  const n = Number(v)
  if (!Number.isFinite(n)) return '0'
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return n.toLocaleString('zh-CN')
}

function formatMoney(v) {
  if (v == null) return '0.00'
  const n = Number(v)
  if (!Number.isFinite(n)) return '0.00'
  if (n >= 10000) return (n / 10000).toFixed(2) + 'w'
  return n.toFixed(2)
}

function formatNumber(v, dec = 2) {
  if (v == null) return '-'
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return n.toFixed(dec)
}

function formatPercent(v) {
  if (v == null) return '-'
  const n = Number(v)
  if (!Number.isFinite(n)) return '-'
  return (n * 100).toFixed(2) + '%'
}

/* ---- 数据加载 ---- */

let loaded = false

async function loadCategories() {
  try {
    const res = await getCategories()
    if (res.code === 0 && Array.isArray(res.data)) {
      categoryList.value = res.data
    }
  } catch (e) { console.error(e) }
}

async function loadData(showLoading = true) {
  if (!dateRange.value?.length || dateRange.value.length !== 2) return
  if (showLoading) loading.value = true
  try {
    const res = await getDramaStats({
      start_date: dateRange.value[0],
      end_date: dateRange.value[1],
      drama_name: filters.dramaName || undefined,
      category_id: filters.categoryId || undefined,
      page: pagination.page,
      page_size: pagination.pageSize,
    })
    if (res.code === 0 && res.data) {
      const d = res.data
      tableData.value = d.list || []
      summary.value = d.summary ?? null
      pagination.total = Number(d.total ?? 0)
      kpi.value = d.kpi || kpi.value
      initCharts(d.top10 || [], d.category_dist || [])
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载短剧数据失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { pagination.page = 1; loadData() }
function handleReset() {
  dateRange.value = [daysAgoStr(6), todayStr()]
  filters.dramaName = ''
  filters.categoryId = null
  pagination.page = 1
  loadData()
}
function handleSizeChange() { pagination.page = 1; loadData() }
function handlePageChange() { loadData() }
function tableRowClassName({ rowIndex }) { return isSummaryRow(rowIndex) ? 'summary-row' : '' }

/* ---- 导出 ---- */

async function handleExport() {
  if (!dateRange.value?.length || dateRange.value.length !== 2) { ElMessage.warning('请先选择日期'); return }
  exportLoading.value = true
  try {
    const res = await getDramaStats({
      start_date: dateRange.value[0], end_date: dateRange.value[1],
      drama_name: filters.dramaName || undefined,
      category_id: filters.categoryId || undefined,
      page: 1, page_size: 10000,
    })
    if (res.code !== 0 || !res.data) { ElMessage.error('导出失败'); return }
    const rows = res.data.list || []
    const cols = [
      { key: 'drama_name', label: '短剧名称' },
      { key: 'status', label: '状态' },
      { key: 'category_name', label: '分类' },
      { key: 'total_episodes', label: '总集数' },
      { key: 'recharge_amount', label: '充值金额' },
      { key: 'recharge_count', label: '充值笔数' },
      { key: 'recharge_users', label: '充值用户' },
      { key: 'first_recharge_count', label: '首充数' },
      { key: 'first_recharge_rate', label: '首充率' },
      { key: 'avg_recharge_per_user', label: '人均充值' },
      { key: 'link_count', label: '推广链接数' },
      { key: 'view_count', label: '播放量' },
    ]
    const esc = (v) => { if (v == null) return ''; const s = String(v); return s.includes(',') || s.includes('"') || s.includes('\n') ? '"' + s.replace(/"/g, '""') + '"' : s }
    const lines = [cols.map(c => esc(c.label)).join(',')]
    for (const row of rows) {
      lines.push(cols.map(c => {
        if (c.key === 'first_recharge_rate') return esc(formatPercent(row[c.key]))
        if (c.key === 'status') return esc(row[c.key] === 'published' ? '上架' : row[c.key] === 'draft' ? '草稿' : '下架')
        return esc(row[c.key] ?? '')
      }).join(','))
    }
    const blob = new Blob(['\uFEFF' + lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `短剧概览_${dateRange.value[0]}_${dateRange.value[1]}.csv`
    document.body.appendChild(a); a.click(); document.body.removeChild(a)
    URL.revokeObjectURL(url)
    ElMessage.success(`导出成功，共 ${rows.length} 条`)
  } catch (e) { console.error(e); ElMessage.error('导出失败') }
  finally { exportLoading.value = false }
}

/* ---- 图表 ---- */

const top10ChartRef = ref(null)
const categoryChartRef = ref(null)
let top10Chart = null
let categoryChart = null

function initCharts(top10Data, categoryData) {
  nextTick(() => {
    initTop10Chart(top10Data)
    initCategoryChart(categoryData)
  })
}

function initTop10Chart(data) {
  if (!top10ChartRef.value) return
  if (top10Chart) top10Chart.dispose()
  top10Chart = echarts.init(top10ChartRef.value)
  const names = data.map(d => d.drama_name || '').reverse()
  const values = data.map(d => Number(d.recharge_amount || 0)).reverse()
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9', '#1890ff', '#52c41a', '#fa8c16']
  top10Chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 30, bottom: 12, top: 12 },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: names, axisLabel: { fontSize: 11, width: 90, overflow: 'truncate' } },
    series: [{
      type: 'bar', data: values.map((v, i) => ({ value: v, itemStyle: { color: colors[i % colors.length] } })),
      barMaxWidth: 20,
      label: { show: true, position: 'right', fontSize: 10, formatter: '{c}' },
    }],
  })
}

function initCategoryChart(data) {
  if (!categoryChartRef.value) return
  if (categoryChart) categoryChart.dispose()
  categoryChart = echarts.init(categoryChartRef.value)
  const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c', '#909399', '#b37feb', '#36cfc9', '#1890ff']
  const chartData = data.map((item, i) => ({
    value: Number(item.amount || 0),
    name: item.name || '未分类',
    itemStyle: { color: colors[i % colors.length] },
  }))
  categoryChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 4, top: 'center', textStyle: { fontSize: 11 } },
    series: [{
      name: '分类分布', type: 'pie', radius: ['40%', '65%'], center: ['60%', '50%'],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 12, fontWeight: 'bold' } },
      data: chartData.length ? chartData : [{ value: 0, name: '暂无数据', itemStyle: { color: '#dcdfe6' } }],
    }],
  })
}

function resizeCharts() { top10Chart?.resize(); categoryChart?.resize() }
function disposeCharts() { top10Chart?.dispose(); categoryChart?.dispose(); top10Chart = null; categoryChart = null }

/* ---- 趋势弹窗 ---- */

const trendDialogVisible = ref(false)
const trendTitle = ref('')
const trendChartRef = ref(null)
let trendChart = null

async function showTrendChart(row) {
  if (!dateRange.value?.length || dateRange.value.length !== 2) { ElMessage.warning('请选择日期'); return }
  trendTitle.value = `${row.drama_name} — 充值趋势`
  trendDialogVisible.value = true
  await nextTick()
  try {
    const res = await getDramaDailyRecharge({
      drama_id: row.drama_id,
      start_date: dateRange.value[0],
      end_date: dateRange.value[1],
    })
    if (res.code === 0 && res.data) {
      renderTrendChart(res.data.chartData || [])
    }
  } catch (e) { console.error(e); ElMessage.error('加载趋势数据失败') }
}

function renderTrendChart(data) {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()
  trendChart = echarts.init(trendChartRef.value)
  const dates = data.map(d => d.date)
  const amounts = data.map(d => Number(d.amount || 0))
  const counts = data.map(d => Number(d.count || 0))
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['充值金额', '充值笔数'], top: 4, textStyle: { fontSize: 11 } },
    grid: { left: 52, right: 52, bottom: 36, top: 40 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 10, rotate: dates.length > 14 ? 30 : 0 } },
    yAxis: [
      { type: 'value', name: '金额', axisLabel: { fontSize: 10 }, nameTextStyle: { fontSize: 10 } },
      { type: 'value', name: '笔数', splitLine: { show: false }, axisLabel: { fontSize: 10 }, nameTextStyle: { fontSize: 10 } },
    ],
    series: [
      {
        name: '充值金额', type: 'line', smooth: true, data: amounts,
        itemStyle: { color: '#e6a23c' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [{ offset: 0, color: 'rgba(230,162,60,0.3)' }, { offset: 1, color: 'rgba(230,162,60,0.05)' }] } },
      },
      { name: '充值笔数', type: 'bar', yAxisIndex: 1, data: counts, itemStyle: { color: '#409eff' }, barMaxWidth: 16 },
    ],
  })
}

function onTrendDialogClosed() { trendChart?.dispose(); trendChart = null }

/* ---- 生命周期 ---- */

watch(() => props.active, (val) => {
  if (val && !loaded) {
    loaded = true
    dateRange.value = [daysAgoStr(6), todayStr()]
    loadCategories()
    loadData()
  }
  if (val) nextTick(() => resizeCharts())
  if (!val) disposeCharts()
})

onMounted(() => {
  window.addEventListener('resize', resizeCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts)
  disposeCharts()
})
</script>

<style scoped>
.drama-overview {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  padding: 0 2px 12px;
  gap: 8px;
}

.filter-card { flex-shrink: 0; border-radius: 8px; }
.filter-form { margin-bottom: 0 !important; }
.filter-form :deep(.el-form-item) { margin-bottom: 0 !important; }
.filter-daterange { width: 260px; }
.filter-w-sm { width: 160px; }
.filter-w-xs { width: 120px; }

/* KPI */
.kpi-cards {
  flex-shrink: 0;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 10px;
}
.kpi-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  transition: box-shadow 0.2s, transform 0.2s;
}
.kpi-card:hover { box-shadow: 0 4px 12px rgba(0,0,0,0.1); transform: translateY(-1px); }
.kpi-icon {
  flex-shrink: 0; width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
}
.kpi-info { min-width: 0; }
.kpi-label { font-size: 12px; color: #909399; margin-bottom: 4px; }
.kpi-value { font-size: 20px; font-weight: 700; color: #303133; line-height: 1.2; white-space: nowrap; }
.kpi-value.kpi-money { color: #e6a23c; }

/* 表格 */
.table-card {
  flex-shrink: 0;
  display: flex; flex-direction: column;
  border-radius: 8px; background: #fff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  border: 1px solid #ebeef5;
}
.table-wrapper {
  flex: 1 1 auto; min-height: 0;
  display: flex; flex-direction: column; overflow: hidden;
}
.table-flex { flex: 1 1 auto; min-height: 0; }
.summary-label { font-weight: 700; color: #409eff; }
:deep(.summary-row) { background-color: #f5f7fa !important; font-weight: 600; }
:deep(.summary-row td) { background-color: #f5f7fa !important; }
.num-money { color: #e6a23c; font-weight: 600; }
:deep(.el-table) { font-size: 11px; }
:deep(.el-table th.el-table__cell) { font-size: 11px; padding: 3px 0; }
:deep(.el-table td.el-table__cell) { padding: 2px 0; }
:deep(.el-table .cell) { padding-left: 3px; padding-right: 3px; line-height: 1.35; }
:deep(.compact-pagination) { font-size: 11px; margin: 6px 8px; }

/* 图表 */
.chart-row { flex-shrink: 0; }
.chart-card { border-radius: 10px; }
.chart-card :deep(.el-card__body) { padding: 8px 10px !important; }
.chart-card :deep(.el-card__header) { padding: 8px 12px !important; }
.chart-header { display: flex; align-items: center; gap: 6px; font-weight: 600; font-size: 13px; }
.chart-inner { width: 100%; height: 260px; }

/* 趋势弹窗 */
.trend-chart-box { width: 100%; height: 380px; }
</style>
