#!/usr/bin/env python3
"""
Assemble the Jakarta Agentic AI project site from the current jakarta.ee shell.

The chrome -- Eclipse toolbar, Jakarta EE mega-menu navigation, and the Solstice
footer -- is lifted from a saved copy of a live jakarta.ee page so the site matches
the current design rather than an older revision of it. Only the parts that identify
the project are substituted.

Refresh the shell by re-saving the reference page and re-running this script:

    curl -s -L https://jakarta.ee/specifications/agentic-ai/1.0/ -o ref-specpage.html
    python3 build-site.py ref-specpage.html site/index.html content.html

Google Tag Manager is deliberately dropped: it loads the Foundation's analytics
container, which is not ours to fire.
"""
import re, sys, pathlib

REF = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])
CONTENT = pathlib.Path(sys.argv[3])

lines = REF.read_text(encoding="utf-8").split("\n")


def find(pattern, after=0):
    return next(i for i, l in enumerate(lines) if i >= after and re.search(pattern, l))


# Chrome header: the skip link through the end of the navigation, i.e. everything
# before the page's own <header class="header-wrapper">.
skip = find(r'class="sr-only" href="#content"')
hdr = find(r'<header class="header-wrapper')
chrome_header = "\n".join(lines[skip:hdr])

# Chrome footer: <footer id="solstice-footer"> through </footer>.
foot = find(r'id="solstice-footer"') - 1
foot_end = find(r"^</footer>", foot)
chrome_footer = "\n".join(lines[foot:foot_end + 1])


def absolutize(markup):
    """The shell is written for jakarta.ee, where root-relative URLs resolve against
    that host. Served from jakartaee.github.io they would resolve against this site,
    so point them back at jakarta.ee."""
    markup = re.sub(r'(href|src|action)="/(?!/)', r'\1="https://jakarta.ee/', markup)
    markup = re.sub(r'(href|src)="//', r'\1="https://', markup)
    return markup


chrome_header = absolutize(chrome_header)
chrome_footer = absolutize(chrome_footer)

TITLE = "Jakarta Agentic AI"
SUMMARY = ("Jakarta Agentic AI provides a set of vendor-neutral APIs that make it easy, "
           "consistent, and reliable to build, deploy, and run AI agents on Jakarta EE runtimes.")

page = f"""<!DOCTYPE html>
<html lang="en-US">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>{TITLE}</title>
<meta name="description" content="{SUMMARY}">
<meta property="og:title" content="{TITLE}">
<meta property="og:description" content="{SUMMARY}">
<meta property="og:type" content="website">
<link href="https://jakarta.ee/images/jakarta/favicon.ico" rel="icon" type="image/x-icon"/>
<link rel="stylesheet" href="https://jakarta.ee/css/styles.v2.css">
<link rel="stylesheet" href="assets/css/style.css">
<link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,400&display=swap" rel="stylesheet" type="text/css"/>
<link href="https://fonts.googleapis.com/css2?family=Exo:wght@400;700&display=swap" rel="stylesheet">
</head>
<body>
{chrome_header}
<header class="header-wrapper header-home header-secondary-bg-img" id="header-wrapper">
  <div class="jumbotron featured-jumbotron featured-jumbotron-default margin-bottom-0">
    <div class="text-center">
      <div class="top">
        <div class="container">
          <div class="row">
            <span class="eyebrow">Jakarta EE Specification</span>
            <h1>Build <span class="text-primary-orange">AI agents</span><br>the Jakarta EE way</h1>
            <h2>Vendor-neutral APIs that make it easy, consistent, and reliable<br/>
                to build, deploy, and run AI agents on Jakarta EE runtimes</h2>
            <ul class="jumbotron-links list-inline">
              <li>
                <a class="btn btn-outline-primary" href="https://jakarta.ee/specifications/agentic-ai/1.0/"
                  >Specification<i class="fa fa-chevron-right"></i></a>
              </li>
              <li>
                <a class="btn btn-primary" href="https://github.com/jakartaee/agentic-ai"
                  >Get involved<i class="fa fa-chevron-right"></i></a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</header>

<main>
  <div class="container">
{CONTENT.read_text(encoding='utf-8')}
  </div>
</main>

{chrome_footer}
<script src="https://jakarta.ee/js/solstice.v2.js"></script>
<script src="assets/js/highlight.js"></script>
</body>
</html>
"""

OUT.write_text(page, encoding="utf-8")
print(f"wrote {OUT} ({len(page):,} bytes)")
print(f"  chrome header: reference lines {skip}-{hdr - 1}")
print(f"  chrome footer: reference lines {foot}-{foot_end}")
