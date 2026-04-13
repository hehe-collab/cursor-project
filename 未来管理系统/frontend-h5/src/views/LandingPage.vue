<template>
  <div class="landing">
    <van-loading v-if="!error" color="#fff" size="32px" vertical>加载中...</van-loading>
    <div v-else class="error-box">
      <van-icon name="warning-o" size="48px" color="#ff4757" />
      <p>{{ error }}</p>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Loading as VanLoading, Icon as VanIcon } from 'vant'
import { captureAttribution, getDeviceId } from '@/utils/device'
import { useDramaStore } from '@/stores/drama'

const router = useRouter()
const store = useDramaStore()
const error = ref('')

onMounted(async () => {
  // 1. 读取并持久化 URL 归因参数
  const attr = captureAttribution()

  // 2. 确保 device_id 已生成
  getDeviceId()

  // 3. 校验 promo_id
  if (!attr.promoId) {
    error.value = '无效的推广链接，请通过广告重新进入'
    return
  }

  // 4. 加载剧集数据
  await store.loadDrama()

  if (store.error) {
    error.value = store.error
    return
  }

  // 5. 直接跳转播放器，用户无感知
  router.replace('/play')
})
</script>

<style scoped>
.landing {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #000;
}
.error-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 24px;
  text-align: center;
  color: #ccc;
  font-size: 14px;
  line-height: 1.6;
}
</style>
