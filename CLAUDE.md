# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Jakarta Agentic AI** specification project — a vendor-neutral Jakarta EE API for building AI agents. It is structured as a multi-module Maven project with five modules: `api`, `spec`, `tck`, `tck-dist`, and `examples`.

## Build Commands

```bash
# Full build (what CI runs)
mvn clean install

# Build only the TCK and its required upstream modules
mvn --projects tck --also-make verify

# Clean build of TCK and upstream
mvn --projects tck --also-make clean install

# Run a single TCK assertion class (tests are compiled from src/main/java and executed by Failsafe)
mvn -pl tck verify -Dgroups=standalone -Dit.test=SignatureTests

# Run a single deployed integration test class
mvn -pl tck verify -Dit.test=AgentSmokeTest

# Generate API signature files
mvn -pl tck verify -Psignature-generation
```

The TCK has the same shape as the Data and Concurrency TCKs: this repository builds the jar and runs only the container-less signature test (`<groups>standalone</groups>` in the `tck` Failsafe config); every deployed test is executed by the implementation (Payara) from its own repository, all of them, `Skipped: 0`. There is no Weld, no stand-in `LargeLanguageModel`, no run mode, no tag, no profile, no marker, no execution condition and no `tck.implementation` property — all of that existed before an implementation did and was removed as scaffolding. Nothing in the module may exist that an implementation is not required to pass; do not add tests whose assertions hold only without an implementation, tests that reflect over fixtures instead of exercising the implementation, or TCK-owned doubles for things the implementation provides. If a spec-dev wants to run the deployed tests, that happens through the implementation's runner, like every other spec.

## Architecture

### API module (`api/`)
Defines the `jakarta.ai.agent` package. Most types are annotations or interfaces.

| Type | Purpose |
|---|---|
| `@Agent` | Declares an agent class. Default scope is `@WorkflowScoped` when no scope annotation is present. |
| `@Trigger` | Workflow entry method invoked by CDI events. Can receive the triggering event (with optional `@Observes`). |
| `@Decision` | Method that decides whether and how the workflow should proceed. Returns `boolean`, `Result`, or a domain object. |
| `@Action` | Step within the workflow. |
| `@Outcome` | Terminal step; marks the end of the workflow. |
| `@OnException` | Exception handler within the workflow. |
| `@WorkflowScoped` | Custom CDI normal scope — one context per workflow execution. |
| `LargeLanguageModel` | Injectable LLM facade. Prompt parameters use `{}` as positional placeholders (like SLF4J). JSON Binding is used for type conversion. |
| `Result` | Built-in record for boolean + detail return from `@Decision`. |
| `LLMException` | Unchecked exception wrapping LLM service errors. |

### TCK module (`tck/`)

TCK tests live in `src/main/java` (not `src/test/java`) and are compiled to classes that implementors run against their implementation. Only the internal framework unit tests live in `src/test/java`.

**Test categories and assertion mapping** (controlled by JUnit 5 tags and meta-annotations):
- `@Standalone` — no container needed; only the signature test carries it. There are no reflection-based structural tests: the strict signature baseline already covers targets, retention, members and meta-annotations, and Data/Concurrency have no such layer either.
- `@Deployed` — Arquillian integration tests; run only by an implementation's runner, never in this repository.
- `@Assertion(id, section, strategy)` — wraps `@Test` and maps the test to a specification requirement ID, section, and verification strategy.

**Test infrastructure classes** (not specification tests; used by implementors and integration tests):
- The TCK deploys **no** `LargeLanguageModel`: the implementation's own facade is injected and verified. Determinism comes from a certification requirement on the model behind it, defined by the constants in `framework/llm/TckModel` and the guide's "Additional Certification Requirements": a rendered prompt containing `TckModel.FAIL` fails (→ `LLMException`), a prompt equal to `TckModel.HISTORY` returns the conversation's prior rendered prompts one per line, anything else is echoed verbatim. Tests derive decisions and observations from the echo; `LlmContractTests` exercises the facade only from inside an agent phase (`LlmProbeAgent`), never from the test method, because conversational state is defined per workflow context. Do not reintroduce a TCK-owned `LargeLanguageModel` anywhere — it would test the TCK instead of the implementation.
- `ExecutionTraceRecorder` — `@ApplicationScoped` CDI bean. Records lifecycle phase calls (`TRIGGER`, `DECISION`, `ACTION`, `OUTCOME`, `HANDLE_EXCEPTION`); tests assert ordering with `assertEquals(List.of(...), trace.phases())`.
- `SignatureTests` — a `@Standalone` JUnit test that drives the Jakarta TCK signature engine (`jakarta.tck:sigtest-maven-plugin`, a compile dependency with the Maven layer excluded) against the shipped `.sig` baseline, strict in both directions, resolving JDK types from the JDK's `lib/ct.sym` at the Java SE 17 level. This is how the Data and Concurrency TCKs do it, so vendors get the signature check in their normal run. Do not reintroduce a plugin-only `strictcheck` execution as the sole check.

