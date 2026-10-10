# Tutorial Generator — Jakarta Agentic AI sample

A Jakarta Agentic AI agent that writes **field-by-field guidance** for a web
form and lets a developer **refine it through chat**. The sample form is a
customer onboarding form.

The page shows the form on the left and the generated guidance on the right; a
chat box below sends refinement instructions to the agent.

```
GET  /tutorial-generator/                            the side-by-side UI
GET  /tutorial-generator/api/form                    the form metadata (FormSpec)
GET  /tutorial-generator/api/tutorial                the current guidance JSON (204 before anything has been written)
POST /tutorial-generator/api/tutorial/generate       write fresh guidance for the form
POST /tutorial-generator/api/tutorial/refine         { "instruction": "..." }                       refine the whole guidance
POST /tutorial-generator/api/tutorial/refine-field   { "fieldName": "...", "instruction": "..." }   refine one field and merge it back
```

## How it works

`CustomerFormSpec` is the single source of truth: the page renders the live
form from it, and the agent writes guidance for the same fields. `TutorialAgent`
runs the four phases:

- `@Trigger` — `@Valid` on the parameter validates the request before the body
  runs.
- `@Decision` — a refine with no instruction stops the workflow here, so the
  LLM is never called for a no-op.
- `@Action` — the LLM writes or revises the guidance JSON, and the output is
  parsed as a JSON object before it is stored; a parse failure throws
  `LLMException`.
- `@Outcome` — logs the size of the stored guidance.

Refinement passes the *current guidance JSON + instruction* to the model on
every turn, so it edits the real artifact rather than relying on conversational
memory alone.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The sample defaults to
**Anthropic (Claude)**; the system prompt lives in config and is reused as the
prompt-cache prefix:

```properties
payara.agentic.llm.provider=anthropic
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=8192
payara.agentic.llm.system=You are a senior technical writer...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation documents.
> To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class model
> is recommended so the output is reliably well-formed JSON.

## Prerequisites

- JDK 17+ and Maven 3.9+
- A Jakarta EE 10 runtime with a Jakarta Agentic AI implementation (for example
  a Payara Server build that includes the `agentic-ai-core` runtime module).
- An LLM backend. For the default configuration, set `ANTHROPIC_API_KEY` in the
  server's environment **before** the domain starts, so the server process
  inherits it.

## Build & deploy

```bash
mvn -pl examples/tutorial-generator -am package
asadmin deploy examples/tutorial-generator/target/tutorial-generator.war
```

Open <http://localhost:8080/tutorial-generator/>, click **Generate guidance**,
then use the chat box to refine it (for example *"make the business email
guidance friendlier and add a format example"*). Watch `server.log` for
`[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`.
