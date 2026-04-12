<template>
  <div class="role-manage page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <div class="filter-left">
          <el-tag type="info" size="small">内置角色不可删除；可编辑描述和权限分配</el-tag>
        </div>
        <div class="filter-right">
          <el-button v-permission="'admin:role'" type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增角色
          </el-button>
        </div>
      </div>

      <el-table :data="roles" size="small" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column prop="code" label="角色代码" min-width="140">
          <template #default="{ row }">
            <code style="font-size:12px">{{ row.code }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.description || '-' }}</template>
        </el-table-column>
        <el-table-column label="内置" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.is_system" type="danger" size="small">是</el-tag>
            <el-tag v-else type="info" size="small">否</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限数" width="90" align="center">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.permissions?.length || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'admin:role'" link type="primary" size="small" @click="handleAssignPerm(row)">分配权限</el-button>
            <el-button
              v-permission="'admin:edit'"
              link type="warning"
              size="small"
              @click="handleEdit(row)"
              :disabled="row.is_system"
              :title="row.is_system ? '内置角色不可编辑' : ''"
            >编辑</el-button>
            <el-button
              v-permission="'admin:delete'"
              link type="danger"
              size="small"
              @click="handleDelete(row)"
              :disabled="row.is_system"
              :title="row.is_system ? '内置角色不可删除' : ''"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" placeholder="如：内容编辑" />
        </el-form-item>
        <el-form-item label="角色代码" prop="code">
          <el-input v-model="form.code" placeholder="如：content_editor" :disabled="!!form.id" />
          <div class="form-tip">唯一标识，建议使用英文字母和下划线</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="请输入角色描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permDialogVisible" :title="`分配权限 - ${currentRole?.name}`" width="700px">
      <div v-loading="permLoading" style="max-height:500px;overflow-y:auto">
        <el-checkbox-group v-model="selectedIds">
          <div v-for="(perms, module) in groupedPermissions" :key="module" class="perm-module">
            <div class="perm-module-title">
              <el-checkbox
                :model-value="isModuleAllSelected(perms)"
                :indeterminate="isModuleIndeterminate(perms)"
                @change="(val) => toggleModule(module, perms, val)"
                style="font-weight:600"
              >{{ module }}</el-checkbox>
            </div>
            <div class="perm-module-items">
              <el-checkbox
                v-for="p in perms"
                :key="p.id"
                :label="p.id"
                :value="p.id"
                style="width:180px;margin-bottom:4px"
              >{{ p.name }}</el-checkbox>
            </div>
          </div>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button size="small" @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleSavePerms" :loading="saving">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getRoles, createRole, updateRole, deleteRole, assignPermissions } from '@/api/role'
import { getPermissions } from '@/api/permission'

const roles = ref([])
const groupedPermissions = ref({})
const loading = ref(false)
const saving = ref(false)
const permLoading = ref(false)
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const dialogTitle = ref('新增角色')
const currentRole = ref(null)
const selectedIds = ref([])
const formRef = ref(null)
const form = reactive({ id: null, name: '', code: '', description: '' })
const formRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色代码', trigger: 'blur' }, { pattern: /^[a-z_][a-z0-9_]*$/, message: '只能包含小写字母、数字和下划线', trigger: 'blur' }],
}

async function loadRoles() {
  loading.value = true
  try {
    const res = await getRoles()
    roles.value = res.data || []
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function loadPermissions() {
  const res = await getPermissions()
  groupedPermissions.value = res.data || {}
}

function handleAdd() {
  dialogTitle.value = '新增角色'
  Object.assign(form, { id: null, name: '', code: '', description: '' })
  dialogVisible.value = true
}

function handleEdit(row) {
  dialogTitle.value = '编辑角色'
  Object.assign(form, { id: row.id, name: row.name, code: row.code, description: row.description })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateRole(form.id, form)
      ElMessage.success('更新成功')
    } else {
      await createRole(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadRoles()
  } catch (e) { /* 错误拦截 */ }
  finally { saving.value = false }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除角色「${row.name}」吗？`, '删除确认', { type: 'warning' })
    await deleteRole(row.id)
    ElMessage.success('删除成功')
    loadRoles()
  } catch (e) { if (e !== 'cancel') ElMessage.error('删除失败') }
}

async function handleAssignPerm(row) {
  currentRole.value = row
  permLoading.value = true
  permDialogVisible.value = true
  selectedIds.value = (row.permissions || []).map(p => p.id)
  try {
    await loadPermissions()
  } finally { permLoading.value = false }
}

function isModuleAllSelected(perms) {
  return perms.length > 0 && perms.every(p => selectedIds.value.includes(p.id))
}

function isModuleIndeterminate(perms) {
  const selectedCount = perms.filter(p => selectedIds.value.includes(p.id)).length
  return selectedCount > 0 && selectedCount < perms.length
}

function toggleModule(module, perms, checked) {
  const ids = perms.map(p => p.id)
  if (checked) {
    ids.forEach(id => {
      if (!selectedIds.value.includes(id)) selectedIds.value.push(id)
    })
  } else {
    selectedIds.value = selectedIds.value.filter(id => !ids.includes(id))
  }
}

async function handleSavePerms() {
  saving.value = true
  try {
    const numericIds = selectedIds.value.filter(id => typeof id === 'number')
    await assignPermissions(currentRole.value.id, numericIds)
    ElMessage.success('权限分配成功')
    permDialogVisible.value = false
    loadRoles()
  } catch (e) { /* 错误拦截 */ }
  finally { saving.value = false }
}

onMounted(() => { loadRoles(); loadPermissions() })
</script>

<style scoped>
.role-manage .filter-bar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.role-manage .filter-left { display: flex; gap: 10px; }
.form-tip { font-size: 12px; color: var(--el-text-color-secondary); margin-top: 4px; }
.perm-module { margin-bottom: 16px; }
.perm-module-title { margin-bottom: 8px; padding-bottom: 4px; border-bottom: 1px solid var(--el-border-color-light); }
.perm-module-items { padding-left: 16px; }
</style>
