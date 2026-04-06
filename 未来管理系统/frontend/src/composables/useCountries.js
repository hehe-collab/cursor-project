import { ref, computed, onMounted } from 'vue'
import { getAccountCountryList } from '@/api/common'
import { ElMessage } from 'element-plus'

/** 常用国家/地区编码 → 中文（与账户管理展示一致，未知编码原样显示） */
export const ACCOUNT_COUNTRY_LABEL = {
  TH: '泰国',
  ID: '印尼',
  VN: '越南',
  US: '美国',
  CN: '中国',
  PH: '菲律宾',
  MY: '马来西亚',
  JP: '日本',
  KR: '韩国',
  GB: '英国',
}

export function formatAccountCountryLabel(code) {
  if (code == null || code === '') return '—'
  const s = String(code).trim()
  return ACCOUNT_COUNTRY_LABEL[s] || s
}

const countries = ref([])
const loaded = ref(false)
const loading = ref(false)

/**
 * 拉取账户管理中已出现的国家（模块级缓存，避免多页重复请求）
 * @param {boolean} force
 */
export async function fetchAccountCountries(force = false) {
  if (!force && loaded.value) {
    return countries.value
  }
  loading.value = true
  try {
    const res = await getAccountCountryList()
    if (res.code === 0) {
      countries.value = Array.isArray(res.data) ? res.data : []
      loaded.value = true
    } else {
      ElMessage.error(res.message || '获取国家列表失败')
      countries.value = []
      loaded.value = true
    }
  } catch (e) {
    console.error('获取国家列表失败:', e)
    countries.value = []
    loaded.value = true
  } finally {
    loading.value = false
  }
  return countries.value
}

export function useCountries() {
  const countryFilterOptions = computed(() => [
    { label: '全部', value: '' },
    ...countries.value.map((c) => ({
      label: formatAccountCountryLabel(c),
      value: c,
    })),
  ])

  const countryMultiOptions = computed(() =>
    countries.value.map((c) => ({
      label: formatAccountCountryLabel(c),
      value: c,
    })),
  )

  onMounted(() => {
    fetchAccountCountries()
  })

  return {
    countries,
    loading,
    loaded,
    fetchCountries: fetchAccountCountries,
    refreshCountries: () => fetchAccountCountries(true),
    countryFilterOptions,
    countryMultiOptions,
    formatAccountCountryLabel,
  }
}
