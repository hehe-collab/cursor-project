<template>
  <div class="stats-page">
    <el-card shadow="never" class="filter-card">
      <template #header><span>统计</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="剧ID">
          <el-input v-model="filter.dramaId" placeholder="请输入" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="投放媒体">
          <el-select v-model="filter.media" placeholder="请选择" clearable style="width:180px">
            <el-option label="TikTok" value="TikTok" />
            <el-option label="Facebook" value="Facebook" />
            <el-option label="Google" value="Google" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onQuery">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card shadow="never" class="table-card">
      <template #header><span>数据明细</span></template>
      <el-table :data="tableData" v-loading="loading" stripe style="width:100%">
        <template #empty>
          <el-empty description="暂无统计数据" />
        </template>
        <el-table-column prop="time" label="时间" width="120" />
        <el-table-column prop="orderCount" label="订单数" width="100" />
        <el-table-column prop="rechargeAmount" label="充值金额" width="120" />
        <el-table-column prop="consume" label="消耗" width="120" />
        <el-table-column prop="roi" label="ROI" width="100" />
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="statsTotal"
        layout="total, sizes, prev, pager, next"
        @current-change="loadData"
        @size-change="loadData"
        style="margin-top: 16px"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../api/request'

const loading = ref(false)
const filter = reactive({ dramaId: '', media: '' })
const query = reactive({ page: 1, pageSize: 10 })
const statsTotal = ref(0)
const tableData = ref([])

async function loadData() {
  loading.value = true
  try {
    const params = { ...filter, page: query.page, pageSize: query.pageSize }
    const res = await request.get('/stats', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    tableData.value = res.data?.list || []
    statsTotal.value = res.data?.total ?? tableData.value.length
  } finally {
    loading.value = false
  }
}

function onQuery() {
  loadData()
}

function onReset() {
  filter.dramaId = ''
  filter.media = ''
  query.page = 1
  loadData()
}

onMounted(loadData)
</script>

<style scoped>
.stats-page { display: flex; flex-direction: column; gap: 16px; }
.filter-card, .table-card { border-radius: 4px; }
.filter-form { margin: 0; }
</style>
