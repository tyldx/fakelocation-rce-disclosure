# 动态验证结果

[English](RESULTS.md)

日期 2026-11-05。Pixel 9 (KernelSU)。方法: LSPosed 模块 com.fl.rcepoc 只 hook
`okhttp3.internal.http.RealInterceptorChain.proceed`, 按 URL 改写响应体, 触发 App 自身代码执行。
不修改厂商服务器与 App 文件。注入命令只有 `id`。

## 实测矩阵

| 通道 | Play 1.5.2 (dev.lerist.fakelocation) | 官网/GitHub 1.5.2 (com.lerist.fakelocation) |
|---|---|---|
| 1 getNotices → sh | 已验证 App uid + 同意前执行 | 已验证 App uid |
| 2 goods/getRenewalGoodsList → 同步回调, su root | 已验证 uid=0 | 已验证 uid=0 |
| 2 version/checkApkUpdate → 异步回调 | 仅 sh (静态分析) | 已验证同意前 su root、另证 sh App uid, 持续重试 |
| 2 app/getAppConfigs → 异步回调 | 仅 sh (静态分析, 未单独实测) | su+sh (静态分析, 未单独实测) |
| 3 ver/vfs 轮询 → su root | 不适用 | 仅静态(需激活 ROOT 模式, 与通道2同机制) |

## 证据文件

- `poc_notice_sh.txt` — Play 通道1, `uid=10358(u0_a358) ... untrusted_app_34`。
- `poc_generic_su_uid0.txt` — Play 通道2 su, `uid=0(root) ... u:r:ksu:s0`。
- `poc_generic_sh_appuid.txt` — Play 通道2 的 sh 分支。
- `cn_path1_notice_sh.txt` — 官网/GitHub 通道1, `uid=10355(u0_a355) ... untrusted_app_27`。
- `cn_path2_generic_su_uid0.txt` — 官网/GitHub 通道2 su, `uid=0(root)`。
- `cn_checkapkupdate_preconsent_root.txt`、`cn_checkapkupdate_consent_screen.png`、
  `cn_checkapkupdate_su_sh_proof.txt` — 官网/GitHub 1.5.2 的版本检查: 持续注入记录 19 次
  `uid=0(root) ... u:r:ksu:s0` 输出, 相邻约隔 6 至 8 秒; 00:57 的截图显示同意框仍在屏,
  另有 `uid=10355(u0_a355)` 的 sh 输出。
- `before-consent/` — Play 首装态: 命令于用户点"同意"之前执行的直接证据(截图 + 时间戳 + 结果文件)。
- `inject_log.txt` `env.txt` — 模块日志与环境。inject_log 中除 getNotices / getRenewalGoodsList 外,
  还有发现阶段对 `getAppConfigs` / `version/checkApkUpdate` 的注入: 它们是同一通用回调的其他接口,
  其中仅 Play 1.5.2 的 checkApkUpdate 异步分支只走 sh; 官网/GitHub 1.5.2 的 su+sh 分支已有上述实测
  (见报告 4.2、6), getAppConfigs 仍未单独实测。

## 边界

客户端侧验证(本机注入响应), 证明客户端会执行服务端可控命令, 未证明厂商服务器当前在下发。
主证据是静态代码分支。通道3未实测(需激活 ROOT 模式, 会连带触发应用自身注入与反篡改, 为避副作用未驱动)。

记录局限: 通道一同意前执行, 据实验记录 uiautomator 显示"同意"按钮在屏且未点击, topResumedActivity=MainActivity, 未找到原始 UI 与输入记录。
官网/GitHub 1.5.2 版本检查的同意框状态依据截图, 原始 UI 记录未保存。该实验在基线模块上临时增加了对应的改写分支, 实验后已回退。
