#!/usr/bin/env python3
import sys, re, html
from markdown_it import MarkdownIt

src, out = sys.argv[1], sys.argv[2]
text = open(src, encoding="utf-8").read()

m = re.search(r'^#\s+(.+)$', text, re.M)
title = html.escape(m.group(1).strip()) if m else "Report"

md = (MarkdownIt("commonmark", {"html": False, "linkify": True, "typographer": False})
      .enable("table").enable("strikethrough"))
body = md.render(text)

CSS = """
@page { size: A4; margin: 18mm 16mm; }
html { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
body {
  font-family: "Noto Sans", "Noto Sans CJK SC", "DejaVu Sans", sans-serif;
  font-size: 10.5pt; line-height: 1.5; color: #1a1a1a; max-width: 100%;
}
h1 { font-size: 18pt; line-height: 1.25; margin: 0 0 4pt; }
h2 { font-size: 13pt; margin: 18pt 0 6pt; padding-bottom: 3pt; border-bottom: 1px solid #ccc; }
h3 { font-size: 11pt; margin: 12pt 0 4pt; }
h1, h2, h3 { break-after: avoid; }
p { margin: 5pt 0; }
ul, ol { margin: 5pt 0; padding-left: 20pt; }
li { margin: 2pt 0; }
code {
  font-family: "Noto Sans Mono CJK SC", "DejaVu Sans Mono", monospace;
  font-size: 9pt; background: #f2f2f2; padding: 0.5pt 3pt; border-radius: 3px;
  word-break: break-word;
}
pre {
  background: #f6f8fa; border: 1px solid #e1e4e8; border-radius: 5px;
  padding: 8pt 10pt; overflow: auto; break-inside: avoid; margin: 6pt 0;
}
pre code { background: none; padding: 0; font-size: 8.5pt; line-height: 1.4; white-space: pre-wrap; }
table { border-collapse: collapse; width: 100%; margin: 6pt 0; font-size: 9pt; break-inside: avoid; }
th, td { border: 1px solid #ccc; padding: 3pt 6pt; text-align: left; vertical-align: top; word-break: break-word; }
th { background: #f2f2f2; }
a { color: #0b5fa5; text-decoration: none; word-break: break-all; }
blockquote { margin: 6pt 0; padding: 2pt 12pt; border-left: 3px solid #ccc; color: #444; }
hr { border: none; border-top: 1px solid #ddd; margin: 12pt 0; }
"""

page = f"""<!doctype html>
<html lang="{'zh' if out.endswith('zh.html') else 'en'}">
<head><meta charset="utf-8"><title>{title}</title>
<style>{CSS}</style></head>
<body>{body}</body></html>"""

open(out, "w", encoding="utf-8").write(page)
print("wrote", out, "title=", title)
