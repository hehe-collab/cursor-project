/**
 * 生成Excel模板 - 推广链接复制工具
 */

const XLSX = require('xlsx');
const path = require('path');

// ⭐ 示例数据（新格式：一行一个新链接名称）
const exampleData = [
  // 任务1：第一部剧，创建4个推广链接
  {
    '任务ID': '1',
    '模板链接名称': '淮金之5W-S+xd-1-5+tr',
    '剧名': '淮村的贫苦',
    '免费/预览集数': 5,
    '新链接名称': 'C-xd-IDN-淮村的贫苦印尼-2',
    '备注': '第一部剧',
  },
  {
    '任务ID': '1',
    '模板链接名称': '',  // 同一任务可以留空
    '剧名': '',
    '免费/预览集数': '',
    '新链接名称': 'C-xd-IDN-淮村的贫苦印尼-3',
    '备注': '',
  },
  {
    '任务ID': '1',
    '新链接名称': 'C-xd-IDN-淮村的贫苦印尼-4',
  },
  {
    '任务ID': '1',
    '新链接名称': 'C-xd-IDN-淮村的贫苦印尼-5',
  },
  
  // 任务2：第二部剧，创建3个推广链接
  {
    '任务ID': '2',
    '模板链接名称': '另一个模板链接',
    '剧名': '另一部剧',
    '免费/预览集数': 3,
    '新链接名称': 'C-xd-IDN-另一部剧-1',
    '备注': '第二部剧',
  },
  {
    '任务ID': '2',
    '新链接名称': 'C-xd-IDN-另一部剧-2',
  },
  {
    '任务ID': '2',
    '新链接名称': 'C-xd-IDN-另一部剧-3',
  },
];

// 创建工作簿
const worksheet = XLSX.utils.json_to_sheet(exampleData);
const workbook = XLSX.utils.book_new();
XLSX.utils.book_append_sheet(workbook, worksheet, '推广链接任务');

// 设置列宽
worksheet['!cols'] = [
  { wch: 10 },  // 任务ID
  { wch: 30 },  // 模板链接名称
  { wch: 20 },  // 剧名
  { wch: 15 },  // 免费/预览集数
  { wch: 50 },  // 新链接名称
  { wch: 30 },  // 备注
];

// 保存文件
const outputPath = path.join(__dirname, 'data', 'link-tasks.xlsx');
XLSX.writeFile(workbook, outputPath);

console.log('✅ Excel模板已生成: data/link-tasks.xlsx');
console.log('\n📝 字段说明：');
console.log('  1. 任务ID: 任务的唯一标识');
console.log('  2. 模板链接名称: 要复制的模板推广链接名称');
console.log('  3. 剧名: 短剧名称');
console.log('  4. 免费/预览集数: 免费集数和预览集数（两者相同）');
console.log('  5. 新链接名称: 要创建的推广链接名称（⭐ 一行一个）');
console.log('     - 第1个新链接：从模板复制并修改参数');
console.log('     - 第2-N个新链接：从第1个复制（已修改好的参数）');
console.log('  6. 备注: 可选，用于记录信息');
console.log('\n💡 填写方式（⭐ 新格式）：');
console.log('  - 每个新链接占一行');
console.log('  - 同一任务的第一行：填写完整信息（任务ID、模板、剧名、集数、第1个新链接）');
console.log('  - 同一任务的后续行：只需填写任务ID和新链接名称即可');
console.log('  - 一个任务处理一个短剧，创建多个推广链接');
console.log('  - 多个短剧可以创建多个任务（不同的任务ID）');

