<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <el-button circle text size="small" class="theme-btn" :title="currentLabel">
      <el-icon :size="16">
        <component :is="currentIcon" />
      </el-icon>
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="light" :class="{ 'is-selected': !followSystem && theme === 'light' }">
          <el-icon><Sunny /></el-icon>
          <span>亮色模式</span>
        </el-dropdown-item>
        <el-dropdown-item command="dark" :class="{ 'is-selected': !followSystem && theme === 'dark' }">
          <el-icon><Moon /></el-icon>
          <span>暗色模式</span>
        </el-dropdown-item>
        <el-dropdown-item command="auto" divided :class="{ 'is-selected': followSystem }">
          <el-icon><Monitor /></el-icon>
          <span>跟随系统</span>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { Sunny, Moon, Monitor } from '@element-plus/icons-vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
const theme = computed(() => themeStore.theme)
const followSystem = computed(() => themeStore.followSystem)

const currentIcon = computed(() => {
  if (followSystem.value) return Monitor
  return theme.value === 'dark' ? Moon : Sunny
})

const currentLabel = computed(() => {
  if (followSystem.value) return '跟随系统'
  return theme.value === 'dark' ? '暗色模式' : '亮色模式'
})

function handleCommand(command) {
  themeStore.setMode(command)
}
</script>

<style scoped>
.theme-btn {
  color: var(--text-color-regular);
}

.theme-btn:hover {
  color: var(--color-primary);
  background-color: var(--fill-color-light) !important;
}

:deep(.el-dropdown-menu__item.is-selected) {
  color: var(--color-primary);
  font-weight: 600;
}

:deep(.el-dropdown-menu__item .el-icon) {
  margin-right: 6px;
}
</style>
