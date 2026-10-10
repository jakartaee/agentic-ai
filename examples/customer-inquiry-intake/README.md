# Customer Inquiry Intake — Jakarta Agentic AI sample

An intake agent that turns a free-text customer inquiry into a **structured
record a teammate can act on**: a one-line summary, a routing category, an
urgency, a cleaned-up version of the customer's message, and a suggested next
step. The user then **refines any field through chat** before submitting.

The page shows the raw inquiry on the top, the triaged record below, and a
side chat for refining a single field. Submit hands the final record off (in
this sample, to the server log).

```
GET  /customer-inquiry-intake/                             the UI
GET  /customer-inquiry-intake/api/form                     the record schema (FormSpec)
GET  /customer-inquiry-intake/api/inquiry                  the current inquiry JSON (204 before anything is drafted)
POST /customer-inquiry-intake/api/inquiry/draft            { "brief": "..." }                              triage a raw inquiry
POST /customer-inquiry-intake/api/inquiry/refine           { "instruction": "..." }                        refine the whole record
POST /customer-inquiry-intake/api/inquiry/refine-field     { "fieldName": "...", "instruction": "..." }   refine one field and merge back
POST /customer-inquiry-intake/api/inquiry/submit           hand the inquiry off
```

## How it works

`InquiryFormSpec` is the single source of truth for the record's shape (the
fields, their types, and the allowed values for `category` and `urgency`). The
page renders the structured UI from it, and the agent is told to produce JSON
with exactly those keys.

`IntakeAgent` runs the four phases:

- `@Trigger` — `@Valid` on the parameter validates the request before the body
  runs.
- `@Decision` — a first draft with no brief, or a refine with no instruction,
  stops the workflow here so the LLM is never called for a no-op.
- `@Action` — the LLM produces or revises the inquiry JSON; the output is
  parsed as a JSON object before it is stored, and a parse failure throws
  `LLMException`.
- `@Outcome` — logs the size of the stored inquiry.

Two patterns are worth taking away from this sample:

- **Iterate on the real artifact.** Refinement passes the *current inquiry
  JSON* back into the prompt on every turn, so the model edits the actual
  record rather than relying on conversational memory alone.
- **Per-field extract and merge.** `refine-field` pulls one key out of the
  inquiry, lets the agent edit only that fragment, and merges the result back
  into the full record. The other fields are preserved and the LLM only sees
  what it needs to.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The sample defaults to
**Anthropic (Claude) on Google Cloud Vertex AI**; the system prompt lives in
config and is reused as the Anthropic prompt-cache prefix:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=4096
payara.agentic.llm.system=You are a customer support triage assistant...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the output is reliably well-formed JSON.

## Prerequisites

- JDK 17+ and Maven 3.9+
- A Jakarta EE 10 runtime with a Jakarta Agentic AI implementation (for example
  a Payara Server build that includes the `agentic-ai-core` runtime module).
- An LLM backend. For the default configuration, Application Default
  Credentials for a GCP project that has Claude enabled on Vertex AI
  (`gcloud auth application-default login`), with
  `ANTHROPIC_VERTEX_PROJECT_ID` and `CLOUD_ML_REGION` set in the server's
  environment **before** the domain starts.

## Build & deploy

```bash
mvn -pl examples/customer-inquiry-intake -am package
asadmin deploy examples/customer-inquiry-intake/target/customer-inquiry-intake.war
```

Open <http://localhost:8080/customer-inquiry-intake/>, paste a customer
inquiry (for example *"Our Jakarta EE 8 app runs on Payara 5 and we need to
be on EE 10 before our next audit in three months. Who should we talk to?"*),
click **Draft structured inquiry**, then click any row to refine it through
chat (*"shorter", "this is support not sales", "add that it's audit-driven"*).
Watch `server.log` for `[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`
on every turn, and `[SUBMIT]` once you hand the inquiry off.
