<template>
  <div class="admin-manage page-container">
    <el-card shadow="never">
      <div class="filter-bar">
        <div class="filter-left">
          <el-input v-model="queryParams.username" placeholder="用户名/昵称" clearable size="small" style="width:200px">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="filter-right">
          <el-button v-permission="'admin:create'" type="primary" size="small" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新增管理员
          </el-button>
        </div>
      </div>

      <el-table :data="admins" size="small" stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="nickname" label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="140">
          <template #default="{ row }">
            <el-tag v-if="row.role_obj" :type="getRoleTagType(row.role)" size="small">
              {{ row.role_obj.name }}
            </el-tag>
            <span v-else class="text-muted">{{ row.role || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="created_at" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'admin:role'" link type="primary" size="small" @click="handleAssignRole(row)">分配角色</el-button>
            <el-button v-permission="'admin:edit'" link type="warning" size="small" @click="handleResetPassword(row)">重置密码</el-button>
            <el-button
              v-permission="'admin:delete'"
              link type="danger"
              size="small"
              @click="handleDelete(row)"
              :disabled="row.id === 1"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px" size="small">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" show-password />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="角色" prop="role_id">
          <el-select v-model="form.role_id" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetDialogVisible" title="重置密码" width="420px">
      <el-form :model="resetForm" label-width="80px" size="small">
        <el-form-item label="新密码">
          <el-input v-model="resetForm.password" type="password" placeholder="请输入新密码（至少6位）" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="resetDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleDoReset" :loading="saving">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="500px">
      <el-form label-width="80px" size="small">
        <el-form-item label="当前用户">
          <span>{{ currentAdmin?.username }}</span>
        </el-form-item>
        <el-form-item label="选择角色">
          <el-select v-model="roleForm.role_id" placeholder="请选择角色" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" size="small" @click="handleDoAssignRole" :loading="saving">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getAdmins, createAdmin, updateAdmin, deleteAdmin, resetAdminPassword } from '@/api/admin'
import { getRoles } from '@/api/role'

const allAdmins = ref([])
const roles = ref([])
const loading = ref(false)
const saving = ref(false)
const queryParams = reactive({ username: '' })

const admins = computed(() => {
  const kw = queryParams.username.trim().toLowerCase()
  if (!kw) return allAdmins.value
  return allAdmins.value.filter(a =>
    (a.username || '').toLowerCase().includes(kw) ||
    (a.nickname || '').toLowerCase().includes(kw)
  )
})
const dialogVisible = ref(false)
const dialogTitle = ref('新增管理员')
const formRef = ref(null)
const resetDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const currentAdmin = ref(null)
const resetForm = reactive({ password: '' })
const roleForm = reactive({ role_id: null })
const form = reactive({ id: null, username: '', password: '', nickname: '', role_id: null })

const formRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少 6 位', trigger: 'blur' }],
  role_id: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

async function loadAdmins() {
  loading.value = true
  try {
    const res = await getAdmins()
    allAdmins.value = res.data || []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadRoles() {
  const res = await getRoles()
  roles.value = res.data || []
}

function getRoleTagType(code) {
  const map = { super_admin: 'danger', operator: 'success', finance: 'warning', analyst: 'info' }
  return map[code] || ''
}

function handleAdd() {
  dialogTitle.value = '新增管理员'
  Object.assign(form, { id: null, username: '', password: '', nickname: '', role_id: null })
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (form.id) {
      await updateAdmin(form.id, { nickname: form.nickname, role_id: form.role_id })
      ElMessage.success('更新成功')
    } else {
      await createAdmin(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadAdmins()
  } catch (e) {
    // request.js 拦截器已提示
  } finally {
    saving.value = false
  }
}

function handleResetPassword(row) {
  currentAdmin.value = row
  resetForm.password = ''
  resetDialogVisible.value = true
}

async function handleDoReset() {
  if (!resetForm.password || resetForm.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  saving.value = true
  try {
    await resetAdminPassword(currentAdmin.value.id, resetForm.password)
    ElMessage.success('密码已重置')
    resetDialogVisible.value = false
  } catch (e) {
    // 错误已拦截
  } finally {
    saving.value = false
  }
}

function handleAssignRole(row) {
  currentAdmin.value = row
  roleForm.role_id = row.role_id
  roleDialogVisible.value = true
}

async function handleDoAssignRole() {
  if (!roleForm.role_id) {
    ElMessage.warning('请选择角色')
    return
  }
  saving.value = true
  try {
    await updateAdmin(currentAdmin.value.id, { role_id: roleForm.role_id })
    ElMessage.success('角色分配成功')
    roleDialogVisible.value = false
    loadAdmins()
  } catch (e) {
    // 错误已拦截
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除管理员「${row.username}」吗？`, '删除确认', { type: 'warning', confirmButtonText: '确认删除' })
    await deleteAdmin(row.id)
    ElMessage.success('删除成功')
    loadAdmins()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => { loadAdmins(); loadRoles() })
</script>

<style scoped>
.admin-manage .filter-bar {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;
}
.admin-manage .filter-left { display: flex; gap: 10px; }
.text-muted { color: var(--el-text-color-placeholder); font-size: 13px; }
</style>
