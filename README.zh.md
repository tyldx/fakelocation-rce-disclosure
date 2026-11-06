# FakeLocation 命令执行披露材料

[English](README.md)

对 Android 应用 FakeLocation (包名 `com.lerist.fakelocation` / `dev.lerist.fakelocation`,
开发者 Lerist / 成都览一科技) 的安全分析:应用含未公开的服务端可控命令执行功能,通用回调路径可达 root,
且在应用启动时、用户同意之前即可触发。受影响版本为 1.3.2.1 至 1.5.2。

报告人: [tyldx](https://github.com/tyldx) · 联系 tyldx@proton.me

## 目录

- `REPORT.zh.md` / `REPORT.en.md` — 正式报告(中/英); `REPORT.zh.pdf` / `REPORT.en.pdf` 为同一报告的
  PDF 渲染版。
- `poc/` — 动态验证。`xposed-rce-poc/` 是 LSPosed PoC 模块源码(只 hook okhttp 响应,
  按 URL 改写触发 App 自身命令执行分支);`results/` 是 `id` 命令输出等证据。
- `hashes/` — 样本 SHA-256。
- `tools/` — `md2html.py` 将报告转为 HTML,再打印为 PDF。
- `evidence/archive-urls.md` — 官网/政策页/商店/仓库的 archive.today 存档链接。

## 不包含

不重分发厂商 APK 与反编译出的厂商代码(受版权保护、且为含未公开代码的二进制)。
样本以哈希引用,原始页面以存档链接引用。

## 边界

PoC 为客户端侧验证(本机注入响应),证明客户端会执行服务端可控命令,未证厂商服务器当前在下发。
研究仅用于安全披露。
