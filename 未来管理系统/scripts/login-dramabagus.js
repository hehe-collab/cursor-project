/**
 * 登录 DramaBagus 后台并抓取菜单结构
 * 运行: node scripts/login-dramabagus.js
 */
const puppeteer = require('puppeteer');
const fs = require('fs');

async function main() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
    await page.setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36');

    console.log('正在访问登录页...');
    await page.goto('https://admin.dramabagus.com', { waitUntil: 'networkidle2', timeout: 15000 });

    await new Promise(r => setTimeout(r, 2000));

    // 尝试查找登录表单
    const usernameSelectors = ['input[name="username"]', 'input[type="text"]', 'input[placeholder*="用户"]', 'input[placeholder*="账号"]', '#username', '.username'];
    const passwordSelectors = ['input[name="password"]', 'input[type="password"]', '#password', '.password'];
    const submitSelectors = ['button[type="submit"]', 'button:has-text("登录")', 'input[type="submit"]', '.login-btn', 'button.el-button'];

    let usernameInput = null;
    for (const sel of usernameSelectors) {
      try {
        usernameInput = await page.$(sel);
        if (usernameInput) break;
      } catch {}
    }

    let passwordInput = null;
    for (const sel of passwordSelectors) {
      try {
        passwordInput = await page.$(sel);
        if (passwordInput) break;
      } catch {}
    }

    if (usernameInput && passwordInput) {
      console.log('找到登录表单，正在填写...');
      await usernameInput.type('qiliang', { delay: 50 });
      await passwordInput.type('xiaodong', { delay: 50 });

      const submitBtn = await page.$('button[type="submit"]') || await page.$('button.el-button--primary') || await page.$('button');
      if (submitBtn) {
        await submitBtn.click();
        console.log('已点击登录...');
        await new Promise(r => setTimeout(r, 3000));
      }
    } else {
      console.log('未找到标准登录表单，尝试截图当前页面...');
    }

    // 截图
    await page.screenshot({ path: 'scripts/dramabagus-after-login.png', fullPage: true });
    console.log('已保存截图: scripts/dramabagus-after-login.png');

    // 提取页面文本和菜单
    const content = await page.evaluate(() => {
      const menuItems = [];
      document.querySelectorAll('.el-menu-item, .el-sub-menu .el-menu-item, [class*="menu"] span, .sidebar a').forEach(el => {
        const text = el.innerText?.trim();
        if (text && text.length < 20) menuItems.push(text);
      });
      return {
        title: document.title,
        menuItems: [...new Set(menuItems)],
        bodyText: document.body?.innerText?.substring(0, 3000),
      };
    });

    fs.writeFileSync('scripts/dramabagus-content.json', JSON.stringify(content, null, 2), 'utf8');
    console.log('已保存页面内容: scripts/dramabagus-content.json');
    console.log('菜单项:', content.menuItems);

  } finally {
    await browser.close();
  }
}

main().catch((e) => {
  console.error('错误:', e.message);
  process.exit(1);
});
