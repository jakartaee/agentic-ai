#!/usr/bin/env python3
"""
Assemble the Jakarta Agentic AI project site from the Solstice shell that the
sibling Jakarta specification sites (REST, JSON-B) use.

The chrome -- Eclipse toolbar, Jakarta EE global nav, and the Solstice footer --
is lifted verbatim from https://jakartaee.github.io/rest/ so the page matches its
peers. Only the parts that identify the project are substituted. Google Tag
Manager is deliberately dropped: it loads the Foundation's analytics container,
which is not ours to fire.
"""
import re, sys, pathlib

REF = pathlib.Path(sys.argv[1])
OUT = pathlib.Path(sys.argv[2])
CONTENT = pathlib.Path(sys.argv[3])

ref = REF.read_text(encoding="utf-8")
lines = ref.split("\n")


def slice_between(start_pat, end_pat, after=0):
    """Return the inclusive slice of lines from the first line matching
    start_pat (at or after `after`) to the first subsequent line matching
    end_pat."""
    start = next(i for i, l in enumerate(lines) if i >= after and re.search(start_pat, l))
    end = next(i for i, l in enumerate(lines) if i > start and re.search(end_pat, l))
    return start, end


# Chrome: everything from the skip link through the end of the global nav,
# i.e. up to the line immediately before <header class="header-wrapper">.
skip = next(i for i, l in enumerate(lines) if 'class="sr-only" href="#content"' in l)
hdr = next(i for i, l in enumerate(lines) if 'class="header-wrapper' in l)
chrome_header = "\n".join(lines[skip:hdr])

# The shell was copied from the REST site, so a handful of links in it still point
# at that project or resolve against this site's root rather than jakarta.ee.
FIXUPS = [
    ("https://jakartaee.github.io/rest/", "https://jakarta.ee/"),
    ('href="/compatibility/download/"', 'href="https://jakarta.ee/compatibility/download/"'),
    ('href="/release/"', 'href="https://jakarta.ee/release/"'),
]

# Footer: the social-media band through </footer> plus the solstice script.
soc = next(i for i, l in enumerate(lines) if 'id="social-media"' in l)
foot_end = next(i for i, l in enumerate(lines) if l.strip() == "</footer>")
chrome_footer = "\n".join(lines[soc:foot_end + 1])

for old, new in FIXUPS:
    chrome_header = chrome_header.replace(old, new)
    chrome_footer = chrome_footer.replace(old, new)

TITLE = "Jakarta Agentic AI"
SUMMARY = ("Jakarta Agentic AI provides a set of vendor-neutral APIs that make it easy, "
           "consistent, and reliable to build, deploy, and run AI agents on Jakarta EE runtimes.")
HOME = "https://jakartaee.github.io/agentic-ai/"

SIDEBAR_LINKS = [
    ("Sources", "https://github.com/jakartaee/agentic-ai"),
    ("APIs", "https://jakarta.ee/specifications/agentic-ai/1.0/apidocs/"),
    ("Documentation", "https://jakarta.ee/specifications/agentic-ai/1.0/jakarta-agentic-ai-1.0.0-M1.html"),
    ("Download", "https://github.com/jakartaee/agentic-ai/releases"),
    ("Issue Tracker", "https://github.com/jakartaee/agentic-ai/issues"),
    ("Mailing list", "https://accounts.eclipse.org/mailing-list/agentic-ai-dev"),
    ("Project page", "https://projects.eclipse.org/projects/ee4j.agentic-ai"),
]

sidebar_items = "\n".join(
    f"""    <li>
      <i class="fa fa-caret-right fa-fw"></i>
      <a target="_self" href="{href}">{label}</a>
    </li>"""
    for label, href in SIDEBAR_LINKS
)

page = f"""<!DOCTYPE html>
<html lang="en-US">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="icon" href="assets/img/favicon.png" type="image/png">
    <!-- Links and stylesheets -->
    <link rel="stylesheet" href="https://jakarta.ee/css/styles.css">
    <link rel="stylesheet" href="assets/css/style.css">
    <link href="https://fonts.googleapis.com/css2?family=Open+Sans:ital,wght@0,300;0,400;0,600;0,700;1,400&display=swap" rel="stylesheet" type="text/css"/>
    <title>{TITLE}</title>
    <meta name="description" content="{SUMMARY}">
    <meta property="og:title" content="{TITLE}">
    <meta property="og:description" content="{SUMMARY}">
    <meta property="og:type" content="website">
  </head>
  <body>
{chrome_header}
<header class="header-wrapper header-default-bg-img" id="header-wrapper">
  <div class="jumbotron featured-jumbotron featured-jumbotron-default margin-bottom-0">
    <div class="container">
      <div class="row">
        <div class="col-md-24 col-sm-18 ">
          <h1>{TITLE}</h1>
          <h2>{SUMMARY}</h2>
        </div>
      </div>
    </div>
  </div>
</header>

    <section class="default-breadcrumbs hidden-print" id="breadcrumb">
  <div class="container">
  <h3 class="sr-only">Breadcrumbs</h3>
  <div class="row">
    <div class="col-sm-24">
      <ol class="breadcrumb">
        <li>
          <a href="https://jakarta.ee/">Home</a>
        </li>
        <li class="active">
          <a href="{HOME}">{TITLE}</a>
        </li>
      </ol>
    </div>
  </div>
</section>

    <main id="content">
      <div class="container">
        <div class="row">
          <div class="col-md-18 padding-bottom-30">
{CONTENT.read_text(encoding='utf-8')}
          </div>
          <div class="col-md-6  padding-bottom-30">
            <!-- nav -->
<aside id="main-sidebar">
  <ul id="leftnav" class="ul-left-nav fa-ul hidden-print">
    <li class="separator">
      <a class="separator" href="{HOME}">Project Resources</a>
    </li>
{sidebar_items}
  </ul>
  </aside>

          </div>
        </div>
      </div>
    </main>

{chrome_footer}
<script src="https://jakarta.ee/js/solstice.js"></script>
<script src="assets/js/highlight.js"></script>
  </body>
</html>
"""

OUT.write_text(page, encoding="utf-8")
print(f"wrote {OUT} ({len(page)} bytes)")
print(f"  chrome header: lines {skip}-{hdr - 1}")
print(f"  chrome footer: lines {soc}-{foot_end}")
