# Customer Inquiry Intake — Jakarta Agentic AI sample

A customer-facing contact form backed by a single-agent pipeline. The
customer types what they have (even just a message) and clicks **Prepare
inquiry**; the agent runs end-to-end — pulling the sender's name and email
out of the message if they aren't already filled in, inferring the sender's
company from the business-email domain, picking a routing category,
gauging the urgency, and rewriting the message in a tone that matches
that category — and the customer reviews the prepared record before
confirming.

```
GET  /customer-inquiry-intake/                       the UI
POST /customer-inquiry-intake/api/intake             { name?, email?, message }              run the pipeline, return the prepared IntakeRecord
POST /customer-inquiry-intake/api/intake/confirm     { identity, classification, message }   hand the (possibly edited) record off
```

## How the agent is wired

`IntakeAgent` has four `@Action` methods and two `@Decision` methods, with no
explicit `order=` anywhere. The runtime orders every phase by the data each
one declares as a parameter.

**Actions, ordered by dataflow:**

- `Identity enrichIdentity(IntakeRequest)`, `Classification classify(IntakeRequest)`
  and `Priority assessPriority(IntakeRequest)` all take only the triggering
  event and can run first.
- `IntakeRecord assemble(IntakeRequest, Identity, Classification, Priority)` runs
  after the three above because it declares each of their returned values as
  a parameter; the runtime matches parameters by type.

That dataflow-ordering is the same mechanism Example 1 uses to pass the
generated `Answer` from `@Action` to `@Outcome`; here it threads a real
pipeline together, and the returned `Identity` and `Classification` records
are workflow state carried between phases.

**The two decisions sit at different gates:**

- `Result canProcess(IntakeRequest)` runs first and returns a `Result` —
  `Result(false, "Message is too short to process meaningfully.")` on a
  trivial submission. Returning `Result` (instead of a plain `boolean`)
  carries the reason through the Result's `details`, visible in the server
  log.
- `boolean hasEnoughContent(Identity, IntakeRequest)` runs after
  `enrichIdentity` — it takes an `Identity` as a parameter, so parameter-type
  dataflow places it there. If the message is short and the agent extracted
  nothing identifying, the workflow stops before `assemble` runs; the
  resource then returns `422` and the customer is asked to elaborate.

**The surrounding phases:**

- `@Trigger void onSubmit(@Valid IntakeRequest)` — the `@Valid` runs Bean
  Validation on the record (`@NotBlank` on `message`, `@Email` on `email`,
  `@Size` on each field). A violation is thrown by the interceptor before
  the workflow body runs; the resource unwraps the `ObserverException` and
  returns 400 with the specific field errors.
- `@Outcome void done(IntakeRequest)` — logs that the record is ready.
- `@OnException void onLlmFailure(LLMException, IntakeRequest)` — catches
  an LLM failure anywhere in the pipeline and writes a minimal fallback
  record (the customer's own values + the unpolished original message), so
  the UI always shows a review screen instead of a 500.

**Typed LLM output.** `enrichIdentity` uses the typed form of the facade,
`model.query(prompt, Identity.class, args)`, so the LLM's JSON response is
bound directly to the `Identity` record by JSON-B. No hand-rolled parsing,
no manual code-fence stripping. If the LLM returns something JSON-B cannot
bind, the facade throws `LLMException` and the exception handler writes the
fallback.

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
`[TRIGGER]` → `[DECISION]` → `[ACTION] enrichIdentity`,
`[ACTION] classify` and `[ACTION] assessPriority` (which may run in any
order), then `[ACTION] assemble`, then `[OUTCOME]`, then `[CONFIRM]`
when you send.
