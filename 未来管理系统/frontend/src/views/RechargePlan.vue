<template>
  <div class="recharge-plan-page">
    <el-card>
      <template #header><span>充值方案</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="名称">
          <el-input v-model="query.name" placeholder="请输入名称" clearable style="width:160px" />
        </el-form-item>
        <el-form-item label="支付平台">
          <el-select v-model="query.payPlatform" placeholder="请选择" clearable style="width:120px">
            <el-option label="stripe" value="stripe" />
            <el-option label="paypal" value="paypal" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="showAdd">新增</el-button>
          <el-button @click="onImport">导入</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无充值方案" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="bean_count" label="金豆数" width="90" />
        <el-table-column prop="extra_bean" label="赠送金豆数" width="100" />
        <el-table-column prop="amount" label="金额" width="90" />
        <el-table-column prop="recharge_info" label="充值信息" min-width="120" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column prop="created_by" label="创建人" width="80" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showEdit(row)">修改</el-button>
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

    <!-- 新增/修改充值方案弹窗 -->
    <el-dialog v-model="formVisible" :title="formId ? '修改充值方案' : '新增充值方案'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="名称" prop="name"><el-input v-model="form.name" placeholder="请输入名称" /></el-form-item>
        <el-form-item label="金豆数"><el-input-number v-model="form.bean_count" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="赠送金豆数"><el-input-number v-model="form.extra_bean" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="充值信息"><el-input v-model="form.recharge_info" placeholder="请输入充值信息" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="支付平台">
          <el-select v-model="form.pay_platform" placeholder="请选择支付平台" style="width:100%">
            <el-option label="stripe" value="stripe" />
            <el-option label="paypal" value="paypal" />
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

function onImport() {
  ElMessage.info('Excel 批量导入功能开发中')
}

const loading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formId = ref(null)
const query = reactive({ page: 1, pageSize: 10, name: '', payPlatform: '' })
const formRef = ref()
const formRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }
const form = reactive({ name: '', bean_count: 0, extra_bean: 0, amount: 0, recharge_info: '', pay_platform: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    if (query.payPlatform) params.payPlatform = query.payPlatform
    const res = await request.get('/recharge-plans', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  Object.assign(query, { page: 1, name: '', payPlatform: '' })
  loadList()
}

function showAdd() {
  formId.value = null
  Object.assign(form, { name: '', bean_count: 0, extra_bean: 0, amount: 0, recharge_info: '', pay_platform: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { name: row.name, bean_count: row.bean_count || 0, extra_bean: row.extra_bean || 0, amount: row.amount || 0, recharge_info: row.recharge_info || '', pay_platform: row.pay_platform || '' })
  formVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }
  try {
    if (formId.value) {
      await request.put(`/recharge-plans/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/recharge-plans', form)
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
  confirmDelete(async (r) => request.delete(`/recharge-plans/${r.id}`), row)
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
