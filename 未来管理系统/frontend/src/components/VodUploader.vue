<template>
  <div class="vod-uploader">
    <el-upload
      :auto-upload="false"
      :show-file-list="false"
      :limit="1"
      accept="video/*"
      @change="handleFileChange"
    >
      <el-button type="primary" plain :loading="state.status === 'uploading' || state.status === 'preparing'">
        {{ uploadButtonText }}
      </el-button>
    </el-upload>

    <div v-if="state.fileName" class="vod-uploader__panel">
      <div class="vod-uploader__meta">
        <span class="vod-uploader__name">{{ state.fileName }}</span>
        <span class="vod-uploader__size">{{ formatSize(state.fileSize) }}</span>
      </div>

      <el-progress :percentage="state.progress" :status="progressStatus" />

      <div class="vod-uploader__status">
        <el-tag v-if="state.videoId" size="small" type="info">VideoId: {{ state.videoId }}</el-tag>
        <el-tag v-if="state.status === 'success'" size="small" type="success">上传成功</el-tag>
        <el-tag v-else-if="state.status === 'error'" size="small" type="danger">上传失败</el-tag>
        <el-tag v-else-if="state.status === 'paused'" size="small" type="warning">已暂停</el-tag>
        <el-tag v-else-if="state.status === 'uploading'" size="small" type="primary">上传中</el-tag>
        <span v-if="state.errorMessage" class="vod-uploader__error">{{ state.errorMessage }}</span>
      </div>

      <div class="vod-uploader__actions">
        <el-button
          v-if="state.status === 'uploading'"
          size="small"
          @click="pauseUpload"
        >
          暂停
        </el-button>
        <el-button
          v-if="state.status === 'paused'"
          size="small"
          type="primary"
          @click="resumeUpload"
        >
          继续
        </el-button>
        <el-button
          v-if="state.status === 'error'"
          size="small"
          type="warning"
          @click="retryUpload"
        >
          重试
        </el-button>
        <el-button
          v-if="state.status !== 'uploading' && state.status !== 'preparing'"
          size="small"
          @click="clearState"
        >
          清空
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const OSS_SDK_URL =
  'https://cdn.jsdelivr.net/npm/aliyun-vod-upload-1.5.5@1.0.0/lib/aliyun-upload-sdk-1.5.5/lib/aliyun-oss-sdk-6.17.1.min.js'
const VOD_SDK_URL =
  'https://cdn.jsdelivr.net/npm/aliyun-vod-upload-1.5.5@1.0.0/lib/aliyun-upload-sdk-1.5.5/aliyun-upload-sdk-1.5.5.min.js'

let sdkLoadingPromise
let vodConfigPromise
let cachedVodConfig = null

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['success', 'error'])

const state = reactive({
  fileName: '',
  fileSize: 0,
  rawFile: null,
  progress: 0,
  status: '',
  errorMessage: '',
  videoId: '',
  uploader: null,
})

const uploadButtonText = computed(() => {
  if (state.status === 'uploading' || state.status === 'preparing') return '上传中...'
  if (state.status === 'success') return '重新上传'
  return '上传到 VOD'
})

const progressStatus = computed(() => {
  if (state.status === 'error') return 'exception'
  if (state.status === 'success') return 'success'
  return undefined
})

