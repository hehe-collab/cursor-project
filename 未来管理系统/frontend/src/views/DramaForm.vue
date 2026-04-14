<template>
  <div class="drama-form">
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="剧名" prop="title">
          <el-input v-model="form.title" placeholder="请输入剧集标题" />
        </el-form-item>
        <el-form-item label="封面" prop="cover">
          <el-input v-model="form.cover" placeholder="封面图片 URL" />
        </el-form-item>
        <el-form-item label="分类" prop="category_id">
          <el-select v-model="form.category_id" placeholder="选择分类" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tag_ids">
          <el-select v-model="form.tag_ids" multiple placeholder="选择标签" clearable>
            <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="published">已发布</el-radio>
            <el-radio value="offline">已下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="剧集简介" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>

        <el-divider>剧集列表</el-divider>
        <div class="episodes">
          <div v-for="(ep, idx) in form.episodes" :key="idx" class="episode-row">
            <div class="episode-row__header">
              <el-input-number v-model="ep.episode_num" :min="1" placeholder="集数" style="width:100px" />
              <el-input v-model="ep.title" placeholder="集标题" style="width:220px" />
              <el-button type="danger" link @click="form.episodes.splice(idx, 1)">删除</el-button>
            </div>
            <div class="episode-row__fields">
              <el-input v-model="ep.vod_video_id" placeholder="VOD VideoId" />
              <el-input v-model="ep.video_url" placeholder="备用播放地址（可选）" />
              <el-input-number v-model="ep.duration" :min="0" placeholder="时长(秒)" style="width:100%" />
            </div>
            <VodUploader
              :title="episodeUploadTitle(ep, idx)"
              @success="(payload) => onVodUploadSuccess(idx, payload)"
            />
            <div class="episode-row__vod">
              <el-tag
                v-if="ep.vod_status"
                size="small"
                :type="ep.vod_status === 'normal' ? 'success' : ep.vod_status === 'failed' ? 'danger' : 'warning'"
              >
                {{ ep.vod_status }}
              </el-tag>
              <span v-if="ep.video_size" class="episode-row__muted">大小：{{ formatSize(ep.video_size) }}</span>
              <span v-if="ep.vod_cover_url" class="episode-row__muted">已生成封面</span>
            </div>
          </div>
          <el-button type="primary" link @click="addEpisode">
            + 添加集数
          </el-button>
        </div>

        <el-form-item style="margin-top: 24px">
          <el-button type="primary" @click="onSubmit" :loading="loading">保存</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'
import VodUploader from '@/components/VodUploader.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const categories = ref([])
const tags = ref([])

const form = reactive({
  title: '',
  cover: '',
  description: '',
  category_id: null,
  tag_ids: [],
  status: 'draft',
  sort: 0,
  episodes: [createEmptyEpisode(1)],
})

const rules = {
  title: [{ required: true, message: '请输入剧名', trigger: 'blur' }],
}

const isEdit = computed(() => !!route.params.id)

async function loadData() {
  try {
    const [catRes, tagRes] = await Promise.all([
      getCategories().catch(() => ({ code: -1, data: [] })),
      getTags().catch(() => ({ code: -1, data: [] })),
    ])
    categories.value = catRes.code === 0 && Array.isArray(catRes.data) ? catRes.data : []
    tags.value = tagRes.code === 0 && Array.isArray(tagRes.data) ? tagRes.data : []
  } catch (e) {
    ElMessage.error('加载分类/标签失败')
  }
}

async function loadDetail() {
  if (!isEdit.value) return
  try {
    const res = await request.get(`/dramas/${route.params.id}`)
    const d = res.data
    Object.assign(form, {
      title: d.title,
      cover: d.cover_image || d.cover || '',
      description: d.description,
      category_id: d.category_id,
      tag_ids: d.tag_ids || [],
      status: d.status,
      sort: d.sort,
      episodes: (d.episodes && d.episodes.length)
        ? d.episodes.map(e => ({
            episode_num: e.episode_num,
            title: e.title,
            video_id: e.video_id || '',
            vod_video_id: e.vod_video_id || e.video_id || '',
            vod_status: e.vod_status || '',
            video_url: e.video_url || '',
            video_size: e.video_size || 0,
            vod_cover_url: e.vod_cover_url || '',
            duration: e.duration || 0,
          }))
        : [createEmptyEpisode(1)],
    })
  } catch (e) {
    ElMessage.error('加载剧集详情失败')
  }
}

function createEmptyEpisode(episodeNum) {
  return {
    episode_num: episodeNum,
    title: '',
    video_id: '',
    vod_video_id: '',
    vod_status: '',
    video_url: '',
    video_size: 0,
    vod_cover_url: '',
    duration: 0,
  }
}

function addEpisode() {
  form.episodes.push(createEmptyEpisode(form.episodes.length + 1))
}

function onVodUploadSuccess(index, payload) {
  const current = form.episodes[index]
  if (!current) return
  form.episodes[index] = {
    ...current,
    ...payload,
  }
}

function episodeUploadTitle(ep, idx) {
  const dramaTitle = (form.title || '').trim() || 'drama'
  const episodeNum = Number(ep?.episode_num || idx + 1)
  return `${dramaTitle}_EP${episodeNum}`
}

function formatSize(bytes) {
  const value = Number(bytes || 0)
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = value
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size.toFixed(size >= 100 || index === 0 ? 0 : 1)} ${units[index]}`
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const payload = buildSubmitPayload()
    if (isEdit.value) {
      await request.put(`/dramas/${route.params.id}`, payload)
      ElMessage.success('更新成功')
    } else {
      await request.post('/dramas', payload)
      ElMessage.success('创建成功')
    }
    router.push('/dramas')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

function buildSubmitPayload() {
  return {
    title: form.title,
    cover_image: form.cover,
    description: form.description,
    category_id: form.category_id,
    tag_ids: form.tag_ids,
    status: form.status,
    sort: form.sort,
    episodes: form.episodes.map((ep) => ({
      episode_num: ep.episode_num,
      title: ep.title,
      video_id: ep.video_id || ep.vod_video_id || '',
      vod_video_id: ep.vod_video_id || ep.video_id || '',
      vod_status: ep.vod_status || '',
      video_url: ep.video_url || '',
      video_size: ep.video_size || 0,
      vod_cover_url: ep.vod_cover_url || '',
      duration: ep.duration || 0,
    })),
  }
}

onMounted(async () => {
  await loadData()
  await loadDetail()
})
</script>

<style scoped>
.episodes { margin-bottom: 16px; }
.episode-row {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
}
.episode-row__header {
  display: flex;
  align-items: center;
  gap: 12px;
}
.episode-row__fields {
  display: grid;
  grid-template-columns: 1.2fr 1.4fr 120px;
  gap: 12px;
  width: 100%;
}
.episode-row__vod {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  width: 100%;
}
.episode-row__muted {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
