# Customer Inquiry Intake — Jakarta Agentic AI example

A customer-facing contact form backed by a single-agent pipeline. The
customer types what they have (even just a message) and clicks **Prepare
inquiry**; the agent runs end-to-end — pulling the sender's name and email
out of the message if they aren't already filled in, inferring the sender's
company from the business-email domain, picking a routing category,
drafting a subject line, and rewriting the message in a tone that matches
that category — and the customer reviews the prepared record before
confirming.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The example defaults to
**Anthropic (Claude) on Google Cloud Vertex AI**; the system prompt lives in
config and is reused as the Anthropic prompt-cache prefix:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=4096
payara.agentic.llm.system=You are an intake assistant for a customer-facing contact form...
```

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this example. On another Jakarta Agentic AI
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
and click **Prepare inquiry**.
