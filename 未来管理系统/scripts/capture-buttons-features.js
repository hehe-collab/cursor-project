/**
 * 抓取 DramaBagus 所有页面的按钮和功能
 */
const puppeteer = require('puppeteer');
const fs = require('fs');

async function main() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  const allData = {};

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
    await page.setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36');

    console.log('登录...');
    await page.goto('https://admin.dramabagus.com', { waitUntil: 'networkidle2', timeout: 15000 });
    await new Promise(r => setTimeout(r, 2000));

    const usernameInput = await page.$('input[type="text"]') || await page.$('input[name="username"]');
    const passwordInput = await page.$('input[type="password"]');
    if (usernameInput && passwordInput) {
      await usernameInput.type('qiliang', { delay: 30 });
      await passwordInput.type('xiaodong', { delay: 30 });
      const submitBtn = await page.$('button[type="submit"]') || await page.$('button.el-button--primary');
      if (submitBtn) await submitBtn.click();
      await new Promise(r => setTimeout(r, 4000));
    }

    const capture = () => page.evaluate(() => {
      const r = { buttons: [], inputs: [], tableHeaders: [], tableActions: [], labels: [], cardTitles: [] };
      document.querySelectorAll('button').forEach(b => {
        const t = b.innerText?.trim();
        if (t) r.buttons.push(t);
      });
      document.querySelectorAll('input.el-input__inner, input[type="text"], input[type="number"]').forEach(i => {
        r.inputs.push({ placeholder: (i.placeholder || '').substring(0, 50), type: i.type });
      });
      document.querySelectorAll('.el-table thead th').forEach(th => r.tableHeaders.push(th.innerText?.trim()));
      document.querySelectorAll('.el-table tbody .el-button, .el-table tbody .el-link').forEach(a => r.tableActions.push(a.innerText?.trim()));
      document.querySelectorAll('.el-form-item__label').forEach(l => r.labels.push(l.innerText?.trim().replace(':', '')));
      document.querySelectorAll('.el-card__header span, .card-header').forEach(c => r.cardTitles.push(c.innerText?.trim()));
      return r;
    });

    // 看板
    allData['看板'] = await capture();
    console.log('看板:', JSON.stringify(allData['看板']));

    // 点击统计
    const statsItem = await page.evaluateHandle(() => {
      const items = [...document.querySelectorAll('.el-menu-item')];
      return items.find(i => i.innerText?.trim() === '统计');
    });
    const statsEl = await statsItem.asElement();
    if (statsEl) {
      await statsEl.click();
      await new Promise(r => setTimeout(r, 3000));
      allData['统计'] = await capture();
      console.log('统计:', JSON.stringify(allData['统计']));
    }

    // 展开短剧管理，点击剧集信息
    const dramaSub = await page.evaluateHandle(() => {
      const subs = [...document.querySelectorAll('.el-sub-menu')];
      return subs.find(s => s.innerText?.includes('短剧管理'));
    });
    const dramaSubEl = await dramaSub.asElement();
    if (dramaSubEl) {
      await dramaSubEl.click();
      await new Promise(r => setTimeout(r, 800));
      const dramaItem = await page.evaluateHandle(() => {
        const items = [...document.querySelectorAll('.el-menu-item')];
        return items.find(i => i.innerText?.trim() === '剧集信息');
      });
      const dramaItemEl = await dramaItem.asElement();
      if (dramaItemEl) {
        await dramaItemEl.click();
        await new Promise(r => setTimeout(r, 3000));
        allData['剧集信息'] = await capture();
        console.log('剧集信息:', JSON.stringify(allData['剧集信息']));
      }
    }

    // 用户列表
    const userSub = await page.evaluateHandle(() => {
      const subs = [...document.querySelectorAll('.el-sub-menu')];
      return subs.find(s => s.innerText?.includes('用户信息'));
    });
    const userSubEl = await userSub.asElement();
    if (userSubEl) {
      await userSubEl.click();
      await new Promise(r => setTimeout(r, 800));
      const userItem = await page.evaluateHandle(() => {
        const items = [...document.querySelectorAll('.el-menu-item')];
        return items.find(i => i.innerText?.trim() === '用户列表');
      });
      const userItemEl = await userItem.asElement();
      if (userItemEl) {
        await userItemEl.click();
        await new Promise(r => setTimeout(r, 3000));
        allData['用户列表'] = await capture();
        console.log('用户列表:', JSON.stringify(allData['用户列表']));
      }
    }

    // 充值记录
    const rechargeItem = await page.evaluateHandle(() => {
      const items = [...document.querySelectorAll('.el-menu-item')];
      return items.find(i => i.innerText?.trim() === '充值记录');
    });
    const rechargeEl = await rechargeItem.asElement();
    if (rechargeEl) {
      await rechargeEl.click();
      await new Promise(r => setTimeout(r, 3000));
      allData['充值记录'] = await capture();
      console.log('充值记录:', JSON.stringify(allData['充值记录']));
    }

    // 投放链接配置
    const deliverySub = await page.evaluateHandle(() => {
      const subs = [...document.querySelectorAll('.el-sub-menu')];
      return subs.find(s => s.innerText?.includes('投放配置'));
    });
    if (deliverySub) {
      const deliverySubEl = await deliverySub.asElement();
      if (deliverySubEl) {
        await deliverySubEl.click();
        await new Promise(r => setTimeout(r, 800));
        const linkItem = await page.evaluateHandle(() => {
          const items = [...document.querySelectorAll('.el-menu-item')];
          return items.find(i => i.innerText?.trim() === '投放链接配置');
        });
        const linkEl = await linkItem.asElement();
        if (linkEl) {
          await linkEl.click();
          await new Promise(r => setTimeout(r, 3000));
          allData['投放链接配置'] = await capture();
          console.log('投放链接配置:', JSON.stringify(allData['投放链接配置']));
        }
      }
    }

    // 充值方案
    const rechargePlanSub = await page.evaluateHandle(() => {
      const subs = [...document.querySelectorAll('.el-sub-menu')];
      return subs.find(s => s.innerText?.includes('充值方案'));
    });
    if (rechargePlanSub) {
      const rpEl = await rechargePlanSub.asElement();
      if (rpEl) {
        await rpEl.click();
        await new Promise(r => setTimeout(r, 800));
        const planItem = await page.evaluateHandle(() => {
          const items = [...document.querySelectorAll('.el-menu-item')];
          return items.find(i => i.innerText?.trim() === '充值方案' && !i.closest('.el-sub-menu__title'));
        });
        const planEl = await planItem.asElement();
        if (planEl) {
          await planEl.click();
          await new Promise(r => setTimeout(r, 3000));
          allData['充值方案'] = await capture();
          console.log('充值方案:', JSON.stringify(allData['充值方案']));
        }
      }
    }

    // 账户管理
    const toolsSub = await page.evaluateHandle(() => {
      const subs = [...document.querySelectorAll('.el-sub-menu')];
      return subs.find(s => s.innerText?.includes('投放工具'));
    });
    if (toolsSub) {
      const toolsEl = await toolsSub.asElement();
      if (toolsEl) {
        await toolsEl.click();
        await new Promise(r => setTimeout(r, 800));
        const accountItem = await page.evaluateHandle(() => {
          const items = [...document.querySelectorAll('.el-menu-item')];
          return items.find(i => i.innerText?.trim() === '账户管理');
        });
        const accEl = await accountItem.asElement();
        if (accEl) {
          await accEl.click();
          await new Promise(r => setTimeout(r, 3000));
          allData['账户管理'] = await capture();
          console.log('账户管理:', JSON.stringify(allData['账户管理']));
        }
      }
    }

    fs.writeFileSync('scripts/dramabagus-buttons.json', JSON.stringify(allData, null, 2), 'utf8');
    console.log('\n已保存 scripts/dramabagus-buttons.json');

  } finally {
    await browser.close();
  }
}

main().catch(e => console.error(e));
