# FakeLocation command-execution disclosure materials

[中文版 / Chinese](README.zh.md)

Security analysis of the Android app FakeLocation (packages `com.lerist.fakelocation` /
`dev.lerist.fakelocation`, developer Lerist / Chengdu Lanyi Technology): the app contains an
undocumented, server-controlled command execution capability; the generic-callback path can run as root,
and fires at app startup before the user accepts the consent dialog. Affected versions: 1.3.2.1 through 1.5.2.

Reporter: [tyldx](https://github.com/tyldx) · contact tyldx@proton.me

## Contents

- `REPORT.en.md` / `REPORT.zh.md` — the full report (English / Chinese). PDF copies are not tracked in
  this repository; generate them on demand from the Markdown with `tools/md2html.py`.
- `poc/` — dynamic verification. `xposed-rce-poc/` is the LSPosed PoC module source (hooks only the
  okhttp response and rewrites it by URL to trigger the app's own command-execution branches); `results/` holds
  the `id` command outputs and other evidence.
- `hashes/` — sample SHA-256 values.
- `tools/` — `md2html.py` renders a report to HTML, which is then printed to the PDF.
- `evidence/archive-urls.md` — archive.today links for the official site, policy pages, store, and repo.

## Not included

The vendor's APKs and the decompiled vendor code are not redistributed here (copyright, and the binaries
contain the undisclosed code). Samples are referenced by hash and the original pages by archive link.

## Boundary

The PoC is a client-side demonstration (responses injected locally); it proves the client executes
server-controlled commands, and does not prove the vendor's server currently sends them. For security
disclosure only.
