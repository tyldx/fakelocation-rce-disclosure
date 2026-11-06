# Dynamic PoC

[中文版 / Chinese](README.zh.md)

Demonstrates that a server response can make FakeLocation run arbitrary shell commands: an LSPosed
module rewrites the okhttp response inside the app process, triggering the app's own command-execution
branches. It does not contact the vendor's server, does not modify the app, and the
injected command is only `id`. Results and evidence are in `results/RESULTS.md` and `results/`.

## Module (`xposed-rce-poc/`)

Hooks only `okhttp3.internal.http.RealInterceptorChain.proceed` and rewrites the response body by
request URL:

- getNotices → `10000:id>...` triggers the notice `sh -c` (path 1)
- goods/getRenewalGoodsList → `{"code":10000,"message":"id>..."}` triggers the synchronous callback's
  su+sh (path 2)
- ver/vfs → same (path 3, the official-site/GitHub build's background poller; requires ROOT mode
  activated; not live-tested)

Path 2 also includes the startup requests `app/getAppConfigs` and `version/checkApkUpdate`, before
user consent: every build examined except Play 1.5.2 runs su+sh; only Play 1.5.2 runs sh alone. The
official-site/GitHub 1.5.2 version check is live-tested; the config request is static-confirmed only.

In the tested packed 1.5.2 builds, okhttp loads later than the default class loader, so the module hooks every
`BaseDexClassLoader` constructor and installs the hook once a new loader can load the target class.
Build with `gradle assembleDebug`; the resulting app-debug.apk takes effect once enabled in LSPosed
with both packages in scope. Versions before 1.3.10 are not packed; their business code can be
decompiled directly from the APK.

## Class / method map (1.5.2; actual obfuscated names in the runtime-decrypted dex)

- Command sink: Play `fv2.ළ(String[], boolean su, boolean)` / official-site-GitHub build `z41`,
  `C13588.m51168`, internally `exec(su ? "su" : "sh")`.
- Synchronous generic callback su+sh: Play `C3685.m13605` / official-site-GitHub build `C5901`, on `code==10000`
  runs su + sh.
- getNotices sh: Play `C6073` / official-site-GitHub build `C11241`,
  `string().startsWith("10000:")` → `sh -c`.

## Results

See `results/RESULTS.md`. Paths 1 and 2 are runtime-verified on both builds (sh = app uid, su = root);
Execution before consent is verified on Play (`results/before-consent/`) and for the official-site/GitHub
1.5.2 version check (see `results/RESULTS.md`); path 3 is static-confirmed only.
