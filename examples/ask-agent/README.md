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

> Payara Server Community (7.2026.8 or newer) is currently the first
> implementation of the Jakarta Agentic AI specification, and the
> `payara.agentic.llm.*` keys are its configuration surface. Provider
> configuration is not yet standardized, so on another implementation
> configure the provider the way that implementation documents.

## Prerequisites

- JDK 21+ and Maven 3.9+ (JDK 21 is what Payara 7 itself requires to run; the example code compiles at Java 17)
- Payara Server Community 7.2026.8 or newer, which bundles Jakarta Agentic AI.
  Download, start and `asadmin` setup are in
  [Set up the environment](../README.md#set-up-the-environment-payara-server-community).
- For the default configuration: [Ollama](https://ollama.com) with the model
  pulled — `ollama pull gemma3:4b` — answering at `http://localhost:11434`.

## Build & deploy

Run from the repository root, with the Payara domain started (`asadmin start-domain`).

```bash
mvn -pl examples/ask-agent -am clean package
asadmin deploy --force examples/ask-agent/target/ask-agent.war
```

Then ask a question:

```bash
curl -s http://localhost:8080/ask-agent/api/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What is Jakarta EE in one sentence?"}'
```

## Verify and troubleshoot

Watch `$PAYARA_HOME/glassfish/domains/domain1/logs/server.log` while you use the example; each agent logs its phases. If answers look canned, the LLM provider is not configured. See [Troubleshooting](../README.md#troubleshooting).
