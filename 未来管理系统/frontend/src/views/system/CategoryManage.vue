<template>
  <div class="category-manage page-container">
    <el-card shadow="never">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="queryParams.name"
            placeholder="分类名称"
            clearable
            size="small"
            style="width: 200px"
            @input="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryParams.isEnabled"
            placeholder="启用状态"
            clearable
            size="small"
            style="width: 120px"
            @change="loadCategories"
          >
            <el-option label="已启用" :value="true" />
            <el-option label="已禁用" :value="false" />
          </el-select>
        </div>
        <div class="filter-right">
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增分类
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="categories" size="small" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="分类名称" min-width="150">
          <template #default="{ row }">
            <el-tag v-if="!row.is_enabled" type="info" size="small" style="margin-right: 6px">已禁用</el-tag>
            {{ row.name }}
          </template>
        </el-table-column>
        <el-table-column prop="slug" label="URL 标识" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-muted">{{ row.slug || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-muted">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="drama_count" label="关联短剧" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.drama_count > 0" type="success" size="small">{{ row.drama_count }}</el-tag>
            <span v-else class="text-muted">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort_order" label="排序" width="110" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.sort_order"
              :min="0" :max="9999"
              size="small"
              controls-position="right"
              style="width: 90px"
              @change="handleSortChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="160">
          <template #default="{ row }">{{ row.created_at || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.is_enabled ? 'warning' : 'success'"
              size="small"
              @click="handleToggleStatus(row)"
            >{{ row.is_enabled ? '禁用' : '启用' }}</el-button>
            <el-button
              link type="danger"
              size="small"
              @click="handleDelete(row)"
              :disabled="row.drama_count > 0"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="580px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="URL 标识" prop="slug">
          <el-input v-model="form.slug" placeholder="留空自动生成，如 urban-romance" maxlength="50" />
          <div class="form-tip">用于 SEO友好的 URL，建议使用英文，自动生成时由名称转换</div>
        </el-form-item>
        <el-form-item label="分类描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入分类描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="排序权重" prop="sort_order">
          <el-input-number v-model="form.sort_order" :min="0" :max="9999" controls-position="right" />
          <div class="form-tip">数值越小越靠前</div>
        </el-form-item>
        <el-form-item label="启用状态" prop="is_enabled">
          <el-switch v-model="form.is_enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import {
  getCategories,
  createCategory,
  updateCategory,
  deleteCategory,
} from '@/api/category'
import { debounce } from 'lodash-es'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增分类')
const formRef = ref(null)

const queryParams = reactive({ name: '', isEnabled: null })
const form = reactive({
  id: null,
  name: '',
  slug: '',
  description: '',
  sort_order: 0,
  is_enabled: true,
})

const formRules = {
  name: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
  slug: [
    { pattern: /^[a-z0-9-]*$/, message: '只能包含小写字母、数字和连字符', trigger: 'blur' },
  ],
}

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategories(queryParams)
    categories.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || e))
  } finally {
    loading.value = false
  }
}

const handleSearch = debounce(loadCategories, 300)

function handleAdd() {
  dialogTitle.value = '新增分类'
  Object.assign(form, { id: null, name: '', slug: '', description: '', sort_order: 0, is_enabled: true })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑分类'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateCategory(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createCategory(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadCategories()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || e))
  } finally {
    saving.value = false
  }
}

async function handleToggleStatus(row) {
  try {
    await updateCategory(row.id, { ...row, is_enabled: !row.is_enabled })
    ElMessage.success('状态已更新')
    loadCategories()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || e))
  }
}

const handleSortChange = debounce(async (row) => {
  try {
    await updateCategory(row.id, row)
    ElMessage.success('排序已更新')
  } catch (e) {
    ElMessage.error('更新失败: ' + (e.message || e))
  }
}, 500)

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除分类「${row.name}」吗？此操作不可恢复。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    })
    await deleteCategory(row.id)
    ElMessage.success('删除成功')
    loadCategories()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || e))
  }
}

onMounted(loadCategories)
</script>

<style scoped>
.category-manage {
  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .filter-bar .filter-left {
    display: flex;
    gap: 10px;
  }
  .form-tip {
    font-size: 12px;
    color: var(--el-text-color-secondary);
    margin-top: 4px;
  }
  .text-muted {
    color: var(--el-text-color-placeholder);
    font-size: 13px;
  }
}
</style>
