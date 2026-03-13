<template>
  <div class="recharge-group-page">
    <el-card>
      <template #header><span>充值方案组</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="方案组名称">
          <el-input v-model="query.name" placeholder="请输入" clearable style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="showAdd">新增</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无方案组" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="方案组名称" />
        <el-table-column prop="plans" label="包含方案" min-width="200" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
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

    <el-dialog v-model="formVisible" :title="formId ? '编辑方案组' : '新增方案组'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="方案组名称"><el-input v-model="form.name" placeholder="请输入" /></el-form-item>
        <el-form-item label="包含方案">
          <el-select v-model="form.plan_ids" multiple placeholder="请选择" style="width:100%">
            <el-option v-for="p in planList" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { useConfirmDelete } from '../composables/useConfirmDelete'

const loading = ref(false)
const list = ref([])
const planList = ref([])
const formVisible = ref(false)
const formId = ref(null)
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, name: '' })
const form = reactive({ name: '', plan_ids: [] })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    const res = await request.get('/recharge-groups', { params }).catch(() => ({ data: { list: [] } }))
    list.value = res.data?.list || []
    total.value = res.data?.total ?? list.value.length
  } finally {
    loading.value = false
  }
}

async function loadPlans() {
  const res = await request.get('/recharge-plans', { params: { pageSize: 100 } }).catch(() => ({ data: { list: [] } }))
  planList.value = res.data?.list || []
}

function onSearch() {
  loadList()
}

function onReset() {
  query.name = ''
  loadList()
}

function showAdd() {
  formId.value = null
  Object.assign(form, { name: '', plan_ids: [] })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { name: row.name, plan_ids: row.plan_ids || [] })
  formVisible.value = true
}

async function submitForm() {
  try {
    if (formId.value) {
      await request.put(`/recharge-groups/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/recharge-groups', form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

const { confirmDelete } = useConfirmDelete({ onSuccess: loadList })
function onDelete(row) {
  confirmDelete(async (r) => request.delete(`/recharge-groups/${r.id}`), row)
}

onMounted(() => {
  loadList()
  loadPlans()
})
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
