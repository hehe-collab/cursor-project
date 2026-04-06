<template>
  <div class="user-container page-list-layout">
    <el-card shadow="never" class="filter-card">
      <el-form :model="filterForm" class="filter-form user-filter-form" inline size="small" label-position="left">
        <el-form-item label="用户ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.userId" placeholder="用户ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label="用户名" label-width="60px">
          <div class="filter-item-s">
            <el-input v-model="filterForm.username" placeholder="用户名" clearable />
          </div>
        </el-form-item>
        <el-form-item label="Token" label-width="50px">
          <div class="filter-item-m">
            <el-input v-model="filterForm.token" placeholder="Token" clearable />
          </div>
        </el-form-item>
        <el-form-item label="推广ID" label-width="60px">
          <div class="filter-item-xs">
            <el-input v-model="filterForm.promotionId" placeholder="推广ID" clearable />
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
              导出 Excel
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        element-loading-text="加载中..."
        :data="tableData"
        border
        stripe
        height="calc(100vh - 220px)"
      >
        <template #empty>
          <el-empty description="暂无用户数据" />
        </template>
        <el-table-column prop="user_id" label="用户ID" width="120" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" show-overflow-tooltip />
        <el-table-column prop="token" label="Token" min-width="200" show-overflow-tooltip />
        <el-table-column prop="promotion_id" label="推广ID" width="180" show-overflow-tooltip />
        <el-table-column
          prop="register_time"
          label="注册时间"
          width="180"
          align="center"
        >
          <template #default="{ row }">{{ formatTime(row.register_time || row.created_at) }}</template>
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

    <el-dialog v-model="detailVisible" title="用户详情" width="600px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户ID">{{ currentUser.user_id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="Token">{{ currentUser.token || '-' }}</el-descriptions-item>
        <el-descriptions-item label="推广ID">{{ currentUser.promotion_id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ formatTime(currentUser.register_time || currentUser.created_at) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft, Download } from '@element-plus/icons-vue'
import request from '../api/request'
import { exportJsonToXlsx } from '../utils/excelExport'
import { useCountries } from '@/composables/useCountries'

const { countries, countryFilterOptions } = useCountries()

const filterForm = ref({
  userId: '',
  username: '',
  token: '',
  promotionId: '',
  country: '',
  dateRange: [],
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
const currentUser = ref({})

function formatTime(t) {
  if (!t) return '-'
  const d = new Date(t)
  if (Number.isNaN(d.getTime())) return t
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`
}

function buildUserListParams(page, pageSize) {
  const params = {
    user_id: filterForm.value.userId,
    username: filterForm.value.username,
    token: filterForm.value.token,
    promotion_id: filterForm.value.promotionId,
    country: filterForm.value.country,
    page,
    pageSize,
  }
  if (filterForm.value.dateRange?.length === 2) {
    params.start_date = filterForm.value.dateRange[0]
    params.end_date = filterForm.value.dateRange[1]
  }
  return params
}

const handleQuery = async () => {
  loading.value = true
  try {
    const res = await request.get('/users', {
      params: buildUserListParams(pagination.value.page, pagination.value.pageSize),
    })
    if (res.code === 0) {
      tableData.value = res.data?.list || []
      pagination.value.total = res.data?.total ?? 0
    }
  } catch (error) {
    ElMessage.error(`查询失败：${error.message || ''}`)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  filterForm.value = {
    userId: '',
    username: '',
    token: '',
    promotionId: '',
    country: '',
    dateRange: [],
  }
  pagination.value.page = 1
  handleQuery()
}

const handleViewDetail = (row) => {
  currentUser.value = { ...row }
  detailVisible.value = true
}

function buildUserExportParams() {
  const p = {
    user_id: filterForm.value.userId,
    username: filterForm.value.username,
    token: filterForm.value.token,
    promotion_id: filterForm.value.promotionId,
    country: filterForm.value.country,
  }
  if (filterForm.value.dateRange?.length === 2) {
    p.start_date = filterForm.value.dateRange[0]
    p.end_date = filterForm.value.dateRange[1]
  }
  return p
}

const handleExportExcel = async () => {
  exporting.value = true
  try {
    const pageSize = 100
    let page = 1
    let total = Infinity
    const all = []
    while (all.length < total) {
      const res = await request.get('/users', {
        params: { ...buildUserExportParams(), page, pageSize },
      })
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
    const rows = all.map((row) => ({
      用户ID: row.user_id ?? row.id,
      用户名: row.username ?? '',
      Token: row.token ?? '',
      推广ID: row.promotion_id ?? '',
      注册时间: row.register_time || row.created_at || '',
    }))
    exportJsonToXlsx(rows, '用户列表', `用户列表_${new Date().toISOString().slice(0, 10)}`)
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
.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

</style>
