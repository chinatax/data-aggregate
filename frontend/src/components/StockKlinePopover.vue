<template>
  <span
    class="skp-trigger"
    ref="triggerRef"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
  >
    {{ name }}
  </span>
  <Teleport to="body">
    <div
      v-if="visible"
      class="skp-popup"
      ref="popupRef"
      :style="popupStyle"
      @mouseenter="onPopupEnter"
      @mouseleave="onPopupLeave"
    >
      <!-- Loading -->
      <div v-if="loading" class="skp-loading">加载中...</div>

      <!-- Error / No data -->
      <div v-else-if="error" class="skp-error">{{ error }}</div>
      <div v-else-if="!klineData.length" class="skp-empty">暂无K线数据</div>

      <!-- Chart -->
      <template v-else>
        <svg
          :viewBox="`0 0 ${PW} ${PH}`"
          class="skp-chart"
          xmlns="http://www.w3.org/2000/svg"
          @mousemove="onChartMouseMove"
          @mouseleave="onChartMouseLeave"
        >
          <!-- Grid lines + Y-axis labels -->
          <g v-for="(g, i) in yGrid" :key="'gl' + i">
            <line
              :x1="PAD_L" :y1="g.y" :x2="PW - PAD_R" :y2="g.y"
              stroke="#eee" stroke-width="0.5"
            />
            <text
              :x="PAD_L - 4" :y="g.y + 4"
              text-anchor="end" font-size="9" fill="#999"
            >{{ g.label }}</text>
          </g>

          <!-- Candlesticks -->
          <g v-for="(d, i) in klineData" :key="d.date">
            <!-- Wick (high-low line) -->
            <line
              :x1="cx(i)" :y1="cy(d.high)"
              :x2="cx(i)" :y2="cy(d.low)"
              :stroke="d.close >= d.open ? '#E83030' : '#1CAD5E'"
              stroke-width="0.8"
            />
            <!-- Body (open-close rect) -->
            <rect
              :x="cx(i) - candleHalfW"
              :y="cy(Math.max(d.open, d.close))"
              :width="candleW"
              :height="Math.max(1, Math.abs(cy(d.open) - cy(d.close)))"
              :fill="d.close >= d.open ? '#E83030' : '#1CAD5E'"
            />
          </g>

          <!-- Crosshair -->
          <template v-if="crosshair.idx >= 0">
            <!-- Vertical line -->
            <line
              :x1="crosshair.x" :y1="PAD_T"
              :x2="crosshair.x" :y2="PH - PAD_B"
              stroke="#666" stroke-width="0.5" stroke-dasharray="3,2"
            />
            <!-- Horizontal line -->
            <line
              :x1="PAD_L" :y1="crosshair.y"
              :x2="PW - PAD_R" :y2="crosshair.y"
              stroke="#666" stroke-width="0.5" stroke-dasharray="3,2"
            />
            <!-- Cross dot -->
            <circle
              :cx="crosshair.x" :cy="crosshair.y"
              r="2.5" fill="#333"
            />
            <!-- Y price label -->
            <rect
              :x="0" :y="crosshair.y - 8"
              width="40" height="16" rx="2"
              fill="rgba(0,0,0,0.75)"
            />
            <text
              :x="PAD_L - 4" :y="crosshair.y + 4"
              text-anchor="end" font-size="9" fill="#fff" font-weight="600"
            >{{ crosshair.priceLabel }}</text>
          </template>

          <!-- X-axis date labels -->
          <text
            v-for="(xl, i) in xLabels"
            :key="'xl' + i"
            :x="cx(xl.idx)" :y="PH - 4"
            text-anchor="middle" font-size="9" fill="#999"
          >{{ xl.text }}</text>
        </svg>

        <!-- Crosshair tooltip below chart -->
        <div v-if="crosshair.idx >= 0" class="skp-cross-tip">
          <span class="ct-date">{{ crosshair.data?.date }}</span>
          <span class="ct-row">开 {{ crosshair.data?.open?.toFixed(2) }}</span>
          <span class="ct-row">高 {{ crosshair.data?.high?.toFixed(2) }}</span>
          <span class="ct-row">低 {{ crosshair.data?.low?.toFixed(2) }}</span>
          <span class="ct-row">收 {{ crosshair.data?.close?.toFixed(2) }}</span>
          <span
            v-if="crosshair.data?.changePct != null"
            class="ct-chg"
            :class="crosshair.data.changePct >= 0 ? 'up' : 'down'"
          >{{ crosshair.data.changePct >= 0 ? '+' : '' }}{{ crosshair.data.changePct?.toFixed(2) }}%</span>
        </div>

        <!-- Detail slot -->
        <div v-if="$slots.detail" class="skp-detail">
          <slot name="detail" />
        </div>
      </template>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { getStockKline, type StockKlineItem } from '@/api/stock'

