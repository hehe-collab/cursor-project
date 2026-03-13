/**
 * 抓取 DramaBagus 各页面的弹窗/对话框结构
 */
const puppeteer = require('puppeteer');
const fs = require('fs');

async function main() {
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox', '--disable-setuid-sandbox'],
  });

  const dialogs = {};

  try {
    const page = await browser.newPage();
    await page.setViewport({ width: 1920, height: 1080 });
    await page.setUserAgent('Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36');

    console.log('登录...');
    await page.goto('https://admin.dramabagus.com', { waitUntil: 'networkidle2', timeout: 20000 });
    await new Promise(r => setTimeout(r, 2000));

    const usernameInput = await page.$('input[type="text"]') || await page.$('input[name="username"]');
    const passwordInput = await page.$('input[type="password"]');
    if (usernameInput && passwordInput) {
      await usernameInput.type('qiliang', { delay: 30 });
      await passwordInput.type('xiaodong', { delay: 30 });
      const submitBtn = await page.$('button[type="submit"]') || await page.$('button.el-button--primary');
      if (submitBtn) await submitBtn.click();
      await new Promise(r => setTimeout(r, 5000));
    }

    const captureDialog = () => page.evaluate(() => {
      const d = document.querySelector('.el-dialog');
      if (!d) return null;
      const title = d.querySelector('.el-dialog__title')?.innerText?.trim() || '';
      const labels = [];
      d.querySelectorAll('.el-form-item__label').forEach(l => labels.push(l.innerText?.trim().replace(/[:：]/g, '')));
      const inputs = [];
      d.querySelectorAll('.el-form-item input, .el-form-item .el-select, .el-form-item textarea').forEach(i => {
        const tag = i.tagName?.toLowerCase();
        const ph = i.placeholder || i.getAttribute('placeholder') || '';
        const type = i.type || (tag === 'textarea' ? 'textarea' : 'text');
        inputs.push({ placeholder: ph.substring(0, 60), type });
      });
      const footBtns = [];
      d.querySelectorAll('.el-dialog__footer .el-button').forEach(b => footBtns.push(b.innerText?.trim()));
      return { title, labels, inputs, footBtns };
    });

    const navTo = async (menuPath) => {
      for (const step of menuPath) {
        if (step.sub) {
          const sub = await page.evaluateHandle((t) => {
            const s = [...document.querySelectorAll('.el-sub-menu')].find(x => x.innerText?.includes(t));
            return s;
          }, step.sub);
          const el = await sub.asElement();
          if (el) { await el.click(); await new Promise(r => setTimeout(r, 600)); }
        }
        const item = await page.evaluateHandle((t) => {
          const items = [...document.querySelectorAll('.el-menu-item')];
          return items.find(i => i.innerText?.trim() === t);
        }, step.item);
        const itemEl = await item.asElement();
        if (itemEl) { await itemEl.click(); await new Promise(r => setTimeout(r, 2500)); }
      }
    };

    // 投放链接配置 - 点击新增
    await navTo([{ sub: '投放配置', item: '投放链接配置' }]);
    const addLinkBtn = await page.evaluateHandle(() => {
      const btns = [...document.querySelectorAll('button')];
      return btns.find(b => b.innerText?.trim() === '新增');
    });
    const addLinkEl = await addLinkBtn.asElement();
    if (addLinkEl) {
      await addLinkEl.click();
      await new Promise(r => setTimeout(r, 800));
      dialogs['投放链接-新增'] = await captureDialog();
      console.log('投放链接-新增:', JSON.stringify(dialogs['投放链接-新增']));
      await page.keyboard.press('Escape');
      await new Promise(r => setTimeout(r, 500));
    }

    // 充值方案 - 点击新增
    await navTo([{ sub: '充值方案', item: '充值方案' }]);
    const addPlanBtn = await page.evaluateHandle(() => {
      const btns = [...document.querySelectorAll('button')];
      return btns.find(b => b.innerText?.trim() === '新增');
    });
    const addPlanEl = await addPlanBtn.asElement();
    if (addPlanEl) {
      await addPlanEl.click();
      await new Promise(r => setTimeout(r, 800));
      dialogs['充值方案-新增'] = await captureDialog();
      console.log('充值方案-新增:', JSON.stringify(dialogs['充值方案-新增']));
      await page.keyboard.press('Escape');
      await new Promise(r => setTimeout(r, 500));
    }

    // 账户管理 - 点击新增
    await navTo([{ sub: '投放工具', item: '账户管理' }]);
    const addAccBtn = await page.evaluateHandle(() => {
      const btns = [...document.querySelectorAll('button')];
      return btns.find(b => b.innerText?.trim() === '新增');
    });
    const addAccEl = await addAccBtn.asElement();
    if (addAccEl) {
      await addAccEl.click();
      await new Promise(r => setTimeout(r, 800));
      dialogs['账户管理-新增'] = await captureDialog();
      console.log('账户管理-新增:', JSON.stringify(dialogs['账户管理-新增']));
      await page.keyboard.press('Escape');
      await new Promise(r => setTimeout(r, 500));
    }

    // 用户列表 - 点击详情
    await navTo([{ sub: '用户信息', item: '用户列表' }]);
    const detailBtn = await page.evaluateHandle(() => {
      const links = [...document.querySelectorAll('.el-table tbody .el-button, .el-table tbody .el-link')];
      return links.find(l => l.innerText?.trim() === '详情');
    });
    const detailEl = await detailBtn.asElement();
    if (detailEl) {
      await detailEl.click();
      await new Promise(r => setTimeout(r, 800));
      dialogs['用户详情'] = await captureDialog();
      console.log('用户详情:', JSON.stringify(dialogs['用户详情']));
      await page.keyboard.press('Escape');
      await new Promise(r => setTimeout(r, 500));
    }

    // 投放链接 - 点击修改，抓取修改弹窗
    await navTo([{ sub: '投放配置', item: '投放链接配置' }]);
    const editLinkBtn = await page.evaluateHandle(() => {
      const links = [...document.querySelectorAll('.el-table tbody .el-button, .el-table tbody .el-link')];
      return links.find(l => l.innerText?.trim() === '修改');
    });
    const editLinkEl = await editLinkBtn.asElement();
    if (editLinkEl) {
      await editLinkEl.click();
      await new Promise(r => setTimeout(r, 800));
      dialogs['投放链接-修改'] = await captureDialog();
      console.log('投放链接-修改:', JSON.stringify(dialogs['投放链接-修改']));
      await page.keyboard.press('Escape');
      await new Promise(r => setTimeout(r, 500));
    }

    fs.writeFileSync('scripts/dramabagus-dialogs.json', JSON.stringify(dialogs, null, 2), 'utf8');
    console.log('\n已保存 scripts/dramabagus-dialogs.json');

  } catch (err) {
    console.error(err);
    fs.writeFileSync('scripts/dramabagus-dialogs.json', JSON.stringify(dialogs, null, 2), 'utf8');
  } finally {
    await browser.close();
  }
}

main().catch(e => console.error(e));
