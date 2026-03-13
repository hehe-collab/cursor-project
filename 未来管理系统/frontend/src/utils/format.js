/**
 * 格式化工具 - 统一数字、金额展示
 */
export function formatNum(n) {
  return String(Math.round(n ?? 0)).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function formatMoney(n) {
  const v = Number(n ?? 0)
  return v.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}
