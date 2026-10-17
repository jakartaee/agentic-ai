# Ask Agent — Jakarta Agentic AI example

A compact Jakarta Agentic AI example: a single `@Agent` answers a question,
exercising the four specification phases (`@Trigger`, `@Decision`, `@Action`,
`@Outcome`). A synchronous REST call fires the trigger event and returns the LLM
answer in the same response.

```
POST /ask-agent/api/ask   { "question": "..." }  ->  { "question", "answer" }
```

## Configure the LLM

The LLM provider is selected by the runtime through MicroProfile Config. This
example ships `src/main/resources/META-INF/microprofile-config.properties`
pointing at a small local **Ollama** model (no API key, no cost):

```properties
payara.agentic.llm.provider=ollama
payara.agentic.llm.model=gemma3:4b
payara.agentic.llm.ollama.base-url=http://localhost:11434
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this example. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation documents.

## Prerequisites

- JDK 17+ and Maven 3.9+
- A Jakarta EE 10 runtime with a Jakarta Agentic AI implementation (for example
  a Payara Server build that includes the `agentic-ai-core` runtime module).
- For the default configuration: [Ollama](https://ollama.com) with the model
  pulled — `ollama pull gemma3:4b` — answering at `http://localhost:11434`.

## Build & deploy

```bash
mvn -pl examples/ask-agent -am package
asadmin deploy examples/ask-agent/target/ask-agent.war
```

Then ask a question:

```bash
curl -s http://localhost:8080/ask-agent/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is Jakarta EE in one sentence?"}'
```
