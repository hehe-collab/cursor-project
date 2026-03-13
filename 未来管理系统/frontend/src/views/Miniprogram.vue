<template>
  <el-card>
    <template #header><span>小程序配置</span></template>
    <el-form label-width="120px" style="max-width:600px">
      <el-form-item label="AppID">
        <el-input v-model="form.appId" placeholder="小程序 AppID" />
      </el-form-item>
      <el-form-item label="AppSecret">
        <el-input v-model="form.appSecret" type="password" placeholder="小程序 AppSecret" show-password />
      </el-form-item>
      <el-form-item label="小程序路径">
        <el-input v-model="form.path" placeholder="如 /pages/index/index" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
const form = reactive({ appId: '', appSecret: '', path: '' })
</script>
