/**
 * Excel 数据测试脚本 - 验证时间格式是否正确读取
 * 
 * 使用方法:
 *   node test-excel.js
 * 
 * 功能:
 *   - 读取 data/tasks.xlsx
 *   - 显示每个任务的详细信息（特别是时间格式）
 *   - 验证必填字段是否完整
 */

const { readTasks, printTaskSummary } = require('./lib/excel');
const { log } = require('./lib/utils');
const config = require('./config');

function testExcelData() {
  console.log('\n' + '='.repeat(70));
  log('Excel 数据测试 - 验证时间格式', 'INFO');
  console.log('='.repeat(70) + '\n');

  try {
    // 读取任务数据
    const taskGroups = readTasks(config.excelPath);

    if (taskGroups.length === 0) {
      log('未读取到任何任务数据', 'WARN');
      log('请检查 data/tasks.xlsx 是否存在且有数据', 'WARN');
      return;
    }

    console.log(`\n📊 读取到 ${taskGroups.length} 个任务组\n`);
    console.log('━'.repeat(70));

    // 遍历每个任务组，显示详细信息
    for (let i = 0; i < taskGroups.length; i++) {
      const task = taskGroups[i];
      
      console.log(`\n【任务 ${i + 1}】任务ID: ${task.taskId}`);
      console.log('─'.repeat(70));
      
      // 基本信息
      console.log(`\n📋 基本信息:`);
      console.log(`  主体:           ${task.entity}`);
      console.log(`  项目名称:       ${task.projectName}`);
      console.log(`  账户数量:       ${task.accounts.length}`);
      
      // 时间信息（重点）
      console.log(`\n⏰ 时间设置:`);
      console.log(`  开始日期:       ${task.startDate}`);
      console.log(`  开始时间:       ${task.startTime}`);
      console.log(`  完整时间:       ${task.startDate} ${task.startTime}`);
      
      // 验证时间格式
      const dateValid = /^\d{4}-\d{2}-\d{2}$/.test(task.startDate);
      const timeValid = /^\d{2}:\d{2}:\d{2}$/.test(task.startTime);
      
      if (dateValid) {
        console.log(`  ✅ 日期格式正确 (YYYY-MM-DD)`);
      } else {
        console.log(`  ❌ 日期格式错误: ${task.startDate}`);
        console.log(`     期望格式: YYYY-MM-DD (如 2026-02-10)`);
      }
      
      if (timeValid) {
        console.log(`  ✅ 时间格式正确 (HH:MM:SS)`);
      } else {
        console.log(`  ❌ 时间格式错误: ${task.startTime}`);
        console.log(`     期望格式: HH:MM:SS (如 02:00:00)`);
        console.log(`     或简化格式: HH:MM (如 02:00)`);
      }
      
      // 广告组设置
      console.log(`\n🎯 广告组设置:`);
      // 显示素材检索方式
      if (task.materialKeyword) {
        console.log(`  素材检索方式:   关键词 "${task.materialKeyword}"`);
      } else if (task.materialIds && task.materialIds.length > 0) {
        console.log(`  素材检索方式:   ID (${task.materialIds.length}个)`);
        console.log(`                  ${task.materialIds.slice(0, 3).join(', ')}${task.materialIds.length > 3 ? '...' : ''}`);
      } else {
        console.log(`  素材检索方式:   ⚠️  未设置`);
      }
      console.log(`  优化目标:       ${task.optimizationTarget}`);
      console.log(`  出价:           ${task.bid}`);
      console.log(`  预算:           ${task.budget}`);
      console.log(`  提交次数:       ${task.submitCount}`);
      
      // 可选设置
      console.log(`\n⚙️  可选设置:`);
      console.log(`  年龄:           ${task.age || '(默认 18+)'}`);
      console.log(`  认证身份:       ${task.identity || '(不选)'}`);
      console.log(`  商品库:         ${task.productStore || '(不选)'}`);
      console.log(`  商品:           ${task.product || '(不选)'}`);
      
      // 账户列表（每个账户有自己的标题）
      console.log(`\n👥 账户列表 (${task.accounts.length} 个):`);
      task.accounts.forEach((account, idx) => {
        const status = account.enable ? '✅ 启用' : '❌ 禁用';
        console.log(`  ${idx + 1}. ${account.accountId} | Pixel: ${account.pixel} | ${status}`);
        console.log(`     推广链接: ${account.linkKeyword}`);
        console.log(`     标题: ${account.titles.join(', ')}`);
      });
      
      // 验证必填字段
      console.log(`\n✔️  必填字段检查:`);
      
      // 检查所有账户是否都有标题
      const allAccountsHaveTitles = task.accounts.every(acc => acc.titles && acc.titles.length > 0);
      
      // 检查素材检索方式（关键词或ID至少有一个）
      const hasMaterialSearch = !!task.materialKeyword || (task.materialIds && task.materialIds.length > 0);
      const materialSearchValue = task.materialKeyword ? `关键词"${task.materialKeyword}"` : 
                                   (task.materialIds && task.materialIds.length > 0 ? `${task.materialIds.length}个ID` : '未设置');
      
      const checks = [
        { name: '主体', value: task.entity, valid: !!task.entity },
        { name: '项目名称', value: task.projectName, valid: !!task.projectName },
        { name: '素材检索', value: materialSearchValue, valid: hasMaterialSearch },
        { name: '标题（所有账户）', value: allAccountsHaveTitles ? '有' : '部分缺失', valid: allAccountsHaveTitles },
        { name: '出价', value: task.bid, valid: !!task.bid },
        { name: '预算', value: task.budget, valid: !!task.budget },
        { name: '开始日期', value: task.startDate, valid: !!task.startDate && dateValid },
        { name: '开始时间', value: task.startTime, valid: !!task.startTime && timeValid },
      ];
      
      let allValid = true;
      checks.forEach(check => {
        const icon = check.valid ? '✅' : '❌';
        const display = check.value === true ? '有' : (check.value === false ? '无' : check.value);
        console.log(`  ${icon} ${check.name}: ${display}`);
        if (!check.valid) allValid = false;
      });
      
      if (allValid) {
        console.log(`\n  🎉 任务 ${task.taskId} 配置完整，可以正常运行！`);
      } else {
        console.log(`\n  ⚠️  任务 ${task.taskId} 存在配置问题，请检查上述标记为 ❌ 的字段`);
      }
      
      console.log('\n' + '━'.repeat(70));
    }

    // 显示总结
    console.log('\n' + '='.repeat(70));
    console.log('📊 数据总结');
    console.log('='.repeat(70) + '\n');
    
    printTaskSummary(taskGroups);
    
    console.log('\n✅ Excel 数据测试完成！');
    console.log('\n如果时间格式显示为 ❌，请参考: Excel时间格式设置指南.txt');
    console.log('如果所有检查都是 ✅，可以运行: npm start\n');

  } catch (err) {
    console.error('\n❌ 测试失败:', err.message);
    console.error('\n可能的原因:');
    console.error('  1. data/tasks.xlsx 文件不存在');
    console.error('  2. Excel 文件格式错误');
    console.error('  3. 缺少必填字段');
    console.error('\n详细错误:', err);
  }
}

// 运行测试
try {
  testExcelData();
} catch (err) {
  console.error('未处理的错误:', err);
  process.exit(1);
}

