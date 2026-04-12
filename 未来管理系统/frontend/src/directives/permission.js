/**
 * v-permission 指令
 * 用法：<el-button v-permission="'drama:delete'">删除</el-button>
 *       <el-button v-permission="['drama:delete', 'drama:edit']" mode="any">操作</el-button>
 *
 * mode 默认 'all'（需要所有权限），设为 'any' 则满足任一即可
 */
import { hasPermission } from '@/utils/permission'

export default {
  mounted(el, binding) {
    const { value, modifiers } = binding
    const mode = modifiers.any ? 'any' : 'all'

    if (!value) return

    const list = Array.isArray(value) ? value : [value]

    let allowed = false
    if (mode === 'any') {
      allowed = list.some((p) => hasPermission(p))
    } else {
      allowed = list.every((p) => hasPermission(p))
    }

    if (!allowed) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  },
}
