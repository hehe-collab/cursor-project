/**
 * 生成Excel模板
 */

const XLSX = require('xlsx');
const path = require('path');

// 示例数据
const exampleData = [
  {
    '任务ID': 1,
    '源账户ID': 'C-MADxd-IDN-2-8W-11',
    '素材关键词': '桶礼盒波',
    '目标账户ID列表': '7561360888110\n7561360897493\n7561360323361',
    '备注': '推送到3个账户',
  },
  {
    '任务ID': 2,
    '源账户ID': 'C-MADxd-IDN-2-8W-11',
    '素材关键词': '另一个关键词',
    '目标账户ID列表': '7605136088811003666\n7605136089749307\n7605136053236148',
    '备注': '推送到3个账户',
  },
  {
    '任务ID': 3,
    '源账户ID': 'C-另一个源账户',
    '素材关键词': '测试素材',
    '目标账户ID列表': '账户1\n账户2\n账户3\n账户4\n账户5',
    '备注': '推送到5个账户',
  },
];

// 创建工作簿
const workbook = XLSX.utils.book_new();
const worksheet = XLSX.utils.json_to_sheet(exampleData);

// 设置列宽
worksheet['!cols'] = [
  { wch: 12 },  // 任务ID
  { wch: 25 },  // 源账户ID
  { wch: 20 },  // 素材关键词
  { wch: 35 },  // 目标账户ID列表
  { wch: 20 },  // 备注
];

// 添加工作表
XLSX.utils.book_append_sheet(workbook, worksheet, '推送任务');

// 保存文件
const outputPath = path.join(__dirname, 'data', 'push-tasks.xlsx');
XLSX.writeFile(workbook, outputPath);

console.log('✅ Excel模板已生成:', outputPath);
console.log('\n📋 使用说明:');
console.log('  1. 任务ID: 任务的唯一标识（可用数字1、2、3...或文字"任务1"，可选）');
console.log('  2. 源账户ID: 素材来源账户的ID（必填）');
console.log('  3. 素材关键词: 搜索素材的关键词（必填）');
console.log('  4. 目标账户ID列表: 推送目标账户ID，每行一个（必填）');
console.log('  5. 备注: 任务备注信息（可选）');
console.log('\n⚠️  注意事项:');
console.log('  - 任务ID支持纯数字（1、2、3...）或文字（任务1、任务2...）');
console.log('  - 目标账户ID列表中每行一个账户ID');
console.log('  - 前后的分号会自动清除');
console.log('  - 空行会自动过滤');

