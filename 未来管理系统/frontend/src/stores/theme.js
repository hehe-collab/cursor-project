import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useThemeStore = defineStore('theme', () => {
  const theme = ref('light')
  const followSystem = ref(false)

  function applyTheme(newTheme) {
    theme.value = newTheme
    if (newTheme === 'dark') {
      document.documentElement.setAttribute('data-theme', 'dark')
    } else {
      document.documentElement.removeAttribute('data-theme')
    }
    localStorage.setItem('theme', newTheme)
  }

  function applySystemTheme() {
    const isDark = window.matchMedia?.('(prefers-color-scheme: dark)').matches
    applyTheme(isDark ? 'dark' : 'light')
  }

  function initTheme() {
    const savedFollowSystem = localStorage.getItem('followSystem')
    const savedTheme = localStorage.getItem('theme')

    if (savedFollowSystem === 'true') {
      followSystem.value = true
      applySystemTheme()
    } else if (savedTheme) {
      followSystem.value = false
      applyTheme(savedTheme)
    } else {
      followSystem.value = false
      applyTheme('light')
    }

    window.matchMedia?.('(prefers-color-scheme: dark)').addEventListener('change', (e) => {
      if (followSystem.value) applyTheme(e.matches ? 'dark' : 'light')
    })
  }

  function toggleTheme() {
    followSystem.value = false
    localStorage.setItem('followSystem', 'false')
    applyTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function setMode(mode) {
    if (mode === 'auto') {
      followSystem.value = true
      localStorage.setItem('followSystem', 'true')
      applySystemTheme()
    } else {
      followSystem.value = false
      localStorage.setItem('followSystem', 'false')
      applyTheme(mode)
    }
  }

  return { theme, followSystem, initTheme, toggleTheme, setMode, applyTheme }
})
