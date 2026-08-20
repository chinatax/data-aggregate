<template>
  <div class="home">
    <!-- Hero 区域 -->
    <section class="hero">
      <div class="hero-glow"></div>
      <div class="hero-content">
        <div class="hero-badge">
          <span>数据驱动 · 智能聚合</span>
        </div>
        <h1>
          <span class="hero-title-main">{{ siteStore.siteName }}</span>
        </h1>
        <p class="hero-desc">{{ siteStore.homeHeroDesc }}</p>
        <div class="hero-divider">
          <span></span>
        </div>
      </div>
      <svg class="hero-illust" viewBox="0 0 320 200" xmlns="http://www.w3.org/2000/svg">
        <rect x="20" y="40" width="80" height="120" rx="8" fill="#F5F5F5"/>
        <rect x="30" y="55" width="60" height="8" rx="4" fill="#E0E0E0"/>
        <rect x="30" y="72" width="40" height="6" rx="3" fill="#E0E0E0"/>
        <rect x="30" y="85" width="50" height="6" rx="3" fill="#E0E0E0"/>
        <rect x="30" y="98" width="35" height="6" rx="3" fill="#E0E0E0"/>
        <rect x="30" y="115" width="60" height="28" rx="4" fill="#E8E8E8"/>
        <circle cx="120" cy="100" r="55" fill="#FAFAFA" stroke="#EEEEEE" stroke-width="2"/>
        <polyline points="85,115 100,85 115,95 130,65 155,80" fill="none" stroke="#38BDF8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="130" cy="65" r="3.5" fill="#38BDF8"/>
        <rect x="190" y="25" width="110" height="150" rx="10" fill="#FAFAFA" stroke="#EEEEEE" stroke-width="1.5"/>
        <rect x="205" y="45" width="80" height="50" rx="6" fill="#F5F5F5"/>
        <path d="M220,75 L235,60 L250,68 L265,52 L280,62" fill="none" stroke="#38BDF8" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        <circle cx="265" cy="52" r="3" fill="#38BDF8"/>
        <rect x="205" y="108" width="55" height="8" rx="4" fill="#E8E8E8"/>
        <rect x="205" y="124" width="70" height="6" rx="3" fill="#E0E0E0"/>
        <rect x="205" y="138" width="45" height="6" rx="3" fill="#E0E0E0"/>
        <rect x="205" y="152" width="60" height="6" rx="3" fill="#E0E0E0"/>
        <circle cx="70" cy="165" r="4" fill="#38BDF8" opacity="0.6" class="svg-dot-pulse"/>
        <circle cx="170" cy="30" r="3" fill="#38BDF8" opacity="0.4" class="svg-dot-pulse svg-dot-delay"/>
        <circle cx="295" cy="180" r="5" fill="#38BDF8" opacity="0.3" class="svg-dot-pulse svg-dot-delay2"/>
      </svg>
    </section>

    <!-- 市场指数条 -->
    <IndexBar />

    <!-- 财经快讯 + 热点题材 双栏 -->
    <section class="section">
      <div class="section-title-bar">
        <h2>财经快讯</h2>
        <span class="title-hint">财联社 + 东财</span>
      </div>
      <div class="news-dual">
        <div class="news-left">
          <div class="timeline-list" v-if="pagedNews.length">
            <a v-for="item in pagedNews" :key="item.key" :href="item.link" target="_blank" rel="noopener" class="timeline-item">
              <span class="tl-dot"></span>
              <div class="tl-content">
                <span class="tl-time">{{ formatTime(item.time) }}</span>
                <span class="tl-text" :title="item.title">{{ item.title }}</span>
              </div>
            </a>
          </div>
          <div v-if="newsLoading" class="news-loading">加载中...</div>
        </div>
        <div class="news-right" ref="newsRightRef">
          <div class="market-head">
            <span>昨成交额</span>
            <div class="hot-pager" v-if="amountPageCount > 1">
              <button class="hot-arr" :disabled="amountPage <= 0" @click="amountPage = Math.max(0, amountPage - 1)">
                <svg viewBox="0 0 24 24" width="14" height="14"><path d="M15 18l-6-6 6-6" fill="none" stroke="currentColor" stroke-width="2"/></svg>
              </button>
              <span class="hot-page-num">{{ amountPage + 1 }}/{{ amountPageCount }}</span>
              <button class="hot-arr" :disabled="amountPage >= amountPageCount - 1" @click="amountPage = Math.min(amountPageCount - 1, amountPage + 1)">
                <svg viewBox="0 0 24 24" width="14" height="14"><path d="M9 18l6-6-6-6" fill="none" stroke="currentColor" stroke-width="2"/></svg>
              </button>
            </div>
          </div>
          <div class="hot-list">
            <div
              v-for="item in pagedAmountStocks"
              :key="item.code"
              class="hot-item"
            >
              <div class="hot-top">
                <StockKlinePopover :code="item.code" :name="(item.boardCount ?? 0) > 0 ? item.name + '（' + item.boardCount + '）' : item.name" class="hot-chip">
                  <template #detail>
                    <em>{{ item.code }}</em>
                    <b>{{ fmtAmount(item.amount) }}</b>
                    <span>
                      换手 {{ item.turnoverPct?.toFixed(2) ?? '-' }}%
                      量比 {{ item.volumeRatio?.toFixed(2) ?? '-' }}
                    </span>
                  </template>
                </StockKlinePopover>
                <span class="hot-pct">{{ fmtAmount(item.amount) }}</span>
              </div>
              <div class="hot-reason">市值{{ fmtMarketCap(item.floatMarketCap) }} · 换手{{ item.turnoverPct?.toFixed(2) ?? '—' }}% · 量比{{ item.volumeRatio?.toFixed(2) ?? '—' }} · {{ item.region || '—' }} · {{ item.industry || '—' }}</div>
            </div>
          </div>
          <div v-if="amountLoading" class="panel-loading">加载中...</div>
        </div>
      </div>
    </section>

    <!-- 市场资金 -->
    <section class="section s-fund">
      <div class="section-title-bar">
        <h2>市场资金</h2>
        <span class="title-hint">数据来源：核新数据 / 东方财富</span>
      </div>
      <div class="fund-grid">
        <div class="fund-card north">
          <div class="fund-label">北向资金净流入</div>
          <div class="fund-num" :class="northboundTotal >= 0 ? 'up' : 'down'">
            {{ fmtYi(northbound?.total) }}
          </div>
          <div class="fund-sub">
            <span>沪股通 {{ fmtYi(northbound?.hgt) }}</span>
            <span>深股通 {{ fmtYi(northbound?.sgt) }}</span>
          </div>
          <div class="fund-date" v-if="northbound?.tradeDate">
            日期 {{ northbound.tradeDate }}
          </div>
          <div class="fund-time" v-if="northbound?.updateTime">
            更新于 {{ northbound.updateTime }}
          </div>
        </div>
        <div class="fund-chart" v-if="marginHistory.length >= 2">
          <div class="chart-legend">
            <span class="legend-rzye">融资余额</span>
            <span class="legend-rqye">融券余额</span>
          </div>
          <svg :viewBox="'0 0 ' + chartW + ' ' + chartH" class="margin-chart-svg">
            <!-- Y轴网格线 + 标签 -->
            <g v-for="(v, i) in chartYGrid" :key="'gl'+i">
              <line :x1="chartPadL" :y1="v.y" :x2="chartW - chartPadR" :y2="v.y"
                stroke="#eee" stroke-width="0.5" />
              <text :x="chartPadL - 4" :y="v.y + 4"
                text-anchor="end" font-size="9" fill="#999">{{ v.label }}</text>
            </g>
            <!-- 融资余额折线 -->
            <polyline :points="chartRzyePoints" fill="none" stroke="#3B82F6" stroke-width="2" stroke-linejoin="round" />
            <!-- 融券余额折线 -->
            <polyline :points="chartRqyePoints" fill="none" stroke="#F59E0B" stroke-width="2" stroke-linejoin="round" />
            <!-- X轴标签 -->
            <text v-for="(d, i) in chartXLabels" :key="'xl'+i"
              :x="chartXPos(d.idx)" :y="chartH - 4"
              text-anchor="middle" font-size="10" fill="#999">{{ d.text }}</text>
          </svg>
        </div>
        <div v-else-if="fundLoaded" class="fund-chart fund-chart-empty">
          <span class="empty-hint">加载中...</span>
        </div>
        <div v-else class="fund-chart fund-chart-empty fund-placeholder"></div>
      </div>
    </section>

    <!-- 龙虎榜 -->
    <section class="section s-dragon">
      <div class="section-title-bar">
        <h2>龙虎榜</h2>
        <span class="title-hint">{{ dragonBoardDate || '净买额排名' }}</span>
      </div>
      <div class="dragon-table-wrap">
        <table class="dragon-table" v-if="dragonBoard.length">
          <thead>
            <tr>
              <th>代码</th><th>名称</th><th>上榜原因</th>
              <th class="num">收盘价</th><th class="num">涨跌幅</th>
              <th class="num">净买额(万)</th><th class="num">换手率</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in paginatedDragonBoard" :key="row.code">
              <td class="code">{{ row.code }}</td>
              <td><StockKlinePopover :code="row.code" :name="row.name" class="dragon-name-pop" /></td>
              <td class="reason">{{ row.reason }}</td>
              <td class="num">{{ row.closePrice?.toFixed(2) }}</td>
              <td class="num" :class="row.changePct >= 0 ? 'up' : 'down'">
                {{ formatPct(row.changePct) }}
              </td>
              <td class="num" :class="row.netBuy >= 0 ? 'up' : 'down'">
                {{ fmtWan(row.netBuy) }}
              </td>
              <td class="num">{{ row.turnoverPct?.toFixed(2) }}%</td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="dragonLoaded && !dragonLoading" class="empty-hint">暂无龙虎榜数据（非交易日或无数据）</div>
        <div v-else-if="!dragonLoaded" class="empty-hint" style="min-height: 120px"></div>
      </div>

      <!-- 分页箭头 -->
      <div class="dragon-pager" v-if="dragonBoard.length > dragonPageSize">
        <button
          class="pager-arrow"
          :disabled="dragonPage <= 1"
          @click="dragonPage = Math.max(1, dragonPage - 1)"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="pager-info">第 {{ dragonPage }} / {{ dragonPageCount }} 页</span>
        <button
          class="pager-arrow"
          :disabled="dragonPage >= dragonPageCount"
          @click="dragonPage = Math.min(dragonPageCount, dragonPage + 1)"
        >
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
      <div v-if="dragonLoading" class="panel-loading">加载中...</div>
    </section>

    <!-- 连板天梯 -->
    <section class="section s-board-ladder">
      <div class="section-title-bar">
        <h2>连板天梯</h2>
        <span class="title-hint">{{ boardLadder?.tradeDate || '' }}</span>
      </div>

      <!-- Tab 切换 -->
      <div class="board-tabs">
        <button
          v-for="tab in boardTabs"
          :key="tab.key"
          :class="['board-tab', { active: activeTab === tab.key }]"
          @click="activeTab = tab.key"
        >
          <span class="tab-label">{{ tab.label }}</span>
          <span class="tab-badge">{{ tab.count }}</span>
        </button>
      </div>

      <!-- 首板：紧凑标签云（hover 自适应方向弹出详情） -->
      <div class="board-chip-cloud" v-if="activeTab === 'first' && currentBoardList.length">
        <span
          v-for="item in currentBoardList"
          :key="item.code"
          class="board-chip-wrapper"
        >
          <StockKlinePopover :code="item.code" :name="item.name" class="board-chip">
            <template #detail>
              <em>{{ item.code }}</em>
              <b :class="item.changePct >= 0 ? 'up' : 'down'">{{ formatPct(item.changePct) }}</b>
              <span>收盘 {{ (item.closePrice || 0).toFixed(2) }} 元 · 换手 {{ (item.turnover || 0).toFixed(2) }}%</span>
              <span class="tip-reason" v-if="item.reason">{{ item.reason }}</span>
            </template>
          </StockKlinePopover>
        </span>
      </div>

      <!-- 二连板 / 三板以上：卡片（数量少，卡片直观） -->
      <div class="board-grid" v-if="activeTab !== 'first' && currentBoardList.length">
        <div
          v-for="item in currentBoardList"
          :key="item.code"
          :class="['board-card', 'board-level-' + Math.min(item.boardCount, 3)]"
        >
          <div class="board-card-header">
            <StockKlinePopover :code="item.code" :name="item.name" class="board-name-pop" />
            <span class="board-badge" v-if="item.boardCount >= 2">{{ item.boardCount }}板</span>
          </div>
          <div class="board-code">{{ item.code }}</div>
          <div class="board-pct" :class="item.changePct >= 0 ? 'up' : 'down'">
            {{ formatPct(item.changePct) }}
          </div>
          <div class="board-reason" :title="item.reason">{{ item.reason || '—' }}</div>
          <div class="board-meta">
            <span>换手 {{ (item.turnover || 0).toFixed(2) }}%</span>
            <span>收盘 {{ (item.closePrice || 0).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <div v-else class="empty-hint board-placeholder"></div>
    </section>

    <!-- 筹码集中度 -->
    <section class="section s-holder">
      <div class="section-title-bar">
        <h2>筹码集中度</h2>
        <span class="title-hint">股东户数环比减少 · 来源：东方财富</span>
      </div>
      <div class="dragon-table-wrap">
        <table class="dragon-table" v-if="pagedHolderNum.length">
          <thead>
            <tr>
              <th>代码</th><th>名称</th><th>行业</th><th>地区</th>
              <th class="num">股东户数</th><th class="num">环比变化</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in pagedHolderNum" :key="row.code">
              <td class="code">{{ row.code }}</td>
              <td><StockKlinePopover :code="row.code" :name="row.name" class="dragon-name-pop" /></td>
              <td>{{ row.industry || '—' }}</td>
              <td>{{ row.region || '—' }}</td>
              <td class="num">{{ fmtHolderNum(row.holderNum) }}</td>
              <td class="num" :class="row.changeRatio < 0 ? 'up' : 'down'">{{ formatPct(row.changeRatio) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-else-if="holderLoaded && !holderLoading" class="empty-hint">暂无筹码集中度数据</div>
        <div v-else-if="!holderLoaded" class="empty-hint" style="min-height: 80px"></div>
      </div>

      <div class="dragon-pager" v-if="holderNum.length > holderPageSize">
        <button class="pager-arrow" :disabled="holderPage <= 1" @click="holderPage = Math.max(1, holderPage - 1)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="pager-info">第 {{ holderPage }} / {{ holderPageCount }} 页</span>
        <button class="pager-arrow" :disabled="holderPage >= holderPageCount" @click="holderPage = Math.min(holderPageCount, holderPage + 1)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
      <div v-if="holderLoading" class="panel-loading">加载中...</div>
    </section>

    <!-- 十大流通股东（机构聚合） -->
    <section class="section s-holder-top10">
      <div class="section-title-bar">
        <h2>十大流通股东</h2>
        <span class="title-hint" v-if="holderTop10Date">报告期 {{ holderTop10Date }} · 机构持仓 · 来源：东方财富</span>
      </div>
      <div class="inst-holder-list" v-if="pagedInstitutions.length">
        <div
          v-for="inst in pagedInstitutions"
          :key="inst.name"
          class="inst-card"
        >
          <div class="inst-header">
            <span class="inst-name">{{ inst.name }}</span>
            <span class="inst-count">持仓 {{ inst.holdings.length }} 只</span>
          </div>
          <div class="inst-holdings">
            <StockKlinePopover
              v-for="h in inst.holdings"
              :key="h.code"
              :code="h.code"
              :name="h.stockName"
              class="inst-stock-chip"
            >
              <template #detail>
                <em>{{ h.code }}</em>
                <b>{{ h.ratio?.toFixed(2) ?? '-' }}%</b>
                <span>{{ h.change || '—' }}</span>
              </template>
            </StockKlinePopover>
          </div>
        </div>
      </div>
      <div v-else-if="holderTop10Loaded && !holderTop10Loading" class="empty-hint">
        暂无机构持仓数据
      </div>
      <div v-else-if="!holderTop10Loaded" class="empty-hint" style="min-height: 80px"></div>

      <div class="dragon-pager" v-if="institutionsPageCount > 1">
        <button class="pager-arrow" :disabled="ht10Page <= 1" @click="ht10Page = Math.max(1, ht10Page - 1)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </button>
        <span class="pager-info">第 {{ ht10Page }} / {{ institutionsPageCount }} 页</span>
        <button class="pager-arrow" :disabled="ht10Page >= institutionsPageCount" @click="ht10Page = Math.min(institutionsPageCount, ht10Page + 1)">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
        </button>
      </div>
      <div v-if="holderTop10Loading" class="panel-loading">加载中...</div>
    </section>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'HomePage' })
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSiteStore } from '@/stores/site'
import {
  getMarketOverview,
  getNorthboundFlow,
  getMarginHistory,
  getDragonBoard,
  getGlobalNews,
  getBoardLadder,
  getDailyDisplay,
  type StockNewsFlash,
  type StockHotTopic,
  type StockDailyAmount,
  type NorthboundFlow,
  type MarginHistoryItem,
  type DragonBoardItem,
  type GlobalNewsItem,
  type BoardLadder,
  getHolderNum,
  type HolderNumItem,
  getHolderTop10,
  type HolderTop10Item,
} from '@/api/stock'
import IndexBar from '@/components/IndexBar.vue'
import StockKlinePopover from '@/components/StockKlinePopover.vue'

const siteStore = useSiteStore()

const newsFlashes = ref<StockNewsFlash[]>([])
const hotTopics = ref<StockHotTopic[]>([])
const newsLoading = ref(false)
const hotLoading = ref(false)

const amountStocks = ref<StockDailyAmount[]>([])
const amountPage = ref(0)
const amountPerPage = ref(8)
const amountTopN = 200
const amountLoading = ref(false)

const northbound = ref<NorthboundFlow | null>(null)
const marginHistory = ref<MarginHistoryItem[]>([])
const dragonBoard = ref<DragonBoardItem[]>([])
const globalNews = ref<GlobalNewsItem[]>([])
const dragonLoading = ref(false)
const globalNewsLoading = ref(false)
const boardLadder = ref<BoardLadder | null>(null)
const activeTab = ref('third')
const dragonPage = ref(1)
const dragonPageSize = 10


// 各区块是否已触发过加载
const fundLoaded = ref(false)
const dragonLoaded = ref(false)
const boardLadderLoaded = ref(false)
const globalNewsLoaded = ref(false)
const holderNum = ref<HolderNumItem[]>([])
const holderLoading = ref(false)
const holderLoaded = ref(false)
const holderPage = ref(1)
const holderPageSize = 10

const holderTop10 = ref<HolderTop10Item[]>([])
const holderTop10Loading = ref(false)
const holderTop10Loaded = ref(false)
const ht10Page = ref(1)
const ht10PageSize = 10

function normalizeTitle(t: string): string {
  return t.replace(/[【】「」〔〕［］《》〈〉""'',，。、：:；;！!？?\s​-‍﻿]+/g, '').trim()
}

function bigramJaccard(a: string, b: string): number {
  if (a.length < 2 || b.length < 2) return 0
  const setA = new Set<string>()
  const setB = new Set<string>()
  for (let i = 0; i < a.length - 1; i++) setA.add(a.slice(i, i + 2))
  for (let i = 0; i < b.length - 1; i++) setB.add(b.slice(i, i + 2))
  let intersect = 0
  setA.forEach(bg => { if (setB.has(bg)) intersect++ })
  const union = setA.size + setB.size - intersect
  return union === 0 ? 0 : intersect / union
}

function dedupNews(items: Array<{ title: string; time: number; source: string; link: string; key: string }>) {
  const kept: typeof items = []
  for (const item of items) {
    const norm = normalizeTitle(item.title)
    if (!norm) { kept.push(item); continue }
    const dup = kept.find(k => {
      const knorm = normalizeTitle(k.title)
      if (!knorm) return false
      // exact match after normalize
      if (norm === knorm) return true
      // one contains the other
      if (norm.includes(knorm) || knorm.includes(norm)) return true
      // bigram similarity
      if (norm.length >= 4 && knorm.length >= 4) {
        return bigramJaccard(norm, knorm) > 0.5
      }
      return false
    })
    if (!dup) kept.push(item)
  }
  return kept
}

const unifiedNews = computed(() => {
  const items: Array<{ title: string; time: number; source: string; link: string; key: string }> = []
  newsFlashes.value.forEach(nf => {
    items.push({
      title: nf.title,
      time: nf.publishTime,
      source: '财联社',
      link: `https://www.cls.cn/detail/${nf.id}`,
      key: `cls-${nf.id}`
    })
  })
  globalNews.value.forEach(gn => {
    const ts = new Date(gn.showTime).getTime() / 1000
    if (!isNaN(ts)) {
      items.push({
        title: gn.title,
        time: ts,
        source: '东财',
        link: `https://finance.eastmoney.com/a/${gn.newsCode}.html`,
        key: `em-${gn.newsCode}`
      })
    }
  })
  items.sort((a, b) => b.time - a.time)
  return dedupNews(items)
})

const newsPerPage = 12
const newsPage = ref(0)
let newsTimer: ReturnType<typeof setInterval> | null = null

const pagedNews = computed(() => {
  const start = newsPage.value * newsPerPage
  return unifiedNews.value.slice(start, start + newsPerPage)
})

const newsPageCount = computed(() => Math.max(1, Math.ceil(unifiedNews.value.length / newsPerPage)))

function startNewsRotate() {
  stopNewsRotate()
  if (newsPageCount.value <= 1) return
  newsTimer = setInterval(() => {
    newsPage.value = (newsPage.value + 1) % newsPageCount.value
  }, 10000)
}

function stopNewsRotate() {
  if (newsTimer) { clearInterval(newsTimer); newsTimer = null }
}

watch(unifiedNews, () => {
  newsPage.value = 0
  setTimeout(startNewsRotate, 100)
})


const rankedAmountStocks = computed(() =>
  [...amountStocks.value]
    .sort((a, b) => (b.amount ?? 0) - (a.amount ?? 0))
    .slice(0, amountTopN)
)

const pagedAmountStocks = computed(() => {
  const start = amountPage.value * amountPerPage.value
  return rankedAmountStocks.value.slice(start, start + amountPerPage.value)
})

const amountPageCount = computed(() =>
  Math.max(1, Math.ceil(rankedAmountStocks.value.length / amountPerPage.value))
)

function fmtAmount(v: number | null | undefined): string {
  if (v == null) return '-'
  if (v >= 10000) return (v / 10000).toFixed(2) + '亿'
  return v.toFixed(0) + '万'
}

function fmtMarketCap(v: number | null | undefined): string {
  if (v == null || v <= 0) return '—亿'
  return v.toFixed(0) + '亿'
}

watch(amountStocks, () => {
  amountPage.value = 0
})

const dragonPageCount = computed(() => Math.max(1, Math.ceil(dragonBoard.value.length / dragonPageSize)))
const paginatedDragonBoard = computed(() => {
  const start = (dragonPage.value - 1) * dragonPageSize
  return dragonBoard.value.slice(start, start + dragonPageSize)
})

const holderPageCount = computed(() => Math.max(1, Math.ceil(holderNum.value.length / holderPageSize)))
const pagedHolderNum = computed(() => {
  const start = (holderPage.value - 1) * holderPageSize
  return holderNum.value.slice(start, start + holderPageSize)
})

function fmtHolderNum(v: number | undefined | null): string {
  if (v == null) return '—'
  if (v >= 10000) return (v / 10000).toFixed(1) + '万'
  return v.toLocaleString()
}

async function fetchHolderNum() {
  holderLoaded.value = true
  holderLoading.value = true
  try {
    const data = await getHolderNum()
    if (Array.isArray(data)) { holderNum.value = data; holderPage.value = 1 }
  } catch { /* silent */ }
  holderLoading.value = false
}

const boardTabs = computed(() => {
  const bl = boardLadder.value
  return [
    { key: 'third', label: '三板以上', count: bl?.thirdPlusBoard?.length ?? 0 },
    { key: 'second', label: '二连板', count: bl?.secondBoard?.length ?? 0 },
    { key: 'first', label: '首板', count: bl?.firstBoard?.length ?? 0 },
  ]
})

const currentBoardList = computed(() => {
  const bl = boardLadder.value
  if (!bl) return []
  switch (activeTab.value) {
    case 'first': return bl.firstBoard || []
    case 'second': return bl.secondBoard || []
    case 'third': return bl.thirdPlusBoard || []
    default: return []
  }
})

const dragonBoardDate = computed(() => {
  const first = dragonBoard.value[0]
  return first?.tradeDate || ''
})

const northboundTotal = computed(() => northbound.value?.total ?? 0)

function fmtYi(v: number | undefined | null): string {
  if (v == null) return '--'
  return v.toFixed(2) + ' 亿'
}

function fmtWan(v: number | undefined | null): string {
  if (v == null) return '--'
  return (v / 10000).toFixed(0)
}

function formatTime(ts: number): string {
  if (!ts) return ''
  const d = new Date(ts * 1000)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return hh + ':' + mm
}

function formatPct(v: number): string {
  if (v == null) return '--'
  return (v >= 0 ? '+' : '') + v.toFixed(2) + '%'
}

// ===== 十大流通股东（机构聚合） =====
const INST_KEYWORDS = [
  '基金','私募','公募','社保','汇金','证金','保险','信托','银行',
  '证券','投资','资产','资本','QFII','年金','养老金','大基金',
  '创投','股权','控股','集团','有限合伙','回购','专用',
  '香港中央结算','中央汇金','中国证券','全国社保','资产管理',
  '产业投资基金','创业投资',
]

function isInstitution(name: string): boolean {
  return INST_KEYWORDS.some(kw => name.includes(kw))
}

const holderTop10Date = computed(() => {
  if (!holderTop10.value.length) return ''
  return holderTop10.value[0].endDate || ''
})

interface InstHolding {
  code: string
  stockName: string
  ratio: number
  change: string
}

interface InstitutionGroup {
  name: string
  holdings: InstHolding[]
}

const institutions = computed(() => {
  const map = new Map<string, Map<string, InstHolding>>() // name → code → holding
  for (const item of holderTop10.value) {
    if (!isInstitution(item.holderName)) continue
    if (!map.has(item.holderName)) {
      map.set(item.holderName, new Map())
    }
    const codeMap = map.get(item.holderName)!
    // 同一机构+股票只保留比例最高的一条（去重不同报告期）
    const existing = codeMap.get(item.code)
    if (!existing || (item.holdRatio ?? 0) > (existing.ratio ?? 0)) {
      codeMap.set(item.code, {
        code: item.code,
        stockName: item.name,
        ratio: item.holdRatio,
        change: item.holdChange,
      })
    }
  }
  // 转为 InstitutionGroup[]，按持仓数量降序
  const result: InstitutionGroup[] = []
  for (const [name, codeMap] of map) {
    const holdings = [...codeMap.values()]
    holdings.sort((a, b) => (b.ratio ?? 0) - (a.ratio ?? 0))
    result.push({ name, holdings })
  }
  result.sort((a, b) => b.holdings.length - a.holdings.length)
  return result
})

const institutionsPageCount = computed(() =>
  Math.max(1, Math.ceil(institutions.value.length / ht10PageSize))
)

const pagedInstitutions = computed(() => {
  const start = (ht10Page.value - 1) * ht10PageSize
  return institutions.value.slice(start, start + ht10PageSize)
})

async function fetchHolderTop10() {
  holderTop10Loaded.value = true
  holderTop10Loading.value = true
  try {
    const data = await getHolderTop10()
    if (Array.isArray(data)) { holderTop10.value = data; ht10Page.value = 1 }
  } catch { /* silent */ }
  holderTop10Loading.value = false
}

// ===== 图表常量 =====
const chartW = 600
const chartH = 200
const chartPadL = 42
const chartPadR = 42
const chartPadT = 12
const chartPadB = 22

function chartXPos(i: number): number {
  const n = marginHistory.value.length - 1
  if (n <= 0) return chartPadL
  return chartPadL + (i / n) * (chartW - chartPadL - chartPadR)
}

function yPos(v: number, min: number, max: number): number {
  if (max === min) return chartH - chartPadB - (chartH - chartPadT - chartPadB) / 2
  const ratio = (v - min) / (max - min)
  return chartH - chartPadB - ratio * (chartH - chartPadT - chartPadB)
}

const chartRzyeRange = computed(() => {
  const vals = marginHistory.value.map(d => d.rzye)
  if (!vals.length) return { min: 0, max: 1 }
  const min = Math.min(...vals)
  const max = Math.max(...vals)
  const pad = (max - min) * 0.1 || 100
  return { min: Math.floor(min - pad), max: Math.ceil(max + pad) }
})

const chartRqyeRange = computed(() => {
  const vals = marginHistory.value.map(d => d.rqye)
  if (!vals.length) return { min: 0, max: 1 }
  const min = Math.min(...vals)
  const max = Math.max(...vals)
  const pad = (max - min) * 0.1 || 10
  return { min: Math.floor(min - pad), max: Math.ceil(max + pad) }
})

const chartYGrid = computed(() => {
  const r = chartRzyeRange.value
  const step = (r.max - r.min) / 4
  return [0, 1, 2, 3, 4].map(i => {
    const v = r.min + step * i
    return {
      v,
      y: yPos(v, r.min, r.max),
      label: v.toFixed(0) + '亿'
    }
  })
})

const chartRzyePoints = computed(() => {
  const r = chartRzyeRange.value
  return marginHistory.value.map((d, i) =>
    chartXPos(i) + ',' + yPos(d.rzye, r.min, r.max)
  ).join(' ')
})

const chartRqyePoints = computed(() => {
  const r = chartRqyeRange.value
  return marginHistory.value.map((d, i) =>
    chartXPos(i) + ',' + yPos(d.rqye, r.min, r.max)
  ).join(' ')
})

const chartXLabels = computed(() => {
  const data = marginHistory.value
  if (!data.length) return []
  const labels: { idx: number; text: string }[] = []
  const step = Math.max(1, Math.floor(data.length / 5))
  for (let i = 0; i < data.length; i += step) {
    labels.push({ idx: i, text: data[i].tradeDate.slice(5) })
  }
  // 确保最后一个点有标签
  const last = data.length - 1
  if (labels.length === 0 || labels[labels.length - 1].idx !== last) {
    labels.push({ idx: last, text: data[last].tradeDate.slice(5) })
  }
  return labels
})

async function fetchAll() {
  newsLoading.value = true
  hotLoading.value = true
  try {
    const overview = await getMarketOverview()
    if (overview) {
      if (overview.newsFlashes) newsFlashes.value = overview.newsFlashes
      if (overview.hotTopics) hotTopics.value = overview.hotTopics
    }
  } catch {
    // silent fail
  } finally {
    newsLoading.value = false
    hotLoading.value = false
  }
  fetchDailyAmounts()
}

async function fetchDailyAmounts() {
  amountLoading.value = true
  try {
    const data = await getDailyDisplay()
    if (Array.isArray(data)) {
      amountStocks.value = data
    }
  } catch {
    // silent fail
  } finally {
    amountLoading.value = false
  }
}

async function fetchFundData() {
  fundLoaded.value = true
  try {
    const nb = await getNorthboundFlow()
    if (nb) northbound.value = nb
  } catch { /* silent */ }
  try {
    const h = await getMarginHistory()
    if (Array.isArray(h) && h.length) marginHistory.value = h.reverse()
  } catch { /* silent */ }
}

async function fetchDragonBoard() {
  dragonLoaded.value = true
  dragonLoading.value = true
  try {
    const data = await getDragonBoard()
    if (Array.isArray(data)) { dragonBoard.value = data; dragonPage.value = 1 }
  } catch { /* silent */ }
  dragonLoading.value = false
}

async function fetchGlobalNewsOnly() {
  globalNewsLoaded.value = true
  globalNewsLoading.value = true
  try {
    const data = await getGlobalNews()
    if (Array.isArray(data)) globalNews.value = data
  } catch { /* silent */ }
  globalNewsLoading.value = false
}

async function fetchBoardLadderData() {
  boardLadderLoaded.value = true
  try {
    const data = await getBoardLadder()
    if (data) boardLadder.value = data
  } catch { /* silent */ }
}

function lazyLoad(selector: string, loadFn: () => void) {
  const el = document.querySelector(selector)
  if (!el) { loadFn(); return }
  const io = new IntersectionObserver((entries) => {
    if (entries[0].isIntersecting) {
      loadFn()
      io.disconnect()
    }
  }, { rootMargin: '300px' })
  io.observe(el)
}

onMounted(() => {
  siteStore.fetchConfig()
  fetchAll()
  fetchGlobalNewsOnly()
  // 滚动到附近才加载，减少首屏请求
  lazyLoad('.s-fund', fetchFundData)
  lazyLoad('.s-dragon', fetchDragonBoard)
  lazyLoad('.s-board-ladder', fetchBoardLadderData)
  lazyLoad('.s-holder', fetchHolderNum)
  lazyLoad('.s-holder-top10', fetchHolderTop10)
  setTimeout(startNewsRotate, 1000)

  // Scroll reveal — 入场渐显动画
  requestAnimationFrame(() => {
    document.querySelectorAll('.home .section').forEach(el => {
      el.classList.add('reveal-pending')
    })
    const revealIO = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('revealed')
          revealIO.unobserve(entry.target)
        }
      })
    }, { threshold: 0.05 })
    document.querySelectorAll('.home .section.reveal-pending').forEach(el => {
      revealIO.observe(el)
    })
  })
})

