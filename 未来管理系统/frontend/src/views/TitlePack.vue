<template>
  <div class="title-pack-page">
    <el-card>
      <template #header><span>标题包</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="标题">
          <el-input v-model="query.title" placeholder="请输入" clearable style="width:160px" />
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
          <el-empty description="暂无标题包" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="group" label="分组" width="120" />
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

    <el-dialog v-model="formVisible" :title="formId ? '编辑标题' : '新增标题'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" placeholder="请输入" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="form.group" placeholder="请输入分组" /></el-form-item>
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
const total = ref(0)
const formVisible = ref(false)
const formId = ref(null)
const query = reactive({ page: 1, pageSize: 10, title: '' })
const form = reactive({ title: '', group: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.title) params.title = query.title
    const res = await request.get('/title-pack', { params }).catch(() => ({ data: { list: [], total: 0 } }))
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
  Object.assign(query, { page: 1, title: '' })
  loadList()
}

function showAdd() {
  formId.value = null
  Object.assign(form, { title: '', group: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { title: row.title, group: row.group })
  formVisible.value = true
}

async function submitForm() {
  try {
    if (formId.value) {
      await request.put(`/title-pack/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/title-pack', form)
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
  confirmDelete(async (r) => request.delete(`/title-pack/${r.id}`), row)
}

function onImport() {
  ElMessage.info('批量导入标题包功能开发中')
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
