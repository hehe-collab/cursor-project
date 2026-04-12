<template>
  <el-select
    v-model="selectedValue"
    filterable
    clearable
    placeholder="请选择广告账户"
    :loading="loading"
    :disabled="disabled"
    style="width: 100%;"
    @change="handleChange"
  >
    <el-option
      v-for="item in accountOptions"
      :key="item.advertiserId"
      :label="formatLabel(item)"
      :value="item.advertiserId"
    >
      <span class="account-option">
        <span class="account-name">{{ item.advertiserName || '未命名账户' }}</span>
        <span class="account-id">{{ item.advertiserId }}</span>
      </span>
    </el-option>
    <template #empty>
      <div class="empty-tip">
        <span v-if="loading">加载中…</span>
        <span v-else-if="accountOptions.length === 0">暂无可用账户，请先在账户管理添加 TikTok 账户</span>
      </div>
    </template>
  </el-select>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { getAccountList } from '@/api/tiktok/account'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  disabled: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const selectedValue = ref(props.modelValue)
const accountOptions = ref([])
const loading = ref(false)

watch(() => props.modelValue, (v) => {
  selectedValue.value = v
})

function handleChange(val) {
  emit('update:modelValue', val)
  const account = accountOptions.value.find(a => a.advertiserId === val)
  emit('change', account || null)
}

function formatLabel(item) {
  const name = item.advertiserName || '未命名'
  return `${name} (${item.advertiserId})`
}

async function fetchAccounts() {
  loading.value = true
  try {
    const res = await getAccountList({ status: 'active' })
    const list = res?.data || []
    accountOptions.value = list.filter(a => a.advertiserId)
  } catch (e) {
    console.error('获取账户列表失败', e)
    accountOptions.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchAccounts()
})
</script>

<style scoped>
.account-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}
.account-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.account-id {
  font-size: 11px;
  color: #909399;
  font-family: 'Courier New', monospace;
  flex-shrink: 0;
}
.empty-tip {
  padding: 12px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
