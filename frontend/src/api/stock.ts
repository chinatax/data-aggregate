import request from '@/utils/request'

export interface StockIndex {
  name: string
  code: string
  price: number
  change: number
  changePct: number
}

export interface StockNewsFlash {
  id: string
  title: string
  content: string
  publishTime: number
}

export interface StockHotTopic {
  code: string
  name: string
  reason: string
  changePct: number
  stocks: string[]
  topicDate: string
}

export interface MarketOverview {
  indices: StockIndex[]
  newsFlashes: StockNewsFlash[]
  hotTopics: StockHotTopic[]
  stats: Record<string, any>
}

export interface GlobalNewsItem {
  newsCode: string
  title: string
  summary: string
  showTime: string
}

export interface NorthboundFlow {
  hgt: number
  sgt: number
  total: number
  updateTime: string
  tradeDate: string
}

export interface MarginTrading {
  rzye: number
  rqye: number
  rzrqye: number
  tradeDate: string
}

export interface DragonBoardItem {
  tradeDate: string
  code: string
  name: string
  reason: string
  closePrice: number
  changePct: number
  netBuy: number
  buyAmt: number
  sellAmt: number
  turnoverPct: number
}

export interface LockupExpiryItem {
  code: string
  name: string
  freeDate: string
  lockupType: string
  shares: number
  ratio: number
}

export interface BoardItem {
  code: string
  name: string
  changePct: number
  closePrice: number
  turnover: number
  reason: string
  boardCount: number
}

export interface BoardLadder {
  tradeDate: string
  firstBoard: BoardItem[]
  secondBoard: BoardItem[]
  thirdPlusBoard: BoardItem[]
  totalCount: number
}

export function getMarketOverview(): Promise<MarketOverview> {
  return request.get('/api/stock/v2/market') as any
}

export function getStockIndices(): Promise<StockIndex[]> {
  return request.get('/api/stock/v2/indices') as any
}

export function getStockNewsFlash(): Promise<StockNewsFlash[]> {
  return request.get('/api/stock/v2/newsflash') as any
}

export function getStockHotTopics(): Promise<StockHotTopic[]> {
  return request.get('/api/stock/v2/hottopics') as any
}

export function getBookCount(): Promise<{ total: number }> {
  return request.get('/api/books/count') as any
}

export function getRecentLogs(): Promise<any[]> {
  return request.get('/api/admin/monitor/recent') as any
}

export function getNorthboundFlow(): Promise<NorthboundFlow> {
  return request.get('/api/stock/v2/northbound') as any
}

export interface MarginHistoryItem {
  tradeDate: string
  rzye: number
  rqye: number
  rzrqye: number
}

export function getMarginTrading(): Promise<MarginTrading> {
  return request.get('/api/stock/v2/margin') as any
}

export function getMarginHistory(): Promise<MarginHistoryItem[]> {
  return request.get('/api/stock/v2/margin/history') as any
}

export function getDragonBoard(): Promise<DragonBoardItem[]> {
  return request.get('/api/stock/v2/dragonboard') as any
}

export function getLockupExpiry(): Promise<LockupExpiryItem[]> {
  return request.get('/api/stock/v2/lockup') as any
}

export function getGlobalNews(): Promise<GlobalNewsItem[]> {
  return request.get('/api/stock/v2/globalnews') as any
}

export function getBoardLadder(): Promise<BoardLadder> {
  return request.get('/api/stock/v2/board-ladder') as any
}

export interface HolderNumItem {
  endDate: string
  code: string
  name: string
  industry: string
  region: string
  holderNum: number
  changeNum: number
  changeRatio: number
  avgShares: number
}

export function getHolderNum(): Promise<HolderNumItem[]> {
  return request.get('/api/stock/v2/holder-num') as any
}

export interface StockDailyAmount {
  tradeDate: string
  code: string
  name: string
  amount: number
  turnoverPct: number
  volumeRatio: number
  floatMarketCap: number
  industry: string
  concept: string
  region: string
  prevAmount: number | null
  boardCount?: number
}

export function getLatestDailyAmounts(): Promise<StockDailyAmount[]> {
  return request.get('/api/stock/v2/daily-amount/latest') as any
}

export function getDailyDisplay(): Promise<StockDailyAmount[]> {
  return request.get('/api/stock/v2/daily-amount/display') as any
}

export interface StockQualityScore {
  code: string
  name: string
  industry: string
  score: number
  revenuePass: number
  cashRevenueRatio: number
  profitPass: number
  cashProfitRatio: number
  detailJson: string
}

export function getQualityScores(): Promise<StockQualityScore[]> {
  return request.get('/api/stock/v2/quality') as any
}

export interface StockKlineItem {
  date: string
  open: number
  close: number
  high: number
  low: number
  volume: number
  changePct: number
}

export function getStockKline(code: string, days?: number): Promise<StockKlineItem[]> {
  return request.get('/api/stock/v2/kline', { params: { code, days } }) as any
}

export interface StockNameItem {
  code: string
  name: string
}

let stockNamesCache: StockNameItem[] | null = null

export interface HolderTop10Item {
  endDate: string
  code: string
  name: string
  holderName: string
  holdNum: number
  holdRatio: number
  holdChange: string
  holderRank: number
  sharesType: string
}

export function getHolderTop10(): Promise<HolderTop10Item[]> {
  return request.get('/api/stock/v2/holder-top10') as any
}

export async function getStockNames(): Promise<StockNameItem[]> {
  if (stockNamesCache) return stockNamesCache
  const res = await request.get('/api/stock/v2/names') as any
  stockNamesCache = (res?.data ?? res ?? []) as StockNameItem[]
  return stockNamesCache!
}
