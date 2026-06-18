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

# Run a single test class
mvn -pl tck test -Dtest=AgentAnnotationTests

# Run a single integration test class (failsafe)
mvn -pl tck verify -Dit.test=AgentSmokeTest

# Generate API signature files
mvn -pl tck verify -Psignature-generation
```

The `weld-embedded` profile must be active when running TCK integration tests locally (it provides the Arquillian container). Without it, `@Deployed` tests will fail to find a container.

## Architecture

### API module (`api/`)
Defines the `jakarta.ai.agent` package. All types are annotations or interfaces; there is no implementation code here.

| Type | Purpose |
|---|---|
| `@Agent` | CDI stereotype for an agent class. Default scope is `@WorkflowScoped`. |
| `@Trigger` | CDI observer method that starts a workflow. Must observe a CDI event. |
| `@Decision` | Method querying the LLM to decide workflow branching. Returns `boolean`, `Result`, or a domain object. |
| `@Action` | Sequential step within the workflow. |
| `@Outcome` | Terminal step; marks the end of the workflow. |
| `@HandleException` | Exception handler within the workflow. |
| `@WorkflowScoped` | Custom CDI normal scope — one context per workflow execution. |
| `LargeLanguageModel` | Injectable LLM facade. Prompt parameters use `{}` as positional placeholders (like SLF4J). JSON Binding is used for type conversion. |
| `Result` | Built-in record for boolean + detail return from `@Decision`. |
| `LLMException` | Unchecked exception wrapping LLM service errors. |

### TCK module (`tck/`)

TCK tests live in `src/main/java` (not `src/test/java`) and are compiled to classes that implementors run against their implementation. Only the internal framework unit tests live in `src/test/java`.

**Test categories** (controlled by JUnit 5 tags):
- `@Standalone` — reflection-based structural tests; no container needed.
- `@Deployed` — Arquillian integration tests; require a CDI container (weld-embedded in CI).

**Test infrastructure classes** (not specification tests; used by implementors and integration tests):
- `LargeLanguageModelStub` — `@ApplicationScoped` CDI bean implementing `LargeLanguageModel`. Queues scripted responses via `enqueueResponse(...)` and records all calls for assertion. Reset between tests with `reset()`.
- `ExecutionTraceRecorder` — `@ApplicationScoped` CDI bean. Records lifecycle phase calls (`TRIGGER`, `DECISION`, `ACTION`, `OUTCOME`, `HANDLE_EXCEPTION`) for ordering assertions via `assertOrder(Phase...)`.

**Test annotations:**
- `@Assertion(id, section, strategy)` — meta-annotation wrapping `@Test`; maps each test to a spec requirement ID.
- `@Deployed` — class-level; adds `ArquillianExtension` + `AssertionExtension`.
- `@Standalone` — class-level; adds only `AssertionExtension`.

### Key design constraints

- The `@Trigger` phase is a CDI observer — plain CDI invokes it without any agent engine. The `@Decision`, `@Action`, and `@Outcome` phases require a Reference Implementation orchestration engine to dispatch them. The `AgentSmokeTest.fullLifecycleRequiresReferenceImplementation` test is `@Disabled` for this reason.
- `LargeLanguageModel` implementations must maintain per-workflow conversational state (isolated across concurrent workflows even for `@ApplicationScoped` agents).
- Serialization uses Jakarta JSON Binding (not Jackson or other libs).
- Java 17 minimum; Jakarta EE 10 / CDI 4.1 minimum.
