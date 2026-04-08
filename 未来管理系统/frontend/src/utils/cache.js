/**
 * API 响应缓存（指令 #091）— 用于分类、标签等变更频率低的 GET 结果
 */

class CacheManager {
  constructor() {
    this.cache = new Map()
    this.expiryTimes = new Map()
  }

  set(key, value, ttl = 5 * 60 * 1000) {
    this.cache.set(key, value)
    this.expiryTimes.set(key, Date.now() + ttl)
  }

  get(key) {
    const expiryTime = this.expiryTimes.get(key)
    if (!expiryTime || Date.now() > expiryTime) {
      this.delete(key)
      return null
    }
    return this.cache.get(key)
  }

  delete(key) {
    this.cache.delete(key)
    this.expiryTimes.delete(key)
  }

  clear() {
    this.cache.clear()
    this.expiryTimes.clear()
  }

  has(key) {
    return this.get(key) !== null
  }

  /** 删除 key 以某前缀开头的条目 */
  clearKeyPrefix(prefix) {
    for (const k of [...this.cache.keys()]) {
      if (k.startsWith(prefix)) {
        this.delete(k)
      }
    }
  }
}

export const cacheManager = new CacheManager()
