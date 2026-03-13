/**
 * 统一错误处理中间件 - DramaBagus 底层对齐
 * 响应格式：{ code, data?, message }
 */
function errorHandler(err, req, res, next) {
  console.error('[Error]', err)
  const code = err.statusCode || err.code || 500
  const message = err.message || '服务器错误'
  res.status(code >= 400 ? code : 500).json({
    code: code >= 400 ? code : 500,
    message,
  })
}

module.exports = { errorHandler }