onBeforeUnmount(() => {
  stopNewsRotate()
})
</script>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.home {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 $gap-lg $gap-xl;
}

// ===== Hero =====
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: $gap-sm 0 $gap-md;
  margin-bottom: 0;
  gap: $gap-xl;
}

.hero-content {
  flex: 1;
  min-width: 0;

  .hero-badge {
    display: inline-block;
    padding: 4px 14px;
    border: 1px solid #38BDF8;
    border-radius: 100px;
    margin-bottom: $gap-md;

    span {
      font-size: 13px;
      color: #38BDF8;
      font-weight: 500;
    }
  }

  h1 {
    font-size: 42px;
    font-weight: 700;
    line-height: 1.2;
    margin-bottom: $gap-md;
    letter-spacing: -0.5px;

    .hero-title-main {
      color: #38BDF8;
    }
  }

  .hero-desc {
    font-size: 16px;
    color: $text-sub;
    max-width: 480px;
    line-height: 1.7;
    margin-bottom: $gap-lg;
  }

  .hero-divider {
    span {
      display: block;
      width: 48px;
      height: 3px;
      background: #38BDF8;
      border-radius: 2px;
    }
  }
}

.hero-illust {
  width: 320px;
  height: 200px;
  flex-shrink: 0;
}

// ===== 区块标题 =====
.section {
  margin-bottom: $gap-md;
}

