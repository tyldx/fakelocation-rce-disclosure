# Dynamic verification results

[中文版 / Chinese](RESULTS.zh.md)

2026-11-05. Pixel 9 (KernelSU). Method: the LSPosed module com.fl.rcepoc hooks only
`okhttp3.internal.http.RealInterceptorChain.proceed` and rewrites the response body by URL, triggering
the app's own code. It does not contact the vendor's server, does not modify the app, and the injected
command is only `id`.

## Verification matrix

| Path | Play 1.5.2 (dev.lerist.fakelocation) | Official-site/GitHub 1.5.2 (com.lerist.fakelocation) |
|---|---|---|
| 1 getNotices → sh | verified, app uid + before-consent | verified, app uid |
| 2 goods/getRenewalGoodsList → synchronous callback, su root | verified, uid=0 | verified, uid=0 |
| 2 version/checkApkUpdate → async callback | sh only (static analysis) | verified su root before consent; separate sh app-uid output, repeated retries |
| 2 app/getAppConfigs → async callback | sh only (static analysis; not separately live-tested) | su+sh (static analysis; not separately live-tested) |
| 3 ver/vfs poll → su root | N/A | static only (needs ROOT mode activated; same mechanism as path 2) |

## Evidence files

- `poc_notice_sh.txt` — Play path 1, `uid=10358(u0_a358) ... untrusted_app_34`.
- `poc_generic_su_uid0.txt` — Play path 2 su, `uid=0(root) ... u:r:ksu:s0`.
- `poc_generic_sh_appuid.txt` — the sh branch of Play path 2.
- `cn_path1_notice_sh.txt` — official-site/GitHub path 1, `uid=10355(u0_a355) ... untrusted_app_27`.
- `cn_path2_generic_su_uid0.txt` — official-site/GitHub path 2 su, `uid=0(root)`.
- `cn_checkapkupdate_preconsent_root.txt`, `cn_checkapkupdate_consent_screen.png`, and
  `cn_checkapkupdate_su_sh_proof.txt` — official-site/GitHub 1.5.2 version check: sustained injection recorded 19
  `uid=0(root) ... u:r:ksu:s0` outputs about 6 to 8 seconds apart; the 00:57 screenshot shows the
  consent dialog still on screen, and a separate sh output shows `uid=10355(u0_a355)`.
- `before-consent/` — Play fresh install: direct evidence that the command ran before the user tapped
  "Agree" (screenshot + timestamp + result file).
- `inject_log.txt`, `env.txt` — module log and environment. Besides getNotices / getRenewalGoodsList,
  inject_log also shows discovery-phase injection into `getAppConfigs` / `version/checkApkUpdate`: these
  are other endpoints of the same generic callback. Only Play 1.5.2 runs the async checkApkUpdate branch
  through sh alone; the official-site/GitHub 1.5.2 su+sh branch is live-tested above (see report 4.2 and 6).
  getAppConfigs remains untested separately.

## Boundary

Client-side verification (responses injected locally); it proves the client executes server-controlled
commands, and does not prove the vendor's server currently sends them. The primary evidence is the
static code branches. Path 3 was not live-tested (its poller starts only after ROOT mode is activated,
which also triggers the app's own injection and anti-tamper, so it was not driven, to avoid side effects).
