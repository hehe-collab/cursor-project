import { ElMessage } from 'element-plus'

/**
 * 复制文本到剪贴板
 * @param {string|number} text - 要复制的文本
 * @param {string} label - 字段名称（用于提示）
 */
export const copyToClipboard = async (text, label = '内容') => {
  try {
    const textToCopy = String(text ?? '')

    if (!textToCopy) {
      ElMessage.warning(`${label}为空，无法复制`)
      return false
    }

    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(textToCopy)
      ElMessage.success(`${label} 已复制：${textToCopy}`)
      return true
    }

    const textarea = document.createElement('textarea')
    textarea.value = textToCopy
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()

    const successful = document.execCommand('copy')
    document.body.removeChild(textarea)

    if (successful) {
      ElMessage.success(`${label} 已复制：${textToCopy}`)
      return true
    }
    throw new Error('复制失败')
  } catch (error) {
    console.error('复制失败:', error)
    ElMessage.error('复制失败，请手动复制')
    return false
  }
}