.section-title-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: $gap-sm;

  h2 {
    font-size: 20px;
    font-weight: 600;
    color: $text-main;
    padding-left: $gap-sm;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 2px;
      bottom: 2px;
      width: 4px;
      background: $border-dark;
      border-radius: 2px;
    }
  }

  .title-hint {
    font-size: 12px;
    color: $text-muted;
  }
}

// ===== 双栏 =====
.news-dual {
  display: grid;
  grid-template-columns: 6fr 4fr;
  gap: $gap-lg;
}

.news-left {
  border-top: 2px solid $border;
  border-bottom: 1px solid $border;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 7px;
    top: 12px;
    bottom: 12px;
    width: 1.5px;
    background: #E5E7EB;
  }
}

.timeline-item {
  position: relative;
  display: block;
  padding: 6px 0 8px 26px;
  text-decoration: none;
  color: inherit;
  border-bottom: 1px dotted $border-light;
  transition: background $ease;
  overflow: hidden;

  &:last-child { border-bottom: none; }

  &:hover {
    .tl-text { color: #38BDF8; }
    .tl-dot { border-color: #0EA5E9; background: #E0F2FE; }
  }
}

.tl-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid #38BDF8;
  background: $white;
  transition: all 0.2s ease;
  z-index: 1;
  position: absolute;
  left: 2px;
  top: 8px;

}

.tl-content {
  display: flex;
  margin-top: 8px;
}

.tl-time {
  font-size: 11px;
  color: $text-muted;
  font-variant-numeric: tabular-nums;
  width: 38px;
  flex-shrink: 0;
}

.tl-text {
  flex: 1;
  font-size: 14px;
  color: $text-main;
  line-height: 1.6;
  transition: color $ease;
  min-width: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-loading {
  text-align: center;
  padding: $gap-md 0;
  font-size: 13px;
  color: $text-muted;
}

.news-right {
  border-top: 2px solid $border;
  border-bottom: 1px solid $border;
}

.market-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  color: $text-main;
  padding: 9px 0 6px;
  border-bottom: 1px solid $border;

  .hot-date {
    font-size: 12px;
    font-weight: 400;
    color: $text-muted;
  }
}

.hot-list {
  display: flex;
  flex-direction: column;
}

.hot-item {
  padding: 10px 0;
  border-bottom: 1px dotted $border-light;

  &:last-child { border-bottom: none; }
}

.hot-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 3px;
}

