/**
 * 删除确认 composable - 统一确认弹窗与请求
 */
import { ElMessage, ElMessageBox } from 'element-plus'

export function useConfirmDelete(options = {}) {
  const {
    message = '确定要删除吗？',
    title = '提示',
    onSuccess,
    onCancel,
  } = options

  async function confirmDelete(deleteFn, row) {
    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
      await deleteFn(row)
      ElMessage.success('删除成功')
      onSuccess?.(row)
    } catch (e) {
      if (e !== 'cancel') {
        ElMessage.error('删除失败')
      } else {
        onCancel?.(row)
      }
    }
  }

  return { confirmDelete }
}
