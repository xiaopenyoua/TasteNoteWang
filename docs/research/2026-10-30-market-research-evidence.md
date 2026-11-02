---
doc: docs/research/2026-10-30-market-research-evidence
title: 市场调研证据附录（2026-10-30）
status: accepted
date: 2026-10-30
owner: wang
related:
  - docs/research/2026-10-30-market-research
---

# 市场调研证据附录（2026-10-30）

> 本文件是主报告的证据底座，由 7 个调研维度在 2026-10-30 的抓取结果整理而成。
> 证据栏中的 URL 为调研时访问的页面；"未验证"表示该条为推断或未取得可复现证据。
> **注意**：所有"实测"均为调研代理的 HTTP 抓取，不是真机实测；平台页面与风控随时变化，引用前需复核。

## 证据分级

| 等级 | 定义 |
|---|---|
| 硬数据 | 应用商店/官网/协议原文/接口返回，可在采集日复现 |
| 二手 | 媒体报道、行业报告转述，未核对原始口径 |
| 推断 | 由多条弱证据合成，非直接观测 |
| 未验证 | 单一来源或本轮无法复现 |


## 国内竞品

**结论摘要**：国内菜谱赛道由下厨房、豆果美食、香哈、美食杰四分，均为内容社区+广告/会员/电商模式，功能高度同质化，核心是找菜谱而不是存菜谱。所有主流产品都不支持从抖音/小红书导入链接，菜谱里的视频也不能按步骤检索，用户只能反复拖进度条。真正的收藏发生在小红书/抖音/微信，但那里的收藏夹是无结构黑洞：上千条、搜索弱、只进不出。平板端是明显空白：安卓平板生态普遍是手机版拉伸或平行视界双窗口，菜谱 HD 应用最后更新停在 2020 年。唯一接近导入社交菜谱的是 Kitchen Stories Plus 和独立 iOS 应用 Plum（明确支持小红书/抖音/B站，但英文、0 评分、无安卓），说明需求真实存在，而中文安卓端无人做透。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | 下厨房 | product | 应用宝累计下载 7591.9 万，iOS 44.0 万评分 4.84；会员连续包月 ¥19、年 ¥228（App Store 描述），内购列表另有 ¥25/月档。菜谱以图文步骤为主、视频菜谱单列；“菜单”可把菜谱归组；官网 FAQ 只讲手动创建菜谱，未提供任何外链/视频导入；iOS 为 iPhone/iPad 通用（含 5 张 iPad 截图）。 | 应用宝 https://sj.qq.com/appdetail/com.xiachufang（V8.8.95，2026-10-17）；App Store https://apps.apple.com/cn/app/id460979760（2026-10 抓取）；下厨房 FAQ https://www.xiachufang.com/faq/ |
| 2 | 豆果美食 | product | 应用宝 3469.7 万下载、iOS 19.9 万评分 4.90；会员首月 ¥5 后 ¥19/月、¥58/季、¥218/年（内购页另显示 ¥25/月、¥198/年，口径不一致）。主打视频课+图文菜谱+社区笔记+精选商城，未见任何外链导入能力。 | 应用宝 https://sj.qq.com/appdetail/com.douguo.recipe（2026-07-24）；App Store https://apps.apple.com/cn/app/id434212566（2026-09 版本） |
| 3 | 懒饭 | product | 下厨房旗下，主打 1 分钟视频+克数/毫升/时间/防翻车提示；iOS 15.9 万评分 4.88，2026-10-21 仍在更新；会员 ¥10-14/月、¥128/年、¥88 半年。2019 年可见评价即指出搜索功能还需要强化；应用宝搜不到官方包（搜索结果首位 com.hfxn.lazyricerecipe 为仿品，227 下载、评论“一堆广告无法使用”），安卓官方分发未验证。 | App Store https://apps.apple.com/cn/app/id1377082167（2026-10 抓取）；iTunes Search API（2026-10）；应用宝搜索页 https://sj.qq.com/search?q=懒饭 |
| 4 | 美食杰 / 美食杰VIP | product | 宣称一百万免费图文视频菜谱；免费版靠广告，另有 ¥8 买断的独立“美食杰VIP”（更新说明称“无广告的纯净版”），该付费版 iOS 1.14 万评分 4.85、付费美食榜第 1；应用宝 722.9 万下载；无导入功能。 | 应用宝 https://sj.qq.com/appdetail/com.jesson.meishi（V8.2.9，2026-07-13）；App Store https://apps.apple.com/cn/app/id502803489 与 id399423833（2026-10 抓取） |
| 5 | 香哈菜谱 | product | 宣称 100 万+菜谱、1 万+视频；应用宝 1419 万下载、iOS 24 万评分 4.9；会员价目混乱——描述写 3 元/月、25 元/季、12 元/年，内购页却列 6-58 元多档（真实档位未验证）。2023-04-10 用户评论：一打开APP各种广告，有的广告弹窗还不好关闭，还自动下载不知名的各种APP；2018-10 用户反映换手机后收藏的菜谱丢失、App 打不开。 | 应用宝 https://sj.qq.com/appdetail/com.xiangha（2023-04-10 评论，2026-07-24 版本）；App Store https://apps.apple.com/cn/app/id1098628542（2026-08 版本） |
| 6 | 厨房故事 Kitchen Stories | feature | 中国区 App Store 可下载，英文名 Kitchen Stories Easy Recipes，3.70 万评分 4.88，v27.5.0（2026-10-27）；Plus 会员可一键保存 Instagram/TikTok 菜谱进个人数字菜谱本，替代截图和丢失的链接，2026-10 更新专门改进从分享链接导入菜谱；Plus 定价页返回 410，国内价格未验证。 | iTunes Lookup https://itunes.apple.com/lookup?id=771068291&country=us 与 &country=cn（2026-10 抓取）；US 描述含 social media saving |
| 7 | Plum 食谱收藏（最接近的独立产品） | product | iOS/iPadOS 17.1+、英文界面、0 评分；免费+¥68 一次性 Plum+；明确支持小红书、抖音、哔哩哔哩和美食博客无限导入，自动整理成步骤，含每步计时、屏幕常亮、按超市分区购物清单、AI 替换建议、打印；2026-09 支持应用内看 Instagram/Facebook 视频，2026-07 支持保存 Claude/ChatGPT 对话里的菜谱。 | App Store https://apps.apple.com/cn/app/id6496861616（v2.4.5，2026-10 抓取）；iTunes Lookup id=6496861616 |
| 8 | 小红书收藏 / 专辑 | feature | 小红书 iOS 2116 万评分、v9.52；收藏本身无数量上限（第三方问答，未验证），支持专辑分组。但 2025-05 人人都是产品经理作者有 1000+ 篇收藏笔记，吐槽层级混乱、搜索弱、每次找灵感翻半天、只进不出；知乎有创建完许多专辑收藏夹后笔记只能显示一个的故障提问（页面 403，仅得摘要）。 | https://www.woshipm.com/ai/6222673.html（2025-05-28）；知乎 https://www.zhihu.com/question/373398957（摘要，原页 403）；iTunes Lookup id=741292507 |
| 9 | 抖音收藏夹 | feature | 抖音 iOS 5212 万评分、v40.9.0；收藏页支持新建分类收藏夹、批量添加视频（抖音官方视频页）；但早期收藏未归入收藏夹，导致难以分类查找（知乎提问摘要）；作者删除或设为私密的视频会让收藏直接失效（第三方教程总结，未验证）。 | iTunes Lookup id=1142110895（2026-10）；抖音官方页 https://www.douyin.com/shipin/7304845110289483827（搜索摘要）；知乎 https://www.zhihu.com/question/547417468（摘要） |
| 10 | 微信收藏 | feature | 2013-08-09 版本加入收藏；可保存聊天中的链接、图片、文件；官方未给统一容量，但维基百科记载微信占用可达 40GB 甚至 100GB，且通过微信发送的文件经过一段时间后无法继续保存；收藏没有面向菜谱的字段与步骤结构。 | 维基百科 https://zh.wikipedia.org/wiki/微信（2026-10 抓取）；微信官网 https://weixin.qq.com/ |
| 11 | 收藏黑洞：收藏了但不做 | pain | 少数派 2022-04《收藏≠学会》指出收藏会制造已掌握的错觉、堆积带来心理压力，主张限期处理+定期清理；2025-05 人人都是产品经理案例：1000+ 篇小红书收藏完全不知道里面有什么，最终靠导出到飞书多维表格才可用。 | https://sspai.com/post/72939（2022-04-28）；https://www.woshipm.com/ai/6222673.html（2025-05-28） |
| 12 | 视频教程要反复拖进度条 | pain | 界面新闻 2020-03 采访原话：时长要退回几秒前看步骤，没有图文菜谱方便查阅；同文总结视频优点是直观、可回放，缺点是做菜时跟不上速度、需要反复回看，图文则便于边做边看。 | 界面新闻 https://www.jiemian.com/article/4048365.html（2020-03-01） |
| 13 | 换机/内容下架导致收藏失效 | risk | 香哈 2018-10-05 用户评论：换手机后收藏的菜谱丢失、App 打不开；抖音收藏的视频会因作者删除/设私密而无法播放（第三方教程总结，未验证）；小红书被维基百科记载会限制加微信、询问价格等站外引流。 | App Store https://apps.apple.com/cn/app/id1098628542（2018-10-05 评论）；维基百科 https://zh.wikipedia.org/wiki/小红书（2026-10 抓取） |
| 14 | 广告与会员疲劳 | pain | 微博 2026-09-20：下厨房就是开屏广告太讨厌了，找不到关的；香哈 2023-04 投诉广告弹窗难关、自动下载陌生 App；美食杰直接把 ¥8 无广告纯净版当卖点；360 搜索可见 2023-2024 多篇下厨房如何关闭自动续费教程。 | 微博（2026-09-20，经 360 搜索摘要）；应用宝 https://sj.qq.com/appdetail/com.xiangha（2023-04-10）；App Store id502803489 |
| 15 | 安卓平板适配空白 | tech | 少数派 2021 把平板应用分为手机版/平板适配版/TV 版，手机版常只能竖屏或居中显示；腾讯新闻 2021：安卓平行视界只是把手机双窗口并排，横屏单窗口不能全屏；独立 HD 菜谱应用家常菜做法大全HD 最后更新停在 2020-03-29。主流菜谱 App 的 iOS 版均有 iPad 截图，但未找到任何安卓平板专属版本的证据。 | 少数派 https://sspai.com/post/80988（2021）；腾讯新闻 https://news.qq.com/rain/a/20210714A09SUY00；iTunes Lookup id=705212448（2020-03-29）；id460979760 等含 iPad 截图 |
| 16 | 市场结构：发现已迁移到小红书/抖音 | insight | 易观 2021-05 数据（人人都是产品经理 2022-07 引用）：美食内容平台中小红书第一、抖音第二（仅差 5%），近半年轻用户因小红书/抖音美食内容产生下厨意愿；2021H1 MAU 下厨房 1199.1 万、豆果 632.9 万；菜谱 App 功能同质化、使用工具化，变现依赖广告/电商/会员。另有二手数据称下厨房 2019 年底 MAU 曾达 2017 万、人均日使用 15 分钟（未验证）。 | https://www.woshipm.com/it/5513872.html（2022-07-07，引易观 2021-05）；360 搜索结果摘要（smzdm，未验证） |
| 17 | 导入的合规边界 | legal | 小红书会限制加微信、询问价格等站外引流（维基百科）；批量抓取平台视频/图文涉及著作权与平台条款（具体条款未验证）；Kitchen Stories 与 Plum 均采用用户主动粘贴/分享单条链接的产品形态，而非批量爬取。 | 维基百科 https://zh.wikipedia.org/wiki/小红书；App Store https://apps.apple.com/cn/app/id6496861616 与 id771068291 |
| 18 | 定价基准 | insight | 订阅：下厨房 ¥19/月、¥228/年；豆果 ¥19/月、¥198-218/年；懒饭 ¥10-14/月、¥128/年；香哈 ¥6-58 元多档。买断：美食杰VIP ¥8、Plum+ ¥68；Nombook 免费无内购（91 评分 4.97，中文）。 | 各 App Store 页（2026-10 抓取）：id460979760、id434212566、id1377082167、id1098628542、id502803489、id6496861616、id6792402315 |

**来源清单**

- https://sj.qq.com/appdetail/com.xiachufang
- https://sj.qq.com/appdetail/com.douguo.recipe
- https://sj.qq.com/appdetail/com.jesson.meishi
- https://sj.qq.com/appdetail/com.xiangha
- https://apps.apple.com/cn/app/id460979760
- https://apps.apple.com/cn/app/id434212566
- https://apps.apple.com/cn/app/id1377082167
- https://apps.apple.com/cn/app/id1098628542
- https://apps.apple.com/cn/app/id399423833
- https://apps.apple.com/cn/app/id502803489
- https://apps.apple.com/cn/app/id6496861616
- https://apps.apple.com/cn/app/id6792402315
- https://apps.apple.com/us/app/id771068291
- https://itunes.apple.com/lookup?id=771068291&country=cn
- https://itunes.apple.com/lookup?id=6496861616,6702013528,6792402315,6811872580&country=cn
- https://itunes.apple.com/lookup?id=1377082167,460979760,434212566,1098628542&country=cn
- https://www.woshipm.com/ai/6222673.html
- https://www.woshipm.com/it/5513872.html
- https://sspai.com/post/72939
- https://sspai.com/post/77985
- https://www.jiemian.com/article/4048365.html
- https://sspai.com/post/80988
- https://news.qq.com/rain/a/20210714A09SUY00
- https://zh.wikipedia.org/wiki/微信
- https://zh.wikipedia.org/wiki/小红书
- https://www.xiachufang.com/faq/
- https://www.douyin.com/shipin/7304845110289483827

