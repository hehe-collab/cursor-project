import { createRouter, createWebHistory } from 'vue-router'
import { markNavigationStart, markNavigationEnd } from '@/utils/performance'
import { hasPermission, getPermissions } from '@/utils/permission'

/** 指令 #076：路由均为 () => import() 懒加载，减小首包 */
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  { path: '/change-password', name: 'ChangePassword', component: () => import('../views/ChangePassword.vue'), meta: { title: '修改密码' } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '看板', permission: 'dashboard:view' } },
      { path: 'dramas', name: 'DramaManage', component: () => import('../views/DramaManage.vue'), meta: { title: '短剧管理', permission: 'drama:view' } },
      { path: 'dramas/add', name: 'DramaAdd', component: () => import('../views/DramaForm.vue'), meta: { title: '新增短剧', permission: 'drama:create' } },
      { path: 'dramas/edit/:id', name: 'DramaEdit', component: () => import('../views/DramaForm.vue'), meta: { title: '编辑短剧', permission: 'drama:edit' } },
      { path: 'users', name: 'User', component: () => import('../views/User.vue'), meta: { title: '用户列表', permission: 'user:view' } },
      { path: 'recharge', name: 'Recharge', component: () => import('../views/Recharge.vue'), meta: { title: '充值记录', permission: 'recharge:view' } },
      { path: 'delivery-links', name: 'DeliveryLinks', component: () => import('../views/DeliveryLinks.vue'), meta: { title: '投放链接', permission: 'promotion:view' } },
      { path: 'callback-config', name: 'CallbackConfig', component: () => import('../views/CallbackConfig.vue'), meta: { title: '回传配置', permission: 'promotion:view' } },
      { path: 'callback-monitor', name: 'CallbackMonitor', component: () => import('../views/CallbackMonitor.vue'), meta: { title: '回传监控', permission: 'promotion:view' } },
      { path: 'recharge-plan', name: 'RechargePlan', component: () => import('../views/RechargePlan.vue'), meta: { title: '充值方案', permission: 'recharge:plan' } },
      { path: 'recharge-group', name: 'RechargeGroup', component: () => import('../views/RechargeGroup.vue'), meta: { title: '充值方案组', permission: 'recharge:plan' } },
      { path: 'account-manage', name: 'AccountManage', component: () => import('../views/AccountManage.vue'), meta: { title: '账户管理', permission: 'account:view' } },
      { path: 'ad-material', name: 'AdMaterial', component: () => import('../views/AdMaterial.vue'), meta: { title: '广告素材', permission: 'account:view' } },
      { path: 'title-pack', name: 'TitlePack', component: () => import('../views/TitlePack.vue'), meta: { title: '标题包', permission: 'account:view' } },
      { path: 'batch-tools', name: 'BatchTools', component: () => import('../views/BatchTools.vue'), meta: { title: '批量工具', permission: 'account:view' } },
      { path: 'tiktok-ad-import', name: 'TikTokAdImport', component: () => import('../views/TikTokAdImport.vue'), meta: { title: '广告导入', permission: 'tiktok:import' } },
      { path: 'ad-task', name: 'AdTask', component: () => import('../views/AdTask.vue'), meta: { title: '广告任务', permission: 'tiktok:view' } },
      { path: 'system/categories', name: 'CategoryManage', component: () => import('../views/system/CategoryManage.vue'), meta: { title: '分类管理', permission: 'system:category' } },
      { path: 'system/tags', name: 'TagManage', component: () => import('../views/system/TagManage.vue'), meta: { title: '标签管理', permission: 'system:category' } },
      { path: 'system/settings', name: 'SiteSettings', component: () => import('../views/system/SiteSettings.vue'), meta: { title: '站点设置', permission: 'system:view' } },
      { path: 'system/admins', name: 'AdminManage', component: () => import('../views/system/AdminManage.vue'), meta: { title: '管理员管理', permission: 'admin:view' } },
      { path: 'system/roles', name: 'RoleManage', component: () => import('../views/system/RoleManage.vue'), meta: { title: '角色管理', permission: 'admin:role' } },
      { path: 'system/logs', name: 'AdminLog', component: () => import('../views/system/AdminLog.vue'), meta: { title: '操作日志', permission: 'admin:view' } },
      { path: ':pathMatch(.*)*', redirect: '/dashboard' },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  markNavigationStart()
  document.title = to.meta?.title ? `${to.meta.title} · 未来管理系统` : '未来管理系统'

  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    return { path: '/login' }
  }

  // 路由级权限检查
  if (to.meta.permission && !hasPermission(to.meta.permission)) {
    const perms = getPermissions()
    // 已登录但本地尚无权限列表（旧会话 / 未成功执行 RBAC SQL）：放行，避免「无权限→跳转看板→看板也无权限」死循环白屏
    if (token && perms.length === 0) {
      return true
    }
    if (to.name === 'Dashboard') {
      return { path: '/login', query: { redirect: to.fullPath } }
    }
    return { path: '/dashboard' }
  }
})

router.afterEach((to) => {
  markNavigationEnd(to)
})

export default router
