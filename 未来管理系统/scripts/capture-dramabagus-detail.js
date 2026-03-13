/**
 * 深度抓取 DramaBagus 界面细节
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

    console.log('访问登录页...');
    await page.goto('https://admin.dramabagus.com', { waitUntil: 'networkidle2', timeout: 15000 });
    await new Promise(r => setTimeout(r, 2000));

    // 登录
    const usernameInput = await page.$('input[type="text"]') || await page.$('input[name="username"]');
    const passwordInput = await page.$('input[type="password"]');
    if (usernameInput && passwordInput) {
      await usernameInput.type('qiliang', { delay: 30 });
      await passwordInput.type('xiaodong', { delay: 30 });
      const submitBtn = await page.$('button[type="submit"]') || await page.$('button.el-button--primary');
      if (submitBtn) await submitBtn.click();
      await new Promise(r => setTimeout(r, 3000));
    }

    // 抓取看板页完整信息
    const dashboardDetail = await page.evaluate(() => {
      const getStyle = (el, prop) => el ? window.getComputedStyle(el).getPropertyValue(prop) : '';
      const sidebar = document.querySelector('.el-aside') || document.querySelector('[class*="sidebar"]') || document.querySelector('aside');
      const header = document.querySelector('.el-header') || document.querySelector('header') || document.querySelector('[class*="header"]');
      const cards = [...document.querySelectorAll('.el-card, [class*="card"], .stat-card, [class*="stat"]')];
      const menuItems = [...document.querySelectorAll('.el-menu-item, .el-sub-menu')].map(m => ({
        text: m.innerText?.trim().split('\n')[0],
        hasSub: !!m.querySelector('.el-sub-menu__title') || m.querySelector('.el-sub-menu'),
      }));

      return {
        sidebar: sidebar ? {
          bg: getStyle(sidebar, 'background-color'),
          width: getStyle(sidebar, 'width'),
          color: getStyle(sidebar, 'color'),
        } : null,
        header: header ? {
          bg: getStyle(header, 'background-color'),
          height: getStyle(header, 'height'),
        } : null,
        mainBg: getStyle(document.querySelector('.el-main') || document.body, 'background-color'),
        cards: cards.slice(0, 4).map(c => ({
          bg: getStyle(c, 'background-color'),
          padding: getStyle(c, 'padding'),
          borderRadius: getStyle(c, 'border-radius'),
        })),
        menuItems,
        logoText: document.querySelector('.logo')?.innerText || document.querySelector('[class*="logo"]')?.innerText || '',
        breadcrumb: document.querySelector('.el-breadcrumb')?.innerText || '',
      };
    });

    await page.screenshot({ path: 'scripts/dramabagus-dashboard.png', fullPage: false });
    console.log('看板截图已保存');

    // 点击「剧集信息」进入剧集页
    const dramaLink = await page.$('a[href*="drama"]') || await page.$('text=剧集信息') || await page.evaluateHandle(() => {
      const items = [...document.querySelectorAll('.el-menu-item, a')];
      return items.find(i => i.innerText?.includes('剧集信息')) || null;
    });
    if (dramaLink) {
      try {
        await dramaLink.asElement().click();
        await new Promise(r => setTimeout(r, 2000));
        const dramaPage = await page.evaluate(() => {
          const table = document.querySelector('.el-table');
          const headers = table ? [...table.querySelectorAll('th')].map(t => t.innerText?.trim()) : [];
          const searchInputs = [...document.querySelectorAll('.el-input__inner')].map(i => i.placeholder || '');
          const buttons = [...document.querySelectorAll('button')].map(b => b.innerText?.trim()).filter(Boolean);
          return { headers, searchInputs, buttons };
        });
        dashboardDetail.dramaPage = dramaPage;
        await page.screenshot({ path: 'scripts/dramabagus-drama.png', fullPage: false });
        console.log('剧集页截图已保存');
      } catch (e) {
        console.log('剧集页导航失败:', e.message);
      }
    }

    // 点击「统计」
    const statsItem = await page.evaluateHandle(() => {
      const items = [...document.querySelectorAll('.el-menu-item')];
      return items.find(i => i.innerText?.trim() === '统计') || null;
    });
    if (statsItem) {
      try {
        const el = await statsItem.asElement();
        if (el) {
          await el.click();
          await new Promise(r => setTimeout(r, 2000));
          await page.screenshot({ path: 'scripts/dramabagus-stats.png', fullPage: false });
          console.log('统计页截图已保存');
        }
      } catch (e) {}
    }

    fs.writeFileSync('scripts/dramabagus-detail.json', JSON.stringify(dashboardDetail, null, 2), 'utf8');
    console.log('详情已保存');

  } finally {
    await browser.close();
  }
}

main().catch(e => console.error(e));
