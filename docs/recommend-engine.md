# 短视频推荐引擎设计说明

本文档对应 `feature/recommend-engine` 分支上的推荐模块，说明数据模型、召回与打分链路、
配置项、评估口径与已知局限。回滚点为 tag `archive-before-recommend-20261120`。

## 1. 目标

给「短视频」信息流加一层可解释、可调参、可评估的推荐，替代原来的
`content_type='VIDEO' AND status=1 ORDER BY create_time DESC` 纯时间序。

要求：

- 不引入新的中间件和 Maven 依赖（Redis/MQ 都没有强制依赖，缓存走进程内 Map）；
- 召回、打分、重排三段可分别观察（每条结果带 `reason`）；
- 全部参数可在库表里热改，不用重启；
- 有可复现的模拟数据与离线评估，能横向比较版本。

## 2. 总体链路

```
埋点上报 POST /api/behaviors
        │
        ▼
  user_behavior（行为流水，保留 90 天）
        │  定时任务（启动后 60s，之后每 5 分钟）
        ▼
  聚合：video_stat（热度统计） / user_interest_tag（画像） / video_similar（ItemCF）
        │
        ▼
在线请求 GET /api/feed/videos
  ① 5 路召回（每路最多 recall.max_candidates 条）
  ② 过滤 + 打分（Wilson / 完播 / 互动 / 新鲜度 / 长期兴趣 / 短期相似）
  ③ 多样性重排（同作者、同话题上限）+ 已看降权
  ④ 结果缓存（cache.feed_ttl_ms）+ 分页游标 traceId/offset
        ▼
  返回 items（含 reason、hotScore）+ 记录 IMPRESSION（10 分钟内去重）
```

## 3. 数据模型

`src/main/resources/sql/recommend_module.sql`（4 张业务表）与
`src/main/resources/sql/recommend_config.sql`（1 张参数表）。

| 表 | 主键 / 唯一键 | 用途 |
| --- | --- | --- |
| `user_behavior` | `id` | 行为流水：`action` ∈ IMPRESSION/PLAY/PLAY_END/LIKE/COMMENT/COLLECT/SHARE/SKIP，带 `watch_ms`、`watch_ratio`、`scene`、`trace_id` |
| `video_stat` | `video_id` | 近 30 天聚合：曝光/播放/完播/赞评藏分享、平均观看比例、`hot_score` |
| `user_interest_tag` | `uk_user_tag(user_id, tag_type, tag_value)` | 画像：TOPIC / AUTHOR / SPECIES 三类标签及归一化权重（上限 10） |
| `video_similar` | `uk_sim_pair(video_id, similar_video_id)` | ItemCF 相似视频，score ∈ [0,1] |
| `recommend_config` | `config_key` | 权重、多样性上限、召回规模、缓存 TTL 等运行期参数 |

所有表统一 `utf8mb4 / utf8mb4_unicode_ci`。这点不是可选项：`post.topic` 是
`utf8mb4_unicode_ci`，新表若用 MySQL 8 默认的 `utf8mb4_0900_ai_ci`，
画像召回 JOIN 会直接抛
`Illegal mix of collations (utf8mb4_0900_ai_ci,IMPLICIT) and (utf8mb4_unicode_ci,IMPLICIT)`。

## 4. 行为埋点

`BehaviorServiceImpl.record(userId, dto)`：

- `action` 必须属于白名单，否则拒绝；
- `targetType` ∈ {VIDEO, POST}，且目标帖子必须存在、`status = 1`；
- 强动作（LIKE/COMMENT/COLLECT/SHARE/PLAY_END）会调用 `invalidateCache(userId)`，
  避免刚点完赞还看旧排序。

前端在 `assets/app.js` 里上报：

- 列表渲染后对前 8 条上报 `IMPRESSION`；
- 详情页 `<video>` 首次 `play` 上报一次 `PLAY`；
- `ended` 上报 `PLAY_END`，带 `watchMs` 与 `watchRatio = currentTime / duration`。

## 5. 离线聚合

`RecommendAggregateServiceImpl.refreshAll()` 依次执行：

1. **热度重算** `RecommendMapper.refreshVideoStat()`：一条
   `INSERT ... SELECT ... ON DUPLICATE KEY UPDATE`，窗口 30 天，

   ```
   hot_score = (like*3 + comment*5 + collect*4 + share*6 + play_end*2)
               / SQRT(imp_cnt + 10)
               * EXP(-0.693147 * hours_since_publish / 72)
   ```

   用 `SQRT(imp)` 做曝光归一，而不是直接比绝对量，避免冷启动视频被大号碾压。

2. **画像重建** `rebuildInterestTags()`：取 30 天行为，按动作加权
   （PLAY_END 1.0 × (0.5 + watch_ratio)、LIKE 2.0、COMMENT 2.5、COLLECT/SHARE 3.0），
   累加到 TOPIC / AUTHOR 两类标签，再按用户把最大权重归一到 10。

