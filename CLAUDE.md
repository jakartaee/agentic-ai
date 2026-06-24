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
- Java 17 minimum; Jakarta EE 10 / CDI 4.1 minimum.
