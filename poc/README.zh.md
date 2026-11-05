# 动态 PoC

[English](README.md)

证明服务端响应能让 FakeLocation 执行任意 shell 命令: LSPosed 模块在 App 进程内改写 okhttp 响应,
触发 App 自身的命令执行分支。不碰厂商服务器、不改 App 文件, 注入命令仅为 `id`。
结果与证据见 `results/RESULTS.md` 与 `results/`。

## 模块 (`xposed-rce-poc/`)

只 hook `okhttp3.internal.http.RealInterceptorChain.proceed`, 按请求 URL 改写响应体:

- getNotices → `10000:id>...` 触发 notice 的 `sh -c` (通道一)
- goods/getRenewalGoodsList → `{"code":10000,"message":"id>..."}` 触发同步回调的 su+sh (通道二)
- ver/vfs → 同上 (通道三, 官网/GitHub 版后台轮询, 需激活 ROOT 模式, 未实测)

加壳 App 的 okhttp 晚于默认 loader, 故 hook 所有 BaseDexClassLoader 构造, 新 loader 能加载目标类时装钩。
构建 `gradle assembleDebug`; 产物 app-debug.apk 在 LSPosed 启用并勾两个包作用域后生效。

## 类/方法映射 (运行时解密 dex 的实际混淆名)

- 命令出口: Play `fv2.ළ(String[], boolean su, boolean)` / 官网-GitHub 版 `z41`、`C13588.m51168`,
  内部 `exec(su ? "su" : "sh")`。
- 通用回调 su+sh: Play `C3685.m13605` / 官网-GitHub 版 `C5901`, `code==10000` 时 su + sh。
- getNotices sh: Play `C6073` / 官网-GitHub 版 `C11241`, `string().startsWith("10000:")` → `sh -c`。

## 结果

见 `results/RESULTS.md`。Play 与官网/GitHub 两构建的通道一、二均已运行时验证 (sh = App uid, su = root);
Play 另验证了"同意前执行" (`results/before-consent/`); 通道三仅静态确认。
