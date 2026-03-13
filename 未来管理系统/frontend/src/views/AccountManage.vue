<template>
  <div class="account-manage-page">
    <el-card>
      <template #header><span>账户管理</span></template>
      <el-form :inline="true" class="filter-form" @submit.prevent="onSearch">
        <el-form-item label="账户媒体">
          <el-select v-model="query.media" placeholder="请选择" clearable style="width:120px">
            <el-option label="Meta" value="Meta" />
            <el-option label="Google" value="Google" />
            <el-option label="TikTok" value="TikTok" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="query.country" placeholder="请选择" clearable style="width:100px">
            <el-option label="美国" value="US" />
            <el-option label="印尼" value="ID" />
            <el-option label="泰国" value="TH" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户主体">
          <el-input v-model="query.subject" placeholder="请输入" clearable style="width:120px" />
        </el-form-item>
        <el-form-item label="账户ID">
          <el-input v-model="query.accountId" placeholder="请输入账户ID" clearable style="width:160px" @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="账户名称">
          <el-input v-model="query.accountName" placeholder="请输入账户名称" clearable style="width:140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="showAdd">新增</el-button>
          <el-button @click="onImport">导入</el-button>
          <el-button @click="onExport">导出</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无账户数据" />
        </template>
        <el-table-column prop="media" label="账户媒体" width="100" />
        <el-table-column prop="country" label="国家" width="80" />
        <el-table-column prop="subject_name" label="主体名称" width="120" />
        <el-table-column prop="account_id" label="账户ID" width="180" />
        <el-table-column prop="account_name" label="账户名称" min-width="120" />
        <el-table-column prop="created_by" label="创建人" width="80" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
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

    <!-- 新增/修改账户弹窗 -->
    <el-dialog v-model="formVisible" :title="formId ? '修改账户' : '新增账户'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="账户媒体" prop="media">
          <el-select v-model="form.media" placeholder="请选择账户媒体" style="width:100%">
            <el-option label="Meta" value="Meta" />
            <el-option label="Google" value="Google" />
            <el-option label="TikTok" value="TikTok" />
          </el-select>
        </el-form-item>
        <el-form-item label="国家">
          <el-select v-model="form.country" placeholder="请选择国家" style="width:100%">
            <el-option label="美国" value="US" />
            <el-option label="印尼" value="ID" />
            <el-option label="泰国" value="TH" />
          </el-select>
        </el-form-item>
        <el-form-item label="账户主体"><el-input v-model="form.subject_name" placeholder="请输入账户主体" /></el-form-item>
        <el-form-item label="账户ID" prop="account_id"><el-input v-model="form.account_id" placeholder="请输入账户ID" /></el-form-item>
        <el-form-item label="账户名称"><el-input v-model="form.account_name" placeholder="请输入账户名称" /></el-form-item>
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
import { exportToCSV } from '../utils/export'
import { useConfirmDelete } from '../composables/useConfirmDelete'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formId = ref(null)
const formRef = ref()
const formRules = {
  media: [{ required: true, message: '请选择账户媒体', trigger: 'change' }],
  account_id: [{ required: true, message: '请输入账户ID', trigger: 'blur' }],
}
const query = reactive({ page: 1, pageSize: 10, media: '', country: '', subject: '', accountId: '', accountName: '' })
const form = reactive({ media: '', country: '', subject_name: '', account_id: '', account_name: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.media) params.media = query.media
    if (query.country) params.country = query.country
    if (query.subject) params.subject = query.subject
    if (query.accountId) params.accountId = query.accountId
    if (query.accountName) params.accountName = query.accountName
    const res = await request.get('/accounts', { params }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  Object.assign(query, { page: 1, media: '', country: '', subject: '', accountId: '', accountName: '' })
  loadList()
}

function showAdd() {
  formId.value = null
  Object.assign(form, { media: '', country: '', subject_name: '', account_id: '', account_name: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { media: row.media, country: row.country, subject_name: row.subject_name, account_id: row.account_id, account_name: row.account_name })
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
      await request.put(`/accounts/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/accounts', form)
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
  confirmDelete(async (r) => request.delete(`/accounts/${r.id}`), row)
}

function onImport() {
  ElMessage.info('Excel 批量导入账户功能开发中')
}

async function onExport() {
  try {
    const params = { pageSize: 2000 }
    const res = await request.get('/accounts', { params }).catch(() => ({ data: { list: [] } }))
    const data = res.data?.list || []
    if (!data.length) return ElMessage.warning('暂无数据可导出')
    exportToCSV(data, [
      { prop: 'media', label: '账户媒体' },
      { prop: 'country', label: '国家' },
      { prop: 'subject_name', label: '主体名称' },
      { prop: 'account_id', label: '账户ID' },
      { prop: 'account_name', label: '账户名称' },
      { prop: 'created_at', label: '创建时间' },
    ], `广告账户_${new Date().toISOString().slice(0, 10)}.csv`)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
