import { createRouter, createWebHistory } from 'vue-router'
import { markNavigationStart, markNavigationEnd } from '@/utils/performance'

/** 指令 #076：路由均为 () => import() 懒加载，减小首包 */
const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true, title: '登录' } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '看板' } },
      { path: 'dramas', name: 'DramaManage', component: () => import('../views/DramaManage.vue'), meta: { title: '短剧管理' } },
      { path: 'dramas/add', name: 'DramaAdd', component: () => import('../views/DramaForm.vue'), meta: { title: '新增短剧' } },
      { path: 'dramas/edit/:id', name: 'DramaEdit', component: () => import('../views/DramaForm.vue'), meta: { title: '编辑短剧' } },
      { path: 'users', name: 'User', component: () => import('../views/User.vue'), meta: { title: '用户列表' } },
      { path: 'recharge', name: 'Recharge', component: () => import('../views/Recharge.vue'), meta: { title: '充值记录' } },
      { path: 'delivery-links', name: 'DeliveryLinks', component: () => import('../views/DeliveryLinks.vue'), meta: { title: '投放链接' } },
      { path: 'callback-config', name: 'CallbackConfig', component: () => import('../views/CallbackConfig.vue'), meta: { title: '回传配置' } },
      { path: 'callback-monitor', name: 'CallbackMonitor', component: () => import('../views/CallbackMonitor.vue'), meta: { title: '回传监控' } },
      { path: 'recharge-plan', name: 'RechargePlan', component: () => import('../views/RechargePlan.vue'), meta: { title: '充值方案' } },
      { path: 'recharge-group', name: 'RechargeGroup', component: () => import('../views/RechargeGroup.vue'), meta: { title: '充值方案组' } },
      { path: 'account-manage', name: 'AccountManage', component: () => import('../views/AccountManage.vue'), meta: { title: '账户管理' } },
      { path: 'ad-material', name: 'AdMaterial', component: () => import('../views/AdMaterial.vue'), meta: { title: '广告素材' } },
      { path: 'title-pack', name: 'TitlePack', component: () => import('../views/TitlePack.vue'), meta: { title: '标题包' } },
      { path: 'batch-tools', name: 'BatchTools', component: () => import('../views/BatchTools.vue'), meta: { title: '批量工具' } },
      { path: 'ad-task', name: 'AdTask', component: () => import('../views/AdTask.vue'), meta: { title: '广告任务' } },
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
})

router.afterEach((to) => {
  markNavigationEnd(to)
})

export default router
