# Customer Inquiry Intake — Jakarta Agentic AI sample

A customer-facing contact form backed by a single-agent pipeline. The
customer types what they have (even just a message) and clicks **Prepare
inquiry**; the agent runs end-to-end — pulling the sender's identity out of
the message if it isn't already filled in, picking a routing category, and
rewriting the message in a tone that matches that category — and the
customer reviews the prepared record before confirming.

```
GET  /customer-inquiry-intake/                       the UI
POST /customer-inquiry-intake/api/intake             { name?, email?, company?, message }    run the pipeline, return the prepared IntakeRecord
POST /customer-inquiry-intake/api/intake/confirm     { identity, classification, message }   hand the (possibly edited) record off
```

## How the agent is wired

`IntakeAgent` has three `@Action` methods, but there is no explicit
`order=` anywhere. The runtime orders them by the data each one needs:

- `Identity enrichIdentity(IntakeRequest)` and `Classification classify(IntakeRequest)`
  take only the triggering event and can run first.
- `IntakeRecord assemble(IntakeRequest, Identity, Classification)` runs after
  both of them because it declares them as parameters; the runtime matches
  parameters by type.

That dataflow-ordering is the same mechanism the ask-agent example uses to
pass the generated `Answer` from `@Action` to `@Outcome`. Here it threads a
real pipeline together.

The other phases:

- `@Trigger` — logs that an inquiry arrived.
- `@Decision canProcess(IntakeRequest)` — stops the workflow on an empty
  message so no LLM call happens for a no-op.
- `@Outcome` — logs that the inquiry is ready.
- `@HandleException onLlmFailure(LLMException, IntakeRequest)` — catches an
  LLM failure anywhere in the pipeline and writes a minimal fallback record
  (the customer's own values + the unpolished original message), so the UI
  always shows a review screen instead of a 500. The resource reads the
  record from the `@RequestScoped IntakeOutput` holder either way.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The sample defaults to
**Anthropic (Claude) on Google Cloud Vertex AI**; the system prompt lives in
config and is reused as the Anthropic prompt-cache prefix:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=4096
payara.agentic.llm.system=You are an intake assistant for a customer-facing contact form...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the extract step produces well-formed JSON.

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

Open <http://localhost:8080/customer-inquiry-intake/>, leave the identity
fields blank, type a message that mentions you by name (for example *"Hi, I'm
Jane Smith from Acme &lt;jane@acme.com&gt; — our EE 8 app on Payara 5 needs
to be on EE 10 before our audit in three months. Who should we talk to?"*),
and click **Prepare inquiry**. Watch `server.log` for
`[TRIGGER]` → `[DECISION]` → `[ACTION] enrichIdentity` and
`[ACTION] classify` (which may run in either order), then
`[ACTION] assemble`, then `[OUTCOME]`, then `[CONFIRM]` when you send.
