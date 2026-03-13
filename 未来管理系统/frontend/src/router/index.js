import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
      { path: 'dramas', name: 'DramaManage', component: () => import('../views/DramaManage.vue') },
      { path: 'episodes', name: 'Episodes', component: () => import('../views/Episodes.vue') },
      { path: 'stats', name: 'Stats', component: () => import('../views/Stats.vue') },
      { path: 'dramas/add', name: 'DramaAdd', component: () => import('../views/DramaForm.vue') },
      { path: 'dramas/edit/:id', name: 'DramaEdit', component: () => import('../views/DramaForm.vue') },
      { path: 'users', name: 'User', component: () => import('../views/User.vue') },
      { path: 'recharge', name: 'Recharge', component: () => import('../views/Recharge.vue') },
      { path: 'delivery-links', name: 'DeliveryLinks', component: () => import('../views/DeliveryLinks.vue') },
      { path: 'callback-config', name: 'CallbackConfig', component: () => import('../views/CallbackConfig.vue') },
      { path: 'callback-monitor', name: 'CallbackMonitor', component: () => import('../views/CallbackMonitor.vue') },
      { path: 'recharge-plan', name: 'RechargePlan', component: () => import('../views/RechargePlan.vue') },
      { path: 'recharge-group', name: 'RechargeGroup', component: () => import('../views/RechargeGroup.vue') },
      { path: 'account-manage', name: 'AccountManage', component: () => import('../views/AccountManage.vue') },
      { path: 'ad-material', name: 'AdMaterial', component: () => import('../views/AdMaterial.vue') },
      { path: 'title-pack', name: 'TitlePack', component: () => import('../views/TitlePack.vue') },
      { path: 'batch-tools', name: 'BatchTools', component: () => import('../views/BatchTools.vue') },
      { path: 'ad-task', name: 'AdTask', component: () => import('../views/AdTask.vue') },
      { path: ':pathMatch(.*)*', name: 'NotFound', component: () => import('../views/NotFound.vue') },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (!to.meta.public && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
