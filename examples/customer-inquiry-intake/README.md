# Customer Inquiry Intake — Jakarta Agentic AI sample

A customer-facing contact form that **lets the AI do the work the customer
would otherwise have to do by hand**: pull their name, email, and company out
of their message, suggest the right routing category, and draft or refine the
free-text message itself.

The form is five fields — `name`, `businessEmail`, `company`, `inquiryType`,
`message` — all user-controlled. Each has a small **✦** link that opens an
assistant panel offering the AI help that fits that field.

```
GET  /customer-inquiry-intake/                             the contact form
GET  /customer-inquiry-intake/api/form                     the form schema (FormSpec)
GET  /customer-inquiry-intake/api/message                  the current message draft (204 before anything is drafted)
POST /customer-inquiry-intake/api/message/draft            { "brief": "..." }           draft a polished message from a brief
POST /customer-inquiry-intake/api/message/refine           { "instruction": "..." }     refine the current message
POST /customer-inquiry-intake/api/classify                 { "message": "..." }         suggest an inquiryType from the message
POST /customer-inquiry-intake/api/extract                  { "text": "..." }            pull name / email / company out of some text
POST /customer-inquiry-intake/api/submit                   { ...the full form... }      send the inquiry
```

## Two shapes of AI call in one sample

- **The agent** (`AssistAgent`) handles the message. Drafting the first version
  from a brief and refining it on later turns is iterative, stateful work; it
  earns the four-phase workflow, the `@Valid` trigger, the `@Decision`
  short-circuit (nothing happens on a blank brief or an empty refine
  instruction), and the iterate-on-the-current-artifact pattern.
- **The `LargeLanguageModel` facade used directly** (in `AssistResource`)
  handles `classify` and `extract`. Those are one-shot, stateless utility
  calls — classification and information extraction — and giving them a full
  agent would be ceremony without content. The resource injects
  `LargeLanguageModel` and makes a single `model.query(...)` call per
  endpoint.

Picking the right shape per task is itself a design point worth taking away
from this sample.

## How the agent is wired

`AssistAgent` runs the four phases:

- `@Trigger` — logs whether this is a draft or a refine.
- `@Decision` — stops the workflow when there is nothing meaningful to do
  (first draft with no brief, refine with no instruction), so the LLM is
  never called for a no-op.
- `@Action` — one prompt for each mode; the response is stored in
  `MessageStore` for the resource to read back.
- `@Outcome` — logs the size of the stored draft.

Refinement passes the *current message* back into the prompt on every turn,
so the model edits the real draft rather than relying on conversational
memory alone.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The sample defaults to
**Anthropic (Claude) on Google Cloud Vertex AI**; the system prompt lives in
config and is reused as the Anthropic prompt-cache prefix:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=4096
payara.agentic.llm.system=You are a writing assistant inside a customer contact form...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the classify and extract outputs stay well-formed.

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

Open <http://localhost:8080/customer-inquiry-intake/>, type a short brief
into the message field's assistant panel (for example *"upgrading EE 8 → 10
on Payara 5, three month deadline, need help scoping"*), click **Draft
message**, then refine it through chat and let the assistant suggest a
category. Watch `server.log` for
`[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]` on every draft or
refine, and `[SUBMIT]` once you send the inquiry.