const props = defineProps<{
  code: string
  name: string
}>()

// ---- Module-level frontend cache ----
const klineCache = new Map<string, { data: StockKlineItem[]; ts: number }>()
const CACHE_TTL = 5 * 60 * 1000 // 5 minutes

// ---- State ----
const triggerRef = ref<HTMLElement | null>(null)
const popupRef = ref<HTMLElement | null>(null)
const visible = ref(false)
const loading = ref(false)
const error = ref('')
const klineData = ref<StockKlineItem[]>([])
const popupStyle = ref<Record<string, string>>({})
let hideTimer: ReturnType<typeof setTimeout> | null = null

// ---- Crosshair state ----
const crosshair = ref<{
  idx: number
  x: number
  y: number
  priceLabel: string
  data: StockKlineItem | null
}>({ idx: -1, x: 0, y: 0, priceLabel: '', data: null })

// ---- Chart constants ----
const PW = 260  // popup width
const PH = 160  // popup height
const PAD_L = 42
const PAD_R = 8
const PAD_T = 10
const PAD_B = 18

const candleW = computed(() => {
  const n = klineData.value.length
  if (n <= 1) return 4
  const slot = (PW - PAD_L - PAD_R) / n
  return Math.max(1, slot * 0.7)
})
const candleHalfW = computed(() => candleW.value / 2)

// ---- Scale helpers ----
const priceRange = computed(() => {
  const all: number[] = []
  klineData.value.forEach(d => {
    all.push(d.high, d.low)
  })
  if (!all.length) return { min: 0, max: 100, range: 100 }
  const min = Math.min(...all)
  const max = Math.max(...all)
  const pad = (max - min) * 0.02 || 1
  return {
    min: min - pad,
    max: max + pad,
    range: max - min + 2 * pad,
  }
})

function cx(i: number): number {
  const n = klineData.value.length
  if (n <= 1) return PAD_L + (PW - PAD_L - PAD_R) / 2
  const slot = (PW - PAD_L - PAD_R) / n
  return PAD_L + i * slot + slot / 2
}

function cy(price: number): number {
  const r = priceRange.value
  return PAD_T + ((r.max - price) / r.range) * (PH - PAD_T - PAD_B)
}

const yGrid = computed(() => {
  const r = priceRange.value
  if (r.range <= 0) return []
  const steps = 4
  const grid: { y: number; label: string }[] = []
  for (let i = 0; i <= steps; i++) {
    const v = r.min + (r.range / steps) * i
    grid.push({
      y: cy(v),
      label: v.toFixed(2),
    })
  }
  return grid
})

const xLabels = computed(() => {
  const data = klineData.value
  if (!data.length) return []
  const labels: { idx: number; text: string }[] = []
  const step = Math.max(1, Math.floor(data.length / 5))
  for (let i = 0; i < data.length; i += step) {
    const d = data[i].date
    labels.push({ idx: i, text: d.length >= 10 ? d.slice(5) : d })
  }
  // Always include last point
  const last = data.length - 1
  if (last > 0 && labels[labels.length - 1]?.idx !== last) {
    const d = data[last].date
    labels.push({ idx: last, text: d.length >= 10 ? d.slice(5) : d })
  }
  return labels
})

// ---- Popup positioning ----
function computePosition() {
  const trigger = triggerRef.value
  if (!trigger) return
  const rect = trigger.getBoundingClientRect()
  const GAP = 8
  const MARGIN = 12

  let left = rect.left
  let top = rect.bottom + GAP

  // Flip X if overflow right
  if (left + PW > window.innerWidth - MARGIN) {
    left = Math.max(MARGIN, rect.right - PW)
  }
  // Flip Y if overflow bottom
  if (top + PH > window.innerHeight - MARGIN) {
    top = rect.top - PH - GAP
  }

  popupStyle.value = {
    position: 'fixed',
    left: Math.round(left) + 'px',
    top: Math.round(top) + 'px',
    zIndex: '9999',
  }
}

