<template>
  <el-card>
    <template #header><span>微信订阅消息</span></template>
    <el-form label-width="120px" style="max-width:600px">
      <el-form-item label="模板ID">
        <el-input v-model="form.templateId" placeholder="订阅消息模板ID" />
      </el-form-item>
      <el-form-item label="模板名称">
        <el-input v-model="form.name" placeholder="模板名称" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
const form = reactive({ templateId: '', name: '' })
</script>
