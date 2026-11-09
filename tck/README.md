# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the
specification: the tests in `core/behavior` deploy workflows to the
implementation under test, and `framework/signature` checks the public API
against a recorded baseline (see [API signature baseline](#api-signature-baseline)).

Tests live in `src/main/java` so they ship in the TCK jar for implementors to run; nothing in `src/test/java` reaches the jar.

## Building and running the TCK

Compatibility runs happen in an implementation's own repository: it runs the published jar from its own runner with its own Arquillian adapter, every test included, as [Running the TCK](../tck-dist/src/main/asciidoc/sections/08running.adoc) in the TCK Reference Guide describes.

This repository has no implementation to deploy to, so, as in the Data and Concurrency TCKs, its build compiles the tests into the jar without running any of them; the signature test deploys to the implementation like every other test. The build is:

```bash
mvn clean install
mvn --projects tck --also-make clean install
mvn --projects tck --also-make verify
```

## Test Assertions

Each test method is annotated with `@Assertion`. The id is unique to the test and is what a TCK challenge quotes; the section is the anchor of the specification section that states the requirement:

```java
@Assertion(id = "AGENTICAI-ORDER-001",
           section = "agent-lifecycle",
           strategy = "Full T→D→A→O pipeline executes in strict phase order")
public void fullPipelineExecutesInStrictOrder() {
    // test implementation
}
```

## API signature baseline

`src/main/resources/ee/jakarta/tck/ai/agent/framework/signature/jakarta.ai.agent.sig_1.0`
records the exact public signature of the `jakarta.ai.agent` package. It ships
inside the TCK jar together with the Jakarta TCK signature test engine
(`jakarta.tck:sigtest-maven-plugin`, a dependency of the TCK). `SignatureTests`
carries both into its deployment and runs the engine inside the implementation,
against the `jakarta.ai.agent` the implementation provides to applications.
Implementors therefore run the signature check in the same execution as every
other test, with no plugin configuration of their own.

The check is strict in both directions: removing, changing **or adding** a
public member fails it. A one-way check would let additions through as
backward-compatible, which is not what a specification wants. JDK types are
resolved at the Java SE 17 level, so the result does not depend on which newer
JDK runs the implementation.

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
