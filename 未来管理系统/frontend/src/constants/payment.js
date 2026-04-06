/**
 * 充值方案：支付平台与国家→货币（指令 #027）
 * 国家键为 ISO 3166-1 alpha-2，与账户管理 ad_accounts.country 一致。
 */

export const PAYMENT_PLATFORMS = [{ label: 'HaiPay', value: 'HaiPay' }]

/** @type {Record<string, { code: string, name: string, symbol: string }>} */
export const COUNTRY_CODE_CURRENCY_MAP = {
  TH: { code: 'THB', name: '泰铢', symbol: '฿' },
  ID: { code: 'IDR', name: '印尼盾', symbol: 'Rp' },
  US: { code: 'USD', name: '美元', symbol: '$' },
  VN: { code: 'VND', name: '越南盾', symbol: '₫' },
  CN: { code: 'CNY', name: '人民币', symbol: '¥' },
  PH: { code: 'PHP', name: '菲律宾比索', symbol: '₱' },
  MY: { code: 'MYR', name: '马来西亚林吉特', symbol: 'RM' },
}

/**
 * 按账户中出现的国家代码生成货币下拉（去重、排序）
 * @param {string[]} countryCodes
 */
export function getCurrencyOptions(countryCodes) {
  const codes = Array.isArray(countryCodes) ? countryCodes.map((c) => String(c || '').trim().toUpperCase()).filter(Boolean) : []
  const options = []
  const added = new Set()
  for (const key of codes) {
    const cur = COUNTRY_CODE_CURRENCY_MAP[key]
    if (cur && !added.has(cur.code)) {
      options.push({
        label: `${cur.name} (${cur.code})`,
        value: cur.code,
        symbol: cur.symbol,
      })
      added.add(cur.code)
    }
  }
  return options.sort((a, b) => a.value.localeCompare(b.value))
}

/**
 * 无账户国家数据时，列出地图中全部货币（便于先配方案后补账户）
 */
export function getCurrencyOptionsForAccounts(countryCodes) {
  const fromAccounts = getCurrencyOptions(countryCodes)
  if (fromAccounts.length > 0) return fromAccounts
  const out = []
  const seen = new Set()
  for (const k of Object.keys(COUNTRY_CODE_CURRENCY_MAP)) {
    const c = COUNTRY_CODE_CURRENCY_MAP[k]
    if (!seen.has(c.code)) {
      seen.add(c.code)
      out.push({
        label: `${c.name} (${c.code})`,
        value: c.code,
        symbol: c.symbol,
      })
    }
  }
  return out.sort((a, b) => a.value.localeCompare(b.value))
}

/** 友商文档中的中文国名 → ISO（仅用于兼容展示，优先用账户国家码） */
export const LEGACY_COUNTRY_NAME_TO_CODE = {
  泰国: 'TH',
  印尼: 'ID',
  美国: 'US',
  越南: 'VN',
  中国: 'CN',
  菲律宾: 'PH',
  马来西亚: 'MY',
}

/**
 * @param {string} currencyCode
 * @returns {{ code: string, name: string, symbol: string } | null}
 */
export function getCurrencyInfo(currencyCode) {
  if (currencyCode == null || currencyCode === '') return null
  const code = String(currencyCode).trim().toUpperCase()
  for (const k of Object.keys(COUNTRY_CODE_CURRENCY_MAP)) {
    const cur = COUNTRY_CODE_CURRENCY_MAP[k]
    if (cur.code === code) return { ...cur }
  }
  return { code, name: code, symbol: '' }
}
