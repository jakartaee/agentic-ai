# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Behavioral** (`core/behavior`, `@Deployed`): Arquillian deployments that run real workflows against the implementation under test, one class per concern — phase ordering and precedence, data propagation, termination, `@OnException` handling, validation, inherited and void phases, topology flexibility, orchestration, CDI integration and event observation, workflow-scope lifecycle, concurrency isolation, deployment errors, and the `LargeLanguageModel` contract
- **API signature** (`framework/signature`, `@Standalone`): the complete public API surface against a recorded baseline; see [API signature baseline](#api-signature-baseline)

Tests live in `src/main/java` so they ship in the TCK jar for implementors to run; nothing in `src/test/java` reaches the jar.

## Building and running the TCK

Compatibility runs happen in an implementation's own repository: it runs the published jar from its own runner with its own Arquillian adapter, every test included, as [Running the TCK](../tck-dist/src/main/asciidoc/sections/08running.adoc) in the TCK Reference Guide describes.

This repository has no implementation to deploy to, so, as in the Data and Concurrency TCKs, its build compiles the deployed tests into the jar without running them. The only test it executes is the container-less signature test (`<groups>standalone</groups>` in this module's Failsafe configuration):

```bash
mvn clean install
mvn --projects tck --also-make clean install
mvn --projects tck --also-make verify
```

The deployed tests are validated by the implementation's run.

## Test Assertions

Each test method is annotated with `@Assertion` which maps to a specific specification requirement:

```java
@Assertion(id = "AGENTICAI-ORDER-002",
           section = "agent-lifecycle",
           strategy = "Full T→D→A→O pipeline executes in strict declaration order")
public void fullPipelineExecutesInStrictOrder() {
    // test implementation
}
```

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

## The model behind the implementation's `LargeLanguageModel`

The TCK deploys no `LargeLanguageModel`; the implementation's own facade is
injected into the agents under test, and the LLM assertions verify its
placeholder substitution, JSON Binding conversion, `LLMException` mapping and
per-workflow conversational state. A real model cannot answer deterministically,
so during a TCK run the implementation must back its facade with the model
behaviour defined by `framework/llm/TckModel` and spelled out in the Reference
Guide's "Additional Certification Requirements": a rendered prompt containing
`TckModel.FAIL` fails, a prompt equal to `TckModel.HISTORY` returns the
conversation's prior rendered prompts one per line, and anything else is echoed
verbatim. The tests derive decisions and observations from that echo.

## License

Eclipse Public License v. 2.0

See [LICENSE](../LICENSE) for more information.