**Conventions that follow the Data/Concurrency TCKs:** tests use plain JUnit `Assertions`, never AssertJ, because deployed tests run inside the container and a third-party assertion library would not be in the archive on a real server (AssertJ is test-scoped for the framework's own unit tests only). Vendors run the TCK jar through Surefire `dependenciesToScan`; the starter in `tck-dist` shows the shape.

### TCK distribution module (`tck-dist/`)

Assembles the certifiable TCK zip required by the Jakarta EE TCK Process: `LICENSE` is the EFTL (`src/main/EFTL.adoc`, verbatim Eclipse text — do not edit), plus the TCK jar and sources, the parent pom, the `.sig` baseline, the AsciiDoc Reference Guide rendered to HTML and PDF, and the `starter/` runner project. The guide includes fragments of `starter/pom.xml` by `tag::` markers, so keep those markers when editing the starter. The starter fileSet is filtered so `${project.version}` becomes the released TCK version. The module is `pom`-packaged and never published to Maven Central (`maven.deploy.skip` and `skipPublishing` both set); final zips go to download.eclipse.org with a SHA-256. Challenges and certification requests use the `.github/ISSUE_TEMPLATE/tck-challenge.yml` and `certification.yml` templates (labels `challenge` and `certification`).

### Key design constraints

- In plain CDI-only execution, the `@Trigger` phase is invoked by CDI events. The `@Decision`, `@Action`, and `@Outcome` phases require a compatible implementation to dispatch them, which is why the deployed tests only run against one.
- `LargeLanguageModel` implementations must maintain per-workflow conversational state (isolated across concurrent workflows even for `@ApplicationScoped` agents).
- Serialization uses Jakarta JSON Binding (not Jackson or other libs).
- Java SE 17 and Jakarta EE 10 are the baseline, declared once in the root pom: `maven.compiler.release` for the language level (enforced by `maven-enforcer-plugin`) and `jakarta.ee.version` for the platform.
- Modules depend on the **platform umbrella** `jakarta.platform:jakarta.jakartaee-api`, never on individual Jakarta APIs. Declare it with no `<version>` and no `<scope>` — both come from the root `dependencyManagement` (`provided`). CDI, JSON-P and JSON-B all arrive through it. Do not add `cdi-api`, `json-api` or `json.bind-api` back as direct dependencies.
- Do not swap the umbrella for an import of `jakarta.platform:jakarta.jakartaee-bom`: its `cdi-api` entry has a wildcard `<exclusion>` that strips the transitive CDI modules and breaks `requires jakarta.cdi` in `module-info.java`.
- The umbrella jar has no `module-info` and no `Automatic-Module-Name`, so maven-compiler-plugin puts it on the classpath while the modular API jars (including `cdi-api`) go on the module path. That is why `requires jakarta.cdi` in `api` still resolves even though `cdi-api` is no longer declared directly.
- `api` and `tck` are published; `spec`, `tck-dist`, `examples` and the aggregator (beyond its pom) are not. Staging suppresses modules with `maven.deploy.skip`, snapshot publishing with `skipPublishing` — a module that should not be published needs **both**. The parent pom is published because `api` and `tck` declare it as their parent; this matches other Jakarta specs (`jakarta.data-parent`, `jakarta.enterprise.cdi-parent`).
- CycloneDX SBOMs are generated for the published modules only: `cyclonedx.skip` is `true` in the root pom and `false` in `api` and `tck`. The aggregator's own BOM would otherwise describe unpublished modules and GAVs that never reach a repository.
- Do not reintroduce a `stage-release` javadoc `<skip>` in `tck`: Maven Central rejects a release without a javadoc jar.

## Project site (`gh-pages` branch)

The site at https://jakartaee.github.io/agentic-ai/ is **not** built from `main`. It lives on the `gh-pages` branch, which shares no history with `main` and is not a Maven module.

- Editable sources are `content.html` (home page body), `tutorial/index.html` (a standalone self-styled page with none of the jakarta.ee chrome) and `assets/`.
- `index.html` is **generated and not committed**. `.github/workflows/pages.yml` fetches https://jakarta.ee/specifications/agentic-ai/1.0/ on every push to the branch, and `build-site.py` wraps `content.html` in the Eclipse toolbar, mega-menu and Solstice footer sliced out of it. It is `.gitignore`d. Never hand-edit it.
- Because the shell is fetched live, a jakarta.ee redesign reaches the site with no commit here.
- The deploy **fails rather than falling back** if the fetch or the slice fails. That is deliberate: `deploy-pages` only replaces the live site on success, so a failure leaves the previous deployment serving and costs a re-run, whereas a fallback would publish stale chrome behind a green tick.
- A slice failure means jakarta.ee restructured and the line-matching patterns in `build-site.py` no longer locate the header or footer. Fix the patterns.
- The home page hero copy (`TITLE`, `SUMMARY` and the jumbotron markup) is in `build-site.py`, not `content.html`.
- To preview locally: `curl -s -L https://jakarta.ee/specifications/agentic-ai/1.0/ -o /tmp/shell.html && python3 build-site.py /tmp/shell.html index.html content.html`.
- GitHub Pages is configured with `build_type: workflow`, so the branch is what CI builds from rather than what is served directly. The `source.branch` value in the Pages config is vestigial; the `github-pages` environment's deployment branch policy is what actually gates deploys.
- `upload-pages-artifact` excludes `.git`, `.github` and dotfiles; the workflow additionally deletes `build-site.py` and `content.html` from the runner workspace right before upload, since both are build-time inputs consumed by the assemble step and nothing at runtime references them. The published artifact is `index.html`, `assets/` and `tutorial/`. Jekyll never runs, which is why the branch carries no `.nojekyll`.
