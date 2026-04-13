<template>
  <div class="player-page">
    <!-- 视频容器 -->
    <div class="video-wrap" @click="toggleControls">
      <video
        ref="videoEl"
        class="video-el"
        playsinline
        webkit-playsinline
        preload="auto"
        @timeupdate="onTimeUpdate"
        @ended="onEnded"
        @waiting="onWaiting"
        @playing="onPlaying"
        @error="onVideoError"
      />

      <!-- 加载中 -->
      <div v-if="buffering" class="overlay-center">
        <van-loading color="#fff" size="36px" />
      </div>

      <!-- 播放/暂停中心按钮（短暂显示） -->
      <transition name="fade">
        <div v-if="showPlayIcon" class="overlay-center pointer-none">
          <van-icon :name="playing ? 'play-circle-o' : 'pause-circle-o'" size="64px" color="rgba(255,255,255,0.85)" />
        </div>
      </transition>

      <!-- 顶部控制栏 -->
      <transition name="slide-down">
        <div v-if="controlsVisible" class="controls-top">
          <van-icon name="arrow-left" size="24px" color="#fff" class="back-btn" @click.stop="goBack" />
          <span class="ep-title">{{ dramaTitle }} · 第 {{ currentEp }} 集</span>
          <van-icon name="bars" size="22px" color="#fff" class="ep-list-btn" @click.stop="showEpList = true" />
        </div>
      </transition>

      <!-- 底部控制栏 -->
      <transition name="slide-up">
        <div v-if="controlsVisible" class="controls-bottom" @click.stop>
          <!-- 进度条 -->
          <van-slider
            v-model="progress"
            :max="100"
            bar-height="3px"
            active-color="#ff4757"
            inactive-color="rgba(255,255,255,0.3)"
            @change="onSeek"
            @drag-start="dragging = true"
            @drag-end="dragging = false"
          />
          <div class="controls-row">
            <van-icon
              :name="playing ? 'pause-circle-o' : 'play-circle-o'"
              size="28px"
              color="#fff"
              @click.stop="togglePlay"
            />
            <span class="time-text">{{ currentTimeStr }} / {{ durationStr }}</span>
            <div class="spacer" />
            <van-icon name="volume-o" size="24px" color="#fff" @click.stop="toggleMute" />
          </div>
        </div>
      </transition>
    </div>

    <!-- 集数列表弹窗 -->
    <van-popup
      v-model:show="showEpList"
      position="bottom"
      round
      :style="{ maxHeight: '60%' }"
    >
      <div class="ep-list-header">选集</div>
      <div class="ep-grid">
        <div
          v-for="ep in store.episodes"
          :key="ep.episode"
          class="ep-item"
          :class="{
            'ep-item--active': ep.episode === currentEp,
            'ep-item--locked': !store.isEpisodeFree(ep.episode),
          }"
          @click="selectEpisode(ep.episode)"
        >
          <span>{{ ep.episode }}</span>
          <van-icon v-if="!store.isEpisodeFree(ep.episode)" name="lock" size="10px" />
        </div>
      </div>
    </van-popup>

    <!-- 付费墙 -->
    <PayWall
      v-if="showPaywall"
      :episode="currentEp"
      :plans="store.plans"
      @paid="onPaid"
      @close="onPaywallClose"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import Hls from 'hls.js'
import {
  Loading as VanLoading,
  Icon as VanIcon,
  Slider as VanSlider,
  Popup as VanPopup,
  showToast,
} from 'vant'
import { useDramaStore } from '@/stores/drama'
import { getPlayUrl } from '@/api/index'
import PayWall from '@/components/PayWall.vue'

const router = useRouter()
const store = useDramaStore()

const videoEl = ref(null)
const hls = ref(null)
const playing = ref(false)
const buffering = ref(false)
const progress = ref(0)
const currentTime = ref(0)
const duration = ref(0)
const dragging = ref(false)
const muted = ref(false)
const controlsVisible = ref(true)
const showPlayIcon = ref(false)
const showEpList = ref(false)
const showPaywall = ref(false)
const currentEp = ref(store.currentEpisode || 1)
let controlsTimer = null
let playIconTimer = null

const dramaTitle = computed(() => store.drama?.title || store.drama?.name || '')

const currentTimeStr = computed(() => formatTime(currentTime.value))
const durationStr = computed(() => formatTime(duration.value))

function formatTime(s) {
  if (!s || isNaN(s)) return '0:00'
  const m = Math.floor(s / 60)
  const sec = Math.floor(s % 60)
  return `${m}:${sec.toString().padStart(2, '0')}`
}

// ── 播放控制 ──────────────────────────────────────────────