.hot-chip {
  font-size: 13px;
  font-weight: 500;
  color: $text-body;
  cursor: default;
}

.hot-pct {
  font-size: 13px;
  font-weight: 600;

  &.up { color: #E83030; }
  &.down { color: #1CAD5E; }
}

.hot-pager {
  display: flex;
  align-items: center;
  gap: 6px;
}

.hot-arr {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: 1px solid $border;
  border-radius: $radius-sm;
  background: $white;
  color: $text-sub;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover:not(:disabled) {
    border-color: #E83030;
    color: #E83030;
    background: #FEF2F2;
  }

  &:disabled {
    opacity: 0.25;
    cursor: not-allowed;
  }
}

.hot-page-num {
  font-size: 11px;
  font-weight: 500;
  color: $text-muted;
  font-variant-numeric: tabular-nums;
  min-width: 36px;
  text-align: center;
}

.hot-reason {
  font-size: 12px;
  color: $text-sub;
  line-height: 1.4;
}

.panel-loading {
  text-align: center;
  padding: $gap-md 0;
  font-size: 13px;
  color: $text-muted;
}

// ===== 市场资金 =====
.fund-grid {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: $gap-md;
}

.fund-card {
  background: $white;
  border: 1px solid $border;
  border-radius: $radius-lg;
  padding: $gap-md $gap-lg;
  text-align: center;
  transition: all $ease;

  .fund-label { font-size: 14px; color: $text-sub; margin-bottom: 8px; }
  .fund-num { font-size: 30px; font-weight: 700; margin-bottom: 6px; letter-spacing: -0.5px; }
  .fund-sub { font-size: 12px; color: $text-muted; display: flex; justify-content: center; gap: 14px; }
  .fund-date { font-size: 12px; color: $text-sub; margin-top: 6px; }
  .fund-time { font-size: 11px; color: $text-muted; margin-top: 2px; }

  .up { color: #E83030; }
  .down { color: #1CAD5E; }

  &.north:hover { border-color: #FCD34D; background: #FFFBEB; }
}

.fund-chart {
  background: $white;
  border: 1px solid $border;
  border-radius: $radius-lg;
  padding: 8px $gap-md $gap-sm;
  display: flex;
  flex-direction: column;

  .chart-legend {
    display: flex;
    gap: 16px;
    margin-bottom: 4px;
    font-size: 12px;
    font-weight: 600;

    .legend-rzye { color: #3B82F6; }
    .legend-rqye { color: #F59E0B; }
  }

  .margin-chart-svg {
    width: 100%;
    height: auto;
  }
}

.fund-chart-empty {
  align-items: center;
  justify-content: center;
  min-height: 120px;
}

// ===== 龙虎榜表格 =====
.dragon-table-wrap { overflow-x: auto; }

.dragon-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;

  th, td { padding: 8px 12px; text-align: left; white-space: nowrap; }
  th { font-weight: 600; color: $text-sub; border-bottom: 2px solid $border; }
  td { border-bottom: 1px solid $border-light; }
  .num { text-align: right; font-variant-numeric: tabular-nums; }
  .code { font-family: monospace; }
  .reason { max-width: 160px; overflow: hidden; text-overflow: ellipsis; }
  .up { color: #E83030; }
  .down { color: #1CAD5E; }
  tbody tr:hover { background: $bg-hover; }
}

.dragon-pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  margin-top: 12px;
  padding: 8px 0;

  .pager-arrow {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border: 1px solid $border;
    border-radius: $radius;
    background: $white;
    color: $text-sub;
    cursor: pointer;
    transition: all 0.15s ease;

    &:hover:not(:disabled) {
      border-color: #38BDF8;
      color: #38BDF8;
      background: #F0F9FF;
    }

    &:disabled {
      opacity: 0.3;
      cursor: not-allowed;
    }
  }

  .pager-info {
    font-size: 13px;
    color: $text-sub;
    font-variant-numeric: tabular-nums;
    min-width: 100px;
    text-align: center;
  }
}

// ===== 连板天梯 =====
.s-holder {
  margin-top: $gap-2xl;
}

.s-board-ladder {
  margin-bottom: 0;

  .board-tabs {
    display: flex;
    gap: 4px;
    margin-bottom: $gap-md;
    background: $bg-section;
    border-radius: $radius;
    padding: 4px;
  }

  .board-tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 16px;
    border: none;
    border-radius: $radius-sm;
    background: transparent;
    color: $text-sub;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;

    &:hover { color: $text-main; background: #F3F4F6; }

    &.active {
      color: #E83030;
      background: #FEF2F2;
      box-shadow: 0 1px 3px rgba(232, 48, 48, 0.1);

      .tab-badge {
        background: #E83030;
        color: #fff;
      }
    }

    .tab-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 22px;
      height: 20px;
      padding: 0 6px;
      border-radius: 10px;
      font-size: 12px;
      font-weight: 600;
      background: #E5E7EB;
      color: $text-muted;
      transition: all 0.2s ease;
    }
  }

  // 首板标签云（每行最多15只）
  .board-chip-cloud {
    display: flex;
    flex-wrap: wrap;
    gap: 2px 6px;
    padding: 4px 0;
  }

  .board-chip {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    color: $text-body;
    cursor: default;
  }

  // Wrapper for board chips to handle spacing
  .board-chip-wrapper {
    display: inline-flex;
  }

  .board-grid {
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: $gap-sm;
  }

  .board-card {
    background: $white;
    border: 1px solid $border;
    border-radius: $radius;
    padding: $gap-sm $gap-md;
    transition: all 0.2s ease;
    cursor: default;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.06);
    }

    &.board-level-2 {
      border-left: 3px solid #F59E0B;
    }
    &.board-level-3 {
      border-left: 3px solid #E83030;
    }

    .board-card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 2px;

      .board-badge {
        font-size: 11px;
        font-weight: 700;
        padding: 2px 6px;
        border-radius: 4px;
        background: #FEF3C7;
        color: #D97706;
        white-space: nowrap;
      }
    }

    .board-code {
      font-size: 11px;
      color: $text-muted;
      font-family: monospace;
      margin-bottom: 4px;
    }

    .board-pct {
      font-size: 18px;
      font-weight: 700;
      margin-bottom: 4px;

      &.up { color: #E83030; }
      &.down { color: #1CAD5E; }
    }

    .board-reason {
      font-size: 11px;
      color: $text-sub;
      line-height: 1.4;
      margin-bottom: 6px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
      min-height: 30px;
    }

    .board-meta {
      display: flex;
      justify-content: space-between;
      font-size: 11px;
      color: $text-muted;
      padding-top: 4px;
      border-top: 1px solid $border-light;
    }
  }

  .board-name-pop {
    font-size: 14px;
    font-weight: 600;
    color: $text-main;
  }

  .board-placeholder { min-height: 60px; }
}

.empty-hint { text-align: center; padding: $gap-lg; color: $text-muted; font-size: 13px; }

.dragon-name-pop {
  font-size: 13px;
  color: $text-main;
}

// ===== 响应式 =====
@media (max-width: 960px) {
  .hero {
    flex-direction: column;
    text-align: center;
  }

  .hero-content {
    .hero-desc {
      margin-left: auto;
      margin-right: auto;
    }

    .hero-divider {
      display: flex;
      justify-content: center;
    }
  }

  .hero-illust {
    width: 260px;
    height: 162px;
  }

  .news-dual {
    grid-template-columns: 1fr;
  }

  .s-board-ladder .board-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .fund-grid {
    grid-template-columns: 1fr;
  }

  .fund-chart {
    min-height: 160px;
  }

  .dragon-table { font-size: 12px; th, td { padding: 6px 8px; } }

}

@media (max-width: 640px) {
  .hero-content h1 {
    font-size: 30px;
  }

  .s-board-ladder .board-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

// 十大流通股东（机构聚合）
.inst-holder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.inst-card {
  background: $bg-section;
  border-radius: $radius;
  padding: 14px 16px;
}
.inst-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}
.inst-name {
  font-weight: 600;
  font-size: 14px;
  color: $text-main;
}
.inst-count {
  font-size: 12px;
  color: $text-muted;
  white-space: nowrap;
}
.inst-holdings {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.inst-stock-chip {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  background: $white;
  border: 1px solid $border;
  border-radius: $radius-sm;
  font-size: 13px;
  cursor: pointer;
  transition: background $ease;
  &:hover {
    background: $bg-hover;
  }
}
</style>
