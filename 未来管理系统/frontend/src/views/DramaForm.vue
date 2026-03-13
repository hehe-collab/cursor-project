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
            <el-input-number v-model="ep.episode_num" :min="1" placeholder="集数" style="width:100px" />
            <el-input v-model="ep.title" placeholder="集标题" style="width:180px" />
            <el-input v-model="ep.video_id" placeholder="阿里云 VideoId" style="width:180px" />
            <el-input v-model="ep.video_url" placeholder="或播放地址" style="width:200px" />
            <el-input-number v-model="ep.duration" :min="0" placeholder="时长(秒)" style="width:100px" />
            <el-button type="danger" link @click="form.episodes.splice(idx, 1)">删除</el-button>
          </div>
          <el-button type="primary" link @click="form.episodes.push({ episode_num: form.episodes.length + 1, title: '', video_id: '', video_url: '', duration: 0 })">
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
  episodes: [{ episode_num: 1, title: '', video_id: '', video_url: '', duration: 0 }],
})

const rules = {
  title: [{ required: true, message: '请输入剧名', trigger: 'blur' }],
}

const isEdit = computed(() => !!route.params.id)

async function loadData() {
  try {
    const [catRes, tagRes] = await Promise.all([
      request.get('/categories').catch(() => ({ data: [] })),
      request.get('/tags').catch(() => ({ data: [] })),
    ])
    categories.value = catRes.data || []
    tags.value = tagRes.data || []
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
    cover: d.cover,
    description: d.description,
    category_id: d.category_id,
    tag_ids: d.tag_ids || [],
    status: d.status,
    sort: d.sort,
    episodes: (d.episodes && d.episodes.length) ? d.episodes.map(e => ({
      episode_num: e.episode_num,
      title: e.title,
      video_id: e.video_id || '',
      video_url: e.video_url || '',
      duration: e.duration || 0,
    })) : [{ episode_num: 1, title: '', video_id: '', video_url: '', duration: 0 }],
  })
  } catch (e) {
    ElMessage.error('加载剧集详情失败')
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    if (isEdit.value) {
      await request.put(`/dramas/${route.params.id}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/dramas', form)
      ElMessage.success('创建成功')
    }
    router.push('/dramas')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
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
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
</style>
