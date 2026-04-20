<template>
  <div class="vod-batch">
    <div class="vod-batch__bar">
      <el-upload
        :auto-upload="false"
        :show-file-list="false"
        :limit="0"
        multiple
        accept="video/*"
        @change="handleFilesChange"
      >
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          选取文件
        </el-button>
      </el-upload>
      <span class="vod-batch__hint">
        请选择 mp4 / mov 视频，可一次多选；上传到阿里云 VOD（{{ summaryText }}）
      </span>
    </div>

    <div v-if="tasks.length > 0" class="vod-batch__list">
      <div v-for="(t, idx) in sortedTasks" :key="t.uid" class="vod-batch__row">
        <div class="vod-batch__row-head">
          <span class="vod-batch__index">{{ String(idx + 1).padStart(2, '0') }}</span>
          <span class="vod-batch__name" :title="t.fileName">{{ t.fileName || '(未命名)' }}</span>
          <span class="vod-batch__size">{{ formatSize(t.fileSize) }}</span>
          <el-button
            type="danger"
            link
            size="small"
            :disabled="t.status === 'uploading' || t.status === 'preparing'"
            @click="removeTask(t.uid)"
          >
            删除
          </el-button>
        </div>
        <div class="vod-batch__row-body">
          <el-progress
            :percentage="t.progress"
            :status="progressStatus(t)"
            :stroke-width="8"
            :show-text="false"
          />
          <el-tag size="small" :type="statusTagType(t.status)">{{ statusText(t) }}</el-tag>
          <span v-if="t.error" class="vod-batch__error">{{ t.error }}</span>
          <el-button
            v-if="t.status === 'failed'"
            link
            size="small"
            type="warning"
            @click="retryTask(t.uid)"
          >
            重试
          </el-button>
        </div>
      </div>
    </div>
    <div v-else class="vod-batch__empty">暂未选择文件</div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Upload } from '@element-plus/icons-vue'
import request from '@/api/request'

const OSS_SDK_URL =
  'https://cdn.jsdelivr.net/npm/aliyun-vod-upload-1.5.5@1.0.0/lib/aliyun-upload-sdk-1.5.5/lib/aliyun-oss-sdk-6.17.1.min.js'
const VOD_SDK_URL =
  'https://cdn.jsdelivr.net/npm/aliyun-vod-upload-1.5.5@1.0.0/lib/aliyun-upload-sdk-1.5.5/aliyun-upload-sdk-1.5.5.min.js'

let sdkLoadingPromise
let vodConfigPromise
let cachedVodConfig = null

const MAX_PARALLEL = 2

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => [],
  },
  dramaTitle: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['update:modelValue'])

const tasks = reactive([])
let uidSeq = 1
let initialized = false

function makeUid() {
  return `t_${Date.now()}_${uidSeq++}`
}

function stripExt(name) {
  if (!name) return ''
  const dot = name.lastIndexOf('.')
  return dot > 0 ? name.slice(0, dot) : name
}

function fileNameSort(a, b) {
  return String(a.fileName || '').localeCompare(String(b.fileName || ''), 'zh', { numeric: true })
}

const sortedTasks = computed(() => [...tasks].sort(fileNameSort))

const summaryText = computed(() => {
  const total = tasks.length
  const ok = tasks.filter((t) => t.status === 'normal' || t.status === 'transcoding' || t.status === 'success').length
  const ing = tasks.filter((t) => t.status === 'uploading' || t.status === 'preparing').length
  const fail = tasks.filter((t) => t.status === 'failed').length
  return `共 ${total} 集 · ${ok} 完成 · ${ing} 上传中 · ${fail} 失败`
})

function progressStatus(t) {
  if (t.status === 'failed') return 'exception'
  if (t.status === 'success' || t.status === 'normal' || t.status === 'transcoding') return 'success'
  return undefined
}

function statusTagType(s) {
  if (s === 'normal' || s === 'success') return 'success'
  if (s === 'failed') return 'danger'
  if (s === 'uploading' || s === 'preparing' || s === 'transcoding') return 'primary'
  return 'info'
}

function statusText(t) {
  const map = {
    pending: '等待中',
    preparing: '准备中',
    uploading: '上传中',
    success: '已上传',
    uploadsucc: '已上传',
    uploadsuccess: '已上传',
    transcoding: '转码中',
    snapshotting: '截图中',
    checking: '审核中',
    reviewing: '人工审核中',
    normal: '已就绪',
    failed: '失败',
    deleted: '已删除',
  }
  if (t.status === 'uploading') return `上传中 ${t.progress}%`
  return map[t.status] || t.status || '-'
}

function formatSize(bytes) {
  const n = Number(bytes) || 0
  if (n <= 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let v = n
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i += 1
  }
  return `${v.toFixed(v >= 100 || i === 0 ? 0 : 1)} ${units[i]}`
}