// ---- Hover logic ----
function clearHideTimer() {
  if (hideTimer) {
    clearTimeout(hideTimer)
    hideTimer = null
  }
}

async function onEnter() {
  clearHideTimer()
  computePosition()

  const code = props.code
  if (!code) return
  visible.value = true

  // Check cache
  const cached = klineCache.get(code)
  if (cached && Date.now() - cached.ts < CACHE_TTL) {
    klineData.value = cached.data
    loading.value = false
    error.value = ''
    return
  }

  // Fetch
  loading.value = true
  error.value = ''
  klineData.value = []
  try {
    const data = await getStockKline(code, 250)
    if (Array.isArray(data)) {
      klineData.value = data
      klineCache.set(code, { data, ts: Date.now() })
    } else {
      error.value = '数据格式异常'
    }
  } catch {
    error.value = '获取失败'
  } finally {
    loading.value = false
  }
}

function onLeave() {
  hideTimer = setTimeout(() => {
    visible.value = false
  }, 200)
}

function onPopupEnter() {
  clearHideTimer()
}

function onPopupLeave() {
  visible.value = false
}

// ---- Crosshair mouse handlers ----
function onChartMouseMove(e: MouseEvent) {
  const svg = e.currentTarget as SVGSVGElement
  const rect = svg.getBoundingClientRect()
  const scaleX = PW / rect.width
  const mx = (e.clientX - rect.left) * scaleX

  const n = klineData.value.length
  if (n === 0) return

  // Find nearest candle index
  const slot = (PW - PAD_L - PAD_R) / n
  let idx = Math.round((mx - PAD_L - slot / 2) / slot)
  idx = Math.max(0, Math.min(n - 1, idx))

  const d = klineData.value[idx]
  const x = cx(idx)
  // Y snaps to close price of the hovered candle
  const y = cy(d.close)

  crosshair.value = {
    idx,
    x,
    y,
    priceLabel: d.close.toFixed(2),
    data: d,
  }
}

function onChartMouseLeave() {
  crosshair.value = { idx: -1, x: 0, y: 0, priceLabel: '', data: null }
}

onUnmounted(() => {
  clearHideTimer()
})
</script>

<style lang="scss">
.skp-trigger {
  cursor: default;
  position: relative;
  display: inline;
}

.skp-popup {
  width: 260px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  border: 1px solid #E0E0E0;
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  padding: 10px;
  pointer-events: auto;
  box-sizing: border-box;
}

.skp-chart {
  width: 100%;
  height: auto;
  display: block;
}

.skp-loading,
.skp-error,
.skp-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 120px;
  font-size: 13px;
  color: #9E9E9E;
}

.skp-cross-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 4px 8px;
  background: #FAFAFA;
  border-radius: 4px;
  font-size: 11px;
  font-family: monospace;
  color: #424242;
  flex-wrap: wrap;

  .ct-date {
    font-weight: 600;
    color: #212121;
    margin-right: 2px;
  }

  .ct-row {
    color: #757575;
  }

  .ct-chg {
    font-weight: 600;
    margin-left: auto;
    &.up { color: #E83030; }
    &.down { color: #1CAD5E; }
  }
}

.skp-detail {
  margin-top: 6px;
  padding: 6px 8px;
  border-top: 1px solid #EEEEEE;
  font-size: 12px;
  color: #616161;
  line-height: 1.5;

  em {
    display: block;
    font-style: normal;
    font-family: monospace;
    font-size: 11px;
    color: #9E9E9E;
    margin-bottom: 2px;
  }

  b {
    display: block;
    font-size: 15px;
    font-weight: 700;
    margin-bottom: 2px;
    &.up { color: #E83030; }
    &.down { color: #1CAD5E; }
  }

  span {
    display: block;
    font-size: 11px;
    color: #757575;
  }

  .tip-reason {
    margin-top: 2px;
    color: #D97706;
  }
}
</style>