**未决问题**

- 主流菜谱 App 2024-2026 的 MAU/收入未能取得（QuestMobile、七麦均需付费或登录），本文规模判断主要基于 2021H1 数据与商店下载量，需补最新一手数据。
- 2024-2026 的近期差评样本不足：Apple 评论 RSS 对 CN 区返回空、应用宝仅展示 2019-2023 年评论，广告/会员/收藏痛点的新鲜度需用真机评论抓取或用户访谈补齐。
- 小红书/抖音对第三方解析单条分享链接的官方态度与是否存在开放接口，无公开文档可查，需在开发前做合规确认。
- 香哈、豆果的会员真实权益与广告强度（开屏/信息流/激励视频占比）未量化，需安装实测。
- 这些 App 在安卓平板上的真实布局（是否横屏双栏、是否强制竖屏）未实测；现有结论基于 iOS iPad 截图与安卓平板生态的二手报道。
- 懒饭在安卓的官方分发渠道未确认（应用宝搜索首位为仿品包），需查华为/小米/OPPO 商店。
- 厨房故事 Plus 在中国区的具体价格未取得（官网 Plus 页返回 410），只确认其为付费墙功能。

## 海外竞品

**结论摘要**：海外菜谱管理已分成三条路线：老牌买断制（Paprika、Mela、Crouton、Pestle，$4.99–$29.99 一次性）、订阅制内容平台（AnyList $9.99/年、Kitchen Stories €79.99/年、SideChef $49.99/年、Samsung Food+）、开源自托管（Mealie 13.7k stars / Tandoor 8.7k stars）。网页 URL 导入在 2026 年已是及格线，真正的分水岭是"短视频导入"：Pestle、Recipe Keeper、ReciMe、Kitchen Stories 都已支持 Instagram/TikTok/YouTube，ReciMe 凭此拿到 32 万条评分，远超 Paprika 的 5.4 万。但没有任何一家公开导入准确率，且视频转写普遍被放进付费层（Just the Recipe 明写 video transcription 为 Premium，ReciMe 免费版每周限 5 次），说明这是按次计费的重成本能力。离线与数据主权方面，Mela/Crouton 只有 iCloud（Android 天然不可用），AnyList 被用户抱怨没有导出，Mealie/Tandoor 虽有导出但依赖自建服务器且 PWA 离线体验有缺陷。对味记・王 的结论：不做菜谱内容库、不做社区，聚焦"中文短视频→可复现步骤"，买断制、纯本地、第一行代码就定好"一条食谱=一个文件夹"的文件规矩，并把 Android 分享面板作为主入口。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | Paprika 3：买断制的基线 | product | iOS 版 $4.99 一次性买断，美区 54,173 个评分、4.90（App Store，2026-10-30 查询）；macOS 版 $29.99 单独购买（iTunes lookup id=1303222628）；官网原文“each version of Paprika is sold separately”；Android 版在 Google Play 显示免费下载+内购（apkpure 镜像显示 2025-08-01 更新，明显落后于 iOS 2026-07-16 的 3.8.5 版本）。 | https://www.paprikaapp.com/ ；https://itunes.apple.com/lookup?id=1303222628&country=us ；https://apkpure.com/paprika-recipe-manager-3/com.hindsightlabs.paprika.android.v3 （2026-10-30 查询；Android 更新日期来自镜像站，未在官方商店核实） |
| 2 | Paprika 网页导入：只有 URL，没有视频 | feature | 实现方式是在 App 内置浏览器打开网页、点一次保存按钮抓取；支持从其他菜谱 App 导入 .paprikarecipes 文件（Tandoor、Mealie 都把 Paprika 列为可导入来源）。官网功能列表（网页导入、餐计划、购物清单、多图、自动识别计时器、份量缩放、单位换算）中完全没有 TikTok/Instagram/YouTube 导入。 | https://www.paprikaapp.com/ （2026-10-30）；https://docs.tandoor.dev/features/import_export/ 列有 Paprika .paprikarecipes |
| 3 | Mela：原生体验好但只活在苹果生态且停滞 | product | 仅 iOS/macOS；iCloud 同步（可共享食谱库，共享库与浏览器历史不同步）；App Store 免费下载+一次性内购解锁全部功能，无订阅；美区 884 个评分、4.64；最后更新 2025-06-08（截至 2026-10-30 已 16 个月未发版）。 | https://mela.recipes/ ；https://itunes.apple.com/search?term=mela%20recipe%20manager&entity=software&country=us&limit=1 （2026-10-30） |
| 4 | Mela 烹饪模式：值得直接抄的交互 | feature | 全屏烹饪模式放大字号、把非当前步骤变暗、可同时打开多道菜谱、计时器支持 iOS 16.1+ 灵动岛/实时活动；iPad 上内置浏览器与原生预览同屏显示，iPhone 上左右滑动切换；购物清单写在系统 Reminders、餐计划写进系统日历。 | https://mela.recipes/ ；https://itunes.apple.com/search?term=mela%20recipe%20manager （2026-10-30） |
| 5 | Recipe Keeper：功能最接近的全平台对手 | product | 覆盖 iPhone/iPad/Android 手机平板/Windows PC/Mac，支持 Amazon Alexa；网站导入+Instagram 与 TikTok 导入+拍照/照片/PDF 的 OCR 扫描；官网写明“recipes, shopping lists and meal plans…remain available online or offline”；美区 31,104 个评分、4.84。 | https://recipekeeperonline.com/ ；https://recipekeeperonline.com/store/ ；https://itunes.apple.com/search?term=paprika%20recipe&entity=software&country=us&limit=5 （2026-10-30） |
| 6 | Crouton：iPad 双面板与“文件夹=标签” | feature | iPad 支持 Split View、Slide Over 和双面板布局，被 MacStories 评测明确称赞；文件夹实为标签，一条食谱可同时属于多个文件夹；支持份量缩放、分步烹饪、PDF 导出、iCloud 同步与家庭共享；美区 2,932 个评分、4.82，2026-10-26 仍在更新（v2026.5.1）。 | https://www.macstories.net/reviews/crouton-review/ ；https://crouton.app/press-kit.html ；https://itunes.apple.com/search?term=crouton%20recipe&entity=software&country=us&limit=3 （2026-10-30） |
| 7 | Pestle：社交视频导入的现成证据 | feature | App Store 描述明确写支持从 Instagram、TikTok、Facebook、YouTube 导入并保留食材与步骤，另有引导式烹饪、免手提控制、多计时器、Household 共享、拍照扫描；美区 1,840 个评分、4.73；免费；2026-10-28 更新 v4.1.2。 | https://itunes.apple.com/search?term=pestle&entity=software&country=us&limit=1 （2026-10-30，描述由商店元数据摘要） |
| 8 | AnyList：把导入锁进订阅 + 无导出被骂 | pain | AnyList Complete $9.99/年（个人）、$14.99/年（家庭），网页/博客导入、浏览器扩展、份量缩放、网页版与 Mac 版都在付费墙后；美区 81,204 个评分、4.86；Trustpilot 22 条评价（4.0 分）中差评集中在“没有内置导出/备份（清单、食谱、日历都无法导出）”“不同步到小组件除非打开 App”“食谱转清单会重复食材并丢失已保存价格”。 | https://www.anylist.com/complete ；https://www.trustpilot.com/review/anylist.com （2026-10-30） |
| 9 | Samsung Food（原 Whisk）：内容平台不是个人仓库 | product | 免费，美区 6,430 个评分、4.78；定位大食谱库、按食材搜索、周计划、共享购物清单、合作零售商生鲜配送、营养信息与社区；付费 Samsung Food+ 增加 AI 个性化计划、食谱调整、引导式烹饪、库存与营养追踪（官网 samsungfood.com 返回 403，订阅价格未验证）。 | https://itunes.apple.com/search?term=samsung%20food&entity=software&country=us&limit=2 ；https://samsungfood.com/ （403，价格未验证，2026-10-30） |
| 10 | Kitchen Stories：€79.99/年 且“保存网页食谱”收费 | product | Plus 定价 €7.99/月 或 €79.99/年（约 €6.67/月），7 天免费试用；“保存网页上找到的食谱”“按食材搜索”“个性化推荐”“7,500+ 菜谱”均为 Plus 专属；美区 22,781 个评分、4.82。 | https://pages.kitchenstories.com/de/plus ；https://itunes.apple.com/search?term=kitchen%20stories&entity=software&country=us&limit=2 （2026-10-30） |
| 11 | SideChef：引导式视频菜谱已被内容方占满 | product | Premium $4.99/月 或 $49.99/年，7 天试用；18,000 道引导式食谱、800+ 独家菜谱与点播烹饪课；美区仅 1,670 个评分、4.73（评分量远低于 Paprika/Recipe Keeper）。 | https://www.sidechef.com/premium ；https://itunes.apple.com/search?term=sidechef&entity=software&country=us&limit=1 （2026-10-30） |
| 12 | Mealie：开源自托管的互操作标准 | product | AGPL-3.0，GitHub 13,730 stars，2026-10-30 仍有提交；支持 URL 导入、粘贴 HTML/JSON、REST API（demo.mealie.io/docs 可交互）、整站备份/恢复；可直接导入 Paprika、Tandoor、Nextcloud Cookbook、Recipe Keeper、Copy Me That、Plan to Eat 等；默认 SQLite（官方建议 ≤20 用户），PostgreSQL 用于多用户/模糊搜索；PWA 仍有缺陷（GitHub issue：#PWA 首屏 404 仍 open、iOS PWA 烹饪模式只显示食材、离线购物清单状态检测失效）。 | https://mealie.io/ ；https://api.github.com/repos/mealie-recipes/mealie ；GitHub issues 检索（2026-10-30） |
| 13 | Tandoor：自托管+托管订阅的混合收费 | product | GitHub 8,703 stars，开源（API 未标注具体 SPDX 许可证）；官方托管：免费版仅 1 人 10 道食谱；Basic €1.99/月（1 人/1,000 食谱）；Standard €3.49/月促销（原 €4.49）；Premium AI €4.99/月促销（原 €6.49，含 100 AI 点数/月、3GB 上传）；PWA 可安装并“缓存最近访问的食谱供离线使用”。 | https://tandoor.dev/ ；https://docs.tandoor.dev/faq/ （2026-10-30） |
| 14 | Tandoor AI 导入：图片/PDF 成本约 $1/月/人 | tech | AI 可从图片、PDF、纯文本导入食谱，自动排序步骤、把食材分配到步骤、抽取营养等属性；通过 LiteLLM 接 OpenAI、OpenRouter(Gemini)、Google AI Studio 等，也支持自建网关；每个空间默认约 1 美元/月额度、60 请求/小时限流、同步调用可能超时、大文件 base64 可能超反向代理限制；官方明确不支持 URL/视频的 AI 导入。 | https://docs.tandoor.dev/features/ai/ （2026-10-30） |
| 15 | recipe-scrapers：网页解析已被开源覆盖 744 站 | tech | 开源 Python 库 documentation 明写开箱支持 744+ 个食谱网站（Allrecipes、BBC Good Food、Food Network、Jamie Oliver、Serious Eats 等），并有 wild_mode 尝试适配结构相似站点；GitHub 2,256 stars，2026-10-29 仍在提交；Mealie/Tandoor 的网页导入即依赖这一思路（schema.org JSON-LD + 站点适配器）。 | https://docs.recipe-scrapers.com/getting-started/supported-sites/ ；https://api.github.com/repos/hhursev/recipe-scrapers （2026-10-30） |
| 16 | 视频导入是“按次计费”的重成本能力 | insight | ReciMe 免费版每周仅 5 次导入，Premium 才不限量，支持 Instagram/TikTok/Facebook/Pinterest/YouTube 与手写食谱拍照；Just the Recipe 把“video transcription（视频转写）”明确放在 Premium 层；ReciMe 美区 323,755 个评分（4.79），是 Paprika 54,173 的 6 倍。 | https://recime.app/ ；https://itunes.apple.com/search?term=crouton%20recipe&entity=software&country=us&limit=3 （Just the Recipe 描述）；https://itunes.apple.com/search?term=paprika%20recipe （2026-10-30） |
| 17 | 数据可迁移性：导出格式就是用户信任 | feature | Paprika 导出 .paprikarecipes；Tandoor 导出 ZIP（另有 RecipeSage JSON-LD）、并列出可导入 Paprika/Mela/Pestle/Recipe Keeper/Mealie 等 20+ 格式；Mealie 提供整站备份/恢复，但 JSON 导入有“忽略步骤标题、分类、摘要、备注、工具”的 bug（issue #6887，2026-01）；AnyList 用户直接抱怨没有导出。 | https://docs.tandoor.dev/features/import_export/ ；https://mealie.io/ ；https://www.trustpilot.com/review/anylist.com （2026-10-30） |
| 18 | 平板不是放大版手机 | feature | Crouton 的 iPad 双面板 + Slide Over 被评测列为亮点；Mela 在 iPad 上把浏览器与原生预览同屏；Paprika 提供 iPad 分栏与烹饪时屏幕常亮；反观 Mealie/Tandoor 只有响应式 Web，烹饪模式在 iOS PWA 上还出过“只显示食材”的 bug。 | https://www.macstories.net/reviews/crouton-review/ ；https://mela.recipes/ ；GitHub issue “Cook mode in ios PWA only shows ingredients”（2026-09 关闭） |
| 19 | 没有一家同时做到 Android+iPad+纯本地+可选同步 | insight | Paprika 走自建 Paprika Cloud Sync（非 iCloud）；Mela/Crouton 仅 iCloud，Android 天然不可用；AnyList 自建云并绑账号；Mealie/Tandoor 必须自建服务器；Recipe Keeper 自建云同步但要求账号。 | https://www.paprikaapp.com/ ；https://mela.recipes/ ；https://tandoor.dev/ ；https://recipekeeperonline.com/ （2026-10-30） |
| 20 | 平台依赖与链接失效风险 | risk | 抖音/小红书均无公开内容 API，分享短链有效期、反爬与签名策略未公开（未验证）；海外同类（Recipe Keeper、Pestle、ReciMe）处理的是公开网页与用户主动分享的链接，未见批量抓取类功能。 | 未验证/推断（基于 Recipe Keeper、Pestle、ReciMe 商店描述的功能边界；2026-10-30） |
| 21 | 版权与开源许可证 | legal | 海外同类均定位“个人收藏、私有库”，未见公开分享社区功能；Mealie 为 AGPL-3.0（复用其代码会要求整体开源），Tandoor 开源但 GitHub API 未标注具体 SPDX 许可证；各平台 ToS 通常禁止批量抓取（未逐条核实）。 | https://api.github.com/repos/mealie-recipes/mealie ；https://api.github.com/repos/TandoorRecipes/recipes （2026-10-30） |
| 22 | 评分规模：市场是“大而全 + 长尾小而美” | insight | 截至 2026-10-30 美区 App Store 评分数：ReciMe 323,755（4.79）、AnyList 81,204（4.86）、Paprika 54,173（4.90）、Recipe Keeper 31,104（4.84）、Kitchen Stories 22,781（4.82）、Samsung Food 6,430（4.78）、Crouton 2,932（4.82）、Pestle 1,840（4.73）、SideChef 1,670（4.73）、Mela 884（4.64）。 | iTunes Search API 多次查询，2026-10-30 |

