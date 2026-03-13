<template>
  <div class="callback-config-page">
    <el-card>
      <template #header><span>回传配置</span></template>
      <el-form :model="form" label-width="120px" style="max-width:600px">
        <el-form-item label="回传地址">
          <el-input v-model="form.url" placeholder="回传 API 地址" />
        </el-form-item>
        <el-form-item label="回传密钥">
          <el-input v-model="form.secret" placeholder="可选" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const form = reactive({ url: '', secret: '' })

async function loadConfig() {
  try {
    const res = await request.get('/callback/config').catch(() => ({ data: {} }))
    Object.assign(form, res.data || {})
  } catch (e) {
    ElMessage.error('加载配置失败')
  }
}

async function onSave() {
  try {
    await request.post('/callback/config', form)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  }
}

onMounted(loadConfig)
</script>
