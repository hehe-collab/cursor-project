<template>
  <div class="title-pack-page">
    <el-card shadow="never" class="filter-card">
      <template #header><span>标题包</span></template>
      <el-form :model="query" class="filter-form" inline size="small" label-position="left">
        <el-form-item label="标题包ID" label-width="70px">
          <div class="filter-item-s">
            <el-input v-model="query.titlePackId" placeholder="ID" clearable />
          </div>
        </el-form-item>
        <el-form-item label-width="0">
          <div class="filter-buttons">
            <el-button type="primary" @click="onSearch">查询</el-button>
            <el-button @click="onReset">重置</el-button>
          </div>
        </el-form-item>
      </el-form>

      <div class="button-group">
        <div class="button-group-left">
          <el-button type="primary" @click="showAdd">新增</el-button>
          <el-button :disabled="selectedRows.length !== 1" @click="showEditSelected">修改</el-button>
          <el-button type="danger" :disabled="selectedRows.length === 0" @click="onBatchDelete">删除</el-button>
        </div>
        <div class="button-group-right">
          <el-button @click="handleExport">导出</el-button>
        </div>
      </div>

      <el-table
        ref="tableRef"
        :data="list"
        v-loading="loading"
        stripe
        height="calc(100vh - 300px)"
        @selection-change="onSelectionChange"
      >
        <template #empty>
          <el-empty description="暂无标题包" />
        </template>
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="标题名称" min-width="140" show-overflow-tooltip />
        <el-table-column prop="content" label="标题内容" min-width="220" show-overflow-tooltip />
        <el-table-column prop="created_by_name" label="创建人" width="120" />
        <el-table-column prop="created_at" label="创建时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[20, 50, 100, 200]"
        layout="total, sizes, prev, pager, next"
        @current-change="loadList"
        @size-change="loadList"
        style="margin-top: 16px"
      />
    </el-card>

    <el-dialog v-model="formVisible" :title="formId ? '编辑标题包' : '新增标题包'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题名称" required>
          <el-input v-model="form.name" placeholder="请输入标题名称" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="标题内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入标题内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="标题包详情" width="520px" destroy-on-close>
      <el-descriptions :column="1" border v-if="detailRow">
        <el-descriptions-item label="标题包ID">{{ detailRow.id }}</el-descriptions-item>
        <el-descriptions-item label="标题名称">{{ detailRow.name }}</el-descriptions-item>
        <el-descriptions-item label="标题内容">
          <div class="detail-content">{{ detailRow.content }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detailRow.created_by_name || '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailRow.created_at || '—' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../api/request'
import { useConfirmDelete } from '../composables/useConfirmDelete'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const tableRef = ref(null)
const selectedRows = ref([])
const formVisible = ref(false)
const detailVisible = ref(false)
const detailRow = ref(null)
const formId = ref(null)
const query = reactive({ page: 1, pageSize: 20, titlePackId: '' })
const form = reactive({ name: '', content: '' })

async function loadList() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize }
    if (query.titlePackId) params.titlePackId = query.titlePackId
    const res = await request.get('/title-pack', { params }).catch(() => ({ code: 0, data: { list: [], total: 0 } }))
    if (res.code === 0) {
      list.value = res.data?.list || []
      total.value = res.data?.total ?? 0
    }
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  loadList()
}

function onReset() {
  Object.assign(query, { page: 1, pageSize: 20, titlePackId: '' })
  loadList()
}

function onSelectionChange(rows) {
  selectedRows.value = rows
}

function showAdd() {
  formId.value = null
  Object.assign(form, { name: '', content: '' })
  formVisible.value = true
}

function showEditSelected() {
  const row = selectedRows.value[0]
  if (!row) return
  formId.value = row.id
  Object.assign(form, { name: row.name, content: row.content })
  formVisible.value = true
}

function showDetail(row) {
  detailRow.value = { ...row }
  detailVisible.value = true
}

async function submitForm() {
  const name = (form.name || '').trim()
  if (!name) {
    ElMessage.warning('请填写标题名称')
    return
  }
  try {
    if (formId.value) {
      await request.put(`/title-pack/${formId.value}`, { name, content: form.content })
      ElMessage.success('修改成功')
    } else {
      await request.post('/title-pack', { name, content: form.content })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    tableRef.value?.clearSelection()
    loadList()
  } catch {
    ElMessage.error('操作失败')
  }
}

const { confirmDelete } = useConfirmDelete({ onSuccess: loadList })
function onDelete(row) {
  confirmDelete(async (r) => request.delete(`/title-pack/${r.id}`), row)
}

async function onBatchDelete() {
  const ids = selectedRows.value.map((r) => r.id)
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条标题包吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    const res = await request.delete('/title-pack/batch', { data: { ids } })
    if (res.code === 0) {
      ElMessage.success('删除成功')
      selectedRows.value = []
      tableRef.value?.clearSelection()
      loadList()
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

function filterQueryParams() {
  const p = {}
  if (query.titlePackId) p.titlePackId = query.titlePackId
  return p
}

const handleExport = async () => {
  loading.value = true
  try {
    const response = await request.get('/title-pack/export', {
      params: filterQueryParams(),
      responseType: 'blob',
    })
    const blob = response.data
    let filename = `标题包_${new Date().toISOString().slice(0, 10)}.xlsx`
    const disposition = response.headers['content-disposition']
    if (disposition) {
      const m = disposition.match(/filename\*?=(?:UTF-8'')?["']?([^"';]+)["']?/i)
      if (m && m[1]) {
        try {
          filename = decodeURIComponent(m[1].trim())
        } catch {
          filename = m[1].trim()
        }
      }
    }
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    // 失败由 request 拦截器提示
  } finally {
    loading.value = false
  }
}

onMounted(loadList)
</script>

<style scoped>
.filter-form {
  margin-bottom: 16px;
}
.title-pack-page .button-group {
  margin-bottom: 12px;
}
.detail-content {
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