3. **相似矩阵** `rebuildSimilarVideos()`：ItemCF
   `sim(a,b) = cooc(a,b) / sqrt(deg(a) * deg(b))`，每源视频保留 top 10、阈值 0.05。

4. 清理 90 天前的行为流水。

聚合由 `RecommendAggregateJob` 驱动：`@Scheduled(initialDelay=60_000, fixedDelay=300_000)`，
主类已加 `@EnableScheduling`。

## 6. 在线召回

`RecommendServiceImpl.buildCandidates()` 的顺序（`reason` 取第一个命中的来源）：

| 来源 | reason | SQL 要点 |
| --- | --- | --- |
| 热门 | `HOT` | `video_stat` 按 `hot_score DESC` |
| 关注 | `FOLLOW` | `user_follow.followed_id = post.user_id` |
| 话题兴趣 | `TOPIC` | `user_interest_tag(tag_type='TOPIC').tag_value = post.topic` |
| 作者兴趣 | `AUTHOR` | `tag_value = CAST(post.user_id AS CHAR) COLLATE utf8mb4_unicode_ci` |
| 相似 | `SIMILAR` | 近 7 天正反馈视频 → `video_similar` |
| 新品 | `FRESH` | 48 小时内发布 |
| 兜底 | `LATEST` | 候选不足 `max_candidates` 时补 |

匿名用户（无 `X-User-Id`）只走 HOT / FRESH / LATEST，不记曝光。

## 7. 打分

`RecommendScorer.computeScore()`，权重全部来自 `recommend_config`：

```
score = w.quality      * Wilson(play_end, play)          // 95% 置信下界
      + w.completeness * avg_watch_ratio
      + w.engagement   * log1p(加权互动) / log1p(候选最大互动)
      + w.freshness    * 0.5 ^ (小时数 / 72)
      + w.interest     * clamp01(0.7 * 话题分 + 0.3 * 作者分)
      + w.similarity   * max(ItemCF 相似度)
```

- 话题分、作者分各自先 `clamp01(tagWeight / 10)` 再加权，
  否则某一项权重异常（例如 999）会把另一项的分吃掉；
- `similarity` 是「短期兴趣」通道：候选与用户近 7 天正反馈视频的最大相似度，
  一次批量查询完成，不单独作为召回源。

默认权重：`quality 0.18 / completeness 0.05 / engagement 0.05 / freshness 0.07 /
interest 0.53 / similarity 0.12`，合计 1.00。

## 8. 重排与分页

- 已看内容不硬过滤：命中近 7 天正反馈的候选，排序分乘 `seen.penalty`（默认 0.65）。
  早期版本直接排到最后，结果用户把话题内视频看完后信息流被其他话题占满。
- 多样性：同作者最多 `diversity.max_per_author`（2）条、同话题最多
  `diversity.max_per_topic`（3）条；超出的进入溢出队列，位次不足时回填。
- 分页：`offset` + 每次请求独立 `traceId`；结果缓存按 `userId:limit:offset` 存
  `cache.feed_ttl_ms`（默认 120 秒）。
- 曝光去重：写 IMPRESSION 前先查 10 分钟内是否已曝过同一批视频，避免刷新一次涨一次曝光。

## 9. 配置热更新

`recommend_config` 由 `RecommendConfigService` 读取，进程内缓存 60 秒；
`PUT /api/admin/recommend/config` 写库后立即失效缓存。

| key | 默认值 | 含义 |
| --- | --- | --- |
| `weight.quality` | 0.18 | Wilson 完播率下界权重 |
| `weight.completeness` | 0.05 | 平均观看比例权重 |
| `weight.engagement` | 0.05 | 互动强度权重 |
| `weight.freshness` | 0.07 | 72 小时半衰权重 |
| `weight.interest` | 0.53 | 长期兴趣（话题+作者）权重 |
| `weight.similarity` | 0.12 | 短期相似度权重 |
| `diversity.max_per_author` | 2 | 同作者上限 |
| `diversity.max_per_topic` | 3 | 同话题上限 |
| `seen.penalty` | 0.65 | 已看内容折扣 |
| `recall.max_candidates` | 60 | 每路召回条数 |
| `cache.feed_ttl_ms` | 120000 | 结果缓存时长 |

## 10. 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/feed/videos?limit&offset&traceId` | 推荐流，`X-User-Id` 可选 |
| POST | `/api/behaviors` | 行为上报，需要 `X-User-Id` |
| POST | `/api/admin/recommend/aggregate` | 手动触发聚合 |
| GET | `/api/admin/recommend/config` | 读取全部参数 |
| PUT | `/api/admin/recommend/config` | 批量改参数，body 为 `{key: value}` |
| GET | `/api/admin/recommend/evaluate?topK&maxUsers` | 离线评估 |
| POST | `/api/admin/recommend/mock?users&videos&behaviors` | 生成模拟数据 |

