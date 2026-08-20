# Claude Code Skills 清单

> 整理日期：2026-05-27  
> 总计：40 个 Skills

---

## 一、金融/数据类（6 个）

| Skill | 用途 |
|---|---|
| **a-stock-data** | A股全栈数据工具包，覆盖行情(mootdx+腾讯+百度K线)、研报(东财+同花顺+iwencai)、信号(同花顺热点+北向+龙虎榜+解禁+行业)、资金面(融资融券+大宗交易+股东户数+分红+资金流分钟级+资金流120日)、新闻(东财+财联社)、基础数据(mootdx财务/F10+东财+新浪三表)、公告(巨潮)七层数据源。适用于个股估值、研报检索、题材归因、龙虎榜跟踪、解禁预警、行业轮动、融资融券跟踪、筹码分析、产业链调研、批量筛选等场景 |
| **finance-expert** | 专家级金融系统、FinTech、银行、支付、金融技术支持 |
| **tushare-finance** | Tushare 金融数据接口调用 |
| **wind-find-finance-skill** | Wind 金融数据查找 |
| **wind-mcp-skill** | Wind MCP 数据服务 |
| **china-tax-law** | 中国税法相关查询与解析 |

---

## 二、学术/研究类（4 个）

| Skill | 用途 |
|---|---|
| **academic-paper** | 学术论文写作辅助 |
| **academic-paper-reviewer** | 学术论文审稿/评审 |
| **academic-pipeline** | 学术研究流水线工具 |
| **deep-research** | 深度研究/调研分析 |

---

## 三、开发流程类（12 个）

| Skill | 用途 |
|---|---|
| **superpowers:brainstorming** | 创意工作前的需求探索与方案设计，任何创建功能、添加组件、修改行为前都应使用 |
| **superpowers:writing-plans** | 有规格说明时，写代码前先制定多步骤实现计划 |
| **superpowers:executing-plans** | 执行已制定的实现计划 |
| **superpowers:test-driven-development** | 测试驱动开发（TDD）流程：先写测试，再写实现 |
| **superpowers:systematic-debugging** | 系统性调试方法，遇到 bug/测试失败/异常行为时先分析再修复 |
| **superpowers:subagent-driven-development** | 在当前会话中使用子代理并行执行独立实现任务 |
| **superpowers:dispatching-parallel-agents** | 调度多个并行代理执行任务 |
| **superpowers:verification-before-completion** | 完成前验证代码变更是否实际生效 |
| **superpowers:requesting-code-review** | 请求对当前代码变更进行审查 |
| **superpowers:receiving-code-review** | 接收并处理代码审查反馈 |
| **superpowers:finishing-a-development-branch** | 开发分支的收尾工作（合并、清理等） |
| **superpowers:using-git-worktrees** | 使用 Git Worktree 进行隔离开发 |

---

## 四、工具/辅助类（9 个）

| Skill | 用途 |
|---|---|
| **blog** | 将本地 Markdown 文件发布到远程博客 (www.3ks2u.com)，支持目录选择与 SCP 上传 |
| **agent-reach** | 全网搜索代理，覆盖 17 个平台：搜索、社交(小红书/抖音/微博/推特/B站/V2EX/Reddit)、招聘(LinkedIn)、开发(GitHub)、网页(文章/公众号/RSS)、视频(YouTube/B站/播客) |
| **bb-browser** | 浏览器相关操作 |
| **notebooklm** | NotebookLM 相关功能 |
| **humanizer-zh** | 中文文本人性化润色/优化 |
| **ui-ux-pro-max** | UI/UX 设计辅助 |
| **tw-tax-basics** | 台湾税务基础知识 |
| **office-hours** | YC 风格创始人 6 问拷问：问题、客户、分发、壁垒、资本、创始人匹配，先质问再给建议 |
| **superpowers:writing-skills** | 创建新 Skill、编辑现有 Skill、部署前验证 Skill |

---

## 五、配置/运维类（9 个）

| Skill | 用途 |
|---|---|
| **update-config** | 配置 Claude Code 的 settings.json（权限、环境变量、hooks 等） |
| **keybindings-help** | 自定义键盘快捷键、和弦绑定 |
| **fewer-permission-prompts** | 扫描常用只读命令，添加白名单以减少权限弹窗 |
| **init** | 项目初始化 |
| **run** | 启动并驱动项目应用，确认变更在真实环境生效 |
| **verify** | 运行应用观察行为，验证代码变更是否达到预期 |
| **code-review** | 对当前 diff 进行正确性审查，支持不同深度级别 |
| **review** | Pull Request 审查 |
| **security-review** | 安全性审查 |
| **loop** | 定时循环执行命令或 prompt（支持固定间隔或自适应节奏） |
| **claude-api** | Claude API / Anthropic SDK 应用开发调试，包含 prompt caching、tool use、模型迁移等 |
| **find-skills** | 查找并发现可用的 Skills |
| **superpowers:using-superpowers** | Skills 系统使用指南（所有会话自动加载） |

---

## 使用建议

- **日常金融数据处理**：优先使用 `a-stock-data`、`finance-expert`
- **开发新功能前**：先 `brainstorming`，再 `writing-plans`，然后 `test-driven-development`
- **遇到 Bug**：先用 `systematic-debugging` 分析，不要直接改代码
- **代码完成后**：先 `verification-before-completion`，再 `requesting-code-review`
- **对外搜索**：使用 `agent-reach` 覆盖全网多平台
