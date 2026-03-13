/**
 * 分页参数校验 - 防止异常 page/pageSize
 */
function parsePage(val, defaultVal = 1) {
  const n = parseInt(val, 10)
  return isNaN(n) || n < 1 ? defaultVal : n
}

function parsePageSize(val, defaultVal = 10, max = 100) {
  const n = parseInt(val, 10)
  if (isNaN(n) || n < 1) return defaultVal
  return Math.min(max, n)
}

function parsePagination(query) {
  const page = parsePage(query.page, 1)
  const pageSize = parsePageSize(query.pageSize, 10, 100)
  const offset = (page - 1) * pageSize
  return { page, pageSize, offset }
}

module.exports = { parsePage, parsePageSize, parsePagination }
