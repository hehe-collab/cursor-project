import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getDramaByPromo, getUserInfo } from '@/api/index'
import { getAttribution } from '@/utils/device'

export const useDramaStore = defineStore('drama', () => {
  const drama = ref(null)       // 剧集信息
  const episodes = ref([])      // 集数列表
  const freeEpisodes = ref(0)   // 免费集数
  const currentEpisode = ref(1) // 当前播放集数
  const beans = ref(0)          // 用户金豆余额
  const plans = ref([])         // 充值套餐列表
  const loading = ref(false)
  const error = ref('')

  const isEpisodeFree = computed(() => (ep) => ep <= freeEpisodes.value)
  const promoId = computed(() => getAttribution().promoId || '')

  async function loadDrama() {
    const pid = promoId.value
    if (!pid) {
      error.value = '无效的推广链接'
      return
    }
    loading.value = true
    try {
      const res = await getDramaByPromo(pid)
      if (res.code === 0) {
        drama.value = res.data.drama
        episodes.value = res.data.episodes || []
        freeEpisodes.value = res.data.free_episodes || 0
        plans.value = res.data.plans || []
      } else {
        error.value = res.message || '加载失败'
      }
    } catch (e) {
      error.value = '网络错误，请刷新重试'
    } finally {
      loading.value = false
    }
  }

  async function loadUserInfo() {
    try {
      const res = await getUserInfo()
      if (res.code === 0) {
        beans.value = res.data.beans ?? 0
      }
    } catch (e) {
      // 静默失败，不影响播放
    }
  }

  function setCurrentEpisode(ep) {
    currentEpisode.value = ep
  }

  return {
    drama, episodes, freeEpisodes, currentEpisode,
    beans, plans, loading, error,
    isEpisodeFree, promoId,
    loadDrama, loadUserInfo, setCurrentEpisode,
  }
})