**来源清单**

- https://www.paprikaapp.com/
- https://itunes.apple.com/lookup?id=1303222628&country=us
- https://apkpure.com/paprika-recipe-manager-3/com.hindsightlabs.paprika.android.v3
- https://mela.recipes/
- https://itunes.apple.com/search?term=mela%20recipe%20manager&entity=software&country=us&limit=1
- https://recipekeeperonline.com/
- https://recipekeeperonline.com/store/
- https://crouton.app/
- https://crouton.app/press-kit.html
- https://www.macstories.net/reviews/crouton-review/
- https://itunes.apple.com/search?term=pestle&entity=software&country=us&limit=1
- https://www.anylist.com/complete
- https://www.trustpilot.com/review/anylist.com
- https://itunes.apple.com/search?term=samsung%20food&entity=software&country=us&limit=2
- https://pages.kitchenstories.com/de/plus
- https://www.sidechef.com/premium
- https://recime.app/
- https://mealie.io/
- https://api.github.com/repos/mealie-recipes/mealie
- https://tandoor.dev/
- https://docs.tandoor.dev/features/import_export/
- https://docs.tandoor.dev/features/ai/
- https://docs.tandoor.dev/faq/
- https://docs.recipe-scrapers.com/getting-started/supported-sites/
- https://api.github.com/repos/hhursev/recipe-scrapers
- https://en.wikipedia.org/wiki/Paprika_(app)
- https://itunes.apple.com/search?term=crouton%20recipe&entity=software&country=us&limit=3

**未决问题**

- 没有任何一家公开“网页/视频导入准确率”的量化数据（只宣传“hundreds of sites”“thousands of websites”）。建议自建 50–100 条测试集（抖音/小红书/B站/下厨房/YouTube/Allrecipes），人工标注食材与步骤字段，才能把“准确率”变成可验证指标。
- Mela、Crouton、Pestle、Recipe Keeper、Samsung Food+、ReciMe 的内购/订阅价格未在官网公开，App Store 商品页在本环境被重定向到 Today 页无法读取 IAP 列表；需用美区真机截图或 App Store Connect 公开页人工核实。
- 抖音/小红书分享链接的有效期、是否需要登录态、反爬与签名机制均未验证；需要实测 30 条不同账号的分享链接（含图文、视频、图文+视频三种）在 7 天/30 天后的可解析率。
- 视频转写的实际单位成本未知（ASR/多模态模型按分钟计价，且中文口播菜谱术语多）。需要用 20 条真实视频测算云端 vs 端侧（whisper.cpp / Android 端侧模型）的成本与准确率差异。
- 本环境的 WebSearch 工具对所有查询返回空结果（多次尝试，含中英文），本次调研改用官方站点、iTunes Search API、GitHub API、厂商文档与 Trustpilot 交叉核实；搜索引擎可用的环境下应补一轮 2025–2026 年的第三方横评（The Verge 2025-11 烹饪 App 盘点、Wirecutter 等）以补齐主观口碑样本。

## 收藏痛点

**结论摘要**：中文社区的真实声音高度一致：抖音/小红书收藏的做饭内容，绝大多数停在「收藏」这一步，用户自己称之为「数字坟场」「收藏的菜谱，不曾开过锅」。失败原因不是不想做，而是四段断链：找不到（收藏越多越搜不到）、看不懂（步骤散落在字幕/口播/评论区，视频线性、要反复拖进度条）、做不了（缺调料缺工具、双手沾油碰不了屏幕、熄屏打断）、接不上（收藏不解决「今天吃什么」、采购清单和份量换算缺位）。现有 workaround——备忘录、Excel、截图存相册、微信文件传输助手、Notion 模板、纸质手账本——证明需求真实且高频，但都只解决「存」，不解决「执行」。2025-2026 年同向产品密集出现（一键大厨 ¥128/年、谱提Pullit、一口计划、开饭啦、下周吃什么），窗口正在打开但竞争正在变密；其中一键大厨公开承认 AI 解析准确率是最大体验风险。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | 收藏夹=数字坟场：收藏行为替代了做菜行为 | pain | 知乎原话：「收藏的知识，没有时间学习；收藏的景点，到不了的远方；收藏的菜谱，不曾开过锅」（2019-10-28，16 赞）。另一条（2025-09-19）：「看到『懒人必备二十道蒸菜』就顺手点收藏，转头该点外卖还是点外卖……我们收藏的其实是『别人的结果』，不是『自己能做的菜』」。小红书《在🍠学到的收藏夹不吃灰大法（合集版）》3,144 赞、《0难度 让你无痛整理收藏夹 J人至爽AI版》7,363 赞（2026-08-23），说明「收藏整理」本身已成一个内容品类。频率：刷内容时每天数次；痛点强度：中（刺痛感强但不紧急）。 | https://zhuanlan.zhihu.com/p/45204157（2019-10-28）；https://zhuanlan.zhihu.com/p/2084500565388804213（2025-09-19）；小红书搜索「收藏夹太多了怎么整理」页（2026-08~10） |
| 2 | 收藏的视频会凭空消失（平台侧失效） | pain | 「抖音收藏的视频不见了」是长期高频求助话题，原因包括发布者删除视频、博主修改可见权限、视频违规被限制（太平洋科技 2023-04-19、3DM 2021-07-23、卡饭网 2021-07-22 等多家媒体反复写同一篇科普，说明搜索需求稳定）。用户最想做饭那一刻点开链接，得到的是「内容不存在」。频率：每次复做都可能遇到，不可预测；痛点强度：高（任务失败且无法补救）。 | 搜狗检索「抖音收藏的视频不见了」：太平洋科技 2023-04-19；3DM手游 2021-07-23；卡饭网 2021-07-22（原文经搜狗跳转，未逐篇打开全文核实） |
| 3 | 视频是线性的，做饭是来回确认的 | pain | 同赛道产品方公开复盘：「食材可能出现在开头字幕里，用量藏在博主的口播中，调料比例则可能要去评论区寻找」「刚才放了多少盐？这个步骤要煮几分钟？收汁应该收到什么状态？下一步是先放蒜，还是先放辣椒？为了找到某个几秒钟的片段，只能不断拖动进度条」（一键大厨开发者，2026-08-27；V2EX 帖 2,282 次点击、27 条回复）。用户实际动作是：暂停、截图、再把食材抄下来。频率：每次跟做新菜；痛点强度：高。 | https://zhuanlan.zhihu.com/p/2076397545723634904（2026-08-27）；https://www.v2ex.com/t/1237565（2026-08-27） |
| 4 | 双手沾油/面粉时碰不了屏幕，还会自动熄屏 | pain | 抖音笔记原话：「知道大家做菜的时候手上都是油不方便看菜谱」（2024-12-15）；B站/腾讯视频长期有「做饭怕弄脏屏幕」妙招内容（2018-2022）。V2EX 用户对隔空手势方案的第一条反馈就是：「一会儿就自动熄屏了，还不是得去点手机」（2025-08-08）。另有公众号记录用豆包语音当厨房助手「不用碰屏幕，开口就能问」（2026-10-22）。频率：每次做饭 5-30 次交互；痛点强度：高。 | https://www.douyin.com/note/7448501624319872256（2024-12-15）；https://www.v2ex.com/t/1151057（2025-08-08）；搜狗检索「做饭时看手机菜谱 手脏」结果（2026-10-22 公众号） |
| 5 | 「今天吃什么」比「怎么做」更难，收藏夹不解决选菜 | insight | V2EX 跟帖：「难的不是做菜。难的是选择做什么菜，『今天吃什么』就是个大难题啊」，并建议按时令（如大闸蟹上市）反推菜品（2026-08-27）。知乎同向：「下次需要安排晚饭时，先看已经做过的几道，就不必重新在大量收藏中寻找答案」（2025-10-26）。频率：每天 1-3 次决策；痛点强度：高（决策成本高于执行成本）。 | https://www.v2ex.com/t/1237565（2026-08-27，用户 justdoitzZ）；https://zhuanlan.zhihu.com/p/2098145207464227918（2025-10-26） |
| 6 | 备菜与采购断链，收藏夹里没有购物清单 | pain | V2EX 首条回复：「做菜这件事，最麻烦的事情在于备菜和饭后收拾，对于懒人来说，这是阻止菜从屏幕里走到饭桌上最大的阻碍，做菜本身或者菜谱之类的，反而是一件简单的事」。知乎长文同样写到：站到厨房才发现「缺一种调料、少一个工具」，并建议买菜前把步骤看完、把「想买的」和「这次确实缺的」分开（2025-10-26）。频率：每周 1-3 次采购；痛点强度：高。 | https://www.v2ex.com/t/1237565（2026-08-27，用户 javalaw2010）；https://zhuanlan.zhihu.com/p/2098145207464227918（2025-10-26） |
| 7 | 份量换算缺失：视频只给适量，家里是 1-2 人 | pain | 社区存在大量份量换算求助与工具：在线「配方缩放/食谱份量换算器」（esk365）、CSDN 上完整的「食谱份量换算 App」实现教程（2026-07-24）、知乎「食物重量怎么简单换算」问答（2023-05-18）、Notion 模板社区讨论热量表自动计算（B站 2023-06-13、抖音 2024-01-27）；V2EX 有「求一人食快手菜」帖（2019-11-19）。频率：每次做新菜；痛点强度：中高（份量错=失败）。 | CSDN《鸿蒙 ArkTS 实战：Recipe Serving Scaler》2026-07-24；esk365 配方缩放工具；https://www.so.com/s?q=Notion 菜谱 模板（B站 2023-06-13、抖音 2024-01-27） |
| 8 | 重复做同一道菜时，上次的改动记不住 | pain | 知乎建议：做完记录两三条具体信息——实际用了多久、分量是否合适、哪一步要提前准备，并标成「愿意再做/有空再研究/暂时不适合」；「愿意再做的，附上适合的分量和自己的注意事项」（2025-10-26）。小红书上大量「自制家庭菜单/私房菜库/点菜自由」笔记（《自制家庭食谱｜实现点菜自由》536 赞、《我的食谱本》1,201 赞），是同一需求的手工版。频率：每周 2-4 次做熟菜；痛点强度：中。 | https://zhuanlan.zhihu.com/p/2098145207464227918（2025-10-26）；小红书搜索「手写菜谱本」页笔记（2025-05~2026-10） |
| 9 | 现有 workaround 矩阵：备忘录/Excel/截图/文件传输助手/Notion/手账，都只解决「存」 | insight | 小红书笔记标题即证据链：《小红书菜谱怎么完整保存？备忘录太好用了》（2026-07-29）、《可视化电子菜谱（备忘录版）》373 赞、《j人的EXCEL菜品食谱》29 赞、《用Gemini3搭建家庭菜谱》1,029 赞、《小红书食谱收藏分类大法》354 赞；微信侧《原来微信的文件传输助手才是打工人真正的备忘录》6,258 赞（2026-03-04）。Notion 有成熟模板生态（B站《用 Notion 管理厨房菜谱和一周烹煮计划（免费模板）》2023-06-13；抖音 Notion 菜谱模板 2024-01-27）。手工侧《DIY手工菜谱//封面版》3,371 赞、《菜谱手账》849 赞。共同缺陷：无结构、不可检索、不能换算份量、不能生成采购清单、不能跨手机/平板同步。 | https://www.xiaohongshu.com/search_result?keyword=菜谱怎么整理（2026-03~10）；https://www.xiaohongshu.com/search_result?keyword=手写菜谱本；微信笔记《原来微信的文件传输助手才是打工人真正的备忘录》6,258 赞（2026-03-04） |
| 10 | 付费意愿有实证，价格带被锚定在 ¥18/月、¥128/年 | insight | 「一键大厨 - AI视频转菜谱」App Store 中国区：免费下载，内购「高级会员（月付）¥18.00」「高级会员（年付）¥128.00」，评分 5.0（仅 4 个评分），版本 2.6.0，最近更新 10 月 7 日（2026）。推广方式是评论区留用户 ID 送一个月 Pro；V2EX 帖 2,282 次点击、27 回复（2026-08-27~09-01）。同期新玩家密集：谱提Pullit「上线两周 200+ 用户创建食谱书」（2026-03-18）、一口计划「内测用户突破 500+」（2026-10-27）、开饭啦「收藏了300个菜谱，真正做过的有几个？」（2026-10-10）、下周吃什么「小红书导入菜谱已上线」（2026-08-01）。 | https://apps.apple.com/cn/app/id6748383363（2026-10-07 更新）；https://www.v2ex.com/t/1237565（2026-08-27）；小红书笔记《谱提Pullit 上线两周》《一口计划内测 500+》（2026-03/2026-10） |
| 11 | AI 从视频抽菜谱的准确率是体验生死线 | tech | 同赛道开发者自述：「不同视频的拍摄方式、字幕质量、口音、剪辑节奏差别很大。有些视频会完整列出食材和用量，有些则只展示过程，甚至省略关键步骤。因此 AI 整理出的食材、用量和步骤仍然可能出现遗漏或者理解错误」，并把「哪些类型的视频最容易解析失败」「用户是否能方便地发现和修正错误」列为重点观察项（2026-08-27）。 | https://zhuanlan.zhihu.com/p/2076397545723634904（2026-08-27）；https://www.v2ex.com/t/1237565（2026-08-27） |
| 12 | 合规红线：抓取平台内容与二次分发 | legal | 用户导入的是抖音/小红书的视频与图文，涉及其用户协议对抓取/下载的限制，以及博主对画面与口播的著作权。可核实的相关事实是：平台内容会因发布者删除、权限修改、违规而下架（多家媒体科普，2021-2023），说明链接本身不是稳定资产。国内同类产品普遍采用用户粘贴链接或上传本地视频的被动导入（一键大厨 V2EX 帖 2026-08-27），而非自建抓取库——这一做法可视为行业规避姿态（推断，未验证其法律依据）。 | 搜狗检索「抖音收藏的视频不见了」（太平洋科技 2023-04-19 等）；https://www.v2ex.com/t/1237565（2026-08-27）；法律定性为推断，未咨询律师 |
| 13 | 平板是厨房里的第二块屏，但现有 App 基本没为它设计 | product | 真实摆放是支架横屏、距离 50-100cm、需要大字与常亮。可核实的相邻证据：熄屏中断是真实抱怨（V2EX 2025-08-08）；「旧手机不卖不扔改用途，厨房看菜谱更省事」（企鹅号，检索于 2026）。同赛道产品均以 iPhone 为主（一键大厨 App Store 页 2026-10-07 仅列 iOS），Android 平板形态基本空白（推断）。 | https://www.v2ex.com/t/1151057（2025-08-08）；https://apps.apple.com/cn/app/id6748383363（2026-10-07）；平板形态空白为推断 |

