# Jakarta Agentic Artificial Intelligence Examples

This module contains example applications and usage patterns for Jakarta Agentic AI. These examples demonstrate how to use the Agentic AI API to build real-world agents on Jakarta EE runtimes.

## Included Examples

### Fraud Detection Agent

Located in `fraud-detection/`, this example demonstrates:

- Using the `@Agent`, `@Trigger`, `@Decision`, `@Action`, and `@Outcome` annotations
- Integrating a Large Language Model (LLM) for fraud analysis
- Handling workflow context and domain objects
- Marking transactions as suspect and sending notifications

**Purpose:**
Detects potentially fraudulent bank transactions and demonstrates a typical agent workflow with LLM integration and CDI.

### Documentation Agent

Located in `docs-agent/`, this example demonstrates:

- Monitoring pull requests for documentation needs
- Using the `@Decision` annotation to analyze PRs with an LLM
- Generating and reviewing documentation pull requests
- Handling exceptions in agent workflows

**Purpose:**
Automatically generates and applies documentation updates based on code changes, showcasing advanced workflow branching and exception handling.

## Building

To build all examples:

```
mvn clean package
```

You can also build individual examples by running Maven in their respective directories.

## Running the Examples

These examples are designed for demonstration and reference. To run them:

1. Ensure you have Java 17+ and Jakarta EE 10+ compatible runtime.
2. Build the desired example with Maven.
3. Deploy or run the example in your Jakarta EE environment as appropriate.

Refer to the source code in each example directory for more details on usage and workflow.
