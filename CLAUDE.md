# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Jakarta Agentic AI** specification project — a vendor-neutral Jakarta EE API for building AI agents. It is structured as a multi-module Maven project with four modules: `api`, `spec`, `tck`, and `examples`.

## Build Commands

```bash
# Full build (required for CI — activates the weld-embedded profile for Arquillian)
mvn clean install -Pweld-embedded

# Build only the TCK and its required upstream modules
mvn --projects tck --also-make verify

# Clean build of TCK and upstream
mvn --projects tck --also-make clean install

# Run a single TCK assertion class (tests are compiled from src/main/java and executed by Failsafe)
mvn -pl tck verify -Dgroups=standalone -Dit.test=AgentAnnotationTests

# Run a single deployed integration test class (requires Arquillian container profile)
mvn -pl tck verify -Pweld-embedded -Dit.test=AgentSmokeTest

# Generate API signature files
mvn -pl tck verify -Psignature-generation
```

The `weld-embedded` profile must be active when you want to execute deployed TCK integration tests locally (it provides the Arquillian container). Without it, deployed-tagged tests are excluded by default in the TCK Maven configuration.

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
| `@HandleException` | Exception handler within the workflow. |
| `@WorkflowScoped` | Custom CDI normal scope — one context per workflow execution. |
| `LargeLanguageModel` | Injectable LLM facade. Prompt parameters use `{}` as positional placeholders (like SLF4J). JSON Binding is used for type conversion. |
| `Result` | Built-in record for boolean + detail return from `@Decision`. |
| `LLMException` | Unchecked exception wrapping LLM service errors. |

### TCK module (`tck/`)

TCK tests live in `src/main/java` (not `src/test/java`) and are compiled to classes that implementors run against their implementation. Only the internal framework unit tests live in `src/test/java`.

**Test categories and assertion mapping** (controlled by JUnit 5 tags and meta-annotations):
- `@Standalone` — reflection-based structural tests; no container needed.
- `@Deployed` — Arquillian integration tests; requires a full container (weld-embedded in CI).
- `@RequiresImplementation` — skips the test when no compatible implementation is present.
- `@RequiresNoImplementation` — skips the test when a compatible implementation is present; used for plain-CDI baseline assertions (trigger-only).
- `@Assertion(id, section, strategy)` — wraps `@Test` and maps the test to a specification requirement ID, section, and verification strategy.

**Test infrastructure classes** (not specification tests; used by implementors and integration tests):
- `LargeLanguageModelStub` — `@ApplicationScoped` CDI bean implementing `LargeLanguageModel`. Queues scripted responses via `enqueueResponse(...)` and records all calls for assertion. Reset between tests with `reset()`.
- `ExecutionTraceRecorder` — `@ApplicationScoped` CDI bean. Records lifecycle phase calls (`TRIGGER`, `DECISION`, `ACTION`, `OUTCOME`, `HANDLE_EXCEPTION`) for ordering assertions via `assertOrder(Phase...)`.

### Key design constraints

- In plain CDI-only execution, the `@Trigger` phase is invoked by CDI events. The `@Decision`, `@Action`, and `@Outcome` phases require a compatible implementation to dispatch them. The `AgentSmokeTest.fullLifecycleRequiresCompatibleImplementation` test is conditionally skipped unless a compatible implementation is present.
- `LargeLanguageModel` implementations must maintain per-workflow conversational state (isolated across concurrent workflows even for `@ApplicationScoped` agents).
- Serialization uses Jakarta JSON Binding (not Jackson or other libs).
- Java SE 17 and Jakarta EE 10 are the baseline, declared once in the root pom: `maven.compiler.release` for the language level (enforced by `maven-enforcer-plugin`) and `jakarta.ee.version` for the platform.
- Modules depend on the **platform umbrella** `jakarta.platform:jakarta.jakartaee-api`, never on individual Jakarta APIs. Declare it with no `<version>` and no `<scope>` — both come from the root `dependencyManagement` (`provided`). CDI, JSON-P and JSON-B all arrive through it. Do not add `cdi-api`, `json-api` or `json.bind-api` back as direct dependencies.
- Do not swap the umbrella for an import of `jakarta.platform:jakarta.jakartaee-bom`: its `cdi-api` entry has a wildcard `<exclusion>` that strips the transitive CDI modules and breaks `requires jakarta.cdi` in `module-info.java`.
- The umbrella jar has no `module-info` and no `Automatic-Module-Name`, so maven-compiler-plugin puts it on the classpath while the modular API jars (including `cdi-api`) go on the module path. That is why `requires jakarta.cdi` in `api` still resolves even though `cdi-api` is no longer declared directly.
- `api` and `tck` are published; `spec`, `examples` and the aggregator (beyond its pom) are not. Staging suppresses modules with `maven.deploy.skip`, snapshot publishing with `skipPublishing` — a module that should not be published needs **both**. The parent pom is published because `api` and `tck` declare it as their parent; this matches other Jakarta specs (`jakarta.data-parent`, `jakarta.enterprise.cdi-parent`).
- CycloneDX SBOMs are generated for the published modules only: `cyclonedx.skip` is `true` in the root pom and `false` in `api` and `tck`. The aggregator's own BOM would otherwise describe unpublished modules and GAVs that never reach a repository.
- Do not reintroduce a `stage-release` javadoc `<skip>` in `tck`: Maven Central rejects a release without a javadoc jar.

## Project site (`gh-pages` branch)

The site at https://jakartaee.github.io/agentic-ai/ is **not** built from `main`. It lives on the `gh-pages` branch, which shares no history with `main` and is not a Maven module.

- Editable sources are `content.html` (home page body), `tutorial/index.html` (a standalone self-styled page with none of the jakarta.ee chrome) and `assets/`.
- `index.html` is **generated and not committed**. `.github/workflows/pages.yml` fetches https://jakarta.ee/specifications/agentic-ai/1.0/ on every push to the branch, and `build-site.py` wraps `content.html` in the Eclipse toolbar, mega-menu and Solstice footer sliced out of it. It is `.gitignore`d. Never hand-edit it.
- Because the shell is fetched live, a jakarta.ee redesign reaches the site with no commit here.
- The deploy **fails rather than falling back** if the fetch or the slice fails. That is deliberate: `deploy-pages` only replaces the live site on success, so a failure leaves the previous deployment serving and costs a re-run, whereas a fallback would publish stale chrome behind a green tick.
- A slice failure means jakarta.ee restructured and the line-matching patterns in `build-site.py` no longer locate the header or footer. Fix the patterns — and re-check the Google Tag Manager point below.
- The home page hero copy (`TITLE`, `SUMMARY` and the jumbotron markup) is in `build-site.py`, not `content.html`.
- To preview locally: `curl -s -L https://jakarta.ee/specifications/agentic-ai/1.0/ -o /tmp/shell.html && python3 build-site.py /tmp/shell.html index.html content.html`.
- GitHub Pages is configured with `build_type: workflow`, so the branch is what CI builds from rather than what is served directly. The `source.branch` value in the Pages config is vestigial; the `github-pages` environment's deployment branch policy is what actually gates deploys.
- `upload-pages-artifact` excludes `.git`, `.github` and (since v4) dotfiles, so the published artifact is `index.html`, `content.html`, `build-site.py`, `assets/` and `tutorial/`. Jekyll never runs, which is why the branch carries no `.nojekyll`.