**来源清单**

- https://zhuanlan.zhihu.com/p/2098145207464227918 — 知乎《收藏了很多菜谱，却总没动手，怎样挑一道真正做起来？》luoting，2025-10-26
- https://zhuanlan.zhihu.com/p/2076397545723634904 — 知乎《收藏了上百个做饭视频，为什么我们还是很少真正照着做？》一键大厨开发者 ShaperDev，2026-08-27
- https://zhuanlan.zhihu.com/p/2084500565388804213 — 知乎《收藏了几十个蒸菜方子还是不会做？》，2025-09-19
- https://zhuanlan.zhihu.com/p/45204157 — 知乎《收藏狂魔——码住！然后再也不看。》林作尘，2019-10-28（16 赞）
- https://www.zhihu.com/question/2406802602/answer/1921610344083724083 — 知乎回答「收藏了五百多个菜谱」，2025-06-26
- https://www.v2ex.com/t/1237565 — V2EX《收藏了一堆做饭视频却从没真正做过？我们做了『一键大厨』》，2026-08-27，2,282 次点击/27 回复
- https://www.v2ex.com/t/1151057 — V2EX《这个菜谱 app 可能是同类应用里最好用的？》，2025-08-08（熄屏反馈）
- https://apps.apple.com/cn/app/id6748383363 — 一键大厨 App Store 中国区页（¥18/月、¥128/年，2.6.0，2026-10-07 更新）
- https://www.sogou.com/web?query=抖音收藏的视频不见了 — 搜狗检索页：太平洋科技 2023-04-19、3DM 2021-07-23、卡饭网 2021-07-22 等
- https://www.xiaohongshu.com/search_result?keyword=收藏夹太多了怎么整理 — 小红书站内搜索页（《0难度 无痛整理收藏夹》7,363 赞、《在🍠学到的收藏夹不吃灰大法》3,144 赞，2026-05~10）
- https://www.xiaohongshu.com/search_result?keyword=菜谱怎么整理 — 小红书站内搜索页（备忘录版菜谱 373 赞、Gemini 家庭菜谱 1,029 赞、饭点/谱提Pullit/下周吃什么等工具笔记，2026-03~10）
- https://www.xiaohongshu.com/search_result?keyword=手写菜谱本 — 小红书站内搜索页（《DIY手工菜谱//封面版》3,371 赞、《我的食谱本》1,201 赞，2025-05~2026-10）
- https://www.xiaohongshu.com/search_result?keyword=文件传输助手 收藏 菜谱 — 小红书站内搜索页（《原来微信的文件传输助手才是打工人真正的备忘录》6,258 赞，2026-03-04）
- https://www.douyin.com/note/7448501624319872256 — 抖音笔记「做菜的时候不方便看手机该怎么办」，2024-12-15（经搜狗检索命中）
- https://www.sov2ex.com/api/search?q=一键大厨 — V2EX 全站搜索 API（话题 1237565、1151057 索引）
- CSDN《鸿蒙 ArkTS 实战：Recipe Serving Scaler 食谱份量换算》，2026-07-24（经 360 搜索命中，URL 未固定）

**未决问题**

- 抖音单个收藏夹/收藏总量的真实上限是多少？搜索只见到「5000」「50」等标题党说法，无官方口径（未验证），需产品内实测或查官方帮助中心。
- 小红书「收藏专辑」的数量与单专辑条目上限、以及导出可行性（是否有 API / 网页端能否批量导出）未验证；已有第三方开源导出工具在传播（2026-07 公众号），合规性与稳定性未知。
- 用户对 AI 解析错误的容忍阈值是多少（例如 10 条错 2 条是否还能留住）？需 5-10 人可用性测试，而不是看竞品自述。
- Android 平板在目标用户中的持有率与厨房使用比例未知；「平板横屏工作台」是强假设，需 3-5 户家庭访谈验证。
- 个人用户真实付费上限：竞品锚定 ¥18/月、¥128/年，但那是 iOS 工具类定价；Android 个人工具是否接受买断制（¥30-68）缺少数据。
- 是否需要内置公共菜谱库做冷启动？V2EX 有用户直接质疑「菜谱没有官方分享的吗，得自己去找视频然后生成」，但这与本地优先定位及版权风险冲突，需要取舍。
- 微信文件传输助手/备忘录的存量内容能否低成本导入（截图 OCR、文本解析）决定冷启动体验，目前只有定性证据。

## 技术栈

