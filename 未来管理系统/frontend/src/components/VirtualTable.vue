<template>
  <div class="virtual-table-root">
    <div class="virtual-table-header">
      <div class="v-row v-row--head" :style="gridStyle">
        <div v-for="col in columns" :key="col.prop" class="v-cell" :style="cellStyle(col)">
          {{ col.label }}
        </div>
      </div>
    </div>
    <RecycleScroller
      class="virtual-table-scroller"
      :items="data"
      :item-size="itemSize"
      :key-field="keyField"
      :page-mode="false"
      v-slot="{ item, index }"
    >
      <div
        class="v-row v-row--body"
        :class="{ 'v-row--stripe': stripe && index % 2 === 1 }"
        :style="{ ...gridStyle, minHeight: itemSize + 'px' }"
      >
        <div v-for="col in columns" :key="col.prop" class="v-cell" :style="cellStyle(col)">
          <slot v-if="col.slot" :name="col.slot" :row="item" :index="index" />
          <span v-else class="v-cell-text">{{ item[col.prop] }}</span>
        </div>
      </div>
    </RecycleScroller>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RecycleScroller } from 'vue-virtual-scroller'

const props = defineProps({
  data: { type: Array, required: true },
  columns: { type: Array, required: true },
  itemSize: { type: Number, default: 44 },
  keyField: { type: String, default: 'id' },
  stripe: { type: Boolean, default: true },
})

const gridStyle = computed(() => ({
  display: 'grid',
  gridTemplateColumns: props.columns.map((c) => c.gridWidth || c.width || 'minmax(64px,1fr)').join(' '),
}))

function cellStyle(col) {
  return {
    minWidth: 0,
  }
}
</script>

<style scoped>
.virtual-table-root {
  height: 100%;
  min-height: 120px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.virtual-table-header {
  flex-shrink: 0;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.virtual-table-scroller {
  flex: 1;
  min-height: 0;
}

:deep(.vue-recycle-scroller) {
  height: 100% !important;
}

:deep(.vue-recycle-scroller__item-wrapper) {
  overflow: visible;
}

.v-row {
  box-sizing: border-box;
  align-items: center;
}

.v-row--head {
  min-height: 36px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.v-row--body {
  font-size: 12px;
  color: var(--el-text-color-primary);
  border-bottom: 1px solid var(--el-border-color-extra-light);
}

.v-row--body:hover {
  background: var(--el-fill-color-light);
}

.v-row--stripe {
  background: var(--el-fill-color-blank);
}

.v-cell {
  padding: 6px 10px;
  border-right: 1px solid var(--el-border-color-extra-light);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.v-cell:last-child {
  border-right: none;
}

.v-cell-text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
