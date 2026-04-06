/**
 * 表格列表通用逻辑 composable
 * 用于分页、加载、筛选、错误处理
 */
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

export function useTableList(options = {}) {
  const {
    fetchApi,
    defaultQuery = {},
    defaultPageSize = 20,
    onError = (e) => ElMessage.error(e?.message || '加载失败'),
  } = options

  const loading = ref(false)
  const list = ref([])
  const total = ref(0)
  const query = reactive({
    page: 1,
    pageSize: defaultPageSize,
    ...defaultQuery,
  })

  async function loadList() {
    if (!fetchApi) return
    loading.value = true
    try {
      const params = { ...query }
      const res = await fetchApi(params).catch(() => ({ data: { list: [], total: 0 } }))
      list.value = res.data?.list || []
      total.value = res.data?.total ?? list.value.length
    } catch (e) {
      onError(e)
    } finally {
      loading.value = false
    }
  }

  function resetPage() {
    query.page = 1
  }

  function onReset(resetValues = {}) {
    Object.assign(query, { page: 1, ...resetValues })
    loadList()
  }

  return { loading, list, total, query, loadList, resetPage, onReset }
}
