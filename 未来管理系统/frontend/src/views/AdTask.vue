<template>
  <div class="ad-task-page">
    <el-card>
      <template #header><span>广告任务</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item>
          <el-button type="primary" @click="showAdd">新增任务</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无广告任务" />
        </template>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="任务名称" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'running' ? 'success' : 'info'">{{ row.status === 'running' ? '运行中' : '已停止' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="create_time" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <el-button link type="primary" @click="showEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'running' ? 'warning' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'running' ? '停止' : '启动' }}
            </el-button>
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

    <el-dialog v-model="formVisible" :title="formId ? '编辑任务' : '新增广告任务'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.name" placeholder="请输入任务名称" maxlength="50" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="任务详情" width="600px">
      <el-descriptions v-if="currentTask" :column="1" border>
        <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.name }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ currentTask.status === 'running' ? '运行中' : '已停止' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTask.create_time }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const formVisible = ref(false)
const formId = ref(null)
const detailVisible = ref(false)
const currentTask = ref(null)
const query = reactive({ page: 1, pageSize: 10 })
const form = reactive({ name: '' })

async function loadList() {
  loading.value = true
  try {
    const res = await request.get('/ad-task', { params: query }).catch(() => ({ data: { list: [], total: 0 } }))
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function showAdd() {
  formId.value = null
  Object.assign(form, { name: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { name: row.name })
  formVisible.value = true
}

function showDetail(row) {
  currentTask.value = row
  detailVisible.value = true
}

async function submitForm() {
  if (!form.name?.trim()) return ElMessage.warning('请输入任务名称')
  try {
    if (formId.value) {
      await request.put(`/ad-task/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/ad-task', form)
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function toggleStatus(row) {
  const nextStatus = row.status === 'running' ? 'stopped' : 'running'
  try {
    await request.put(`/ad-task/${row.id}`, { ...row, status: nextStatus })
    ElMessage.success(nextStatus === 'running' ? '已启动' : '已停止')
    loadList()
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

async function onDelete(row) {
  try {
    await ElMessageBox.confirm('确定要删除该任务吗？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await request.delete(`/ad-task/${row.id}`)
    ElMessage.success('删除成功')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