function mapVodStatus(t) {
  if (!t.videoId) return ''
  if (t.status === 'success') return 'uploading'
  if (['uploading', 'transcoding', 'normal', 'failed', 'uploadsucc', 'checking', 'reviewing'].includes(t.status)) {
    return t.status
  }
  return 'uploading'
}

function emitChange() {
  const list = sortedTasks.value.map((t, idx) => ({
    episode_num: idx + 1,
    title: t.title || '',
    video_id: t.videoId || '',
    vod_video_id: t.videoId || '',
    vod_status: mapVodStatus(t),
    video_url: t.video_url || '',
    video_size: Number(t.fileSize) || 0,
    vod_cover_url: t.vod_cover_url || '',
    duration: Number(t.duration) || 0,
  }))
  emit('update:modelValue', list)
}

function loadFromModel(list) {
  tasks.splice(0, tasks.length)
  ;(list || []).forEach((ep) => {
    const vid = ep.vod_video_id || ep.video_id || ''
    const baseStatus = vid ? (ep.vod_status || 'normal') : 'pending'
    tasks.push({
      uid: makeUid(),
      file: null,
      fileName: ep.title || (vid ? `已上传_${vid}` : ''),
      fileSize: Number(ep.video_size) || 0,
      progress: vid ? 100 : 0,
      status: baseStatus,
      videoId: vid,
      title: ep.title || '',
      duration: Number(ep.duration) || 0,
      video_url: ep.video_url || '',
      vod_cover_url: ep.vod_cover_url || '',
      error: '',
      uploader: null,
    })
  })
}

// onMounted in unified hook below

watch(
  () => props.modelValue,
  (val) => {
    if (!initialized) return
    const incomingIds = (val || []).map((e) => e.vod_video_id || e.video_id).filter(Boolean).sort().join(',')
    const currentIds = tasks.map((t) => t.videoId).filter(Boolean).sort().join(',')
    const inFlight = tasks.some((t) => t.status === 'uploading' || t.status === 'preparing')
    if (!inFlight && incomingIds !== currentIds) {
      loadFromModel(val)
    }
  },
)

async function handleFilesChange(file, fileList) {
  if (!file?.raw) return
  const exists = new Set(tasks.map((t) => `${t.fileName}|${t.fileSize}`))
  const incoming = (fileList || []).map((f) => f).filter((f) => f?.raw)
  let added = 0
  for (const f of incoming) {
    const key = `${f.name}|${f.size}`
    if (exists.has(key)) continue
    exists.add(key)
    tasks.push({
      uid: makeUid(),
      file: f.raw,
      fileName: f.name,
      fileSize: f.size,
      progress: 0,
      status: 'pending',
      videoId: '',
      title: stripExt(f.name),
      duration: 0,
      video_url: '',
      vod_cover_url: '',
      error: '',
      uploader: null,
    })
    added += 1
  }
  if (added > 0) {
    ElMessage.success(`已加入 ${added} 个文件`)
    emitChange()
    schedule()
  }
}

function removeTask(uid) {
  const idx = tasks.findIndex((t) => t.uid === uid)
  if (idx < 0) return
  const t = tasks[idx]
  if (t.uploader && (t.status === 'uploading' || t.status === 'preparing')) {
    try {
      t.uploader.stopUpload?.()
      t.uploader.cancelFile?.()
    } catch {
      // ignore
    }
  }
  tasks.splice(idx, 1)
  emitChange()
  schedule()
}

function retryTask(uid) {
  const t = tasks.find((x) => x.uid === uid)
  if (!t || !t.file) {
    ElMessage.warning('该任务无原始文件，无法重试（请重新选取文件）')
    return
  }
  t.status = 'pending'
  t.progress = 0
  t.error = ''
  t.videoId = ''
  emitChange()
  schedule()
}

function activeCount() {
  return tasks.filter((t) => t.status === 'uploading' || t.status === 'preparing').length
}

async function schedule() {
  while (activeCount() < MAX_PARALLEL) {
    const next = tasks.find((t) => t.status === 'pending' && t.file)
    if (!next) break
    next.status = 'preparing'
    startUpload(next).catch(() => {})
  }
}

async function startUpload(t) {
  try {
    const cfg = await getVodConfig()
    if (!cfg?.enabled) {
      throw new Error('当前环境未配置阿里云 VOD AccessKey')
    }
    await ensureVodSdk()
    const uploader = createUploader(t, cfg.regionId || 'cn-shanghai')
    t.uploader = uploader
    t.status = 'uploading'
    uploader.addFile(t.file, null, null, null, '{"Vod":{}}')
    uploader.startUpload()
  } catch (e) {
    failTask(t, e?.message || '上传失败')
    schedule()
  }
}