**结论摘要**：结论明确：味记・王 只做 Android，应选 100% Kotlin + Jetpack Compose + Material3 adaptive 的原生方案，不选 Flutter，也不要在第一版引入 KMP/iOS。核心理由是它重度依赖系统能力（分享接收、MediaStore、Media3、WorkManager、前台服务），而这些能力在 Flutter 里全要写平台通道，还额外背上 Impeller/Dart 引擎的包体与 16KB 合规成本。平台侧的硬约束已经排到 2027 年：Play 2026-08-31 起新应用与更新必须 target API 36；target 36 起 edge-to-edge 不能退出、平板 sw≥600dp 忽略屏幕方向与固定宽高比；API 37 连这个 opt-out 都会被移除，并且局域网访问（WebDAV/NAS）变成强制运行时权限。数据侧的正确切分是：Room 存结构化数据、视频进 MediaStore、导出走 SAF、云备份只保 DB（25MB 硬上限）。剪贴板后台监听在 Android 10+ 已被彻底封死，"后台自动收菜谱"这条产品设想不能成立，入口必须收敛到分享面板 + 前台粘贴 + Direct Share 置顶三条路。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | Kotlin + Jetpack Compose 是 Google 官方路线 | tech | Google 官方数据：Kotlin 被 60%+ 专业 Android 开发者使用，Kotlin 应用崩溃率低 20%；Compose 被称为 Android 的现代 UI 工具包，官方明确写 'Use Compose for UI and adaptive app development to support multiple device form factors'（developer.android.com/kotlin，2026-10-30 抓取）。 | https://developer.android.com/kotlin（2026-10-30 抓取） |
| 2 | Flutter 3.47 的现状与代价 | tech | Flutter 官方文档页脚标注反映 Flutter 3.47（页面更新 2026-10-24），支持 Android/iOS/Web/Windows/macOS/Linux，原生平台使用 Impeller 渲染、Dart AOT 编译；官方 app size 页只给了 Flutter 1.17 时代 iOS 5.4MB 压缩/13.7MB 未压缩的旧样例，没有 Android 的实测数字。 | https://docs.flutter.dev/overview；https://docs.flutter.dev/perf/app-size（2026-10-30 抓取） |
| 3 | Compose Multiplatform 1.12.2（保留 iOS 退路） | tech | GitHub Releases 显示最新稳定版 1.12.2（10 月 15 日发布），1.13.0-beta01/alpha02 为预发布；CM 是 JetBrains 对 Jetpack Compose 的跨平台扩展，Android 端直接复用 Google 的 androidx.compose 产物。JetBrains 关于 iOS 稳定性的博文抓取返回 403，未能核实具体表述。 | https://github.com/JetBrains/compose-multiplatform/releases；https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html（2026-10-30） |
| 4 | ACTION_SEND / ACTION_SEND_MULTIPLE 接收契约 | tech | 官方接收分享文档：ACTION_SEND 为单条、ACTION_SEND_MULTIPLE 为多条；按 MIME 分别声明 text/plain、image/*、video/* 的 intent-filter；内容取 EXTRA_TEXT 与 EXTRA_STREAM（单个用 IntentCompat.getParcelableExtra，多个用 getParcelableArrayListExtra，API 33 起无参 getParcelableExtra 已废弃）；官方警告不要声明 */*，除非真能处理任意类型；Android 12+ 带 intent-filter 的组件必须显式 android:exported。 | https://developer.android.com/training/sharing/receive（2026-10-30 抓取） |
| 5 | 分享必然拉起前台 Activity，没有静默后台接收 | tech | 分享目标本身是一个 exported Activity，由系统 startActivity 拉起并显示在 Sharesheet / intent resolver 中；Android 没有提供「后台静默接收分享」的官方通道。 | https://developer.android.com/training/sharing/receive（据接收模型推断） |
| 6 | Direct Share / Sharing Shortcuts 置顶能力 | tech | 用 res/xml/shortcuts.xml 的 <share-target> + ShortcutInfoCompat.setLongLived(true)、setCategories 与 manifest 中的 category 对应，再用 ShortcutManagerCompat.pushDynamicShortcut 发布；每个 Activity 的动态快捷方式上限通常为 5 个，应用 ShortcutManagerCompat.getMaxShortcutCountPerActivity() 查询而非硬编码；ChooserTarget/ChooserTargetService 自 API 30 起废弃；Android 10+ Sharesheet 只认 application 标签上的图标。 | https://developer.android.com/training/sharing/receive（2026-10-30 抓取） |
| 7 | 分享大概率只给短链，不给视频文件 | insight | 按官方模型，纯文本分享走 EXTRA_TEXT，图片/视频才走 EXTRA_STREAM。抖音/小红书分享面板实际塞了什么（纯链接 or 附带视频 Uri）本轮无法实测；但常规做法是分享短链（如 v.douyin.com/xxx）+ 标题。 | https://developer.android.com/training/sharing/receive；平台实际行为未验证（推断） |
| 8 | App Links 无法接管抖音/小红书域名 | legal | Android App Links 的自动验证要求目标域名托管 assetlinks.json 且与本应用签名匹配；douyin.com、xiaohongshu.com 不由我们控制，无法完成验证，只能退化为带选择器的普通 deep link（Android 12+ 起未验证的链接默认不自动打开）。 | Android App Links 验证机制（本轮未逐条抓取，推断） |
| 9 | 剪贴板后台读取被系统封死（Android 10+） | risk | 官方原文：'Unless your app is the default input method editor (IME) or is the app that currently has focus, your app cannot access clipboard data on Android 10 or higher.' 该限制位于 Permissions 章节，对所有应用生效，与 targetSdkVersion 无关。 | https://developer.android.com/about/versions/10/privacy/changes（2026-10-30 抓取） |
| 10 | 应用专属外部目录：免权限但卸载即删 | tech | getExternalFilesDir() 典型路径 /storage/emulated/0/Android/data/<pkg>/files/，API 19+ 免存储权限；Android 10+ 其他应用无法访问；卸载应用即删除；官方明确内部 cache 可能在存储不足时被系统删除，且「内部存储空间有限，大量数据请用其他类型」。 | https://developer.android.com/training/data-storage/app-specific；https://developer.android.com/training/data-storage（2026-10-30） |
| 11 | MediaStore：大视频的正确归宿 | tech | 写入 MediaStore.Video.Media（VOLUME_EXTERNAL_PRIMARY）+ RELATIVE_PATH=Movies/味记王 + IS_PENDING=1→写字节→置 0；自己创建的媒体在 Android 10+ 无需存储权限；官方明确 'Even after your app is uninstalled, these files remain on the user's device'，但重装后归属丢失，需 READ_EXTERNAL_STORAGE 才能访问旧文件。 | https://developer.android.com/training/data-storage/shared/media（2026-10-30 抓取） |
| 12 | SAF 是导出与逃生通道 | tech | Storage Access Framework 走系统文件选择器、无需权限、卸载不删文件，官方把它列为「文档、下载等可共享文件」的方案；与 MediaStore 的区别是 SAF 面向任意目录与文档，MediaStore 面向媒体索引。 | https://developer.android.com/training/data-storage（2026-10-30 抓取） |
| 13 | Room 3.0.3 作为本地单一事实源 | tech | Room 官方页当前示例为 room 3.0.3，且 'Room 3.0 requires KSP for annotation processing'；提供编译期 SQL 校验、suspend DAO、迁移路径。官方 offline-first 指南要求 'The local data source is the canonical source of truth'，UI 只读本地 Flow，网络结果先写本地再暴露。 | https://developer.android.com/training/data-storage/room；https://developer.android.com/topic/architecture/data-layer/offline-first（2026-10-30） |
| 14 | 中文菜名/食材搜索的分词坑 | tech | SQLite FTS 默认分词对中文按空白切分不友好，中文检索通常需要 trigram 分词或自建倒排；Room 提供 @Fts4 虚拟表，但其分词器能力有限，FTS5/trigram 在 Room 中的支持本轮未核实。 | 未验证（SQLite/Room FTS 中文分词，推断） |
| 15 | Media3 ExoPlayer 承担播放 | tech | Media3 的 Player 接口 + ExoPlayer 默认实现，相比 MediaPlayer 增加多 streaming 协议、默认音视频 renderer、缓冲组件，且可随 Play 更新；媒体可本地或网络；文档页更新于 2026-10-21；后台播放走 MediaSessionService。 | https://developer.android.com/media/media3/exoplayer（2026-10-30 抓取） |
| 16 | 抽帧封面：优先 getScaledFrameAtTime | tech | MediaMetadataRetriever.getScaledFrameAtTime(timeUs, option, dstW, dstH) 自 API 27 起可直接解码到目标尺寸，避免先产出全尺寸 Bitmap 再缩放导致 OOM；OPTION_CLOSEST_SYNC 快但不保证精确时间点，OPTION_CLOSEST 更准但更慢；抽帧开销大，应在后台线程执行并及时 release()。 | https://developer.android.com/reference/android/media/MediaMetadataRetriever（API 参考，页面文本被截断，部分为通用 API 知识） |
| 17 | Media3 Transformer 只用于按需裁剪/转码 | tech | Transformer 支持转码/转封装（输入输出同格式时自动 transmux，不重编码）、setRemoveAudio、MediaItem.ClippingConfiguration 裁剪、缩放旋转与 OpenGL 特效；trim-only 的 MP4 edit list 与 trim optimization 仍标注为 experimental（文档页 2026-09-08 更新），且 edit list 可能把「删掉的内容」藏在 pre-roll 里造成隐私风险。 | https://developer.android.com/media/media3/transformer/transformations（2026-10-30 抓取） |
| 18 | 图片压缩：WebP 为主 | tech | Bitmap.compress(format, quality 0–100, stream)；JPEG/PNG 自 API 1，WEBP 自 API 14，WEBP_LOSSY/WEBP_LOSSLESS 自 API 30；PNG 与 WEBP_LOSSLESS 会忽略 quality 参数。 | https://developer.android.com/reference/android/graphics/Bitmap（API 参考，文本被截断，格式与版本为通用 API 知识） |
| 19 | Material 3 adaptive 三件套 | tech | currentWindowAdaptiveInfo().windowSizeClass 分 compact/medium/expanded；NavigationSuiteScaffold 自动在 navigation bar 与 navigation rail 间切换；ListDetailPaneScaffold 做列表-详情（expanded 并排，compact 单栏）；SupportingPaneScaffold 做主内容 + 辅助面板；折叠屏用 Jetpack WindowManager 的 WindowInfoTracker/FoldingFeature 与 windowPosture；Compose 1.7+ 默认支持键盘 Tab 与鼠标/触控板点击滚动。 | https://developer.android.com/develop/ui/compose/layouts/adaptive（2026-10-30 抓取） |
| 20 | Android 16/17 强制自适应与 edge-to-edge | legal | targeting API 36：sw≥600dp 屏幕上 screenOrientation、resizableActivity、min/maxAspectRatio 及 setRequestedOrientation 全部被忽略，应用填满窗口；该 opt-out 属性在 API 37 彻底移除。targeting 36 起无法退出 edge-to-edge；predictive back 默认开启且 onBackPressed 不再被调用。Play 要求 2026-08-31 起新应用与更新必须 target API 36（存量应用 API 35，可申请延至 2026-11-01）。 | https://developer.android.com/about/versions/16/behavior-changes-16；https://developer.android.com/about/versions/17/behavior-changes-17；https://support.google.com/googleplay/android-developer/answer/11926878（2026-10-30） |
| 21 | 大屏质量分级 Tier 1/2/3 | insight | Google 分三档：Tier 3 adaptive ready（不 letterbox、全窗口运行）、Tier 2 adaptive optimized（所有尺寸布局优化 + 键鼠/触控笔增强）、Tier 1 differentiated（多任务、折叠姿态、拖放、手写笔）；Play 大屏徽章要求至少 Tier 2。测试尺寸建议 8 英寸 1024×640dp、10.5 英寸 1280×800dp、折叠 841×701dp、13 英寸 Chromebook 1600×900dp。 | https://developer.android.com/docs/quality-guidelines/large-screen-app-quality（2026-10-30 抓取） |
| 22 | WorkManager 承担全部重活 | tech | WorkManager 用于可延迟但必须可靠的后台工作，任务持久化到内部 SQLite、重启后重排；expedited 适合「几分钟内完成」；超过 10 分钟的长任务需在 Worker 里 setForeground()；Android 15 起 dataSync 与 mediaProcessing 前台服务各限 6 小时/24 小时，超时抛 ForegroundServiceStartNotAllowedException；Android 16 起与前台服务并发的 Job 也受配额约束。 | https://developer.android.com/topic/libraries/architecture/workmanager；https://developer.android.com/about/versions/15/behavior-changes-15；https://developer.android.com/about/versions/16/behavior-changes-all |
| 23 | Auto Backup 只有 25MB 硬上限 | tech | 每个应用每用户最多 25MB 云备份，不占用户 Drive 配额，只保留最近一份；超限触发 onQuotaExceeded 且完全不上传；默认包含 sharedpref、getFilesDir、数据库、getExternalFilesDir，永远排除 cache；Android 12+ 用 dataExtractionRules，可设 disableIfNoEncryptionCapabilities=true，且 device-transfer 规则可单独放宽。 | https://developer.android.com/identity/data/autobackup（2026-10-30 抓取） |
| 24 | 同步路径：WebDAV 优先，托管同步次之 | tech | Nextcloud WebDAV：根为 /remote.php/dav，用户文件在 /remote.php/dav/files/{user}/...，Basic Auth 或 app password，PROPFIND 列目录、PUT 上传、MKCOL 建目录，只有文件语义没有记录级同步；Electric 为 Apache 2.0 的 Postgres→HTTP 同步引擎（站点已宣布 joining Neon at Databricks）；PowerSync 提供本地 SQLite 双向同步，SDK 列表含 Kotlin Multiplatform；Supabase 的本地开发文档未提供移动端离线同步方案。 | https://docs.nextcloud.com/server/latest/developer_manual/client_apis/WebDAV/basic.html；https://electric.ax/；https://www.powersync.com/；https://supabase.com/docs/guides/local-development（2026-10-30） |
| 25 | 「先不做云」的真实代价 | risk | 无云端时：换机/丢机只能靠 25MB Auto Backup 或手动 zip；没有多端实时；没有分享协作；用户数据锁在本机，卸载即失去 app-specific 目录里的全部文件。 | 综合 https://developer.android.com/identity/data/autobackup 与 app-specific 存储文档（推断） |
| 26 | Android 17 局域网权限会咬 WebDAV | risk | Android 16 本地网络保护为 opt-in；Android 17（API 37）起对 target 37 的应用强制，访问局域网地址的 TCP/UDP 均需 ACCESS_LOCAL_NETWORK（属 NEARBY_DEVICES 权限组），仅发往 53 端口的 DNS 豁免；被拒后 TCP 表现为超时、UDP 表现为 EPERM。 | https://developer.android.com/privacy-and-security/local-network-permission；https://developer.android.com/about/versions/17/behavior-changes-17（2026-10-30） |
| 27 | 16KB 页大小与纯 Kotlin 的红利 | legal | Google Play：2027-02-01 起，target API 35+ 且含 64 位原生库的应用更新必须支持 16KB 页，否则无法发布；纯 Java/Kotlin（含全部依赖）天然满足，含 .so 的需 AGP 8.5.1+ / NDK r28+ 重新编译。 | https://developer.android.com/guide/practices/page-sizes（2026-10-30 抓取） |
| 28 | 包体与启动：R8 + Baseline Profile | tech | R8 full mode 自 AGP 8.0 起默认；optimized resource shrinking 在 AGP 8.12 可 opt-in、AGP 9.0 起默认，官方称对跨形态共享大量资源的应用实测可减小 50%+ 体积；AGP 9.3 提供 optimization { enable = true } 新 DSL；官方建议为启动创建 Startup/Baseline Profile。 | https://developer.android.com/topic/performance/app-optimization/enable-app-optimization（2026-10-30 抓取） |
| 29 | 「边做边看」需要的前台能力 | feature | Media3 MediaSessionService 支持后台播放；Android 17 起后台音频交互必须有前台服务且满足 while-in-use 或精确闹钟 + USAGE_ALARM；Android 15 起请求音频焦点需处于前台或运行前台服务，否则返回 AUDIOFOCUS_REQUEST_FAILED。 | https://developer.android.com/media/media3/exoplayer；https://developer.android.com/about/versions/17/behavior-changes-17；https://developer.android.com/about/versions/15/behavior-changes-15 |

**来源清单**

- https://developer.android.com/kotlin
- https://docs.flutter.dev/overview
- https://docs.flutter.dev/perf/app-size
- https://github.com/JetBrains/compose-multiplatform/releases
- https://kotlinlang.org/docs/multiplatform/compose-multiplatform-and-jetpack-compose.html
- https://developer.android.com/training/sharing/receive
- https://developer.android.com/training/sharing/send
- https://developer.android.com/about/versions/10/privacy/changes
- https://developer.android.com/training/data-storage
- https://developer.android.com/training/data-storage/app-specific
- https://developer.android.com/training/data-storage/shared/media
- https://developer.android.com/training/data-storage/room
- https://developer.android.com/topic/architecture/data-layer/offline-first
- https://developer.android.com/media/media3/exoplayer
- https://developer.android.com/media/media3/transformer/transformations
- https://developer.android.com/reference/android/media/MediaMetadataRetriever
- https://developer.android.com/reference/android/graphics/Bitmap
- https://developer.android.com/develop/ui/compose/layouts/adaptive
- https://developer.android.com/docs/quality-guidelines/large-screen-app-quality
- https://developer.android.com/about/versions/15/behavior-changes-15
- https://developer.android.com/about/versions/16/behavior-changes-16
- https://developer.android.com/about/versions/16/behavior-changes-all
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developer.android.com/guide/practices/page-sizes
- https://developer.android.com/topic/libraries/architecture/workmanager
- https://developer.android.com/identity/data/autobackup
- https://developer.android.com/topic/performance/app-optimization/enable-app-optimization
- https://support.google.com/googleplay/android-developer/answer/11926878
- https://docs.nextcloud.com/server/latest/developer_manual/client_apis/WebDAV/basic.html
- https://electric.ax/
- https://www.powersync.com/
- https://supabase.com/docs/guides/local-development

**未决问题**

- 抖音/小红书分享面板实际塞进 Intent 的 extras 到底是什么（纯文本短链 / 带封面图 Uri / 带视频 Uri）？需要真机用一个最小 receiver App 抓一次 intent 才能定稿解析逻辑。
- Compose Multiplatform 的 iOS 是否已官方标注 stable（JetBrains 1.8.0 博文抓取 403，GitHub Release Notes 未写 stability tier），这会决定「留 iOS 退路」的可行性。
- Flutter 3.47 在 Android 上的真实 release 包体与冷启动数据，官方 app size 页没有给 Android 数字，需要本地 build appbundle 实测才能做公平对比。
- Room/SQLite 的中文分词方案：FTS5 trigram、ICU tokenizer 还是应用层切词？本轮未核实 Room 对 FTS5 的支持程度。
- 「王」本人的目标设备：是否有 Android 13 以下的老平板？这决定 minSdk 取 26 还是 28，以及 16KB/edge-to-edge 的测试矩阵。
- 是否上 Google Play？若只侧载自用，可放松 target API 与 16KB 合规；若上架，2026-08-31 的 target 36 与 2027-02-01 的 16KB 是硬deadline。
- WebDAV 服务端具体是哪一种（群晖 DSM / Nextcloud / 坚果云），是否支持大文件分块与 Range 请求，直接决定同步实现复杂度。

## 内容导入

