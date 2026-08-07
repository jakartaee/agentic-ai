/*
 * Minimal syntax highlighting for the code samples on this page.
 *
 * Deliberately tiny and dependency-free: the site ships two static snippets,
 * which does not justify pulling in a highlighting library. Operates on the
 * already-escaped text content of each block, so it cannot inject markup.
 */
(function () {
  "use strict";

  var KEYWORDS = [
    "abstract", "boolean", "break", "case", "catch", "char", "class", "continue",
    "default", "do", "double", "else", "enum", "extends", "final", "finally",
    "float", "for", "if", "implements", "import", "instanceof", "int", "interface",
    "long", "new", "null", "package", "private", "protected", "public", "record",
    "return", "static", "super", "switch", "this", "throw", "throws", "true",
    "false", "try", "void", "while"
  ];

  // One pass, alternation ordered so that comments and strings win over
  // everything they contain.
  var JAVA = new RegExp(
    "(/\\*[\\s\\S]*?\\*/|//[^\\n]*)" +          // 1: comment
    "|(\"(?:\\\\.|[^\"\\\\])*\")" +             // 2: string
    "|(@[A-Za-z_$][\\w$]*)" +                   // 3: annotation
    "|\\b(" + KEYWORDS.join("|") + ")\\b",      // 4: keyword
    "g"
  );

  var XML = new RegExp(
    "(&lt;/?[A-Za-z][\\w.-]*&gt;)",             // 1: tag
    "g"
  );

  function escape(text) {
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  }

  function span(cls, body) {
    return '<span class="tok-' + cls + '">' + body + "</span>";
  }

  function highlightJava(escaped) {
    return escaped.replace(JAVA, function (match, comment, string, annotation, keyword) {
      if (comment) { return span("comment", comment); }
      if (string) { return span("string", string); }
      if (annotation) { return span("ann", annotation); }
      if (keyword) { return span("key", keyword); }
      return match;
    });
  }

  function highlightXml(escaped) {
    return escaped.replace(XML, function (match) {
      return span("tag", match);
    });
  }

  var blocks = document.querySelectorAll("pre.code > code");
  for (var i = 0; i < blocks.length; i++) {
    var block = blocks[i];
    var escaped = escape(block.textContent);
    block.innerHTML = block.classList.contains("xml")
      ? highlightXml(escaped)
      : highlightJava(escaped);
  }
})();
