/**
 * 请求优化：防抖、去重、短期内存缓存（指令 #076）
 * 适用于返回 Promise 的 API 函数；缓存 key 由调用方生成，避免误缓存 POST。
 */

const requestCache = new Map()
const pendingRequests = new Map()
const debounceTimers = new Map()

/**
 * @param {(...args: any[]) => string} keyResolver
 * @param {(...args: any[]) => Promise<any>} fn
 */
export function deduplicateAsync(keyResolver, fn) {
  return async (...args) => {
    const key = keyResolver(...args)
    if (pendingRequests.has(key)) {
      return pendingRequests.get(key)
    }
    const p = Promise.resolve(fn(...args)).finally(() => {
      pendingRequests.delete(key)
    })
    pendingRequests.set(key, p)
    return p
  }
}

/**
 * @param {(...args: any[]) => string} keyResolver
 * @param {number} ttlMs
 * @param {(...args: any[]) => Promise<any>} fn
 */
export function cacheAsync(keyResolver, ttlMs, fn) {
  return async (...args) => {
    const key = keyResolver(...args)
    const now = Date.now()
    const hit = requestCache.get(key)
    if (hit && now - hit.t < ttlMs) {
      return hit.v
    }
    const v = await fn(...args)
    requestCache.set(key, { v, t: now })
    return v
  }
}

/**
 * @param {(...args: any[]) => string} keyResolver
 * @param {number} delayMs
 * @param {(...args: any[]) => Promise<any>} fn
 */
export function debounceAsync(keyResolver, delayMs, fn) {
  return (...args) =>
    new Promise((resolve, reject) => {
      const key = keyResolver(...args)
      const t = debounceTimers.get(key)
      if (t) clearTimeout(t)
      debounceTimers.set(
        key,
        setTimeout(async () => {
          debounceTimers.delete(key)
          try {
            resolve(await fn(...args))
          } catch (e) {
            reject(e)
          }
        }, delayMs),
      )
    })
}

/** 删除 key 前缀匹配的缓存项 */
export function clearCacheKeyPrefix(prefix) {
  for (const k of requestCache.keys()) {
    if (k.startsWith(prefix)) {
      requestCache.delete(k)
    }
  }
}

export function clearAllRequestCache() {
  requestCache.clear()
}
