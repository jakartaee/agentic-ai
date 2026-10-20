# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI 1.0 Specification.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Agent Annotations**: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@OnException`
- **CDI Integration**: `@WorkflowScoped` custom scope
- **Core Interfaces**: `LargeLanguageModel` interface
- **API Signature**: Verification of the complete API surface

## Test Structure

```
tck/
├── src/main/java/ee/jakarta/tck/ai/agent/
│   ├── core/
│   │   ├── agent/           # Agent annotation and interface tests
│   │   ├── lifecycle/       # Workflow lifecycle annotation tests
│   │   └── cdi/             # CDI scope tests
│   └── framework/
│       ├── junit/anno/      # Custom test annotations
│       └── signature/       # API signature tests
└── src/main/resources/
    └── ee/jakarta/tck/ai/agent/framework/signature/
```

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

### Agent Annotation Tests
- Verify `@Agent` annotation exists and has correct attributes
- Verify retention policy is RUNTIME
- Verify target is TYPE

### Lifecycle Annotation Tests
- `@Trigger` - workflow entry point
- `@Decision` - decision making with LLM
- `@Action` - action execution
- `@Outcome` - workflow completion
- `@OnException` - error handling

### LargeLanguageModel Interface Tests
- Verify interface exists with all required methods:
  - `query(String prompt)`
  - `query(String prompt, Class<T> resultType)`
  - `query(String prompt, Object... inputs)`
  - `query(String prompt, Class<T> resultType, Object... inputs)`
  - `unwrap(Class<T> implClass)`

### CDI Integration Tests
- `@WorkflowScoped` annotation validation
- NormalScope compliance
- Literal class implementation

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
- Maven 3.8+
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
