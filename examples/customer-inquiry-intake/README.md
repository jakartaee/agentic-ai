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

> Payara Server Community (7.2026.8 or newer) is currently the first
> implementation of the Jakarta Agentic AI specification, and the
> `payara.agentic.llm.*` keys are its configuration surface. Provider
> configuration is not yet standardized, so on another implementation
> configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the extract step produces well-formed JSON.

## Prerequisites

- JDK 21+ and Maven 3.9+ (JDK 21 is what Payara 7 itself requires to run; the example code compiles at Java 17)
- Payara Server Community 7.2026.8 or newer, which bundles Jakarta Agentic AI.
  Download, start and `asadmin` setup are in
  [Set up the environment](../README.md#set-up-the-environment-payara-server-community).
- An LLM backend. For the default configuration, Application Default
  Credentials for a GCP project that has Claude enabled on Vertex AI
  (`gcloud auth application-default login`), with
  `ANTHROPIC_VERTEX_PROJECT_ID` and `CLOUD_ML_REGION` set in the server's
  environment **before** the domain starts.

## Build & deploy

Run from the repository root, with the Payara domain started (`asadmin start-domain`).

```bash
mvn -pl examples/customer-inquiry-intake -am clean package
asadmin deploy --force examples/customer-inquiry-intake/target/customer-inquiry-intake.war
```

Open <http://localhost:8080/customer-inquiry-intake/>, leave the identity
fields blank, type a message that mentions you by name (for example *"Hi, I'm
Jane Smith from Acme &lt;jane@acme.com&gt; — our EE 8 app on Payara 5 needs
to be on EE 10 before our audit in three months. Who should we talk to?"*),
and click **Prepare inquiry**.

## Verify and troubleshoot

Watch `$PAYARA_HOME/glassfish/domains/domain1/logs/server.log` while you use the example; each agent logs its phases. If answers look canned, the LLM provider is not configured. See [Troubleshooting](../README.md#troubleshooting).
