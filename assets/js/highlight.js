/*
 * Minimal syntax highlighting for the code samples on this page.
 *
 * Emits Chroma token classes -- the same ones Hugo produces on jakarta.ee -- so the
 * samples are coloured by the site's own stylesheet and match the specification
 * pages exactly. Operates on the escaped text content of each block, so it cannot
 * inject markup.
 */
(function () {
  "use strict";

  // Chroma class per keyword group: kd declaration, kt type, kc constant, k other.
  var GROUPS = {
    kd: ["abstract", "class", "final", "interface", "private", "protected", "public",
         "record", "static"],
    kt: ["boolean", "char", "double", "float", "int", "long", "short", "void"],
    kc: ["false", "null", "true"],
    k:  ["break", "case", "catch", "continue", "default", "do", "else", "enum",
         "extends", "finally", "for", "if", "implements", "import", "instanceof",
         "new", "package", "return", "super", "switch", "this", "throw", "throws",
         "try", "while"]
  };

  var KEYWORD_CLASS = {};
  var ALL_KEYWORDS = [];
  Object.keys(GROUPS).forEach(function (cls) {
    GROUPS[cls].forEach(function (word) {
      KEYWORD_CLASS[word] = cls;
      ALL_KEYWORDS.push(word);
    });
  });

  // One pass, alternation ordered so comments and strings win over what they contain.
  var JAVA = new RegExp(
    "(/\\*[\\s\\S]*?\\*/)" +                        // 1: block comment  -> cm
    "|(//[^\\n]*)" +                                // 2: line comment   -> c1
    "|(\"(?:\\\\.|[^\"\\\\])*\")" +                 // 3: string         -> s
    "|(@[A-Za-z_$][\\w$]*)" +                       // 4: annotation     -> nd
    "|\\b(" + ALL_KEYWORDS.join("|") + ")\\b",      // 5: keyword
    "g"
  );

  var XML = /(&lt;\/?[A-Za-z][\w.-]*&gt;)/g;        // tag -> nt

  function escape(text) {
    return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
  }

  function span(cls, body) {
    return '<span class="' + cls + '">' + body + "</span>";
  }

  function highlightJava(escaped) {
    return escaped.replace(JAVA, function (match, block, line, string, annotation, keyword) {
      if (block) { return span("cm", block); }
      if (line) { return span("c1", line); }
      if (string) { return span("s", string); }
      if (annotation) { return span("nd", annotation); }
      if (keyword) { return span(KEYWORD_CLASS[keyword], keyword); }
      return match;
    });
  }

  function highlightXml(escaped) {
    return escaped.replace(XML, function (match) {
      return span("nt", match);
    });
  }

  var blocks = document.querySelectorAll("pre.chroma > code");
  for (var i = 0; i < blocks.length; i++) {
    var block = blocks[i];
    var escaped = escape(block.textContent);
    block.innerHTML = block.getAttribute("data-lang") === "xml"
      ? highlightXml(escaped)
      : highlightJava(escaped);
  }
})();