**结论摘要**：内容导入是「味记・王」最关键也最脆弱的一环，两个平台难度完全不同。抖音侧路径清晰且无需登录：v.douyin.com 短链一次 302 就把视频 ID 写在 Location 里（任意 UA 均可），再用白名单爬虫 UA（Googlebot/Baiduspider/Bingbot）请求 www.douyin.com/video/{id} 可拿到完整 JSON-LD（标题、作者、封面、ISO 时长、发布时间、点赞），2026-10-30 实测有效；普通 UA 只得到 72,914 字节的空壳 JS 页，带签名的详情 API 直接 403。小红书侧明显更难：短链会过期（2024-12 的链接如今只 307 到首页），笔记页需要 xsec_token，匿名与非大陆出口访问一律被 300031/300015 拦下，官方开放平台也没有"按链接取内容"的能力。因此 MVP 应定在"本地解析链接 + 尽力自动抓元数据 + 导入瞬间保存快照"，下载原始视频与自动菜谱结构化留到后续，并且每条抓取路径都必须能静默降级到"链接 + 手填"。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | 抖音短链 302 直接暴露视频 ID | tech | 2026-10-30 实测：GET https://v.douyin.com/Ub_h21ccQAw/ 返回 HTTP 302（body 963 字节），Location 为 https://www.iesdouyin.com/share/video/7688336916470993728/?from_ssr=1&did=...&share_sign=...；用 curl 默认 UA 也能拿到，不需要 cookie。再跟随一跳落到 https://www.douyin.com/video/7688336916470993728?previous_page=app_code_link。抖音同时存在 v.douyin.com/{code} 与 www.douyin.com/video/{19位id} 两种形态。 | 实测 2026-10-30，curl -D 抓取 v.douyin.com/Ub_h21ccQAw/ 的响应头 |
| 2 | 小红书短链会过期，307 只回首页 | tech | xhslink.com 根域实测 307 到 http://www.xiaohongshu.com；样本 xhslink.com/a/qsoHVeD0Liw1（2024-12-13 出现在 yt-dlp issue #11797）在 2026-10-30 已只返回 307 到首页，目标丢失。公开实现接受的形态包括 https://xhslink.com/{分享码}、/a/{code}、/o/{code}。XHS-Downloader 文档明确：链接"携带日期信息"，用旧链接会被风控。 | 实测 2026-10-30 xhslink.com 响应头；https://github.com/yt-dlp/yt-dlp/issues/11797（2024-12-13）；XHS-Downloader README |
| 3 | 抖音元数据只对白名单爬虫 UA 返回（JSON-LD） | tech | 实测同一视频页：UA 为 Googlebot/Baiduspider/Bingbot 时返回 94,355–102,461 字节 SSR 页，含 <title>、name="description"、lark:url:video_cover_image_url，以及 JSON-LD VideoObject：name、description、thumbnailUrl、uploadDate=2026-09-22T20:33:54+08:00、duration=PT0H0M23S、creator 名称与主页 URL、点赞/关注/评论数、甚至前几条评论。换成普通 Chrome UA、随机 Bot、curl、YandexBot，一律只返回 72,914 字节的 jsvmprt 空壳，无 title、无 og、无 JSON-LD。注意页面没有 og:* 标签，元数据来自 JSON-LD 与 name= 系列 meta。 | 实测 2026-10-30，多种 UA 对比 www.douyin.com/video/7688336916470993728 的响应体 |
| 4 | 抖音 Web 详情 API 有 Argus 签名风控 | tech | 实测无签名请求 https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=7628438784602389247&device_platform=webapp&aid=6383 返回 HTTP 403，正文仅 46 字节："Blocked by ArgusSecurityPlugin Uifid Not Found"。开源项目（Douyin_TikTok_Download_API、MediaCrawler）的做法是：纯 Python 实现 a_bogus/X-Bogus/X-Gnarly 签名 + msToken，或用无头浏览器铸造 guest 身份、复用登录态取签名参数。 | 实测 2026-10-30 403 响应；https://github.com/Evil0ctal/Douyin_TikTok_Download_API README（v5.1.x） |
| 5 | 视频直链/无水印依赖 play_addr.uri，不是 aweme_id | tech | 公开实现路径：详情接口取 aweme_detail.video.play_addr.uri，再请求 https://www.douyin.com/aweme/v1/play/?video_id={uri}&ratio=default&watermark=0&improve_bitrate=1，302 到 CDN（douyin-8k-parser 文档）。实测用 aweme_id 冒充 video_id 请求该 play 端点，返回 HTTP 200 但 content-length: 0（软拒绝）。 | https://github.com/lijinrui182/douyin-8k-parser README（无更新日期）；实测 2026-10-30 play 端点 200/0 字节 |
| 6 | 抖音封面可即时抓取落盘 | tech | SEO 页 JSON-LD 的 thumbnailUrl 指向 p3-pc-sign.douyinpic.com/image-cut-tos-priv/...（带 x-expires=2108703600 与 x-signature）。实测无 Referer 直接 GET 返回 200、image/jpeg、12,367 字节；带 Referer 结果相同。 | 实测 2026-10-30，下载 thumbnailUrl 得到 12,367 字节 JPEG |
| 7 | 小红书笔记页仍以 __INITIAL_STATE__ 承载数据 | tech | yt-dlp 的 XiaoHongShuIE 直接从页面 window.__INITIAL_STATE__ 取 note.noteDetailMap[{id}].note；视频流取 video.media.stream[*].masterUrl/backupUrls，封面取 imageList[*].urlDefault/urlPre，标题优先 og:title。实测 2026-10-30 即使落到 /404 错误页，HTML 里仍存在 __INITIAL_STATE__ 与 noteDetailMap 字段。 | https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/xiaohongshu.py；实测 2026-10-30 页面标记计数 |
| 8 | 小红书匿名访问被风控（300031 / 300015） | risk | 实测 2026-10-30，从非中国大陆出口（AWS 东京 IP）访问 https://www.xiaohongshu.com/explore/6411cf99000000001300b6d9（yt-dlp 测试用例，蛋糕视频）与带 xsec_token=CBgeL8... 的 discovery/item 链接，全部 302 到 /404?error_code=300031「当前笔记暂时无法浏览」；改用 Googlebot/Baiduspider UA 则跳 /website-login/error?error_code=300015「浏览器运行环境异常」。 | 实测 2026-10-30 三个真实/文档给出的笔记 URL，三种 UA 全部被拦 |
| 9 | 小红书图文多图与正文靠登录态 + 签名链接 | tech | 图文笔记的配料与步骤大多画在图片里。图片地址是 sns-webpic-qc.xhscdn.com 等签名 URL。XHS-Downloader（13.2k star）称"无需登录、Cookie 非必需"，但未设 Cookie 只能下低分辨率视频；MediaCrawler 则明确要求扫码登录，并"利用保留登录态的浏览器上下文，通过 JS 表达式获取签名参数"（README 原文，未点名 X-s/X-t）。 | https://github.com/JoeanAmier/XHS-Downloader README；https://github.com/NanmiCoder/MediaCrawler README |
| 10 | 分享文案口令抽取是高频失败点 | insight | 抖音分享文本形如"7.43 复制打开抖音，看看【作者的作品】… https://v.douyin.com/Ub_h21ccQAw/ 复制此链接…"（issue #5 正文实测可见）；小红书形如"…复制本条信息，打开【小红书】App查看精彩内容！ http://xhslink.com/a/xxxx"。Douyin_TikTok_Download_API 明确"会跟随短链并从 caption 中提取被埋藏的链接"。 | https://github.com/joeseesun/qiaomu-youtube-download/issues/5（2026-10-12）；Douyin_TikTok_Download_API README |
| 11 | 自动元数据 ≠ 自动菜谱结构化 | insight | 开源 recipe-parser 把小红书链接转成菜谱卡：图文笔记用 Playwright + EasyOCR 识别图内文字，视频笔记用 ffmpeg 抽帧 + Whisper 转写；平台字段本身只有标题/正文/图片列表。也就是说配料表、克数、步骤几乎拿不到结构化版本。 | https://github.com/yugeboice/recipe-parser README |
| 12 | 两平台可拿到的字段不对等 | product | 抖音 JSON-LD 可拿到：标题、作者名+主页、封面、时长（PT0H0M23S）、发布时间（2026-09-22T20:33:54+08:00）、点赞/关注/评论数。小红书可拿到：标题、正文 desc、标签 tagList、作者 userId、图片列表（imageList），图文笔记没有时长，yt-dlp 测试用例里也没有独立的发布时间字段。 | 实测抖音 JSON-LD（2026-10-30）；yt-dlp xiaohongshu.py _TESTS 字段清单 |
| 13 | 官方开放平台不提供"链接解析"能力 | tech | 抖音开放平台有 https://open.douyin.com/share-id/（POST，Scope: aweme.forward，文档写"不需要用户授权"，但仍需 client_token 生成的 access-token），用途是分享前生成 share_id、1 小时有效、经 Webhook 回调追踪"自己发起的分享"是否成功，不能按分享链接查任意公开视频。小红书开放平台文档站为纯 SPA（抓取只得到 23,838 字节的 <title>小红书</title> 空壳），公开可见的官方 SDK 是广告开放平台（乘风/聚光/蒲公英）。 | https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/video-management/douyin/search-video/video-share-result；实测 open.xiaohongshu.com/document 2026-10-30；https://github.com/hirokicha/xiaohongshu-ads-openapi |
| 14 | 链接会消失，快照才是资产 | risk | 抖音视频可被作者删除或设为私密；小红书短链过期后只 307 到首页（实测）；XHS-Downloader 文档称旧链接会被风控。也就是说"只存 URL"的收藏夹会在几个月后大面积失效。 | 实测 xhslink 过期 2026-10-30；XHS-Downloader README 风控说明 |
| 15 | MVP 分层：L1 链接 / L2 元数据 / L3 媒体 | product | 三层成本差一个数量级：L1 纯本地解析 0 网络、几乎不会坏；L2 需要伪造爬虫 UA 或解析页面，抖音可行、小红书不稳定；L3 下载视频需要签名接口 + 去水印，条款与版权风险最高。同类工具（XHS-Downloader 13.2k star、res-downloader 21k star）都把 L3 做成独立下载器，而不是笔记应用。 | GitHub 搜索 star 数 2026-10-30；各项目 README 功能范围 |
| 16 | 抓取与去水印的合规边界 | legal | 抓取公开页面、绕过 UA 白名单、去水印下载都可能违反平台用户协议；个人离线保存他人视频/图文仍涉及著作权中的复制权。目前未见平台对"个人菜谱收藏类工具"的明确许可条款。 | 未验证：无平台官方条款原文抓取，属行业通识推断 |

**来源清单**

- 实测 2026-10-30：https://v.douyin.com/Ub_h21ccQAw/ 302 响应头（Location 含 iesdouyin.com/share/video/7688336916470993728）
- 实测 2026-10-30：https://www.douyin.com/video/7688336916470993728 多 UA 对比（Googlebot/Baiduspider/Bingbot 返回 JSON-LD，普通 UA 返回 72,914 字节空壳）
- 实测 2026-10-30：https://www.douyin.com/aweme/v1/web/aweme/detail/ 无签名返回 403 ArgusSecurityPlugin
- 实测 2026-10-30：https://www.xiaohongshu.com/explore/6411cf99000000001300b6d9 与 discovery/item/674051740000000007027a15?xsec_token=... 均 300031/300015
- 实测 2026-10-30：https://xhslink.com/a/qsoHVeD0Liw1 307 到首页（2024-12 链接已失效）
- https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/xiaohongshu.py（__INITIAL_STATE__ / noteDetailMap 解析）
- https://github.com/JoeanAmier/XHS-Downloader（13.2k star，链接格式与过期说明）
- https://github.com/Evil0ctal/Douyin_TikTok_Download_API（a_bogus/X-Bogus 签名与 caption 抽链）
- https://github.com/NanmiCoder/MediaCrawler（扫码登录 + 浏览器上下文取签名）
- https://github.com/lijinrui182/douyin-8k-parser（play 端点与 ratio/improve_bitrate 参数）
- https://github.com/yugeboice/recipe-parser（小红书图文 OCR、视频 Whisper）
- https://github.com/bonchiang96/xiaohongshu-importer（Obsidian 插件接受 share text，字段清单）
- https://developer.open-douyin.com/docs/resource/zh-CN/dop/develop/openapi/video-management/douyin/search-video/video-share-result（share-id 接口，Scope aweme.forward）
- https://github.com/joeseesun/qiaomu-youtube-download/issues/5（2026-10-12，短链下载报 Fresh cookies needed）
- https://open.xiaohongshu.com/document（实测 SPA 空壳，无能力说明）
- https://github.com/hirokicha/xiaohongshu-ads-openapi（小红书广告开放平台 SDK）

**未决问题**

- 国内网络（家宽/4G）下，小红书匿名访问或仅带 xsec_token 的笔记链接是否真能返回 noteDetailMap 实际数据？本机出口在境外，全部被 300031/300015 拦截，必须用国行设备复测后再定小红书抓取策略。
- xhslink 有效短链的 Location 是否稳定形如 xiaohongshu.com/discovery/item/{id}?xsec_token=...，xsec_token 有效期多长？本次没拿到有效样本（手上唯一真实短链已过期）。
- 抖音 SEO 通道（伪造爬虫 UA）对低频个人请求的稳定性如何？限流是按 IP 还是按 UA？平台收紧时会不会直接返回空壳——需要连续 30 天埋点观察失效率。
- 抖音图文笔记（/note/{id} 或 video 路由下的图文作品）是否同样有 JSON-LD，images 字段怎么给？未验证。
- 小红书开放平台是否存在面向服务商的笔记/内容读取接口、需要什么资质？文档站是 SPA，本次抓不到任何能力说明，未验证。
- Android 端用 WebView 复用用户自己的小红书登录态去抓页面，是否违反其用户协议、以及 Google Play 审核会不会判定为"抓取第三方内容"？未验证，需要法务评估。
- 本环境的 WebSearch 工具多次返回空结果，通用搜索改用 GitHub 搜索页 + 直连实测；必应中国、DuckDuckGo、搜狗均被反爬或返回无关结果，因此本维度结论以一手实测与开源项目源码为主，通用舆情/政策类信息缺失。

