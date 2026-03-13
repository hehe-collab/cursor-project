<template>
  <div class="recharge-page">
    <el-card>
      <template #header><span>充值记录</span></template>
      <el-form :inline="true" class="filter-form" @submit.prevent="onQuery">
        <el-form-item label="用户ID">
          <el-input v-model="query.userId" placeholder="请输入用户ID" clearable style="width:140px" @keyup.enter="onQuery" />
        </el-form-item>
        <el-form-item label="交易币种">
          <el-select v-model="query.currency" placeholder="请选择" clearable style="width:120px">
            <el-option label="全部" value="" />
            <el-option label="USD" value="USD" />
            <el-option label="IDR" value="IDR" />
          </el-select>
        </el-form-item>
        <el-form-item label="商户ID">
          <el-input v-model="query.merchantId" placeholder="请输入商户ID" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="请输入订单号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="外部订单号">
          <el-input v-model="query.externalOrderNo" placeholder="请输入外部订单号" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="推广ID">
          <el-input v-model="query.promoteId" placeholder="请输入推广ID" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="支付结果">
          <el-select v-model="query.payResult" placeholder="请选择" clearable style="width:120px">
            <el-option label="成功" value="success" />
            <el-option label="失败" value="fail" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-input v-model="query.country" placeholder="请输入" clearable style="width:100px" />
        </el-form-item>
        <el-form-item label="注册时间">
          <el-date-picker v-model="query.registerTime" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width:240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onQuery">查询</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button @click="onExport">导出</el-button>
        </el-form-item>
      </el-form>
      <div v-if="totalAmount > 0" class="summary">充值金额：{{ formatMoney(totalAmount) }}</div>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无充值记录" />
        </template>
        <el-table-column prop="user_id" label="用户ID" width="90" />
        <el-table-column prop="drama_name" label="剧名" />
        <el-table-column prop="amount" label="充值金额" width="100" />
        <el-table-column prop="pay_status" label="支付状态" width="90" />
        <el-table-column prop="is_first" label="是否首充" width="90" />
        <el-table-column prop="is_callback" label="是否回传" width="90" />
        <el-table-column prop="promote_link_id" label="推广链接ID" width="120" />
        <el-table-column prop="order_no" label="订单号" width="180" />
        <el-table-column prop="local_time" label="当地注册时间" width="180" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { exportToCSV } from '../utils/export'
import { formatMoney } from '../utils/format'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const totalAmount = ref(0)
const query = reactive({
  page: 1,
  pageSize: 10,
  userId: '',
  currency: '',
  merchantId: '',
  orderNo: '',
  externalOrderNo: '',
  promoteId: '',
  payResult: '',
  country: '',
  registerTime: null,
})

async function loadList() {
  loading.value = true
  try {
    const params = { ...query }
    if (query.registerTime && query.registerTime.length === 2) {
      params.registerStart = query.registerTime[0]
      params.registerEnd = query.registerTime[1]
    }
    delete params.registerTime
    const res = await request.get('/recharge', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
    totalAmount.value = res.data?.totalAmount ?? list.value.reduce((s, r) => s + (r.amount || 0), 0)
  } finally {
    loading.value = false
  }
}

function onQuery() {
  query.page = 1
  loadList()
}

function onReset() {
  Object.assign(query, { page: 1, userId: '', currency: '', merchantId: '', orderNo: '', externalOrderNo: '', promoteId: '', payResult: '', country: '', registerTime: null })
  loadList()
}

async function onExport() {
  try {
    const params = { pageSize: 2000 }
    if (query.userId) params.userId = query.userId
    if (query.currency) params.currency = query.currency
    if (query.promoteId) params.promoteId = query.promoteId
    if (query.registerTime?.length === 2) { params.registerStart = query.registerTime[0]; params.registerEnd = query.registerTime[1] }
    const res = await request.get('/recharge', { params }).catch(() => ({ data: { list: [] } }))
    const data = res.data?.list || []
    if (!data.length) return ElMessage.warning('暂无数据可导出')
    exportToCSV(data, [
      { prop: 'user_id', label: '用户ID' },
      { prop: 'drama_name', label: '剧名' },
      { prop: 'amount', label: '充值金额' },
      { prop: 'pay_status', label: '支付状态' },
      { prop: 'order_no', label: '订单号' },
      { prop: 'created_at', label: '创建时间' },
    ], `充值记录_${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
.summary { margin-bottom: 16px; font-weight: bold; color: #409EFF; }
</style>
