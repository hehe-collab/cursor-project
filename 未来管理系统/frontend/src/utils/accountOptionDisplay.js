function text(value) {
  return value == null ? '' : String(value).trim()
}

export function formatHistoryAccountOptions(items = [], options = {}) {
  const {
    countKey = '',
    countLabel = '',
    includeSubjectName = true,
  } = options

  return items
    .map((item) => {
      const accountId = text(item?.accountId)
      const accountName = text(item?.accountName)
      const subjectName = text(item?.subjectName)
      const nameParts = []

      if (accountName) nameParts.push(accountName)
      if (includeSubjectName && subjectName && subjectName !== accountName) {
        nameParts.push(subjectName)
      }

      let label = accountId
      if (nameParts.length > 0) {
        label += ` - ${nameParts.join(' / ')}`
      }

      const countValue = countKey ? Number(item?.[countKey] ?? 0) : 0
      if (countKey && Number.isFinite(countValue) && countValue > 0 && countLabel) {
        label += `（${countValue}${countLabel}）`
      }

      return {
        ...item,
        accountId,
        label,
      }
    })
    .filter((item) => item.accountId)
}

export function buildHistoryAccountEmptyText(sceneName) {
  return `这里只显示${sceneName}中已出现过的账户ID；如果为空，说明当前${sceneName}还没有账户数据。`
}