## 合规

**结论摘要**：味记・王 的核心合规矛盾只有一句话：用户"自己看"有《著作权法》第24条第1项的个人使用抗辩空间，但"帮用户把抖音/小红书视频下载下来"这件事本身，既违反平台用户协议，又已经被多地法院按《反不正当竞争法》第12条判过（武汉20万、上海30万、成都2024当庭认定）。真正的分水岭不是"存不存视频"，而是"应用里有没有一个针对平台的技术解析/下载通道"——有，就是工具提供方，风险等级等同已被判赔的"去水印"软件；没有，只是一个本地笔记应用，风险骤降。国内安卓商店的硬门槛（软著、App备案、隐私政策）与"首启即收集信息"的拒审雷区，比版权问题更早卡住上线。结论：MVP 应做"零抓取、零下载、零账号"的本地菜谱库，元数据靠用户分享文案/剪贴板本地解析获得，所有红线功能一律不做。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | 抖音《用户协议》禁止下载、抓取、模拟下载、深度链接 | legal | 抖音《用户协议》2.4 仅授予"个人的、可撤销的、不可转让的、非独占的和非商业的"使用权，并禁止"改编、复制、传播、爬虫、垂直搜索、镜像或交易"；5.3(4) 明确禁止"以盗链、冗余盗取、爬虫抓取、模拟下载、深度链接、假冒注册等任何不正当方式……上传、下载、使用抖音中的信息或内容"；5.3(7) 禁止商业使用或"以任何形式向第三方提供"；5.1 禁止"使用任何自动化程序、软件或类似工具接入抖音，收集或处理其中信息、内容"。2026-10-30 抓取协议原文核实。 | https://www.douyin.com/agreements/?id=6773906068725565448（2026-10-30 抓取） |
| 2 | 小红书 robots.txt 对全部第三方爬虫关闭 | legal | 2026-10-30 实测 https://www.xiaohongshu.com/robots.txt：`User-agent: *` 组为 `Disallow: /`；Baiduspider/bingbot 等只放行 `/$`、`/explore`、`/download`、`/sitemap.xml` 等少数路径；Googlebot 仅放行 `/worldcup26`。第三方解读普遍称小红书《用户协议》禁止未经授权抓取（协议原文未取到，标记未验证）。 | https://www.xiaohongshu.com/robots.txt（2026-10-30 实测）；协议原文未验证 |
| 3 | 去水印/绕过下载限制已被判不正当竞争（武汉案，判赔20万） | risk | 武汉中院一审（2022年报道，搜狐转载页面标注2026-05-31）：被告运营"易解析""爱解析"，覆盖PC/App/小程序/公众号，实现去水印、绕过下载限制批量下载，并提供评论导出、音频提取，靠会员+广告获利，原告索赔100万。法院认定平台水印与下载限制属受保护的经营利益，针对性开发构成《反不正当竞争法》第十二条不正当竞争；判单独赔偿7万元+连带13万元=20万元。值得注意的是："评论批量导出、音频提取"因未实质妨碍平台正常运行，未被认定为不正当竞争。 | https://m.sohu.com/a/1029447724_120169366/（页面标注 2026-05-31，案件为2022年报道） |
| 4 | 成都互联网法庭2024年当庭认定"一键去水印"构成不正当竞争 | risk | 2024-04-28 封面新闻报道：成都某公司运营"一键去水印小工具神器"小程序，解析链接去除平台来源与创作者标识、修改MD5规避防重检测，并在小程序内教搬运方法、靠广告变现。成都铁路运输第一法院（成都互联网法庭）认定其"割裂了短视频与其创作者和发布平台的联系"，突破技术限制、规避知识产权技术监管，构成不正当竞争，判令赔偿（金额未披露）。 | https://www.thecover.cn/news/bx6MzTuMWlCH90qSdq8Jkw==（2024-04-28） |
| 5 | 平台有主动维权与强制执行的先例（判赔40万） | risk | 2022-07-08 报道：抖音（微播视界）就"一键去水印呀""易推去水印"等小程序/公众号/网站起诉易启点公司，此前已判赔40万元，并申请强制执行。另有2025-04-25 上海报道的开发去水印软件案，判赔30万元并要求公开消除影响。 | chinaz.com 2022-07-08 报道（经360搜索索引）；shfzb.com.cn 2025-04-25 报道（经360搜索索引）；原文未逐一抓取，标记部分未验证 |
| 6 | 《著作权法》个人使用抗辩与技术措施禁令的边界 | legal | 2020修正《著作权法》第24条第1项："为个人学习、研究或者欣赏，使用他人已经发表的作品"可不经许可、不付报酬，但须"指明作者姓名或者名称、作品名称"，且"不得影响该作品的正常使用，也不得不合理地损害著作权人的合法权益"。第49条：禁止故意避开或破坏权利人的技术措施；第53条第(六)项对规避行为追究民事/行政/刑事责任。 | https://www.gov.cn/guoqing/2021-10/29/content_5647633.htm（2020修正版全文） |
| 7 | 只存链接+元数据 vs 下载视频：风险差一个数量级 | insight | 存 URL、标题、作者、自写备注，本质是对事实性信息的记载，不构成对作品的复制；下载整段视频是复制作品，同时违反平台协议并落入反法判例射程；连封面图也是摄影/美术作品，长期缓存原图属于复制。 | 推断（基于上述协议条款与判例的综合判断），未验证具体司法解释 |
| 8 | 国内商店仍上架"视频下载去水印"类应用，但属高危品类 | risk | 2026-10 经360搜索检索，应用宝官网仍索引到"视频下载去水印app"等条目，说明国内安卓商店对该品类并非全品类一刀切下架；但该类应用高度依赖版权投诉触发下架。华为审核侧的高频拒审点是权限与隐私而非品类（见下条）。 | 360搜索"视频下载去水印app 应用宝"（2026-10-30 检索）；未取得商店官方品类政策原文，标记未验证 |
| 9 | Google Play 明令禁止"以违反服务条款的方式访问第三方服务/API" | legal | Google Play《Device and Network Abuse》政策原文：应用不得"access or use a service or API in a manner that violates its terms of service"，违规示例直接列出"Violating a service's or API's terms of service"，后果为拒审/下架/封号。这意味着即使功能"技术上做得到"，只要违反抖音 ToS，Play 侧就有明文下架依据。 | https://support.google.com/googleplay/android-developer/answer/9888379（2026-10-30 抓取） |
| 10 | 国内上架三件套：软著 + App备案 + 隐私政策 | legal | 多篇2025年个人开发者上架指南一致指出：华为/荣耀/OPPO/vivo/应用宝 需 软件著作权证书（权利人与开发者账号主体一致）、App备案号、隐私政策公网链接；苹果中国区自2023-09起要求填写ICP备案号。工信部2023-08印发App备案通知，2024-04至06开展监督检查。个人主体可以备案，但腾讯云备案指引要求域名所有人与备案主体一致、个人每单限1个。 | 工信部通知经中国政府网2023-08-09转载（经360搜索索引）；腾讯云ICP备案文档摘要；CSDN 2025年个人开发者上架指南；官方原文未逐一抓取，标记部分未验证 |
| 11 | 首启即收集信息是华为最高频拒审原因 | legal | 2025-02-26 DCloud 案例：应用因"同意隐私政策前，应用获取了用户的个人信息"及SDK隐私政策网址未在AppGallery Connect提交而被拒。另一案例因获取IMEI且隐私政策披露不足被退回。Google Play 侧要求：即使不收集任何数据也必须提供隐私政策链接+应用内入口+Data safety 表单，同意必须是独立的 affirmative action（返回键/自动消失的弹窗不算同意）。 | https://support.google.com/googleplay/android-developer/answer/10144311（2026-10-30 抓取）；DCloud 2025-02-26 案例（经360搜索索引） |
| 12 | 笔记/工具类应用可以做到"无须个人信息" | legal | 《常见类型移动互联网应用程序必要个人信息范围规定》（2021-03-22，中央网信办）第五条列明"实用工具类""短视频类""电子图书类"等"无须个人信息，即可使用基本功能服务"；"网络社区类"基本功能仅需"注册用户移动电话号码"。PIPL 第6条最小必要、第13条合法性基础、第17条告知义务、第51条加密与权限控制。 | https://www.cac.gov.cn/2021-03/22/c_1617990997054277.htm；https://www.cac.gov.cn/2021-08/20/c_1631050028355286.htm（均2026-10-30抓取） |
| 13 | 权限最小化清单（Android 13/14 适配） | tech | 实际只需要：照片选择器（Android 13+ Photo Picker，无需 READ_MEDIA_* 权限）/ 按需 READ_MEDIA_IMAGES / 可选 CAMERA / 前台剪贴板读取。明确不要：READ_PHONE_STATE 与 IMEI、QUERY_ALL_PACKAGES、后台定位、通讯录、短信、后台读剪贴板（Android 10+ 系统已禁止后台读取）。 | Android 平台行为与 Google Play 隐私政策要求（2026-10-30 抓取 Play 政策）；具体 API 行为为工程常识，标记推断 |
| 14 | 无障碍服务自动抓取界面 = 账号级风险 | risk | Google Play《AccessibilityService API》政策：仅允许"deterministic, rule-based automation"、基于"static, human-defined script"、"narrow and clearly understood purpose"；非无障碍工具的任何"autonomously initiate, plan, and execute actions or decisions"被"strictly prohibited"；未声明使用可致应用下架、开发者账号终止。 | https://support.google.com/googleplay/android-developer/answer/10964491（2026-10-30 抓取） |
| 15 | 零抓取采集链路：从"分享文案"本地正则解析 | product | 抖音"复制链接"得到的分享文案通常形如"…看看【作者的作品】标题… https://v.douyin.com/xxxx/"，小红书类似"标题 - 小红书 http://xhslink.com/xxx"，文案本身已包含作者/标题/短链，无需访问平台服务器即可获得元数据。风险最低的采集路径因此是：系统分享 Intent / 用户粘贴 → 本地正则提取平台+作者+标题+链接 → 用户确认落库。 | 推断（基于平台分享文案的常见形态），未逐条实测文案格式 |
| 16 | 名称与商标：软著/备案/商店名必须一致 | risk | 国内商店要求应用名称与实际功能一致、不得侵犯他人商标；多篇开发者指南称软著名称须与上架名称一致（官方原文未取到，标记未验证）。"味记・王"含间隔号"・"，部分商店的名称校验对特殊字符有限制（未验证）。 | 推断 + 开发者指南二手信息，未验证官方原文 |
| 17 | 二次分发边界：分享"我写的"，不分享"别人的" | insight | 反法判例的违法核心是"使同一视频可被重复上传""割裂作品与创作者和平台的联系"；著作权法第24条要求指明作者与作品名称。个人使用抗辩一旦跨到"向他人提供"就失效（抖音协议5.3(7)亦禁止向第三方提供）。 | 综合上述判例与法条，推断 |

**来源清单**

- https://www.douyin.com/agreements/?id=6773906068725565448
- https://www.douyin.com/robots.txt
- https://www.xiaohongshu.com/robots.txt
- https://www.gov.cn/guoqing/2021-10/29/content_5647633.htm
- https://www.cac.gov.cn/2021-08/20/c_1631050028355286.htm
- https://www.cac.gov.cn/2021-03/22/c_1617990997054277.htm
- https://www.thecover.cn/news/bx6MzTuMWlCH90qSdq8Jkw==
- https://m.sohu.com/a/1029447724_120169366/
- https://support.google.com/googleplay/android-developer/answer/9888379
- https://support.google.com/googleplay/android-developer/answer/10144311
- https://support.google.com/googleplay/android-developer/answer/10964491
- https://www.so.com/s?q=App备案 个人主体 能不能 需要域名 经验 2025
- https://www.so.com/s?q=剑网2025 专项行动 短视频 侵权 重点打击
- https://www.so.com/s?q=华为应用市场 审核指南 知识产权

**未决问题**

- 纯本地、无服务器的App是否必须做App备案？个人主体在没有自有已备案域名时能否走通备案？需向阿里云/腾讯云备案客服逐条确认（各管局口径不同）。
- 华为/小米/OPPO/vivo/应用宝 的审核指南原文中，是否存在针对"视频下载/去水印/解析"品类的明文禁止条款？本次未取到官方原文（相关页面为JS渲染或需登录），只能确认"权限与隐私"是高频拒审点。
- 抖音与小红书的"复制链接"分享文案格式是否稳定包含标题与作者？需要真机抓 10-20 条样本验证正则的命中率，否则本地解析方案的产品体验会打折。
- "味记・王"在第9类/第42类是否已被注册？含"・"的名称在各商店名称校验中是否被允许？
- 仅保存标题/作者/链接/封面缩略图，是否会被平台以"不正当竞争"起诉？目前判例均针对"提供下载/去水印工具"，尚未见到针对"个人书签类工具"的判例，属法律空白区。
- 若未来做云同步，用户上传的自己拍摄的成品图与自写菜谱不涉第三方内容，但条目中的封面缩略图上传到服务器是否构成对他人作品的复制与存储？需要一次正式的合规评估。