管理端接口统一先走 `AdminService.requireAdmin(operatorId)`。

## 11. 模拟数据

`MockDataService.generate()`：清掉历史 `[MOCK]` 视频与 `mock_u*` 用户后重建。

- 视频标题 `[MOCK] 萌宠日常 N`，话题在 3 个真实话题间轮转，
  `videoUrl`/`cover` 轮转 3 个真实 MinIO 地址（无需准备素材）；
- 每个用户有「主兴趣话题 + 4 个常看作者」，强互动 85% 落在该集合、
  完播 70%、随手刷 55%；
- 全站曝光按 `1/(rank+5)^0.8` 的长尾分布抽样 —— 均匀抽样会让
  ItemCF 共现矩阵退化成全 1（实测 `avg(score)=1.000`，相似度彻底失去区分度）；
- 行为时间落在最近 7 天且不早于视频发布时间。

## 12. 离线评估

`RecommendEvaluationService.evaluate()`：leave-one-out。

- 对交互 ≥ 3 条的用户，取最近一条**强反馈**（LIKE/COLLECT/COMMENT/SHARE，近 7 天）作为 holdout；
- 用当前召回+打分给候选排序，看 holdout 的名次；
- 指标：`HR@K = 命中人数 / 评估人数`、`NDCG@K = Σ 1/log2(rank+1) / N`；
- 对照组：同一批用户的「纯热门 TopK」「纯最新 TopK」，随机基线 `K / 视频总数`；
- 补充指标：`recInterestAlign` / `hotInterestAlign` —— 推荐结果与热门榜单里
  命中用户主兴趣话题的比例，用来绕开曝光偏差看个性化有没有生效。

实测（20 用户 / 600 视频 / 6000 行为）：

| topK | HR@K | NDCG | 热门基线 | 最新基线 | 随机 | 兴趣对齐(推荐/热门) |
| --- | --- | --- | --- | --- | --- | --- |
| 10 | 0.00 | 0.000 | 0.20 | 0.15 | 0.017 | 0.400 / 0.340 |
| 50 | 0.50 | 0.096 | 0.30 | 0.25 | 0.083 | 0.584 / 0.335 |

**必须说清楚的局限**：这套模拟数据里每个话题约 200 条视频，
用户的「相关项」在话题内近乎均匀，`HR@10` 的理论上限就只有 10/200 ≈ 5%；
同时用户最后一次互动大概率落在高曝光视频上（长尾抽样放大了这一点），
所以「纯热门」基线天然很强。这两个原因叠加，导致 `HR@10` 对版本差异不敏感，
它更适合做「相对基线」的长期观察，而不是线上效果的绝对估计。
真正的增益要看 `HR@50`、兴趣对齐率，以及线上 A/B。

另：画像由全量行为聚合而来，包含 holdout 自身的信息（时间泄漏），
所以指标只能用于版本间横向比较。

## 13. 前端

`frontend-tomcat/petcommunity/videos.html` + `assets/app.js`：

- `loadVideos()` 请求 `/api/feed/videos?limit=12&offset=...`，带 `X-User-Id`；
- 卡片左上角徽标显示 `reason`（热门推荐 / 关注的人 / 兴趣话题 / 常看作者 / 相似内容 / 新发布 / 最新），
  右上角显示热度；
- 「换一批」按钮重置 `offset` 并重新拉取；
- 详情页播放/播完时上报行为，失败静默，不阻塞播放。

## 14. 测试

`mvn test` 共 17 个推荐相关用例：

- `RecommendScorerTest`：Wilson 边界与样本惩罚、72 小时半衰、兴趣匹配 0.7/0.3、clamp、打分单调性；
- `RecommendServiceImplTest`：同作者上限、同话题上限、已看降权；
- `RecommendConfigServiceTest`：解析/缺省/非法值回退、60 秒缓存、只更新传入键；
- `RecommendEvaluationServiceTest`：HR / NDCG / 随机基线、热门与最新基线。

## 15. 已知限制

1. 结果缓存在进程内 `ConcurrentHashMap`，多实例部署时各节点缓存不一致；
   要上多实例得换 Redis，并把 `invalidateCache` 广播出去。
2. 相似矩阵是离线全量重算，视频量到十万级时需要改成增量 + 分片。
3. 冷启动只做了「新品召回 + 热度兜底」，没有做内容向量（标题/封面），
   新用户前几条仍偏热门内容。
4. 曝光去重窗口固定 10 分钟，长会话下同一视频仍可能重复曝光。
5. `HR@10` 在本数据集上区分度有限，见第 12 节。
