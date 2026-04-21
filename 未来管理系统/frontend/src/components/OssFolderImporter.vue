<template>
  <div class="oss-importer">
    <div class="oss-importer__bar">
      <el-input
        v-model="ossPath"
        placeholder="OSS 路径，如 dramas/myshow/  或  oss://bucket/dramas/myshow/"
        clearable
        :disabled="loading"
        class="oss-importer__input"
        @keyup.enter="handleScan"
      />
      <el-button type="primary" :loading="loading" @click="handleScan">
        <el-icon><Search /></el-icon>
        扫描
      </el-button>
    </div>
    <div class="oss-importer__hint">
      只扫描当前文件夹一层（不递归子目录），仅识别 mp4 / mov / m3u8 / mpd / flv / mkv / ts / webm / avi / wmv 后缀。
    </div>

    <div v-if="scanResult" class="oss-importer__summary">
      <span>已扫描 bucket：<b>{{ scanResult.bucket }}</b></span>
      <span>prefix：<b>{{ scanResult.prefix || '(根目录)' }}</b></span>
      <span>共 <b>{{ files.length }}</b> 个视频文件</span>
    </div>

    <div v-if="files.length > 0" class="oss-importer__table">
      <el-table :data="files" size="small" border stripe>
        <el-table-column label="#" width="46" align="center">
          <template #default="{ $index }">
            <span class="oss-importer__epnum">{{ $index + 1 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="文件名" prop="name" min-width="100" show-overflow-tooltip />
        <el-table-column label="大小" prop="sizeText" width="86" align="right" />
        <el-table-column label="" width="80" align="center">
          <template #default="{ $index }">
            <span class="oss-importer__actions">
              <el-button size="small" link :disabled="$index === 0" @click="moveItem($index, -1)">
                <el-icon><ArrowUp /></el-icon>
              </el-button>
              <el-button size="small" link :disabled="$index === files.length - 1" @click="moveItem($index, 1)">
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <el-button size="small" link type="danger" @click="removeItem($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <el-empty
      v-else-if="scanResult && files.length === 0"
      description="该路径下未发现视频文件"
      :image-size="80"
    />
    <div v-else class="oss-importer__empty">
      尚未扫描。在上方填入 OSS 路径后点「扫描」。
    </div>

    <div v-if="files.length > 0" class="oss-importer__mode">
      <el-radio-group v-model="modeValue" @change="emitChange">
        <el-radio value="append">追加到现有分集（episode_num 顺延）</el-radio>
        <el-radio value="replace">替换现有分集（先清空再导入）</el-radio>
      </el-radio-group>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowDown, ArrowUp, Delete, Search } from '@element-plus/icons-vue'
import request from '@/api/request'

const props = defineProps({
  /**
   * v-model 输出：当前选定要导入的文件数组。
   * 每项形如 { bucket, key, name, size, sizeText }
   */
  modelValue: {
    type: Array,
    default: () => [],
  },
  /** 默认导入模式：append | replace */
  mode: {
    type: String,
    default: 'append',
  },
})

const emit = defineEmits(['update:modelValue', 'update:mode', 'scanned'])

const ossPath = ref('')
const loading = ref(false)
const scanResult = ref(null)
const files = reactive([])
const modeValue = ref(props.mode || 'append')

watch(
  () => props.mode,
  (v) => {
    if (v && v !== modeValue.value) modeValue.value = v
  },
)

const summary = computed(() => ({
  count: files.length,
  bucket: scanResult.value?.bucket || '',
  prefix: scanResult.value?.prefix || '',
}))

function emitChange() {
  emit(
    'update:modelValue',
    files.map((f) => ({
      bucket: f.bucket,
      key: f.key,
      name: f.name,
      size: f.size,
      sizeText: f.sizeText,
    })),
  )
  emit('update:mode', modeValue.value)
}

async function handleScan() {
  if (!ossPath.value.trim()) {
    ElMessage.warning('请输入 OSS 路径')
    return
  }
  loading.value = true
  try {
    const res = await request.post('/oss/scan-videos', { ossPath: ossPath.value.trim() })
    if (res?.code !== 0) {
      ElMessage.error(res?.message || '扫描失败')
      return
    }
    const data = res.data || {}
    scanResult.value = {
      bucket: data.bucket,
      prefix: data.prefix,
      count: data.count,
    }
    files.splice(
      0,
      files.length,
      ...(data.files || []).map((f) => ({
        bucket: data.bucket,
        key: f.key,
        name: f.name,
        size: f.size,
        sizeText: f.sizeText,
        lastModified: f.lastModified,
      })),
    )
    emitChange()
    emit('scanned', { ...summary.value, files: [...files] })
    ElMessage.success(`扫描到 ${files.length} 个视频文件`)
  } catch (e) {
    ElMessage.error(e?.message || '扫描失败，请检查 OSS 配置和路径')
  } finally {
    loading.value = false
  }
}

function moveItem(index, delta) {
  const target = index + delta
  if (target < 0 || target >= files.length) return
  const tmp = files[index]
  files[index] = files[target]
  files[target] = tmp
  emitChange()
}

function removeItem(index) {
  files.splice(index, 1)
  emitChange()
}

defineExpose({
  reset() {
    ossPath.value = ''
    scanResult.value = null
    files.splice(0, files.length)
    modeValue.value = props.mode || 'append'
    emitChange()
  },
  getMode() {
    return modeValue.value
  },
  hasFiles() {
    return files.length > 0
  },
})
</script>

<style scoped>
.oss-importer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.oss-importer__bar {
  display: flex;
  gap: 6px;
  align-items: center;
}

.oss-importer__input {
  flex: 1;
}

.oss-importer__hint {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.4;
}

.oss-importer__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px 10px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.oss-importer__table {
  max-height: 260px;
  overflow-y: auto;
}

.oss-importer__epnum {
  font-weight: 600;
  color: var(--el-color-primary);
}

.oss-importer__empty {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-align: center;
  padding: 14px 0;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}

.oss-importer__mode {
  padding-top: 2px;
}

.oss-importer__actions {
  display: inline-flex;
  gap: 0;
  white-space: nowrap;
  align-items: center;
}
.oss-importer__actions .el-button + .el-button {
  margin-left: 2px;
}
</style>
