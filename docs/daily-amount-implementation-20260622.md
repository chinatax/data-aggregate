# 个股日成交数据采集 —— 完整实施记录

**日期**: 2026-06-22  
**触发**: 通达信指标含义咨询 → 数据源梳理 → 功能实现

---

## 1. 需求背景

用户咨询通达信四个指标含义（昨成交额、总金额、内盘、外盘），确认内盘/外盘需Level-2数据源（免费接口无覆盖），最终聚焦两个可行指标：**总金额**(成交额) 和 **昨成交额**。后追加：**换手率**、**量比**、**概念**、**行业**、**地区**、**细分行业**、**流通市值**。

## 2. 数据源梳理

### 本项目已用数据源（14个外部域名）

| 来源 | 域名 | 用途 |
|------|------|------|
| 腾讯 | `qt.gtimg.cn` | 指数+个股批量行情 |
| 腾讯 | `ifzq.gtimg.cn` | 日K线(前复权) |
| 东财 | `push2.eastmoney.com` | 行业排名 |
| 东财 | `datacenter-web.eastmoney.com` | 融资融券/龙虎榜/解禁/财务/股东户数 |
| 东财 | `np-weblist.eastmoney.com` | 7x24快讯 |
| 东财 | `emweb.securities.eastmoney.com` | F10公司资料 |
| 财联社 | `www.cls.cn` | 快讯电报 |
| 同花顺 | `zx.10jqka.com.cn` | 热点题材 |
| 核新 | `data.hexin.cn` | 北向资金 |
| 新浪 | `hq.sinajs.cn` | 指数(旧) |
| NewsData.io | `newsdata.io` | 国际新闻(需API Key) |
| 阿里云 | `dashscope.aliyuncs.com` | AI文生图 |
| 微信 | `open.weixin.qq.com` / `api.weixin.qq.com` | OAuth登录 |
| 世界银行 | `api.worldbank.org` | 税负数据 |

### 自有数据库
- `data_aggregate` — 主库（业务数据）
- `da_win10.da_01_stock_basic` — 跨库查股票名称字典

## 3. 腾讯行情字段映射

测试 `curl -s "https://qt.gtimg.cn/q=sh600519"`

| 业务指标 | 腾讯字段索引 | 示例(茅台) | 说明 |
|----------|------------|-----------|------|
| 当前价 | f[3] | 1222.61 | |
| 昨收 | f[4] | 1215.00 | |
| 总金额(成交额) | f[37] | 287870 | 万元 |
| 换手率 | f[38] | 0.19 | % |
| 流通市值 | f[45] | 15279.87 | 亿元 |
| 量比 | f[49] | 1.77 | |

> **关键发现**: 沪市(f[7]起为买卖盘)和深市(f[7]-f[8]为内盘/外盘，f[9]起为买卖盘)从f[30]起字段对齐，因此f[37]/f[38]/f[45]/f[49]对沪深两市通用。

## 4. 东财 push2 分类字段

URL: `https://push2.eastmoney.com/api/qt/clist/get?fs=m:0+t:6,m:0+t:80,m:1+t:2,m:1+t:23&fields=f12,f14,f100,f102,f103`

| 指标 | 字段 | 示例(浦发银行) |
|------|------|---------------|
| 股票代码 | f12 | 600000 |
| 名称 | f14 | 浦发银行 |
| 行业(申万二级) | f100 | 银行Ⅱ |
| 地区板块 | f102 | 上海板块 |
| 概念 | f103 | 长三角,互联网金融,蚂蚁概念,跨境支付 |

> f100 返回申万二级行业(含罗马数字后缀)，一级行业可通过去掉后缀获得。  
> 细分行业在 push2 中未暴露，F10 API `sszjhhy` 字段有但只能单只查询(太慢)。

## 5. 实现方案

### 数据采集流程

