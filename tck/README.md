# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI 1.0 Specification.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Structural**: the annotations, `LargeLanguageModel`, `Result` and `LLMException` exist with the attributes, targets and retention the specification requires
- **Behavioral**: a compatible implementation dispatches workflows as specified — phase ordering and precedence, data propagation into phase parameters, termination, error handling, validation, inherited phases, CDI scoping and event observation, concurrency isolation, deployment errors, and the `LargeLanguageModel` contract
- **API signature**: the complete public API surface against a recorded baseline

## Test Structure

```
tck/
├── src/main/java/ee/jakarta/tck/ai/agent/
│   ├── core/
│   │   ├── agent/            # @Agent, LargeLanguageModel, Result, LLMException (structural)
│   │   ├── lifecycle/        # @Trigger, @Decision, @Action, @Outcome, @OnException (structural)
│   │   ├── cdi/              # @WorkflowScoped and @Agent CDI metadata (structural)
│   │   ├── behavior/         # Deployed workflow tests, one class per concern
│   │   │   └── agents/       # The agents those tests deploy, one package per concern
│   │   └── integration/      # Deployed smoke test
│   └── framework/
│       ├── junit/anno/       # @Assertion, @Standalone, @Deployed, @RequiresImplementation, ...
│       ├── junit/extensions/ # The JUnit conditions behind those annotations
│       ├── arquillian/       # Adds the framework beans to every deployment
│       ├── stub/             # LargeLanguageModelStub: scripted responses, recorded calls
│       ├── trace/            # ExecutionTraceRecorder: phase-order assertions
│       ├── workflow/         # Per-workflow keying for the stub outside a container
│       └── signature/        # API signature test
├── src/main/resources/
│   ├── META-INF/services/    # Registers the Arquillian extension
│   └── ee/jakarta/tck/ai/agent/framework/signature/   # Signature baseline
└── src/test/java/            # Unit tests of the framework itself; not part of the TCK
```

Tests live in `src/main/java` so they ship in the TCK jar for implementors to run; `src/test/java` holds only the framework's own unit tests.

## Building and running the TCK

This repository has no implementation under test, so its plain build runs the TCK's plain-CDI baseline in Weld Embedded. The `baseline` and `weld-embedded` profiles auto-activate whenever the `tck.implementation` property is unset:

```bash
mvn clean install
mvn --projects tck --also-make clean install
mvn --projects tck --also-make verify
```

The baseline run must succeed while skipping implementation-dependent assertions. It is not a compatibility result.

A compatibility run requires the implementation under test and its Arquillian container configuration. Naming the implementation with `-Dtck.implementation` turns off both auto-activated profiles; this repository uses Weld Embedded to prove the fail-closed default. With no compatible implementation present, this command must fail its implementation-dependent assertions:

```bash
mvn --projects tck --also-make verify -Pweld-embedded -Dtck.implementation=none
```

## Test Assertions

Each test method is annotated with `@Assertion` which maps to a specific specification requirement:

```java
@Assertion(id = "AGENTICAI-AGENT-001",
           strategy = "Verify @Agent annotation exists in the jakarta.ai.agent package")
public void testAgentAnnotationExists() {
    // test implementation
}
```

## Test Categories

### Structural tests (`@Standalone`)
Reflection over the API; no container needed. These are `core/agent`, `core/lifecycle`, `core/cdi` and the signature test.

### Behavioral tests (`@Deployed`)
Arquillian deployments that run real workflows against the implementation under test. Each class in `core/behavior` covers one concern: phase ordering and precedence, data propagation and resolution tie-breaks, termination, `@OnException` handling, validation, inherited and void phases, topology flexibility, orchestration, CDI integration and event observation, workflow-scope lifecycle, concurrency isolation, deployment errors, and the `LargeLanguageModel` contract. Assertions a plain CDI container cannot satisfy carry `@RequiresImplementation`; the trigger-only assertions the baseline must pass carry `@RequiresNoImplementation`. See [TESTING-GUIDELINE](../TESTING-GUIDELINE.md) for the run modes.

## API signature baseline

`src/main/resources/ee/jakarta/tck/ai/agent/framework/signature/jakarta.ai.agent.sig_1.0`
records the exact public signature of the `jakarta.ai.agent` package. It ships
inside the TCK jar, so implementors receive the baseline they are verified
against.

Every build compares the API to it via `sigtest-maven-plugin` in `strictcheck`
mode, which is bidirectional: removing, changing **or adding** a public member
fails the build. A one-way check would let additions through as
backward-compatible, which is not what a specification wants.

When an API change is intended, regenerate the baseline and commit it:

```bash
mvn -pl tck verify -Psignature-generation
```

Review the diff before committing — it is the record of what the specification
promises.

## Requirements

- Java 17 or higher
- Jakarta EE 10 or higher
- Maven 3.9+
- A Jakarta JSON Binding provider on the test classpath

The TCK does not ship a JSON-B provider, so that it does not impose one on
implementations. A Jakarta EE 10 runtime already supplies one, so nothing extra
is needed when the TCK runs inside a container. When running the tests outside a
container, add a provider yourself, for example:

```xml
<dependency>
    <groupId>org.eclipse</groupId>
    <artifactId>yasson</artifactId>
    <version>3.0.3</version>
    <scope>test</scope>
</dependency>
```

A provider is required because `LargeLanguageModelStub` performs the type
conversion mandated of `LargeLanguageModel` via `JsonbBuilder.create()`, and
`LlmContractTests` asserts JSON Binding semantics directly. Without one, those
assertions error rather than fail informatively.

## License

Eclipse Public License v. 2.0

See [LICENSE](../LICENSE) for more information.
