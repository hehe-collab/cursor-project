<template>
  <el-card>
    <template #header><span>代理商主页</span></template>
    <el-form label-width="120px" style="max-width:600px">
      <el-form-item label="主页标题">
        <el-input v-model="form.title" placeholder="代理商主页标题" />
      </el-form-item>
      <el-form-item label="主页描述">
        <el-input v-model="form.desc" type="textarea" :rows="4" placeholder="主页描述" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive } from 'vue'
const form = reactive({ title: '', desc: '' })
</script>
