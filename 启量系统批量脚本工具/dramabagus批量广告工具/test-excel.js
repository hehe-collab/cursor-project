/**
 * 校验 config.excelPath 能否被 lib/excel.js 正确解析
 * 运行: npm test
 */
const path = require('path');
const fs = require('fs');
const { readTasks, printTaskSummary, allowsEmptyBid, normalizeBiddingStrategy } = require('./lib/excel');
const { log } = require('./lib/utils');
const config = require('./config');

function main() {
  console.log('\n' + '='.repeat(70));
  log('Excel 校验（DramaBagus）', 'INFO');
  console.log('='.repeat(70) + '\n');

  const excelPath = path.isAbsolute(config.excelPath)
    ? config.excelPath
    : path.join(__dirname, config.excelPath);

  if (!fs.existsSync(excelPath)) {
    log(`未找到文件: ${excelPath}`, 'ERROR');
    log('请先运行: npm run template', 'INFO');
    process.exit(1);
  }

  let taskGroups;
  try {
    taskGroups = readTasks(config.excelPath);
  } catch (err) {
    log(`读取失败: ${err.message}`, 'ERROR');
    process.exit(1);
  }

  if (taskGroups.length === 0) {
    log('未解析到任何任务组', 'WARN');
    process.exit(1);
  }

  console.log(`\n共 ${taskGroups.length} 个任务组\n`);

  for (let i = 0; i < taskGroups.length; i++) {
    const g = taskGroups[i];
    const blocks = g.accountBlocks || [];
    const firstB = blocks[0];
    const firstAg = firstB?.adGroups?.[0];
    console.log('─'.repeat(70));
    console.log(`任务 ${g.taskId} | 主体: ${g.entity} | 提交次数: ${g.submitCount}`);
    console.log(
      `  首条广告组: 优化目标=${firstAg?.optimizationTarget || '—'} | 策略=${normalizeBiddingStrategy(firstAg?.optimizationTarget, firstAg?.biddingStrategy)}`
    );

    const dateOk = /^\d{4}-\d{2}-\d{2}$/.test(firstAg?.startDate || '');
    const timeOk = /^\d{2}:\d{2}:\d{2}$/.test(firstAg?.startTime || '');
    console.log(
      `  开始: ${firstAg?.startDate || ''} ${firstAg?.startTime || ''}  ${dateOk && timeOk ? '✓' : '⚠ 日期/时间格式'}`
    );

    for (const block of blocks) {
      const pd = block.projectDailyBudget ? `项目预算=${block.projectDailyBudget}` : '项目预算=空';
      console.log(`  账户 ${block.accountId} | ${pd} | 广告组条数=${block.adGroups?.length || 0}`);
      for (const ag of block.adGroups || []) {
        const bidEmpty = !String(ag.bid || '').trim();
        const needBid = bidEmpty && !allowsEmptyBid(ag.optimizationTarget, ag.biddingStrategy);
        const matOk = !!(ag.materialKeyword || (ag.materialIds && ag.materialIds.length));
        const titleOk = ag.titles && ag.titles.length > 0;
        const flags = [
          needBid ? '缺出价(策略要求)' : null,
          !matOk ? '缺素材' : null,
          !titleOk ? '缺标题' : null,
          !ag.linkKeyword ? '缺链接关键词' : null,
        ].filter(Boolean);
        console.log(
          `    · 序号${ag.adGroupSeq} 预算=${ag.budget || '空'} 链接OK=${!!ag.linkKeyword}` +
            (flags.length ? ` ⚠ ${flags.join('; ')}` : '')
        );
      }
    }
  }

  console.log('\n' + '='.repeat(70));
  printTaskSummary(taskGroups);
  console.log('✅ 校验完成\n');
}

main();
