/**
 * 防抖 composable - 搜索输入等场景
 */
import { ref } from 'vue'

export function useDebounceFn(fn, delay = 300) {
  const timer = ref(null)
  return function (...args) {
    if (timer.value) clearTimeout(timer.value)
    timer.value = setTimeout(() => {
      fn(...args)
      timer.value = null
    }, delay)
  }
}