async function loadEpisode(ep) {
  const video = videoEl.value
  if (!video) return

  // 检查是否需要付费
  if (!store.isEpisodeFree(ep)) {
    showPaywall.value = true
    return
  }

  buffering.value = true
  try {
    const res = await getPlayUrl(store.promoId, ep)
    if (res.code === 402) {
      // 金豆不足
      showPaywall.value = true
      buffering.value = false
      return
    }
    if (res.code !== 0) {
      showToast(res.message || '加载失败')
      buffering.value = false
      return
    }

    const url = res.data.url
    currentEp.value = ep
    store.setCurrentEpisode(ep)

    if (Hls.isSupported() && url.includes('.m3u8')) {
      if (hls.value) hls.value.destroy()
      hls.value = new Hls({ enableWorker: true, lowLatencyMode: false })
      hls.value.loadSource(url)
      hls.value.attachMedia(video)
      hls.value.on(Hls.Events.MANIFEST_PARSED, () => {
        video.play().catch(() => {})
      })
    } else {
      video.src = url
      video.load()
      video.play().catch(() => {})
    }
  } catch (e) {
    showToast('网络错误，请重试')
    buffering.value = false
  }
}

function togglePlay() {
  const video = videoEl.value
  if (!video) return
  if (video.paused) {
    video.play()
  } else {
    video.pause()
  }
  flashPlayIcon()
}

function toggleMute() {
  const video = videoEl.value
  if (!video) return
  video.muted = !video.muted
  muted.value = video.muted
}

function onSeek(val) {
  const video = videoEl.value
  if (!video || !duration.value) return
  video.currentTime = (val / 100) * duration.value
}

function onTimeUpdate() {
  const video = videoEl.value
  if (!video || dragging.value) return
  currentTime.value = video.currentTime
  duration.value = video.duration || 0
  if (duration.value > 0) {
    progress.value = (video.currentTime / duration.value) * 100
  }
}

function onEnded() {
  playing.value = false
  // 自动播下一集
  const next = currentEp.value + 1
  if (next <= store.episodes.length) {
    loadEpisode(next)
  }
}

function onWaiting() { buffering.value = true }
function onPlaying() {
  buffering.value = false
  playing.value = true
}
function onVideoError() {
  buffering.value = false
  showToast('视频加载失败，请重试')
}

// ── 控制栏显隐 ────────────────────────────────────────────

function toggleControls() {
  controlsVisible.value = !controlsVisible.value
  if (controlsVisible.value) resetControlsTimer()
}

function resetControlsTimer() {
  clearTimeout(controlsTimer)
  controlsTimer = setTimeout(() => {
    controlsVisible.value = false
  }, 3500)
}

function flashPlayIcon() {
  showPlayIcon.value = true
  clearTimeout(playIconTimer)
  playIconTimer = setTimeout(() => { showPlayIcon.value = false }, 800)
}

// ── 集数选择 ──────────────────────────────────────────────

function selectEpisode(ep) {
  showEpList.value = false
  loadEpisode(ep)
}

function goBack() {
  router.back()
}

// ── 付费回调 ──────────────────────────────────────────────

async function onPaid() {
  showPaywall.value = false
  await store.loadUserInfo()
  loadEpisode(currentEp.value)
}

function onPaywallClose() {
  showPaywall.value = false
}

// ── 生命周期 ──────────────────────────────────────────────

onMounted(async () => {
  if (!store.drama) {
    await store.loadDrama()
  }
  await store.loadUserInfo()
  loadEpisode(currentEp.value)
  resetControlsTimer()

  // 监听 video 播放状态
  const video = videoEl.value
  if (video) {
    video.addEventListener('play', () => { playing.value = true })
    video.addEventListener('pause', () => { playing.value = false })
  }
})

onBeforeUnmount(() => {
  if (hls.value) hls.value.destroy()
  clearTimeout(controlsTimer)
  clearTimeout(playIconTimer)
})
</script>

<style scoped>
.player-page {
  position: fixed;
  inset: 0;
  background: #000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-wrap {
  position: relative;
  width: 100%;
  height: 100%;
}

.video-el {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

/* 中心叠层（loading / 播放图标） */
.overlay-center {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.pointer-none { pointer-events: none; }

/* 顶部控制栏 */
.controls-top {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 48px 16px 16px;
  background: linear-gradient(to bottom, rgba(0,0,0,0.6), transparent);
  display: flex;
  align-items: center;
  gap: 12px;
}
.back-btn { cursor: pointer; flex-shrink: 0; }
.ep-title {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.ep-list-btn { cursor: pointer; flex-shrink: 0; }

/* 底部控制栏 */
.controls-bottom {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 16px 32px;
  background: linear-gradient(to top, rgba(0,0,0,0.7), transparent);
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.controls-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.time-text {
  font-size: 12px;
  color: rgba(255,255,255,0.8);
  white-space: nowrap;
}
.spacer { flex: 1; }

/* 集数列表弹窗 */
.ep-list-header {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #f0f0f0;
}
.ep-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  padding: 16px;
  overflow-y: auto;
  max-height: calc(60vh - 56px);
}
.ep-item {
  aspect-ratio: 1;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #333;
  gap: 2px;
  cursor: pointer;
  background: #fafafa;
}
.ep-item--active {
  border-color: #ff4757;
  color: #ff4757;
  background: #fff0f1;
  font-weight: 600;
}
.ep-item--locked {
  color: #bbb;
  background: #f5f5f5;
}

/* 过渡动画 */
.fade-enter-active, .fade-leave-active { transition: opacity 0.3s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.25s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-100%); }

.slide-up-enter-active, .slide-up-leave-active { transition: all 0.25s ease; }
.slide-up-enter-from, .slide-up-leave-to { opacity: 0; transform: translateY(100%); }
</style>