function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size.toFixed(size >= 100 || index === 0 ? 0 : 1)} ${units[index]}`
}

async function handleFileChange(file) {
  if (!file?.raw) return
  clearState()
  state.fileName = file.name || ''
  state.fileSize = file.size || 0
  state.rawFile = file.raw
  await startUpload()
}

async function startUpload() {
  if (!state.rawFile) return
  try {
    state.status = 'preparing'
    state.progress = 0
    state.errorMessage = ''
    const vodConfig = await getVodConfig()
    if (!vodConfig?.enabled) {
      throw new Error('当前环境还未配置阿里云 VOD AccessKey')
    }
    await ensureVodSdk()
    const uploader = createUploader(vodConfig.regionId || 'cn-shanghai')
    state.uploader = uploader
    state.status = 'uploading'
    uploader.addFile(state.rawFile, null, null, null, '{"Vod":{}}')
    uploader.startUpload()
  } catch (error) {
    failUpload(error)
  }
}

function createUploader(regionId) {
  const uploader = new window.AliyunUpload.Vod({
    userId: resolveUserId(),
    region: regionId || 'cn-shanghai',
    partSize: 1048576,
    parallel: 3,
    retryCount: 3,
    retryDuration: 2,
    addFileSuccess: () => {},
    onUploadstarted: (uploadInfo) => {
      handleUploadStarted(uploader, uploadInfo)
    },
    onUploadProgress: (_uploadInfo, _totalSize, loadedPercent) => {
      state.progress = Math.max(0, Math.min(100, Math.floor((loadedPercent || 0) * 100)))
      state.status = 'uploading'
    },
    onUploadSucceed: async () => {
      state.progress = 100
      state.status = 'success'
      state.errorMessage = ''
      try {
        const infoRes = await request.get(`/vod/info/${state.videoId}`)
        const info = infoRes?.data || {}
        emit('success', {
          vod_video_id: state.videoId,
          video_id: state.videoId,
          vod_status: info.status || 'uploading',
          duration: Number(info.duration || 0),
          video_size: Number(info.size || state.fileSize || 0),
          vod_cover_url: info.coverUrl || '',
        })
      } catch {
        emit('success', {
          vod_video_id: state.videoId,
          video_id: state.videoId,
          vod_status: 'uploading',
          duration: 0,
          video_size: Number(state.fileSize || 0),
          vod_cover_url: '',
        })
      }
      ElMessage.success('VOD 上传成功')
    },
    onUploadFailed: (_uploadInfo, code, message) => {
      failUpload(new Error(message || code || '上传失败'))
    },
    onUploadCanceled: () => {
      state.status = 'paused'
    },
    onUploadTokenExpired: () => {
      refreshUploadAuth(uploader)
    },
  })
  return uploader
}

async function handleUploadStarted(uploader, uploadInfo) {
  try {
    const res = uploadInfo?.videoId
      ? await request.get('/vod/refresh-upload-auth', {
          params: { videoId: uploadInfo.videoId },
        })
      : await request.post('/vod/upload-auth', {
          title: props.title || state.fileName,
          fileName: state.fileName,
        })
    const data = res?.data || {}
    state.videoId = data.videoId || uploadInfo?.videoId || state.videoId
    uploader.setUploadAuthAndAddress(
      uploadInfo,
      data.uploadAuth,
      data.uploadAddress,
      data.videoId || uploadInfo?.videoId || state.videoId,
    )
  } catch (error) {
    failUpload(error)
  }
}

async function refreshUploadAuth(uploader) {
  try {
    const res = await request.get('/vod/refresh-upload-auth', {
      params: { videoId: state.videoId },
    })
    const data = res?.data || {}
    if (typeof uploader.resumeUploadWithAuth === 'function' && data.uploadAuth) {
      uploader.resumeUploadWithAuth(data.uploadAuth)
    } else if (typeof uploader.startUpload === 'function') {
      uploader.startUpload()
    }
  } catch (error) {
    failUpload(error)
  }
}

function pauseUpload() {
  if (typeof state.uploader?.stopUpload === 'function') {
    state.uploader.stopUpload()
    state.status = 'paused'
  }
}

function resumeUpload() {
  if (typeof state.uploader?.startUpload === 'function') {
    state.uploader.startUpload()
    state.status = 'uploading'
  }
}

function retryUpload() {
  startUpload()
}

function clearState() {
  state.fileName = ''
  state.fileSize = 0
  state.rawFile = null
  state.progress = 0
  state.status = ''
  state.errorMessage = ''
  state.videoId = ''
  state.uploader = null
}

function failUpload(error) {
  const message = error?.message || '上传失败'
  state.status = 'error'
  state.errorMessage = message
  emit('error', message)
  ElMessage.error(message)
}

async function getVodConfig() {
  if (cachedVodConfig) return cachedVodConfig
  if (!vodConfigPromise) {
    vodConfigPromise = request
      .get('/vod/config')
      .then((res) => {
        cachedVodConfig = res?.data || { enabled: false, regionId: 'cn-shanghai' }
        return cachedVodConfig
      })
      .finally(() => {
        vodConfigPromise = null
      })
  }
  return vodConfigPromise
}

async function ensureVodSdk() {
  if (window.AliyunUpload && window.OSS) return
  if (!sdkLoadingPromise) {
    sdkLoadingPromise = (async () => {
      await loadScriptOnce(OSS_SDK_URL, () => window.OSS)
      await loadScriptOnce(VOD_SDK_URL, () => window.AliyunUpload)
    })().finally(() => {
      sdkLoadingPromise = null
    })
  }
  return sdkLoadingPromise
}

function loadScriptOnce(src, checker) {
  if (checker()) {
    return Promise.resolve()
  }
  return new Promise((resolve, reject) => {
    const existing = Array.from(document.querySelectorAll('script')).find((item) => item.src === src)
    if (existing) {
      existing.addEventListener('load', () => resolve(), { once: true })
      existing.addEventListener('error', () => reject(new Error(`加载脚本失败: ${src}`)), { once: true })
      return
    }
    const script = document.createElement('script')
    script.src = src
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error(`加载脚本失败: ${src}`))
    document.head.appendChild(script)
  })
}

function resolveUserId() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || '{}')
    return String(user.id || user.username || user.nickname || 'admin')
  } catch {
    return 'admin'
  }
}
</script>

<style scoped>
.vod-uploader {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vod-uploader__panel {
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-blank);
}

.vod-uploader__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 12px;
}

.vod-uploader__name {
  color: var(--el-text-color-primary);
  word-break: break-all;
}

.vod-uploader__size {
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}

.vod-uploader__status {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  margin-top: 8px;
}

.vod-uploader__actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.vod-uploader__error {
  color: var(--el-color-danger);
  font-size: 12px;
}
</style>
