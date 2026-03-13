<template>
  <div class="ad-material-page">
    <el-card>
      <template #header><span>广告素材</span></template>
      <el-form :inline="true" class="filter-form">
        <el-form-item label="素材名称">
          <el-input v-model="query.name" placeholder="请输入" clearable style="width:160px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">搜索</el-button>
          <el-button @click="onReset">重置</el-button>
          <el-button type="primary" @click="showAdd">新增</el-button>
          <el-button @click="onUpload">批量上传</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <template #empty>
          <el-empty description="暂无广告素材" />
        </template>
        <el-table-column label="预览" width="80">
          <template #default="{ row }">
            <el-image v-if="row.url" :src="row.url" style="width:50px;height:50px" fit="cover" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="素材名称" />
        <el-table-column prop="type" label="类型" width="100" />
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

    <el-dialog v-model="formVisible" :title="formId ? '编辑素材' : '新增素材'" width="500px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="素材名称"><el-input v-model="form.name" placeholder="请输入" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择" style="width:100%">
            <el-option label="图片" value="image" />
            <el-option label="视频" value="video" />
          </el-select>
        </el-form-item>
        <el-form-item label="素材链接"><el-input v-model="form.url" placeholder="请输入URL" /></el-form-item>
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
const query = reactive({ page: 1, pageSize: 10, name: '' })
const form = reactive({ name: '', type: 'image', url: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.name) params.name = query.name
    const res = await request.get('/ad-material', { params }).catch(() => ({ data: { list: [], total: 0 } }))
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
  Object.assign(query, { page: 1, name: '' })
  loadList()
}

function showAdd() {
  formId.value = null
  Object.assign(form, { name: '', type: 'image', url: '' })
  formVisible.value = true
}

function showEdit(row) {
  formId.value = row.id
  Object.assign(form, { name: row.name, type: row.type || 'image', url: row.url })
  formVisible.value = true
}

async function submitForm() {
  try {
    if (formId.value) {
      await request.put(`/ad-material/${formId.value}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/ad-material', form)
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
  confirmDelete(async (r) => request.delete(`/ad-material/${r.id}`), row)
}

function onUpload() {
  ElMessage.info('批量上传功能开发中')
}

onMounted(loadList)
</script>

<style scoped>
.filter-form { margin-bottom: 16px; }
</style>
