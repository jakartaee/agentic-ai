# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI 1.0 Specification.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Behavioral**: a compatible implementation dispatches workflows as specified — phase ordering and precedence, data propagation into phase parameters, termination, error handling, validation, inherited phases, CDI scoping and event observation, concurrency isolation, deployment errors, and the `LargeLanguageModel` contract
- **API signature**: the complete public API surface against a recorded baseline

## Test Structure

```
tck/
├── src/main/java/ee/jakarta/tck/ai/agent/
│   ├── core/
│   │   ├── behavior/         # Deployed workflow tests, one class per concern
│   │   │   └── agents/       # The agents those tests deploy, one package per concern
│   │   └── integration/      # Deployed smoke test
│   └── framework/
│       ├── junit/anno/       # @Assertion, @Standalone, @Deployed, @RequiresImplementation, ...
│       ├── junit/extensions/ # The JUnit conditions behind those annotations
│       ├── arquillian/       # Adds the framework beans to every deployment
│       ├── stub/             # LargeLanguageModelStub: scripted responses, recorded calls
│       ├── trace/            # ExecutionTraceRecorder: phase-order assertions
│       └── signature/        # API signature test
├── src/main/resources/
│   ├── META-INF/services/    # Registers the Arquillian extension
│   └── ee/jakarta/tck/ai/agent/framework/signature/   # Signature baseline
└── src/test/java/            # A unit test of the TCK's concurrency harness; not part of the TCK
```

Tests live in `src/main/java` so they ship in the TCK jar for implementors to run; nothing in `src/test/java` reaches the jar.

## Building and running the TCK

Compatibility runs happen in an implementation's own repository: it runs the published jar from its own runner with its own Arquillian adapter, every test included, as the TCK Reference Guide in `tck-dist` describes.

This repository has no implementation, so its build runs what plain CDI can satisfy on Weld Embedded, a test dependency of this module. Assertions that need an implementation carry `@RequiresImplementation`, a JUnit tag that this module's Failsafe configuration excludes; the rest of the jar, including the signature test, runs here:

```bash
mvn clean install
mvn --projects tck --also-make clean install
mvn --projects tck --also-make verify
```

This run must succeed. It is not a compatibility result.

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

### Signature test (`@Standalone`)
The API signature check in `framework/signature`; no container needed. It is the structural test: the recorded baseline covers every type, member, target, retention and meta-annotation of the API, so there are no separate reflection tests.

### Behavioral tests (`@Deployed`)
Arquillian deployments that run real workflows against the implementation under test. Each class in `core/behavior` covers one concern: phase ordering and precedence, data propagation and resolution tie-breaks, termination, `@OnException` handling, validation, inherited and void phases, topology flexibility, orchestration, CDI integration and event observation, workflow-scope lifecycle, concurrency isolation, deployment errors, and the `LargeLanguageModel` contract. Assertions a plain CDI container cannot satisfy carry `@RequiresImplementation`, a JUnit tag; a compatibility run excludes nothing, and only this repository's own build excludes the tag. See [TESTING-GUIDELINE](../TESTING-GUIDELINE.md).

## API signature baseline

`src/main/resources/ee/jakarta/tck/ai/agent/framework/signature/jakarta.ai.agent.sig_1.0`
records the exact public signature of the `jakarta.ai.agent` package. It ships
inside the TCK jar together with the Jakarta TCK signature test engine
(`jakarta.tck:sigtest-maven-plugin`, a dependency of the TCK), and
`SignatureTests` runs the engine against it as an ordinary `@Standalone` JUnit
test. Implementors therefore run the signature check in the same execution as
every other test, with no plugin configuration of their own.

The check is strict in both directions: removing, changing **or adding** a
public member fails it. A one-way check would let additions through as
backward-compatible, which is not what a specification wants. JDK types are
resolved from the running JDK's `lib/ct.sym` at the Java SE 17 level, so the
result does not depend on which newer JDK runs the test; a JDK, not a JRE, is
required.

When an API change is intended, regenerate the baseline and commit it:

```bash
mvn -pl tck verify -Psignature-generation
```

Review the diff before committing — it is the record of what the specification
promises.

## Distribution, challenges and certification

The jar this module publishes to Maven Central is EPL-licensed and exists for
development convenience; the Jakarta EE TCK Process does not let it support a
compatibility claim. The certifiable distribution is built by the
[`tck-dist`](../tck-dist/) module: a zip under the Eclipse Foundation
Technology Compatibility Kit License containing this jar, its sources, the
signature baseline, the TCK Reference Guide and a starter runner project.
Implementors should start from the guide in that zip.

Challenges to a test and compatibility certification requests are filed as
GitHub issues with the `TCK Challenge` and `Compatibility Certification
Request` templates, which apply the `challenge` and `certification` labels the
TCK Process requires.

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
