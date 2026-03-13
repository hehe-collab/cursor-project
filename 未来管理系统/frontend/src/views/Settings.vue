<template>
  <div class="settings-page">
    <el-card>
      <template #header>
        <span>系统设置</span>
      </template>
      <el-form :model="form" label-width="120px" style="max-width: 500px">
        <el-form-item label="站点名称">
          <el-input v-model="form.site_name" placeholder="未来管理系统" />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <el-input v-model="form.site_logo" placeholder="Logo 图片 URL" />
        </el-form-item>
        <el-form-item label="备案号">
          <el-input v-model="form.icp" placeholder="ICP 备案号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="save" :loading="loading">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const loading = ref(false)
const form = reactive({
  site_name: '',
  site_logo: '',
  icp: '',
})

async function load() {
  try {
    const res = await request.get('/settings')
    Object.assign(form, res.data)
  } catch {}
}

async function save() {
  loading.value = true
  try {
    await request.post('/settings', form)
    ElMessage.success('保存成功')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
