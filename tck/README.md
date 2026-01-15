# Jakarta Agentic AI TCK

Technology Compatibility Kit (TCK) for Jakarta Agentic AI 1.0 Specification.

## Overview

This TCK verifies that implementations of Jakarta Agentic AI conform to the specification requirements. It includes tests for:

- **Agent Annotations**: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@HandleException`
- **CDI Integration**: `@WorkflowScoped` custom scope
- **Core Interfaces**: `LargeLanguageModel` interface
- **Core Classes**: `WorkflowContext` class
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

# Build only the TCK module
mvn clean install -pl tck
```

## Running Tests

Tests are executed using Maven Failsafe, which provides useful pre-integration-test and post-integration-test lifecycle phases:

```bash
# Run integration tests
mvn verify -pl tck

# Or run the entire build including tests
mvn clean install -pl tck
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

### WorkflowContext Tests
- Verify class exists with required methods
- Test attribute storage and retrieval
- Test trigger event handling

### CDI Integration Tests
- `@WorkflowScoped` annotation validation
- NormalScope compliance
- Literal class implementation

## Requirements

- Java 17 or higher
- Maven 3.8+
- Jakarta CDI 4.1+ (for CDI-related tests)

## License

Eclipse Public License v. 2.0

See [LICENSE](../LICENSE) for more information.
