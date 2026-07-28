# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI 1.0 Specification.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Agent Annotations**: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@HandleException`
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

## Building the TCK

```bash
# Build the entire project including TCK
mvn clean install

# Build the TCK module together with required upstream modules
mvn --projects tck --also-make clean install
```

## Running Tests

Tests are executed using Maven Failsafe, which provides useful pre-integration-test and post-integration-test lifecycle phases:

```bash
# Run integration tests
mvn --projects tck --also-make verify

# Or run the full clean build for the TCK and required upstream modules
mvn --projects tck --also-make clean install
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
- `@HandleException` - error handling

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
