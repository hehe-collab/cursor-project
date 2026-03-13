<template>
  <div class="dashboard">
    <el-card>
      <template #header><span>看板</span></template>

      <!-- 今日概览（DramaBagus 风格） -->
      <div class="overview-row">
        <div class="overview-card">
          <div class="overview-title">今日用户数</div>
          <div class="overview-value">{{ formatNum(stats.todayUsers) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-title">今日金额</div>
          <div class="overview-value">{{ formatNum(stats.todayAmount) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-title">今日订单</div>
          <div class="overview-value">{{ formatNum(stats.todayOrders) }}</div>
        </div>
        <div class="overview-card">
          <div class="overview-title">未结算</div>
          <div class="overview-value">{{ formatNum(stats.unsettled) }}</div>
        </div>
      </div>

      <!-- KPI 卡片：消耗、时速、充值、ROI、利润 -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-title">消耗</div>
          <div class="kpi-value">${{ formatMoney(stats.spend) }}</div>
          <div class="kpi-sub">IDR {{ formatNum(stats.spend * (stats.exchangeRate || 16779)) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">时速</div>
          <div class="kpi-value">${{ formatMoney(stats.hourlyRate) }}</div>
          <div class="kpi-sub">IDR {{ formatNum(stats.hourlyRate * (stats.exchangeRate || 16779)) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">充值</div>
          <div class="kpi-value">${{ formatMoney(stats.recharge) }}</div>
          <div class="kpi-sub">IDR {{ formatNum(stats.recharge * (stats.exchangeRate || 16779)) }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">ROI</div>
          <div class="kpi-value" :class="{ red: (stats.roi || 0) < 1 }">{{ (stats.roi || 0).toFixed(4) }}</div>
          <div class="kpi-sub">收入/消耗</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">利润</div>
          <div class="kpi-value" :class="{ red: (stats.profit || 0) < 0 }">${{ formatMoney(stats.profit) }}</div>
          <div class="kpi-sub">IDR {{ formatNum((stats.profit || 0) * (stats.exchangeRate || 16779)) }}</div>
        </div>
        <div class="kpi-card exchange-card">
          <div class="kpi-title">汇率 (BCA)</div>
          <div class="kpi-value small">1 USD = {{ formatNum(stats.exchangeRate || 16779) }} IDR</div>
          <div class="kpi-sub">{{ stats.exchangeTime || '-' }}</div>
        </div>
      </div>

      <!-- 筛选区域 -->
      <el-form :inline="true" class="filter-form" @submit.prevent="onSearch">
        <el-form-item label="推广ID">
          <el-input v-model="filter.promoteId" placeholder="请输入推广ID" clearable style="width:140px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="推广名称">
          <el-input v-model="filter.promoteName" placeholder="请输入推广名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="剧ID">
          <el-input v-model="filter.dramaId" placeholder="请输入剧ID" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="剧名称">
          <el-input v-model="filter.dramaName" placeholder="请输入剧名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="账户">
          <el-input v-model="filter.account" placeholder="请输入账户" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="投放媒体">
          <el-select v-model="filter.media" placeholder="请选择" clearable style="width:120px">
            <el-option label="TikTok" value="TikTok" />
            <el-option label="Facebook" value="Facebook" />
            <el-option label="Google" value="Google" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="filter.country" placeholder="请选择" clearable style="width:100px">
            <el-option label="印尼" value="ID" />
            <el-option label="美国" value="US" />
            <el-option label="泰国" value="TH" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="filter.dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格区域 -->
      <div class="table-toolbar">
        <el-button @click="onExport"><el-icon><Download /></el-icon> 导出</el-button>
        <span class="local-time">当地时间: {{ localTime }}</span>
      </div>
      <el-table :data="tableData" v-loading="loading" stripe class="dashboard-table">
        <template #empty>
          <el-empty description="暂无数据" />
        </template>
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column prop="promote_id" label="推广ID" width="120" />
        <el-table-column prop="promote_name" label="推广名称" min-width="140" />
        <el-table-column prop="account" label="账户" width="100">
          <template #default="{ row }">
            <span v-if="row.account && row.account !== '-' && !row.is_summary" class="account-link">...{{ row.account }}</span>
            <span v-else>{{ row.account || (row.is_summary ? '' : '-') }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="series" label="系列广告组" width="100">
          <template #default="{ row }">
            <el-button v-if="!row.is_summary" type="success" size="small" link>调试</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="spend" label="消耗" width="90" sortable>
          <template #default="{ row }">{{ row.promote_name === '汇总' ? '-' : formatMoney(row.spend) }}</template>
        </el-table-column>
        <el-table-column prop="hourly_rate" label="时速" width="90" sortable>
          <template #default="{ row }">{{ row.promote_name === '汇总' ? '-' : formatMoney(row.hourly_rate) }}</template>
        </el-table-column>
        <el-table-column prop="roi" label="ROI" width="90" sortable>
          <template #default="{ row }">
            <span v-if="row.promote_name === '汇总'">-</span>
            <span v-else :class="{ red: (row.roi || 0) < 1 }">{{ (row.roi || 0).toFixed(4) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="users" label="用户数" width="80">
          <template #default="{ row }">{{ row.promote_name === '汇总' ? '-' : row.users }}</template>
        </el-table-column>
        <el-table-column prop="recharge" label="充值金额" width="100" sortable>
          <template #default="{ row }">{{ row.is_summary && row.promote_name === '汇总' ? '-' : formatNum(row.recharge) }}</template>
        </el-table-column>
        <el-table-column prop="profit" label="利润" width="90" sortable>
          <template #default="{ row }">
            <span v-if="row.promote_name === '汇总'">-</span>
            <span v-else :class="{ red: (row.profit || 0) < 0, green: (row.profit || 0) > 0 }">{{ formatMoney(row.profit) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="orders" label="订单数" width="80">
          <template #default="{ row }">{{ row.promote_name === '汇总' ? '-' : row.orders }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="showProfitChart(row)">利润图</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 利润图弹窗 -->
    <el-dialog v-model="profitChartVisible" title="利润趋势" width="720px" destroy-on-close>
      <div v-if="profitChartData.length" class="profit-chart">
        <div class="profit-bars">
          <div v-for="(item, i) in profitChartData" :key="i" class="bar-row">
            <span class="bar-label">{{ item.label }}</span>
            <div class="bar-track">
              <div class="bar-fill" :class="{ negative: item.profit < 0 }" :style="{ width: barWidth(item) }" />
            </div>
            <span class="bar-value" :class="{ red: item.profit < 0, green: item.profit > 0 }">{{ formatMoney(item.profit) }}</span>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无利润数据" />
      <template #footer>
        <el-button @click="profitChartVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { exportToCSV } from '../utils/export'
import { formatNum, formatMoney } from '../utils/format'

const loading = ref(false)
const stats = ref({
  spend: 0,
  hourlyRate: 0,
  recharge: 0,
  roi: 0,
  profit: 0,
  exchangeRate: 16779,
  exchangeTime: '',
  todayUsers: 0,
  todayAmount: 0,
  todayOrders: 0,
  unsettled: 0,
})
const tableData = ref([])
const profitChartVisible = ref(false)
const profitChartData = ref([])
const filter = reactive({
  promoteId: '',
  promoteName: '',
  dramaId: '',
  dramaName: '',
  account: '',
  media: '',
  country: '',
  dateRange: null,
})

const localTime = computed(() => {
  const d = new Date()
  return d.toLocaleString('zh-CN', { hour12: false }).replace(/\//g, '-')
})

async function loadData() {
  loading.value = true
  try {
    const params = {}
    if (filter.promoteId) params.promoteId = filter.promoteId
    if (filter.promoteName) params.promoteName = filter.promoteName
    if (filter.dramaId) params.dramaId = filter.dramaId
    if (filter.dramaName) params.dramaName = filter.dramaName
    if (filter.account) params.account = filter.account
    if (filter.media) params.media = filter.media
    if (filter.country) params.country = filter.country
    if (filter.dateRange && filter.dateRange.length === 2) {
      params.dateStart = filter.dateRange[0]
      params.dateEnd = filter.dateRange[1]
    }
    const res = await request.get('/dashboard/stats', { params }).catch(() => ({ data: {} }))
    const d = res.data || {}
    stats.value = {
      spend: d.spend ?? 0,
      hourlyRate: d.hourlyRate ?? 0,
      recharge: d.recharge ?? 0,
      roi: d.roi ?? 0,
      profit: d.profit ?? 0,
      exchangeRate: d.exchangeRate ?? 16779,
      exchangeTime: d.exchangeTime ?? '',
      todayUsers: d.todayUsers ?? 0,
      todayAmount: d.todayAmount ?? 0,
      todayOrders: d.todayOrders ?? 0,
      unsettled: d.unsettled ?? 0,
    }
    tableData.value = d.tableData || []
  } finally {
    loading.value = false
  }
}

function onSearch() {
  loadData()
}

function onReset() {
  Object.assign(filter, { promoteId: '', promoteName: '', dramaId: '', dramaName: '', account: '', media: '', country: '', dateRange: null })
  loadData()
}

function showProfitChart(row) {
  const data = tableData.value.filter(r => !r.is_summary && r.promote_id)
  if (!data.length) {
    profitChartData.value = []
  } else {
    const byPromote = {}
    data.forEach(r => {
      const key = r.promote_id || r.promote_name || '-'
      if (!byPromote[key]) byPromote[key] = { label: key, profit: 0 }
      byPromote[key].profit += r.profit || 0
    })
    profitChartData.value = Object.values(byPromote).sort((a, b) => b.profit - a.profit).slice(0, 10)
  }
  profitChartVisible.value = true
}

function barWidth(item) {
  const max = Math.max(...profitChartData.value.map(x => Math.abs(x.profit)), 1)
  const pct = Math.min(100, (Math.abs(item.profit) / max) * 100)
  return pct + '%'
}

function onExport() {
  const data = tableData.value.filter(r => !r.is_summary)
  if (!data.length) return ElMessage.warning('暂无数据可导出')
  exportToCSV(data, [
    { prop: 'date', label: '日期' },
    { prop: 'promote_id', label: '推广ID' },
    { prop: 'promote_name', label: '推广名称' },
    { prop: 'account', label: '账户' },
    { prop: 'spend', label: '消耗' },
    { prop: 'recharge', label: '充值金额' },
    { prop: 'profit', label: '利润' },
    { prop: 'orders', label: '订单数' },
  ], `看板数据_${new Date().toISOString().slice(0, 10)}.csv`)
  ElMessage.success('导出成功')
}

onMounted(loadData)
</script>

<style scoped>
.dashboard { padding: 0; }
.overview-row {
  display: flex;
  gap: 20px;
  margin-bottom: 28px;
  flex-wrap: wrap;
}
.overview-card {
  flex: 1;
  min-width: 180px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  color: #fff;
  box-shadow: 0 4px 16px rgba(102,126,234,0.35);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.overview-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(102,126,234,0.4);
}
.overview-card:nth-child(2) {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  box-shadow: 0 4px 16px rgba(245,87,108,0.35);
}
.overview-card:nth-child(3) {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  box-shadow: 0 4px 16px rgba(79,172,254,0.35);
}
.overview-card:nth-child(4) {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  box-shadow: 0 4px 16px rgba(67,233,123,0.35);
}
.overview-title { font-size: 13px; opacity: 0.95; margin-bottom: 10px; letter-spacing: 0.5px; }
.overview-value { font-size: 26px; font-weight: 700; letter-spacing: 0.5px; }
.kpi-row {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  margin-bottom: 28px;
}
.kpi-card {
  flex: 1;
  min-width: 150px;
  padding: 18px 22px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  transition: all 0.2s ease;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.kpi-card:hover {
  border-color: #409EFF;
  box-shadow: 0 4px 12px rgba(64,158,255,0.12);
}
.kpi-card.exchange-card { min-width: 220px; }
.kpi-title { font-size: 13px; color: #909399; margin-bottom: 10px; font-weight: 500; }
.kpi-value { font-size: 22px; font-weight: 700; color: #303133; letter-spacing: 0.3px; }
.kpi-value.small { font-size: 15px; }
.kpi-value.red { color: #f56c6c; }
.kpi-value.green { color: #67c23a; }
.kpi-sub { font-size: 12px; color: #909399; margin-top: 6px; }
.filter-form { margin-bottom: 16px; }
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.local-time { font-size: 13px; color: #909399; }
.account-link { color: #409EFF; cursor: pointer; text-decoration: underline; }
.dashboard-table .red { color: #f56c6c; }
.dashboard-table .green { color: #67c23a; }
.profit-chart { padding: 12px 0; }
.profit-bars { display: flex; flex-direction: column; gap: 12px; }
.bar-row { display: flex; align-items: center; gap: 12px; }
.bar-label { width: 120px; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.bar-track { flex: 1; height: 12px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.bar-fill { height: 100%; background: #67c23a; border-radius: 4px; min-width: 2px; transition: width 0.3s; }
.bar-fill.negative { background: #f56c6c; }
.bar-value { width: 80px; text-align: right; font-size: 13px; font-weight: 500; }
.bar-value.red { color: #f56c6c; }
.bar-value.green { color: #67c23a; }
</style>
