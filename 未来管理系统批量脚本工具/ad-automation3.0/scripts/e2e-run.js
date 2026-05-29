/**
 * E2E 全链路测试：自动登录 → 批量工具页 → 执行 1 个 Excel 任务 → 退出
 *
 * 用法: node scripts/e2e-run.js
 */
process.env.E2E_AUTO = '1';

const config = require('../config');
config.excelPath = './data/tasks-e2e.xlsx';
config.parallel.enabled = false;
config.parallel.pollAdTaskAfterBatch = false;
config.validation.enableScreenshot = true;
config.browser.headless = process.env.E2E_HEADLESS !== '0';

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const excelPath = path.join(__dirname, '..', 'data', 'tasks-e2e.xlsx');

console.log('\n═══ E2E 准备：测试 Excel ═══\n');
if (!fs.existsSync(excelPath)) {
  execSync('node scripts/generate-e2e-excel.js', {
    cwd: path.join(__dirname, '..'),
    stdio: 'inherit',
  });
} else {
  console.log('✅ 复用已有 Excel:', excelPath);
}

console.log('\n═══ E2E 开始：执行自动化主流程 ═══\n');
require('../index.js');
