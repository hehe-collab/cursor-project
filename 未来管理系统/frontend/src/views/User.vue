<template>
  <div class="user-page">
    <el-card class="page-card">
      <template #header><span>用户列表</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="query.username" placeholder="请输入用户名" clearable style="width:140px" />
        </el-form-item>
        <el-form-item label="推广ID">
          <el-input v-model="query.promoteId" placeholder="请输入推广ID" clearable style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadList">查询</el-button>
          <el-button @click="onReset">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无用户数据" />
        </template>
        <el-table-column prop="id" label="用户ID" width="100" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="coin_balance" label="金币余额" width="100">
          <template #default="{ row }">{{ row.coin_balance ?? 0 }}</template>
        </el-table-column>
        <el-table-column prop="promote_id" label="推广ID" width="140" show-overflow-tooltip />
        <el-table-column prop="promote_name" label="推广名称" min-width="120" show-overflow-tooltip />
        <el-table-column prop="created_at" label="注册时间" width="180" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>
    <el-dialog v-model="detailVisible" title="用户详情" width="500px" destroy-on-close>
      <el-descriptions v-if="currentUser" :column="1" border>
        <el-descriptions-item label="用户ID">{{ currentUser.id }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ currentUser.username }}</el-descriptions-item>
<el-descriptions-item label="金币余额">{{ currentUser.coin_balance ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="推广ID">{{ currentUser.promote_id || '-' }}</el-descriptions-item>
        <el-descriptions-item label="推广名称">{{ currentUser.promote_name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ currentUser.created_at }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const detailVisible = ref(false)
const currentUser = ref(null)
const query = reactive({ page: 1, pageSize: 10, username: '', promoteId: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
if (query.username) params.username = query.username
    if (query.promoteId) params.promoteId = query.promoteId
    const res = await request.get('/users', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = (res.data?.list || []).map(u => ({ ...u, coin_balance: u.coin_balance ?? 0, promote_id: u.promote_id || '', promote_name: u.promote_name || '' }))
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onReset() {
  query.username = ''
  query.promoteId = ''
  query.page = 1
  loadList()
}

function showDetail(row) {
  currentUser.value = row
  detailVisible.value = true
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
