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

### Lesson Builder

Located in `lesson-builder/`, a deployable web application with two
agents and the teacher in between. The first writes a lesson's introduction,
quiz and conclusion from a topic and its material. The teacher edits and
publishes the lesson. The publication event triggers the second agent, which
works out what the lesson covers, decides whether the content matches the stated title
and whether the catalog already carries it, then tags curriculum standards,
estimates class time, writes a short abstract and files the entry — or sends
the teacher back to fix a mismatch or merge a duplicate. See its
[README](lesson-builder/README.md).

## Building

To build all examples:

```
mvn clean package
```

You can also build individual examples by running Maven in their respective directories.
