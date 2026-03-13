<template>
  <div class="tag-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标签管理</span>
          <el-button type="primary" @click="openDialog()">新增标签</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="名称" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑标签' : '新增标签'" width="400px">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="标签名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
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
const dialogVisible = ref(false)
const editId = ref(null)
const submitLoading = ref(false)
const formRef = ref()
const form = reactive({ name: '' })
const formRules = { name: [{ required: true, message: '请输入名称', trigger: 'blur' }] }

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/tags')
    list.value = res.data
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  editId.value = row?.id || null
  form.name = row?.name || ''
  dialogVisible.value = true
}

async function submitForm() {
  await formRef.value.validate()
  submitLoading.value = true
  try {
    if (editId.value) {
      await request.put(`/tags/${editId.value}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/tags', form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadList()
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(id) {
  await request.delete(`/tags/${id}`)
  ElMessage.success('删除成功')
  loadList()
}

onMounted(loadList)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