```
收盘后 15:30 (MON-FRI)
├── 东财 push2: 1次请求 → 全A股行业/概念/地区 Map<code, String[]>
└── 腾讯 qt.gtimg.cn: 200只/批 × 8线程 → 25次请求 → f[37]/f[38]/f[45]/f[49]
                              ↓
                    按 code 合并两类数据
                              ↓
                @Transactional: deleteByTradeDate + saveBatch
                              ↓
                       写 Redis + Caffeine
```

### 昨成交额计算

不额外拉取数据。查询时按 `trade_date DESC` 排序，列表相邻记录的前一条即为昨成交额（`prevAmount`）。API 返回 VO 中直接包含。

## 6. 去重 & 事务设计

```java
@Transactional  // 保证 delete + saveBatch 原子性
public List<StockDailyAmountVO> fetchAndCacheDailyAmounts() {
    dailyAmountService.deleteByTradeDate(tradeDate);  // 先删当日旧数据
    dailyAmountService.saveBatch(entities);            // 再批量写入
}
// DB层 UNIQUE KEY uk_code_date (code, trade_date) 兜底
```

> 复用项目现有模式：龙虎榜(`fetchAndCacheDragonBoard`)和连板天梯(`fetchAndCacheBoardLadder`)均为"先删后写"。

## 7. 新建/修改文件清单

### 新建 (5个)

| 文件 | 路径 |
|------|------|
| 建表SQL | `docker/mysql/init/16_stock_daily_amount.sql` |
| Entity | `dataagg-common/.../entity/StockDailyAmount.java` |
| VO | `dataagg-common/.../dto/StockDailyAmountVO.java` |
| Mapper | `dataagg-service/.../mapper/StockDailyAmountMapper.java` |
| Service | `dataagg-service/.../service/StockDailyAmountService.java` |

### 修改 (3个)

| 文件 | 改动 |
|------|------|
| `StockDataService.java` | +`fetchAndCacheDailyAmounts()`(带@Transactional)、+`fetchStockClassifications()`、+`collectAllMarketData()`、+`getDailyAmountList()`、+`getLatestDailyAmounts()`、+`mapToVO()` |
| `StockDataScheduler.java` | +`@Scheduled(cron = "0 30 15 * * MON-FRI")`、+cleanup保留365天 |
| `StockV2Controller.java` | +3个API端点 |

### 表结构

```sql
stock_daily_amount (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    trade_date       DATE NOT NULL,
    code             VARCHAR(10) NOT NULL,
    name             VARCHAR(50),
    amount           DECIMAL(20,4),   -- 成交额(万元)
    turnover_pct     DECIMAL(10,4),   -- 换手率%
    volume_ratio     DECIMAL(10,4),   -- 量比
    float_market_cap DECIMAL(20,4),   -- 流通市值(亿)
    industry         VARCHAR(100),    -- 行业(申万二级)
    concept          VARCHAR(500),    -- 概念标签
    region           VARCHAR(50),     -- 地区板块
    create_time      DATETIME,
    UNIQUE KEY uk_code_date (code, trade_date),
    INDEX idx_trade_date (trade_date),
    INDEX idx_industry (industry)
)
```

## 8. API 端点

```
GET /api/stock/v2/daily-amount/latest            # 全量最新日
GET /api/stock/v2/daily-amount?code=600519&days=30  # 个股历史(含prevAmount)
GET /api/stock/v2/daily-amount/refresh            # 手动触发采集
```

## 9. 三级缓存

| 层 | 存储 | TTL |
|----|------|-----|
| L1 | Caffeine 本地 | 30min |
| L2 | Redis `stock:daily:amount:*` | 4h(全量) / 30min(个股) |
| L3 | MySQL | 持久, 1年保留 |

## 10. 已知局限

- **内盘/外盘**: 免费接口无覆盖，仅深市腾讯接口在内盘/外盘字段有值(f[7]/f[8])但未采集
- **细分行业**: push2 仅返回申万二级(f100)，更细分类需 F10 单只查询
- **北交所**: 腾讯接口 `bj` 前缀支持但字段映射未验证
- **昨成交额**: 通过"前一天记录"推算，若某天漏采则会缺失
