/**
 * 配置文件 - 登录信息、网站URL、延时参数等
 */
module.exports = {
  // ===== 网站地址 =====
  baseUrl: 'https://adminxyz.dramahub8.com',
  taskUrl: 'https://adminxyz.dramahub8.com/advertiseTools/task',

  // ===== 登录信息 =====
  username: 'chuhai',
  password: '123456',

  // ===== Excel 数据文件路径 =====
  excelPath: './data/tasks.xlsx',

  // ===== 浏览器配置 =====
  browser: {
    headless: false,               // 是否无头模式（false = 显示浏览器窗口）
    profileDir: './browser-data',  // 浏览器用户数据目录（保存登录状态）
    browsersPath: './pw-browsers', // Playwright 浏览器存放路径（项目目录内）
    slowMo: 0,                     // 全局慢动作（毫秒），调试时可设为 50-100
  },

  // ===== 延时配置（毫秒）===== 固定延时，不再随机
  delay: {
    short: 200,      // 简单操作间（点击、输入）
    medium: 400,     // 弹窗打开/关闭
    long: 800,       // 搜索等待结果
  },
};

