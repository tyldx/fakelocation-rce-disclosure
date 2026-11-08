# Dynamic PoC

[中文版 / Chinese](README.zh.md)

Demonstrates that a server response can make FakeLocation run arbitrary shell commands: an LSPosed
module rewrites the okhttp response inside the app process, triggering the app's own command-execution
branches. It does not modify the vendor's server or the app, and the
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
user consent: every build examined except Play 1.5.2 runs su+sh; only Play 1.5.2 runs sh alone. In 1.3.1.6 the
branch exists only on the config request. The official-site/GitHub 1.5.2 version check is live-tested;
the config request is static-confirmed only.

In the tested packed 1.5.2 builds, okhttp loads later than the default class loader, so the module hooks every
`BaseDexClassLoader` constructor and installs the hook once a new loader can load the target class.
Build with `gradle assembleDebug`; the resulting app-debug.apk takes effect once enabled in LSPosed
with both packages in scope.

Class and method names are in section 4 of the [report](../REPORT.en.md).

## Results

See `results/RESULTS.md`. Paths 1 and 2 are runtime-verified on both builds (sh = app uid, su = root);
Execution before consent is verified on Play (`results/before-consent/`) and for the official-site/GitHub
1.5.2 version check (see `results/RESULTS.md`); path 3 is static-confirmed only.
