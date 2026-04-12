import DOMPurify from 'dompurify'

/**
 * 清理富文本 HTML（用于 v-html 渲染的场景）
 */
export function sanitizeHtml(dirty) {
  return DOMPurify.sanitize(dirty, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'a', 'p', 'br', 'ul', 'ol', 'li', 'span'],
    ALLOWED_ATTR: ['href', 'target', 'rel'],
  })
}

/**
 * 转义普通文本（用于插值表达式 {{ }} 之外的展示）
 */
export function escapeHtml(str) {
  if (!str) return ''
  const div = document.createElement('div')
  div.textContent = str
  return div.innerHTML
}

/**
 * 清理用户输入（提交前通用净化）
 */
export function cleanInput(value) {
  if (typeof value !== 'string') return value
  return value
    .replace(/<script[^>]*>.*?<\/script>/gi, '')
    .replace(/javascript:/gi, '')
    .replace(/on\w+\s*=/gi, '')
    .replace(/data:text\/html/gi, '')
    .trim()
}

/**
 * 清理对象中所有字符串字段
 */
export function cleanObject(obj) {
  if (!obj || typeof obj !== 'object') return obj
  const cleaned = Array.isArray(obj) ? [] : {}
  for (const key in obj) {
    const value = obj[key]
    if (typeof value === 'string') {
      cleaned[key] = cleanInput(value)
    } else if (typeof value === 'object' && value !== null) {
      cleaned[key] = cleanObject(value)
    } else {
      cleaned[key] = value
    }
  }
  return cleaned
}
