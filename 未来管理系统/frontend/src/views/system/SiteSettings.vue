<template>
  <div class="site-settings page-container">
    <el-card shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>站点配置</span>
          <el-button type="primary" size="small" @click="handleSave" :loading="saving">
            <el-icon><Check /></el-icon> 保存设置
          </el-button>
        </div>
      </template>

      <el-form :model="settings" label-width="150px" size="small" style="max-width:600px">
        <el-divider content-position="left">基本信息</el-divider>
        <el-form-item label="站点名称">
          <el-input v-model="settings.site_name" placeholder="请输入站点名称" />
        </el-form-item>
        <el-form-item label="站点 Logo">
          <el-input v-model="settings.site_logo" placeholder="Logo 图片 URL" />
        </el-form-item>
        <el-form-item label="ICP 备案号">
          <el-input v-model="settings.icp" placeholder="如：京ICP备XXXXXXXX号" />
        </el-form-item>

        <el-divider content-position="left">充值配置</el-divider>
        <el-form-item label="首充奖励比例">
          <el-input-number v-model="settings.first_recharge_bonus" :min="0" :max="100" :step="5" />
          <span style="margin-left:8px">%</span>
        </el-form-item>
        <el-form-item label="最低充值金额">
          <el-input-number v-model="settings.min_recharge_amount" :min="1" :step="1" />
          <span style="margin-left:8px">元</span>
        </el-form-item>

        <el-divider content-position="left">视频播放</el-divider>
        <el-form-item label="免费试看集数">
          <el-input-number v-model="settings.free_episodes" :min="0" :max="10" />
          <span style="margin-left:8px">集</span>
        </el-form-item>
        <el-form-item label="单集观看消耗">
          <el-input-number v-model="settings.episode_cost" :min="1" :step="1" />
          <span style="margin-left:8px">金币</span>
        </el-form-item>

        <el-divider content-position="left">推广配置</el-divider>
        <el-form-item label="默认落地页">
          <el-input v-model="settings.default_landing_page" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="回传延迟时间">
          <el-input-number v-model="settings.callback_delay" :min="0" :max="3600" :step="10" />
          <span style="margin-left:8px">秒</span>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { getSettings, saveSettings } from '@/api/settings'

const saving = ref(false)
const settings = reactive({
  site_name: '',
  site_logo: '',
  icp: '',
  first_recharge_bonus: 0,
  min_recharge_amount: 1,
  free_episodes: 2,
  episode_cost: 10,
  default_landing_page: '',
  callback_delay: 60,
})

async function loadSettings() {
  try {
    const res = await getSettings()
    const data = res.data || {}
    // 扁平 key-value 展开到 settings
    for (const key in data) {
      if (settings.hasOwnProperty(key) && typeof settings[key] === 'number' && typeof data[key] === 'string') {
        settings[key] = Number(data[key]) || 0
      } else if (settings.hasOwnProperty(key)) {
        settings[key] = data[key]
      }
    }
  } catch (e) {
    ElMessage.error('加载失败: ' + (e.message || e))
  }
}

async function handleSave() {
  saving.value = true
  try {
    await saveSettings(settings)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || e))
  } finally {
    saving.value = false
  }
}

onMounted(loadSettings)
</script>

<style scoped>
.site-settings :deep(.el-divider__text) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}
</style>