## 需求验证

**结论摘要**：需求侧证据充分：小红书 2024 年中月活约 3.2 亿、日活 1.2 亿，女性约七成、95 后占一半、一二线占一半，千瓜 2025 报告称其食品行业人群 1.5 亿+；下厨房官网现有 360 万道菜谱、8045 万个"作品"，豆果自称 5000 万用户，说明"看教程—照着做—晒成品"是成熟行为。但供给侧严重过剩（豆果 500 万菜谱），真正的断点在"收藏→做过"：下厨房最近 50 条 App Store 评论几乎被开屏广告、摇一摇跳转电商、启动 5 秒、改版短视频化占据，有用户明确说"再这样就去小红书"，大平台把菜谱当流量入口而非个人工具。海外已用真金白银验证了"从社交平台导入菜谱"这一细分：ReciMe 2021 年上线、美区 iOS 32.4 万评分 4.79、自称 1500 万用户，支持 TikTok/Instagram/YouTube/截图导入，免费版每周 5 次导入，且已覆盖 Android；而 Mela、Crouton 等精品工具仍只做 Apple 平台，Android 平板是明显空档。因此"味记・王"不应做另一个菜谱社区，而应做"抖音/小红书收藏夹的厨房"：本地优先、无广告、秒开、以"做过"为核心数据模型，先服务王本人，再小范围家庭共享，商业化走一次性 Pro 买断而非订阅/广告。验证是否成立的核心指标建议是"7 日复做率"（7 天内至少标记一次"做过"的周活用户占比）与"导入→首次烹饪转化率"，而非收藏总数。

| # | 条目 | 类型 | 细节 | 证据 |
|---|---|---|---|---|
| 1 | 下厨房：360万菜谱 / 8045万作品，"做过"是成熟行为 | product | 下厨房官网 2026-10-30 实时显示：菜谱 3,601,611 道、作品 80,452,555 个；iOS 评分 4.84、44.0 万个评分；Google Play 端 100 万+ 安装、4.7 分、1.46 万条评价。作品数约为菜谱数的 22 倍（总量口径，非中位数），说明大量用户会真的下厨并记录成品。 | https://www.xiachufang.com/ 抓取于 2026-10-30；https://itunes.apple.com/cn/lookup?id=460979760（2026-10-30）；Google Play 搜索页（2026-10-30） |
| 2 | 下厨房近50条评论：广告与电商跳转是第一弃用原因 | pain | Apple 评论 RSS 最新 50 条中，1-2 星占比很高，反复出现：开屏广告连续两次、跳过按钮位置每次不同、摇一摇/轻触即跳转淘宝京东美团、从微信切回又弹广告、冷启动约 5 秒、改版像"嘈杂的短视频平台"。多条评论写明"卸载"，一条直接警告"用户会转去小红书"。 | https://itunes.apple.com/cn/rss/customerreviews/id=460979760/sortBy=mostRecent/json（抓取 2026-10-30，50 条） |
| 3 | 豆果美食：5000万用户、500万菜谱、2000万互动 | product | 豆果官网称菜谱 500 万+、互动内容 2000 万+、美食课堂 3000+、用户 5000 万+；iOS 评分 4.90、19.9 万评分。与下厨房合计构成中文菜谱内容的绝对存量。 | https://www.douguo.com/ 抓取于 2026-10-30；iTunes 中国区搜索（2026-10-30） |
| 4 | 小红书：3.2亿 MAU / 1.2亿 DAU，女性约七成、一二线50%、95后50% | insight | 截至 2024 年中，小红书 MAU 3.2 亿、DAU 1.2 亿；男女比例约 3:7（部分口径女性近 80%）；95 后 50%、00 后 35%；一二线城市用户 50%。千瓜 2025 年 11 月报告称小红书"食品行业人群 1.5 亿+"，其中 45% 爱尝鲜、44% 爱分享。 | 千瓜《2024 活跃用户研究报告》2024-04；腾讯新闻 2025-02-18；雪球 2025-01-17；市场部网转载千瓜《2025 美食饮品行业种草洞察数据报告》2025-11-28（经搜狗/360 检索获得） |
| 5 | 小红书美食内容体量：二手转述"美食笔记浏览量195.7亿" | insight | 《2024 小红书中式正餐行业营销通案》被今日头条、道客巴巴、豆瓣多处转述为"美食类笔记浏览量 195.7 亿""美食相关笔记 230 万+篇、探店打卡 146 万+篇"。原始 PDF 在道客巴巴需积分下载，未能核对统计口径与时间范围。 | https://www.toutiao.com（2024-09 摘要）；https://www.doc88.com 报告条目（2024-09）；标注：二手数据，未核实原始报告 |
| 6 | ReciMe："从社交平台导入菜谱"已被规模验证 | product | 2021-11-12 上线，美区 iOS 32.4 万评分、4.79；官网自称 1500 万+ cooks；支持 Instagram、TikTok、Facebook、Pinterest、YouTube、截图、手写菜谱照片导入；免费版每周 5 次导入，购物清单与周计划免费；覆盖 iOS、Android、Web 和 Chrome 插件。 | https://recime.app/（2026-10-30）；https://itunes.apple.com/lookup?id=1593779280&country=us（2026-10-30，releaseDate 2021-11-12，version 更新 2026-10-22）；Google Play 搜索（2026-10-30，com.recime.app，4.7） |
| 7 | Paprika 3：$4.99 买断、多端分开卖、4.9分/5.4万评分 | product | Paprika 3 售价 $4.99（买断），iOS 5.4 万评分/4.90；官方明确"每个平台版本单独购买"（iOS/Android/Mac/Windows）；核心能力是内置浏览器抓网页菜谱、按货架分类并合并重复项的购物清单、烹饪时常亮与步骤高亮、份量缩放与单位换算；Paprika 4 已开放公测。 | https://www.paprikaapp.com/（2026-10-30）；https://itunes.apple.com/search?term=recipe+manager&country=us（2026-10-30） |
| 8 | Mela / Crouton：精品菜谱工具仍只做 Apple，Android 平板是空档 | product | Mela 仅 iOS/macOS（884 评分），主打内置浏览器识别菜谱、全屏烹饪模式、食材勾选、计时器、与日历/提醒事项集成；Crouton 仅 Apple 平台（2932 评分），主打步骤高亮与多菜并行。两者都不支持 Android。 | https://mela.recipes/（2026-10-30）；https://itunes.apple.com/search?term=recipe+manager&country=us（2026-10-30） |
| 9 | 菜谱 App 高度集中：89.1% 用户只装一款，替换成本高 | insight | 全拓数据称 89.1% 的用户仅安装一款菜谱类应用（原文未标年份，推断为 2020-2021）；历史月活口径：下厨房 1321.7 万（某年 4 月）、1374 万→1479 万（2020-01）、2021 上半年月均 1199.1 万；豆果 637.4 万、香哈 390.5 万属同一历史口径。2024 年之后的公开 MAU 未找到。 | 全拓数据/知乎摘要（年份未标注）；智研咨询 2021H1；搜狗检索摘要（2019-2021）；标注：历史数据，2024 后无公开更新 |
| 10 | "收藏=做过"的结构性断层 | pain | 没有任何平台公开"收藏→实际制作"转化率。可观察的结构性摩擦：小红书收藏夹没有"做过"状态、不分类、视频教程不能离线、做菜时要反复拖进度条；下厨房虽有"作品"机制但被广告打断。推断：多数收藏终身未被打开第二次。 | 未验证/推断（基于产品形态与 2026-10 下厨房评论；无公开转化率数据） |
| 11 | 目标人群：25-35 岁一二线女性为主，男性与独居场景在增长 | insight | 下厨房历史画像：女性 70%-88%，核心 24-30 岁；小红书女性约 70%、男性 20%-30% 且增长；一人食/独居场景扩大（2024 年一人户最新口径未取得，标注推断）。核心动机是"工作日晚餐、带饭、减脂、给孩子做"而非钻研厨艺。 | 简书/CSDN 下厨房产品分析（2018-2020，二手）；千瓜 2024 用户画像；一人户数据标注：未验证/推断 |
| 12 | 平台内容导入的合规边界 | legal | 小红书、抖音的用户协议通常禁止未经许可的抓取、去水印与批量转载；ReciMe 的规避方式是只处理"用户主动分享的链接/截图"，不做站内爬虫、不提供去水印下载。国内可直接商用（收费）的解析方式是否合规，未见明确判例。 | 未验证（未逐条核对协议原文）；产品形态参考 https://recime.app/（2026-10-30） |
| 13 | 商业化路径与个人化命名的双刃剑 | risk | 工具类付费天花板低：ReciMe 用免费额度+订阅，Paprika 用 $4.99 买断，国内菜谱 App 普遍免费+广告/电商，而下厨房评论证明用户对广告极度反感、且已有"求一次性去广告会员"的呼声。"味记・王"利于自用与口碑传播，但"味记"不利 ASO（用户搜"菜谱""食谱"），"王"降低大众化想象。 | ReciMe/Paprika 定价 2026-10；下厨房评论 2026-10（含"想要一次性去广告会员"）；命名为推断 |
| 14 | 成功指标：用"7日复做率"而不是收藏数证明价值 | insight | 建议北极星指标＝7 日复做率（7 天内至少标记 1 次"做过"的周活用户占比）；配套：导入→首次烹饪转化率（导入后 7 天内进入烹饪模式并完成）、人均月"做过"次数、D7/D30 留存、导入成功率（链接解析/OCR）、首日导入≥3 条比例、购物清单生成率。目标值无公开基准，首版建议 7 日复做率≥25%、导入→做过≥30%（推断值）。 | 未验证/推断；机制参考 ReciMe 免费导入额度与下厨房"作品"设计（2026-10） |
| 15 | 抖音美食教程以短视频为主，需要"边做边看" | feature | 抖音与新榜发布《2025 抖音美食内容创作年度报告》，把美食创作分为邪修派、家常派、市井派、Cosfood 派等，说明教程内容高度视频化、娱乐化；具体播放量数字未取得正文。抖音非遗美食话题播放量 31.4 亿次（2025-03 报道）可侧面说明美食话题量级。 | 新榜/微信公众号《2025 抖音美食内容创作年度报告》2025（正文受验证拦截，未取得数字）；360 检索摘要 2025-03-19（非遗美食 31.4 亿） |

**来源清单**

- https://www.xiachufang.com/ （实时统计：菜谱 3,601,611 / 作品 80,452,555，抓取 2026-10-30）
- https://www.douguo.com/ （菜谱 500万+ / 用户 5000万+ / 互动 2000万+，抓取 2026-10-30）
- https://itunes.apple.com/cn/lookup?id=460979760 （下厨房 iOS 4.84 分 / 440,073 评分，2026-10-30）
- https://itunes.apple.com/cn/rss/customerreviews/id=460979760/sortBy=mostRecent/json （下厨房最新 50 条评论，2026-10-30）
- https://itunes.apple.com/search?term=%E7%BE%8E%E9%A3%9F%E8%8F%9C%E8%B0%B1&country=cn&entity=software （中国区菜谱类 App 评分/价格对比，2026-10-30）
- https://itunes.apple.com/search?term=recipe+manager&country=us&entity=software （美区菜谱管理 App 对比，2026-10-30）
- https://itunes.apple.com/lookup?id=1593779280&country=us （ReciMe：2021-11-12 上线，323,755 评分，4.79，免费，2026-10-30）
- https://recime.app/ （ReciMe：15m+ cooks，支持 IG/TikTok/YouTube/截图导入，免费每周 5 次导入，2026-10-30）
- https://mela.recipes/ （仅 iOS/macOS，2026-10-30）
- https://www.paprikaapp.com/ （$4.99 买断、各平台单独购买、Paprika 4 公测，2026-10-30）
- https://play.google.com/store/search?q=%E4%B8%8B%E5%8E%A8%E6%88%BF&c=apps （下厨房 Android：100万+ 安装、4.7 分、1.46万评价，2026-10-30）
- https://www.199it.com/archives/74476.html （Google 2012：27% 用户在做饭时使用平板电脑）
- 搜狗检索结果：千瓜《2024 活跃用户研究报告》（小红书 MAU 3.2亿、男女 3:7、95后50%、一二线50%），2024-04；腾讯新闻 2025-02-18；雪球 2025-01-17
- 360 检索结果：千瓜《2025 美食饮品行业种草洞察数据报告》（小红书食品行业人群 1.5亿+），2025-11-28；《2024 小红书中式正餐行业营销通案》（美食笔记浏览量 195.7亿，二手未核实），2024-09
- 搜狗检索结果：全拓数据“89.1% 用户仅安装一款菜谱类应用”（年份未标注）；智研咨询“2021H1 下厨房月均活跃 1199.1 万”

**未决问题**

- 下厨房、豆果 2024-2026 年的真实 MAU/DAU 与付费率是多少？公开渠道只查到 2019-2021 年约 1200-1480 万的历史口径。
- 小红书/抖音菜谱内容的"收藏→实际制作"转化率是否有任何平台侧或第三方公开数据？目前只能推断。
- 国内 Android 平板（华为/小米/联想/OPPO）上菜谱类 App 的真实装机与日活是多少？Google Play 数据不能代表国内安卓大盘。
- 小红书、抖音链接解析与截图 OCR 用于个人工具的合规边界在哪里？需要核对平台协议原文与是否有相关判例。
- 中国用户对"一次性买断 ¥25-45 的菜谱工具"的接受度如何？家庭共享（多档案）是否愿意额外付费？
- 「味记」在 App/软件类别的商标注册情况，以及"味记・王"作为商店名称是否会被审核或检索问题影响。
