<template>
  <div class="tag-manage page-container">
    <el-card shadow="never">
      <!-- 筛选区 -->
      <div class="filter-bar">
        <div class="filter-left">
          <el-input
            v-model="queryParams.name"
            placeholder="标签名称"
            clearable
            size="small"
            style="width: 200px"
            @input="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-radio-group v-model="queryParams.isHot" size="small" @change="loadTags">
            <el-radio-button :label="null">全部</el-radio-button>
            <el-radio-button :label="true">热门</el-radio-button>
            <el-radio-button :label="false">普通</el-radio-button>
          </el-radio-group>
        </div>
        <div class="filter-right">
          <el-button type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增标签
          </el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="tags" size="small" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="标签名称" min-width="150">
          <template #default="{ row }">
            <el-tag :color="row.color" effect="dark" size="small" style="border: none">{{ row.name }}</el-tag>
            <el-tag v-if="row.is_hot" type="danger" size="small" style="margin-left: 6px">HOT</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="color" label="颜色" width="130">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:8px">
              <div :style="{ width:'18px',height:'18px',backgroundColor:row.color,borderRadius:'4px',border:'1px solid #eee' }" />
              <span style="font-size:12px;color:#909399">{{ row.color }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="usage_count" label="使用次数" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.usage_count > 0" type="success" size="small">{{ row.usage_count }}</el-tag>
            <span v-else class="text-muted">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="drama_count" label="关联短剧" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.drama_count > 0" type="success" size="small">{{ row.drama_count }}</el-tag>
            <span v-else class="text-muted">0</span>
          </template>
        </el-table-column>
        <el-table-column prop="sort_order" label="排序" width="100" align="center">
          <template #default="{ row }">{{ row.sort_order }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              link
              :type="row.is_hot ? 'warning' : 'success'"
              size="small"
              @click="handleToggleHot(row)"
            >{{ row.is_hot ? '取消热门' : '设为热门' }}</el-button>
            <el-button
              link type="danger"
              size="small"
              @click="handleDelete(row)"
              :disabled="row.usage_count > 0"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="标签颜色" prop="color">
          <el-color-picker v-model="form.color" show-alpha />
          <span style="margin-left:12px;font-size:13px;color:#909399">{{ form.color }}</span>
        </el-form-item>
        <el-form-item label="排序权重" prop="sort_order">
          <el-input-number v-model="form.sort_order" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="热门标签" prop="is_hot">
          <el-switch v-model="form.is_hot" />
          <span style="margin-left:10px;font-size:12px;color:#909399">热门标签会在前端优先展示</span>
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
import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'
import { debounce } from 'lodash-es'

const tags = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const dialogTitle = ref('新增标签')
const formRef = ref(null)

const queryParams = reactive({ name: '', isHot: null })

const form = reactive({
  id: null,
  name: '',
  color: '#409EFF',
  sort_order: 0,
  is_hot: false,
})

const formRules = {
  name: [
    { required: true, message: '请输入标签名称', trigger: 'blur' },
    { min: 1, max: 30, message: '长度在 1 到 30 个字符', trigger: 'blur' },
  ],
}

async function loadTags() {
  loading.value = true
  try {
    const res = await getTags(queryParams)
    tags.value = Array.isArray(res.data) ? res.data : (res.data?.list || [])
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || e))
  } finally {
    loading.value = false
  }
}

const handleSearch = debounce(loadTags, 300)

function handleAdd() {
  dialogTitle.value = '新增标签'
  Object.assign(form, { id: null, name: '', color: '#409EFF', sort_order: 0, is_hot: false })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑标签'
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateTag(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createTag(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadTags()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || e))
  } finally {
    saving.value = false
  }
}

async function handleToggleHot(row) {
  try {
    await updateTag(row.id, { ...row, is_hot: !row.is_hot })
    ElMessage.success('状态已更新')
    loadTags()
  } catch (e) {
    ElMessage.error('操作失败: ' + (e.message || e))
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除标签「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteTag(row.id)
    ElMessage.success('删除成功')
    loadTags()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败: ' + (e.message || e))
  }
}

onMounted(loadTags)
</script>

<style scoped>
.tag-manage {
  .filter-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }
  .filter-bar .filter-left {
    display: flex;
    gap: 10px;
    align-items: center;
  }
  .text-muted {
    color: var(--el-text-color-placeholder);
    font-size: 13px;
  }
}
</style>
