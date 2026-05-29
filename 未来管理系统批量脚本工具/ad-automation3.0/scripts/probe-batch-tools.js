/** 快速探测：登录 + 批量工具页 + 选择主体 */
process.env.E2E_AUTO = '1';
const config = require('../config');
config.browser.headless = true;

const { launchBrowser, closeBrowserGracefully, preparePage } = require('../lib/automation');
const { readTasks } = require('../lib/excel');
const { shortDelay } = require('../lib/utils');

async function main() {
  const tasks = readTasks('./data/tasks-e2e.xlsx');
  const entity = tasks[0].entity;
  const { context, page } = await launchBrowser();
  try {
    await preparePage(page);
    const select = page.locator('.filter-card .el-form-item').filter({
      has: page.locator('.el-form-item__label', { hasText: /^主体$/ }),
    }).locator('.el-select').first();
    await select.click();
    await shortDelay();
    const dropdown = page.locator('.el-select-dropdown:visible').last();
    await dropdown.locator('.el-select-dropdown__item').filter({ hasText: entity })
      .click({ timeout: 8000 });
    console.log('PROBE_OK entity=' + entity);
  } finally {
    await closeBrowserGracefully(context);
  }
}

main().catch((e) => {
  console.error('PROBE_FAIL', e.message);
  process.exit(1);
});
