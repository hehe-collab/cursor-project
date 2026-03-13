<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">短剧出海</div>
      <el-menu :default-active="activeMenu" router background-color="#304156" text-color="#bfcbd9" active-text-color="#409EFF" :default-openeds="['drama', 'user', 'delivery', 'recharge-sub', 'tools']">
        <el-menu-item index="/dashboard">
          <el-icon><DataLine /></el-icon>
          <span>看板</span>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><DataAnalysis /></el-icon>
          <span>统计</span>
        </el-menu-item>
        <el-sub-menu index="drama">
          <template #title>
            <el-icon><VideoPlay /></el-icon>
            <span>短剧管理</span>
          </template>
          <el-menu-item index="/dramas">短剧管理</el-menu-item>
          <el-menu-item index="/episodes">剧集信息</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="user">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户信息</span>
          </template>
          <el-menu-item index="/users">用户列表</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/recharge">
          <el-icon><Money /></el-icon>
          <span>充值记录</span>
        </el-menu-item>
        <el-sub-menu index="delivery">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>投放配置</span>
          </template>
          <el-menu-item index="/delivery-links">投放链接配置</el-menu-item>
          <el-menu-item index="/callback-config">回传配置</el-menu-item>
          <el-menu-item index="/callback-monitor">回传监控</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="recharge-sub">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>充值方案</span>
          </template>
          <el-menu-item index="/recharge-plan">充值方案</el-menu-item>
          <el-menu-item index="/recharge-group">充值方案组</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="tools">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>投放工具</span>
          </template>
          <el-menu-item index="/account-manage">账户管理</el-menu-item>
          <el-menu-item index="/ad-material">广告素材</el-menu-item>
          <el-menu-item index="/title-pack">标题包</el-menu-item>
          <el-menu-item index="/batch-tools">批量工具</el-menu-item>
          <el-menu-item index="/ad-task">广告任务</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>
    <el-container class="right-container">
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <template v-for="(item, i) in breadcrumbItems" :key="i">
              <el-breadcrumb-item v-if="item.to" :to="item.to">{{ item.label }}</el-breadcrumb-item>
              <el-breadcrumb-item v-else>{{ item.label }}</el-breadcrumb-item>
            </template>
          </el-breadcrumb>
          <el-alert v-if="showLoginSuccess" type="success" :closable="true" show-icon class="login-success-alert" @close="showLoginSuccess = false">
            登录成功
          </el-alert>
        </div>
        <div class="user-info">
          <el-icon><User /></el-icon>
          <span>{{ user?.nickname || user?.username }}</span>
          <el-button type="danger" link @click="logout">退出</el-button>
        </div>
      </el-header>
      <div class="tabs-bar">
        <div v-for="t in tabs" :key="t.path" :class="['tab-item', { active: activeTab === t.path }]" @click="onTabChange(t.path)">
          <span>{{ t.title }}</span>
          <el-icon v-if="t.path !== '/dashboard'" class="tab-close" @click.stop="closeTab(t.path)"><Close /></el-icon>
        </div>
      </div>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const tabs = ref([{ path: '/dashboard', title: '首页' }])
const activeTab = ref('/dashboard')
const showLoginSuccess = ref(!!sessionStorage.getItem('loginSuccess'))
if (showLoginSuccess.value) {
  setTimeout(() => {
    sessionStorage.removeItem('loginSuccess')
    showLoginSuccess.value = false
  }, 3000)
}

const titleMap = {
  '/dashboard': '首页',
  '/stats': '统计',
  '/dramas': '短剧管理',
  '/episodes': '剧集信息',
  
  '/dramas/add': '新增剧集',
  '/dramas/edit': '编辑剧集',
  '/users': '用户列表',
  '/recharge': '充值记录',
  '/delivery-links': '投放链接配置',
  '/callback-config': '回传配置',
  '/callback-monitor': '回传监控',
  '/recharge-plan': '充值方案',
  '/recharge-group': '充值方案组',
  '/account-manage': '账户管理',
  '/ad-material': '广告素材',
  '/title-pack': '标题包',
  '/batch-tools': '批量工具',
  '/ad-task': '广告任务',
}

function getTitle(path) {
  if (path.startsWith('/dramas/edit')) return '编辑剧集'
  return titleMap[path] || '首页'
}

const breadcrumbItems = computed(() => {
  const path = route.path
  if (path === '/users') return [{ label: '用户信息' }, { label: '用户列表' }]
  return [{ label: getTitle(path) }]
})

watch(() => route.path, (path) => {
  const title = getTitle(path)
  const tabPath = path.startsWith('/dramas/edit') ? '/dramas' : path
  if (!tabs.value.find(t => t.path === tabPath)) {
    tabs.value.push({ path: tabPath, title })
  }
  activeTab.value = tabPath
}, { immediate: true })

function onTabChange(name) {
  if (name && name !== route.path) router.push(name)
}

function closeTab(path) {
  const idx = tabs.value.findIndex(t => t.path === path)
  if (idx < 0) return
  tabs.value.splice(idx, 1)
  if (route.path === path) {
    const next = tabs.value[idx] || tabs.value[idx - 1] || tabs.value[0]
    router.push(next?.path || '/dashboard')
  }
}

const activeMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/dramas/edit')) return '/dramas'
  return path
})

const currentTitle = computed(() => getTitle(route.path))

const user = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('user') || '{}')
  } catch {
    return {}
  }
})

function logout() {
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside {
  background: linear-gradient(180deg, #2c3e50 0%, #34495e 100%);
  color: #fff;
  box-shadow: 2px 0 8px rgba(0,0,0,0.08);
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
  text-shadow: 0 1px 2px rgba(0,0,0,0.1);
}
.aside :deep(.el-menu-item),
.aside :deep(.el-sub-menu__title) { transition: all 0.2s; }
.aside :deep(.el-menu-item:hover),
.aside :deep(.el-sub-menu__title:hover) { background: rgba(255,255,255,0.08) !important; }
.aside :deep(.el-menu-item.is-active) { background: rgba(64,158,255,0.2) !important; color: #66b1ff; }
.right-container { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.header {
  background: #fff;
  height: 60px;
  border-bottom: 1px solid #e8eaed;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.header-left { display: flex; align-items: center; gap: 16px; }
.login-success-alert {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(103,194,58,0.2);
}
.user-info { display: flex; align-items: center; gap: 10px; color: #606266; font-size: 14px; }
.tabs-bar {
  display: flex;
  background: #fff;
  padding: 0 20px;
  border-bottom: 1px solid #e8eaed;
  min-height: 44px;
  align-items: center;
  gap: 4px;
}
.tab-item {
  padding: 10px 18px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}
.tab-item:hover { background: #f5f7fa; color: #409EFF; }
.tab-item.active {
  background: linear-gradient(135deg, #409EFF 0%, #66b1ff 100%);
  color: #fff;
  font-weight: 500;
}
.tab-close { font-size: 12px; margin-left: 4px; opacity: 0.7; }
.tab-close:hover { opacity: 1; color: #f56c6c; }
.tab-item.active .tab-close:hover { color: rgba(255,255,255,0.9); }
.main {
  background: #f0f2f5;
  padding: 24px;
  overflow-y: auto;
}
</style>
