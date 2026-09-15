# Quickstart — Jakarta Agentic AI sample

A compact Jakarta Agentic AI example: a single `@Agent` answers a question,
exercising the four specification phases (`@Trigger`, `@Decision`, `@Action`,
`@Outcome`). A synchronous REST call fires the trigger event and returns the LLM
answer in the same response.

```
POST /quickstart/api/ask   { "question": "..." }  ->  { "question", "answer" }
```

## Configure the LLM

The LLM provider is selected by the runtime through MicroProfile Config. This
sample ships `src/main/resources/META-INF/microprofile-config.properties`
pointing at a small local **Ollama** model (no API key, no cost):

```properties
payara.agentic.llm.provider=ollama
payara.agentic.llm.model=gemma3:4b
payara.agentic.llm.ollama.base-url=http://localhost:11434
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this sample. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation documents.

## Prerequisites

- JDK 17+ and Maven 3.9+
- A Jakarta EE 10 runtime with a Jakarta Agentic AI implementation (for example
  a Payara Server build that includes the `agentic-ai-core` runtime module).
- For the default configuration: [Ollama](https://ollama.com) with the model
  pulled — `ollama pull gemma3:4b` — answering at `http://localhost:11434`.

## Build & deploy

```bash
mvn -pl examples/quickstart -am package
asadmin deploy examples/quickstart/target/quickstart.war
```

Then ask a question:

```bash
curl -s http://localhost:8080/quickstart/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is Jakarta EE in one sentence?"}'
```

Watch `server.log` for `[TRIGGER]` → `[DECISION]` → `[ACTION]` → `[OUTCOME]`.

Ask the **same question again** to see early termination: `@Decision` finds the
answer already stored, returns `false`, and neither `@Action` nor `@Outcome`
runs — so the second call never reaches the model. The log stops after
`[DECISION]` and the answer comes back just as fast.

An empty `question` is rejected with a 400 before the event is fired; `@NotBlank`
on the event record is what `@Valid` on the trigger enforces for any other
caller.

If the configured backend is not running — Ollama not started, for instance —
the call returns a 503 saying so, rather than a stack trace. `Event.fire(...)`
is synchronous, so the `LLMException` surfaces at the resource.
