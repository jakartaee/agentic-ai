# Tutorial Generator — Jakarta Agentic AI sample

A Jakarta Agentic AI agent that writes a **field-by-field guide** for a web form
and lets a developer **refine it through chat**. The sample form is a customer
registration form.

The page shows the form on the left and the generated guide on the right; a chat
box below sends refinement instructions to the agent.

```
GET  /tutorial-generator/                       the side-by-side UI
GET  /tutorial-generator/api/form               the form metadata (FormSpec)
POST /tutorial-generator/api/tutorial/generate  generate a fresh guide
POST /tutorial-generator/api/tutorial/refine    { "instruction": "..." }  refine the current guide
```

## How it works

`CustomerFormSpec` is the single source of truth: the page renders the live form
from it, and the agent explains the same fields. `TutorialAgent` runs the four
phases — `@Trigger`, `@Decision` (enough fields?), `@Action` (the LLM generates
or revises the HTML), `@Outcome` (store it). Refinement passes the *current
HTML + instruction* to the model each turn, so it edits the real artifact rather
than relying on memory alone.

## Configure the LLM

The LLM provider is selected by the runtime via MicroProfile Config. This sample
uses **Anthropic (Claude)** for high-quality HTML; the system prompt is shipped
as config and reused as the prompt-cache prefix:

```properties
payara.agentic.llm.provider=anthropic
payara.agentic.llm.model=claude-opus-4-8
payara.agentic.llm.max-tokens=8192
payara.agentic.llm.system=You are a senior technical writer...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation documents.
> To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` (a 12B-class model
> is recommended for HTML quality).

## Prerequisites

- JDK 17+ and Maven 3.9+
- A Jakarta EE 10 runtime with a Jakarta Agentic AI implementation (for example
  a Payara Server build that includes the `agentic-ai-core` runtime module).
- An LLM backend. For the default configuration, an `ANTHROPIC_API_KEY` exported
  in the server environment **before** the domain starts, so the server process
  inherits it.

## Build & deploy

```bash
mvn -pl examples/tutorial-generator -am package
asadmin deploy examples/tutorial-generator/target/tutorial-generator.war
```

Open <http://localhost:8080/tutorial-generator/>, click **Generate tutorial**,
then use the chat box to refine it (e.g. *"make the business email explanation
friendlier and add an example"*). Watch `server.log` for
`[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`.
