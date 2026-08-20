# 稳定分红模块 — 实施计划

> 创建日期：2026-05-25
> 状态：待实施

---

## 一、设计决策

| 决策项 | 结论 |
|--------|------|
| 数据源 | 东财 datacenter `RPT_SHAREBONUS_DET` |
| 抓取范围 | 近 5 年全市场分红记录 |
| 筛选门槛 | 连续分红 ≥ 3 年 + 股息率 ≥ 1.5% |
| 排序依据 | 股息率降序 |
| 存储方式 | 全市场明细落库，查询时动态筛选 |
| 展示标签 | 行业、派息稳定性(CV)、派息率健康度 |
| 银行股 | 不排除，标签标注行业 |
| 刷新频率 | 每天凌晨（分红数据低频，日级变动） |
| 前端 | 本次不做，仅后端数据抓取 |

---

## 二、筛选流程

```
─────────────────────────────────────────────────────────
  拉取 RPT_SHAREBONUS_DET (REPORT_DATE ≥ 5年前)
  │
  ├─ 聚合: 同股票 + 同财年多期分红 → 每股派息加总
  │
  ├─ 门槛1: 连续分红年数 ≥ 3 (按 REPORT_DATE 年份)
  │
  ├─ 门槛2: 股息率 ≥ 1.5% (最新年度派息 / 腾讯行情当前价)
  │
  ├─ 计算标签:
  │   ├─ CV = 近5年派息标准差 / 均值
  │   │   ≤10% 🟢稳定 | 10%~30% 🟡中等 | >30% 🔴波动
  │   ├─ 派息率 = 最新每股派息 / 最新 BASIC_EPS
  │   │   30%~70% 🟢健康 | 70%~100% 🟡偏高 | >100% 🔴异常
  │   └─ 行业 = F10 CompanySurveyAjax.sshy
  │
  ├─ 排序: 股息率降序, Top 100
  │
  └─ 写表: 先删全表, 批量写入 Top 100
─────────────────────────────────────────────────────────
```

---

## 三、表结构

```sql
CREATE TABLE IF NOT EXISTS `stock_dividend` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `name` VARCHAR(50) COMMENT '股票名称',
    `industry` VARCHAR(100) COMMENT '行业(F10补全)',
    `report_year` INT NOT NULL COMMENT '财报年度',
    `latest_bonus` DECIMAL(10,4) COMMENT '最近年度每股派息(元)',
    `dividend_yield` DECIMAL(10,4) COMMENT '股息率%',
    `consecutive_years` INT DEFAULT 0 COMMENT '连续分红年数',
    `cv_pct` DECIMAL(10,4) COMMENT '近5年派息变异系数%',
    `payout_ratio` DECIMAL(10,4) COMMENT '派息率%(每股派息/EPS)',
    `snap_date` DATE COMMENT '快照日期',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    INDEX `idx_snap_date` (`snap_date`),
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='稳定分红筛选结果Top100';
```

说明：这张表只存 Top 100 结果，不是全市场明细。每次抓取先 `TRUNCATE` 再批量写入。

---

## 四、需要新增/修改的文件

### 新增文件（4个）

| 文件 | 路径 | 说明 |
|------|------|------|
| `StockDividend.java` | `backend/dataagg-common/.../entity/` | Entity 实体，映射 `stock_dividend` 表 |
| `StockDividendMapper.java` | `backend/dataagg-service/.../mapper/` | MyBatis-Plus Mapper 接口 |
| `StockDividendService.java` | `backend/dataagg-service/.../service/` | 业务 Service（增删查） |

### 修改文件（3个）

| 文件 | 修改内容 |
|------|---------|
| `docker/mysql/init/10_stock_v2.sql` | 末尾新增 `stock_dividend` DDL |
| `StockDataService.java` | 新增 `fetchAndCacheDividend()` + `getDividendFromCache()` |
| `StockDataScheduler.java` | 新增 `@Scheduled` 分红定时任务 |

### 配置文件修改

| 文件 | 修改内容 |
|------|---------|
| `application.yml` | 新增 `stock.dividend.initialDelay` + `stock.dividend.interval` |

---

## 五、核心代码模板

### 5.1 Entity

```java
// StockDividend.java
package com.dataagg.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("stock_dividend")
public class StockDividend {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String industry;
    private Integer reportYear;
    private BigDecimal latestBonus;
    private BigDecimal dividendYield;
    private Integer consecutiveYears;
    private BigDecimal cvPct;
    private BigDecimal payoutRatio;
    private LocalDate snapDate;
    private LocalDateTime createTime;
}
```

### 5.2 Mapper

```java
// StockDividendMapper.java
package com.dataagg.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dataagg.common.entity.StockDividend;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockDividendMapper extends BaseMapper<StockDividend> {
}
```

### 5.3 Service

```java
// StockDividendService.java
package com.dataagg.service.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dataagg.common.entity.StockDividend;
import com.dataagg.service.mapper.StockDividendMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class StockDividendService extends ServiceImpl<StockDividendMapper, StockDividend> {

    public List<StockDividend> listAll() {
        LambdaQueryWrapper<StockDividend> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(StockDividend::getDividendYield);
        return this.list(wrapper);
    }

    public void truncateAll() {
        this.baseMapper.delete(new LambdaQueryWrapper<>());
    }
}
```

