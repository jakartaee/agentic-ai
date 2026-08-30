# Jakarta Agentic Artificial Intelligence Examples

This module contains example applications and usage patterns for Jakarta Agentic AI. These examples demonstrate how to use the Agentic AI API to build real-world agents on Jakarta EE runtimes.

## Included Examples

### Quickstart

Located in `quickstart/`, the smallest possible sample: a single `@Agent` that
answers a question, exercising all four phases (`@Trigger`, `@Decision`,
`@Action`, `@Outcome`) over a synchronous REST call. Runs on a small local
Ollama model, so it needs no API key. Start here. See its
[README](quickstart/README.md).

### Tutorial Generator

Located in `tutorial-generator/`, a deployable web application where an agent
writes a field-by-field guide for a web form and refines it through chat,
editing the stored artifact each turn. See its
[README](tutorial-generator/README.md).

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

### Course Content Studio

Located in `course-content-studio/`, this deployable web application demonstrates:

- Ordered `@Decision`/`@Action` phases (intro → quiz → conclusion)
- Typed LLM results bound with Jakarta JSON Binding
- Per-workflow conversational memory via `@WorkflowScoped` agents
- Multi-agent chaining through CDI events with a human-in-the-loop approval gate
- Resilience with `@HandleException`, plus live phase progress over Server-Sent Events

**Purpose:**
A teacher generates, refines and publishes a lesson (introduction, quiz and
conclusion) for a chapter. It is a deliberately advanced, end-to-end sample that
exercises features the basic examples do not. See its
[README](course-content-studio/README.md) for how to run it on a Jakarta EE
runtime with a Jakarta Agentic AI implementation.

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