function createUploader(t, regionId) {
  const uploader = new window.AliyunUpload.Vod({
    userId: resolveUserId(),
    region: regionId || 'cn-shanghai',
    partSize: 1048576,
    parallel: 3,
    retryCount: 3,
    retryDuration: 2,
    addFileSuccess: () => {},
    onUploadstarted: (uploadInfo) => {
      handleUploadStarted(t, uploader, uploadInfo).catch((e) => failTask(t, e?.message || '获取凭证失败'))
    },
    onUploadProgress: (_uploadInfo, _totalSize, loadedPercent) => {
      t.progress = Math.max(0, Math.min(100, Math.floor((loadedPercent || 0) * 100)))
      t.status = 'uploading'
    },
    onUploadSucceed: async () => {
      t.progress = 100
      t.status = 'success'
      t.error = ''
      try {
        const infoRes = await request.get(`/vod/info/${t.videoId}`)
        const info = infoRes?.data || {}
        t.duration = Number(info.duration || 0)
        t.fileSize = Number(info.size || t.fileSize || 0)
        t.vod_cover_url = info.coverUrl || ''
        t.status = info.status || 'uploading'
      } catch {
        t.status = 'uploading'
      }
      emitChange()
      schedule()
    },
    onUploadFailed: (_uploadInfo, code, message) => {
      failTask(t, message || code || '上传失败')
      schedule()
    },
    onUploadCanceled: () => {
      // 用户主动删除时已经处理；这里仅同步状态
      if (t.status !== 'failed') {
        t.status = 'pending'
      }
    },
    onUploadTokenExpired: () => {
      refreshAuth(t, uploader).catch((e) => failTask(t, e?.message || '凭证刷新失败'))
    },
  })
  return uploader
}

async function handleUploadStarted(t, uploader, uploadInfo) {
  const res = uploadInfo?.videoId
    ? await request.get('/vod/refresh-upload-auth', { params: { videoId: uploadInfo.videoId } })
    : await request.post('/vod/upload-auth', {
        title: composeTitle(t),
        fileName: t.fileName,
      })
  const data = res?.data || {}
  t.videoId = data.videoId || uploadInfo?.videoId || t.videoId
  uploader.setUploadAuthAndAddress(
    uploadInfo,
    data.uploadAuth,
    data.uploadAddress,
    data.videoId || uploadInfo?.videoId || t.videoId,
  )
}

async function refreshAuth(t, uploader) {
  const res = await request.get('/vod/refresh-upload-auth', { params: { videoId: t.videoId } })
  const data = res?.data || {}
  if (typeof uploader.resumeUploadWithAuth === 'function' && data.uploadAuth) {
    uploader.resumeUploadWithAuth(data.uploadAuth)
  } else {
    uploader.startUpload?.()
  }
}

function composeTitle(t) {
  const drama = (props.dramaTitle || '').trim()
  const base = stripExt(t.fileName) || 'episode'
  return drama ? `${drama}_${base}` : base
}

function failTask(t, msg) {
  t.status = 'failed'
  t.error = msg
  emitChange()
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
  if (checker()) return Promise.resolve()
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

function beforeUnloadHandler(e) {
  if (activeCount() > 0) {
    e.preventDefault()
    e.returnValue = '有上传未完成，确认离开？'
    return e.returnValue
  }
  return undefined
}

onMounted(() => {
  loadFromModel(props.modelValue)
  initialized = true
  window.addEventListener('beforeunload', beforeUnloadHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', beforeUnloadHandler)
  tasks.forEach((t) => {
    if (t.uploader && (t.status === 'uploading' || t.status === 'preparing')) {
      try {
        t.uploader.stopUpload?.()
      } catch {
        // ignore
      }
    }
  })
})
</script>

<style scoped>
.vod-batch {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.vod-batch__bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.vod-batch__hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.vod-batch__list {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 8px;
  max-height: 320px;
  overflow-y: auto;
  background: var(--el-fill-color-blank);
}

.vod-batch__row + .vod-batch__row {
  border-top: 1px dashed var(--el-border-color-lighter);
  margin-top: 8px;
  padding-top: 8px;
}

.vod-batch__row-head {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}

.vod-batch__index {
  font-family: 'SF Mono', 'Monaco', monospace;
  color: var(--el-text-color-secondary);
  font-weight: 600;
  flex-shrink: 0;
}

.vod-batch__name {
  flex: 1;
  color: var(--el-text-color-primary);
  word-break: break-all;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.vod-batch__size {
  color: var(--el-text-color-secondary);
  white-space: nowrap;
  font-size: 12px;
}

.vod-batch__row-body {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
}

.vod-batch__row-body :deep(.el-progress) {
  flex: 1;
}

.vod-batch__error {
  color: var(--el-color-danger);
  font-size: 12px;
}

.vod-batch__empty {
  color: var(--el-text-color-placeholder);
  text-align: center;
  padding: 16px 0;
  border: 1px dashed var(--el-border-color);
  border-radius: 6px;
  font-size: 13px;
}
</style>
