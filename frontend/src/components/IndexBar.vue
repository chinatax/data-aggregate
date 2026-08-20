<template>
  <div class="index-bar" v-if="tickerItems.length">
    <div
      class="index-track"
      @mouseenter="paused = true"
      @mouseleave="paused = false"
    >
      <div class="index-scroll" :class="{ paused }">
        <div class="index-group">
          <div
            v-for="(item, i) in tickerItems"
            :key="i"
            class="ticker-item"
            :class="item.trend"
          >
            <span class="ticker-label">{{ item.label }}</span>
            <span class="ticker-value">{{ item.value }}</span>
            <span v-if="item.change" class="ticker-change" :class="item.trend">
              {{ item.trend === 'up' ? '↑' : item.trend === 'down' ? '↓' : '' }}
              {{ item.change }}
            </span>
            <span class="ticker-divider"></span>
          </div>
        </div>
        <div class="index-group">
          <div
            v-for="(item, i) in tickerItems"
            :key="'dup-' + i"
            class="ticker-item"
            :class="item.trend"
          >
            <span class="ticker-label">{{ item.label }}</span>
            <span class="ticker-value">{{ item.value }}</span>
            <span v-if="item.change" class="ticker-change" :class="item.trend">
              {{ item.trend === 'up' ? '↑' : item.trend === 'down' ? '↓' : '' }}
              {{ item.change }}
            </span>
            <span class="ticker-divider"></span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getMarketOverview, getStockIndices, type StockIndex } from '@/api/stock'

interface TickerItem {
  label: string
  value: string
  change: string
  trend: 'up' | 'down' | ''
}

const tickerItems = ref<TickerItem[]>([])
const paused = ref(false)
let timer: ReturnType<typeof setInterval> | null = null

function fmtPrice(v: number): string {
  return v?.toFixed(2) ?? '--'
}

function fmtPct(v: number): string {
  if (v == null) return '--'
  return (v >= 0 ? '+' : '') + v.toFixed(2) + '%'
}

function buildTicker(indices: StockIndex[], stats: Record<string, any>): TickerItem[] {
  const items: TickerItem[] = []

  // 20 个指数
  for (const idx of indices) {
    items.push({
      label: idx.name,
      value: fmtPrice(idx.price),
      change: fmtPct(idx.changePct),
      trend: idx.changePct >= 0 ? 'up' : 'down',
    })
  }

  // 4 个市场统计
  const up = stats?.upCount
  const down = stats?.downCount
  if (up != null && down != null) {
    items.push({
      label: '上涨家数',
      value: up.toLocaleString(),
      change: '',
      trend: 'up',
    })
    items.push({
      label: '下跌家数',
      value: down.toLocaleString(),
      change: '',
      trend: 'down',
    })
    const total = up + down
    const ratio = total > 0 ? (up / total * 100).toFixed(1) + '%' : '--'
    items.push({
      label: '涨跌比',
      value: up + ':' + down,
      change: ratio,
      trend: up >= down ? 'up' : 'down',
    })
    items.push({
      label: '涨跌差',
      value: (up - down >= 0 ? '+' : '') + (up - down).toLocaleString(),
      change: '',
      trend: up >= down ? 'up' : 'down',
    })
  }

  return items
}

async function fetchAll() {
  try {
    const [indices, overview] = await Promise.all([
      getStockIndices(),
      getMarketOverview(),
    ])
    if (Array.isArray(indices)) {
      tickerItems.value = buildTicker(indices, overview?.stats || {})
    }
  } catch {
    // silent fail
  }
}

onMounted(() => {
  fetchAll()
  timer = setInterval(fetchAll, 30000)
})

onUnmounted(() => {
  if (timer) { clearInterval(timer); timer = null }
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.index-bar {
  border-top: 1px solid $border-light;
  border-bottom: 1px solid $border-light;
  margin-bottom: $gap-md;
  overflow: hidden;
}

.index-track {
  width: 100%;
  overflow: hidden;
  padding: 10px 0;
}

.index-scroll {
  display: flex;
  width: fit-content;
  animation: ticker-scroll 60s linear infinite;

  &.paused {
    animation-play-state: paused;
  }
}

.index-group {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.ticker-item {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
  padding: 0 4px;

  .ticker-label {
    font-size: 12px;
    color: $text-sub;
    white-space: nowrap;
  }

  .ticker-value {
    font-size: 15px;
    font-weight: 700;
    color: $text-main;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;
  }

  .ticker-change {
    font-size: 12px;
    font-weight: 600;
    font-variant-numeric: tabular-nums;
    white-space: nowrap;

    &.up { color: #E83030; }
    &.down { color: #1CAD5E; }
  }

  &.up { .ticker-value { color: #E83030; } }
  &.down { .ticker-value { color: #1CAD5E; } }

  .ticker-divider {
    display: inline-block;
    width: 1px;
    height: 14px;
    background: $border;
    margin: 0 10px;
    flex-shrink: 0;
  }
}

@keyframes ticker-scroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

@media (max-width: 640px) {
  .ticker-value { font-size: 13px; }
  .ticker-label { font-size: 11px; }
  .ticker-change { font-size: 11px; }
}
</style>
