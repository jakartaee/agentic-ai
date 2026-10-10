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

## One agent, four flows

Every endpoint that reaches the LLM goes through `AssistAgent`. The resource
never calls `LargeLanguageModel` directly — the agent is the single surface
for the model.

The event is a sealed interface, `AssistRequest`, with four concrete subtypes:

- `AssistRequest.Draft(brief)` — draft a polished message from a short brief.
- `AssistRequest.Refine(currentMessage, instruction)` — refine the current
  message. The whole current message goes into the prompt on every refine
  turn, so the model edits the real draft rather than relying on
  conversational memory alone (the iterate-on-current-artifact pattern).
- `AssistRequest.Classify(message)` — classify the message into one of the
  configured inquiry types.
- `AssistRequest.Extract(text)` — pull the sender's name, email, and company
  out of a block of text.

The agent runs the same four phases for every flow:

- `@Trigger` — logs which flow this is.
- `@Decision` — stops the workflow when there is nothing meaningful to do
  (empty brief, empty refine instruction, empty classify message, empty
  extract text), so the LLM is never called for a no-op.
- `@Action` — a single `dispatch` method pattern-matches on the subtype and
  runs the right prompt. Results are written to `MessageStore` (for the two
  message flows, which are persistent across HTTP requests) and to the
  `@RequestScoped AssistResponse` (which the resource reads back inside the
  same request).
- `@Outcome` — logs that the flow is done.

That one agent handles multiple flows and stages is the design point worth
taking away: the four phases are not a per-task shape — they are the shape
of the agent itself, and the concrete event picks which work to do inside
them.

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