### 5.4 StockDataService 核心方法

```java
// 常量
private static final String REDIS_DIVIDEND = "stock:dividend";
private static final int DIVIDEND_LOOKBACK_YEARS = 5;

/**
 * 抓取并缓存稳定分红 Top 100
 *
 * 流程:
 * 1. 从东财 datacenter 拉取近5年全市场分红记录
 * 2. 按 (code, 财年) 聚合，同年多期派息加总
 * 3. 交叉腾讯行情 API 获取当前股价计算股息率
 * 4. 筛选: 连续分红≥3年 + 股息率≥1.5%
 * 5. 计算 CV、派息率标签
 * 6. 按股息率降序取 Top 100
 * 7. 补充行业信息(F10)
 * 8. 写入 MySQL + Redis + Caffeine
 */
public List<StockDividendVO> fetchAndCacheDividend() {
    // 1. 拉取全市场分红数据
    List<DividendRaw> rawList = fetchAllDividends();

    // 2. 按 (code, 财年) 聚合
    Map<String, Map<Integer, BigDecimal>> aggregated = aggregateByCodeAndYear(rawList);

    // 3. 获取当前股价(批量腾讯 API)
    Map<String, BigDecimal> prices = getStockPrices(aggregated.keySet());

    // 4. 筛选 + 计算
    List<StockDividendVO> result = new ArrayList<>();
    for (String code : aggregated.keySet()) {
        Map<Integer, BigDecimal> yearBonus = aggregated.get(code);
        if (yearBonus.size() < 3) continue; // 门槛1: 连续≥3年

        BigDecimal latestBonus = getLatestBonus(yearBonus);
        BigDecimal price = prices.get(code);
        if (price == null || price.compareTo(BigDecimal.ZERO) == 0) continue;

        BigDecimal dividendYield = latestBonus.divide(price, 4, RoundingMode.HALF_UP)
                                              .multiply(BigDecimal.valueOf(100));
        if (dividendYield.compareTo(BigDecimal.valueOf(1.5)) < 0) continue; // 门槛2: ≥1.5%

        StockDividendVO vo = new StockDividendVO();
        vo.setCode(code);
        vo.setLatestBonus(latestBonus);
        vo.setDividendYield(dividendYield);
        vo.setConsecutiveYears(yearBonus.size());
        vo.setCvPct(calcCv(yearBonus.values()));
        // payoutRatio 从最新 BASIC_EPS 计算
        result.add(vo);
    }

    // 5. 排序取 Top 100
    result.sort((a, b) -> b.getDividendYield().compareTo(a.getDividendYield()));
    if (result.size() > 100) result = result.subList(0, 100);

    // 6. 补全行业 + 写库
    // ... F10 逐股查询（300ms 间隔）
    // ... truncate + saveBatch + Redis + Caffeine

    return result;
}
```

### 5.5 Scheduler

```java
// StockDataScheduler.java 新增

@Scheduled(initialDelayString = "${stock.dividend.initialDelay:45000}",
           fixedDelayString = "${stock.dividend.interval:86400000}")
public void pollDividend() {
    stockDataService.fetchAndCacheDividend();
}
```

### 5.6 配置 `application.yml`

```yaml
stock:
  dividend:
    initialDelay: 45000
    interval: 86400000  # 24小时
```

---

## 六、实施步骤（按先后顺序）

| 步骤 | 内容 | 文件 |
|------|------|------|
| 1 | 新建 DDL 到 init SQL | `docker/mysql/init/10_stock_v2.sql` |
| 2 | 新建 Entity | `dataagg-common/.../entity/StockDividend.java` |
| 3 | 新建 VO | `dataagg-common/.../dto/StockDividendVO.java` |
| 4 | 新建 Mapper | `dataagg-service/.../mapper/StockDividendMapper.java` |
| 5 | 新建 Service | `dataagg-service/.../service/StockDividendService.java` |
| 6 | 新增 fetchAndCacheDividend() | `StockDataService.java` |
| 7 | 新增 getDividendFromCache() | `StockDataService.java` |
| 8 | 新增定时任务 | `StockDataScheduler.java` |
| 9 | 新增配置项 | `application.yml` |
| 10 | 清理策略(如需) | `StockDataScheduler.cleanOldData()` |
| 11 | 控制器端点(暂不做) | `StockV2Controller.java` |

---

## 七、注意事项

- 同年多期分红（中期+年末）需加总，否则股息率偏低
- F10 行业补全需要 300ms 间隔，100 支股票约 30 秒
- 腾讯行情 API 单次最多 50 个代码，Top 100 需要分 2 批调用
- `PRETAX_BONUS_RMB` 是税前每股派息，A 股持股超 1 年免红利税，直接用税前
- 非交易日分红暂停拉取无意义，可判断是否交易日再执行
- 已公告未除权的分红（ASSIGN_PROGRESS = 董事会决议通过/股东大会决议通过）也算，用于计算"即将到来"的股息率
