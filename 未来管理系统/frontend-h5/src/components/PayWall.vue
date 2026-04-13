<template>
  <div class="paywall-mask" @click.self="$emit('close')">
    <div class="paywall-sheet">
      <!-- 标题 -->
      <div class="paywall-header">
        <div class="paywall-title">解锁后续剧情</div>
        <div class="paywall-sub">第 {{ episode }} 集及后续内容需要金豆解锁</div>
        <van-icon name="cross" class="paywall-close" size="20px" @click="$emit('close')" />
      </div>

      <!-- 当前余额 -->
      <div class="beans-row">
        <van-icon name="gold-coin-o" color="#ffb300" size="18px" />
        <span>当前余额：<b>{{ store.beans }}</b> 金豆</span>
      </div>

      <!-- 套餐列表 -->
      <div class="plan-list">
        <div
          v-for="(plan, idx) in plans"
          :key="plan.id"
          class="plan-card"
          :class="{ 'plan-card--selected': selectedPlan === plan.id, 'plan-card--hot': idx === 1 }"
          @click="selectedPlan = plan.id"
        >
          <div v-if="idx === 1" class="plan-badge">推荐</div>
          <div class="plan-beans">
            <van-icon name="gold-coin-o" color="#ffb300" size="16px" />
            <span>{{ planBeans(plan) }} 金豆</span>
          </div>
          <div class="plan-price">${{ planPrice(plan) }}</div>
          <div class="plan-desc">约看 {{ planBeans(plan) }} 集</div>
        </div>
      </div>

      <!-- 支付按钮 -->
      <van-button
        type="primary"
        block
        round
        :loading="paying"
        :disabled="!selectedPlan"
        class="pay-btn"
        @click="handlePay"
      >
        {{ paying ? '处理中...' : '立即充值' }}
      </van-button>

      <div class="pay-tip">安全支付 · 支持 Visa / Mastercard / Apple Pay</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Icon as VanIcon, Button as VanButton, showToast } from 'vant'
import { useDramaStore } from '@/stores/drama'
import { createPayment } from '@/api/index'

const props = defineProps({
  episode: { type: Number, default: 1 },
  plans: { type: Array, default: () => [] },
})
const emit = defineEmits(['paid', 'close'])

const store = useDramaStore()
const selectedPlan = ref(null)
const paying = ref(false)

onMounted(() => {
  // 默认选中中间套餐（推荐档）
  if (props.plans.length >= 2) {
    selectedPlan.value = props.plans[1].id
  } else if (props.plans.length === 1) {
    selectedPlan.value = props.plans[0].id
  }
})

async function handlePay() {
  if (!selectedPlan.value) return
  paying.value = true
  try {
    const res = await createPayment(selectedPlan.value, store.promoId)
    if (res.code === 0) {
      // TODO: 接入 Stripe.js 完成支付流程
      // 目前模拟支付成功（联调时替换为真实 Stripe 逻辑）
      showToast({ type: 'success', message: '充值成功！' })
      emit('paid')
    } else {
      showToast(res.message || '支付失败，请重试')
    }
  } catch (e) {
    showToast('网络错误，请重试')
  } finally {
    paying.value = false
  }
}

function planBeans(plan) {
  return plan?.beans ?? ((plan?.bean_count ?? 0) + (plan?.extra_bean ?? 0))
}

function planPrice(plan) {
  return plan?.price ?? plan?.amount ?? 0
}
</script>

<style scoped>
.paywall-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.65);
  z-index: 1000;
  display: flex;
  align-items: flex-end;
}

.paywall-sheet {
  width: 100%;
  background: #fff;
  border-radius: 20px 20px 0 0;
  padding: 24px 20px 40px;
  position: relative;
}

.paywall-header {
  margin-bottom: 16px;
  padding-right: 32px;
}
.paywall-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}
.paywall-sub {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
}
.paywall-close {
  position: absolute;
  top: 24px;
  right: 20px;
  color: #999;
  cursor: pointer;
}

.beans-row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #555;
  margin-bottom: 16px;
}
.beans-row b { color: #ff4757; }

/* 套餐卡片 */
.plan-list {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.plan-card {
  flex: 1;
  border: 2px solid #e8e8e8;
  border-radius: 12px;
  padding: 14px 8px;
  text-align: center;
  cursor: pointer;
  position: relative;
  transition: border-color 0.2s;
  background: #fafafa;
}
.plan-card--selected {
  border-color: #ff4757;
  background: #fff0f1;
}
.plan-card--hot {
  border-color: #ffb300;
  background: #fffbf0;
}
.plan-card--hot.plan-card--selected {
  border-color: #ff4757;
  background: #fff0f1;
}
.plan-badge {
  position: absolute;
  top: -10px;
  left: 50%;
  transform: translateX(-50%);
  background: #ffb300;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
}
.plan-beans {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a1a;
  margin-bottom: 4px;
}
.plan-price {
  font-size: 18px;
  font-weight: 700;
  color: #ff4757;
  margin-bottom: 2px;
}
.plan-desc {
  font-size: 11px;
  color: #aaa;
}

.pay-btn {
  --van-button-primary-background: #ff4757;
  --van-button-primary-border-color: #ff4757;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.pay-tip {
  text-align: center;
  font-size: 12px;
  color: #bbb;
}
</style>
