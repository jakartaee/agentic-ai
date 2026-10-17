# Jakarta Agentic Artificial Intelligence Examples

This module contains example applications and usage patterns for Jakarta Agentic AI. These examples demonstrate how to use the Agentic AI API to build real-world agents on Jakarta EE runtimes.

## Included Examples

### Ask Agent

Located in `ask-agent/`, a compact example: a single `@Agent` that answers a
question, exercising all four phases (`@Trigger`, `@Decision`, `@Action`,
`@Outcome`) over a synchronous REST call. Runs on a small local
Ollama model, so it needs no API key. Start here. See its
[README](ask-agent/README.md).

### Customer Inquiry Intake

Located in `customer-inquiry-intake/`, a deployable web application: a
customer-facing contact form backed by a single-agent pipeline. On submit,
the agent enriches the sender's identity from the message, infers the
sender's company from the business-email domain, classifies the inquiry,
drafts a subject line, and polishes the message in that tone; the customer
reviews the prepared record before confirming. Each phase receives the
earlier results it declares as parameters. See its
[README](customer-inquiry-intake/README.md).

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

Located in `course-content-studio/`, a deployable web application with one
agent per teacher request, each triggered by its own CDI event. The first
writes a chapter's introduction, quiz and conclusion as ordered `@Action`s
over a draft its `@Trigger` returns, binding the quiz to a record with the
typed `LargeLanguageModel` overload; the second rewrites any one section on
request; on **Approve & publish**, the third has the model review the packet
and, if it passes, publishes the lesson with learning objectives. See its
[README](course-content-studio/README.md).

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
