# Lesson Builder — Jakarta Agentic AI example

A teacher names a topic, pastes the material and clicks **Generate lesson**. An
agent writes an introduction, a quiz and a conclusion. The teacher edits
anything and clicks **Approve & publish**. The lesson goes live and a second
agent files it in the course catalog with its coverage, curriculum-standard
tags, class-time estimate and a short abstract. The catalog agent holds the
lesson back when its content does not match its title, or when the catalog
already carries a lesson covering the same ground.

## Configure the LLM

The LLM provider is selected via MicroProfile Config. The example defaults to
**Anthropic (Claude) on Google Cloud Vertex AI**; the system prompt lives in
config and is reused as the Anthropic prompt-cache prefix:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=8192
payara.agentic.llm.system=You are an expert curriculum designer and teacher...
```

> Payara Server Community (7.2026.8 or newer) is currently the first
> implementation of the Jakarta Agentic AI specification, and the
> `payara.agentic.llm.*` keys are its configuration surface. Provider
> configuration is not yet standardized, so on another implementation
> configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the quiz and catalog steps produce well-formed
> JSON.

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
mvn -pl examples/lesson-builder -am clean package
asadmin deploy --force examples/lesson-builder/target/lesson-builder.war
```

Open <http://localhost:8080/lesson-builder/>, pick **Physics**, enter the topic
*Newton's Second Law*, paste a few paragraphs of material and click **Generate
lesson**. Edit anything in the draft and click **Approve & publish**. Switch
to the **Catalog** tab to see the entry the catalog agent filed, with its
coverage, curriculum-standard tags, class-time estimate and abstract. Then
publish the same lesson a second time to see the duplicate-detection halt, or
rename it to *Momentum* and publish to see the content-match halt.

## Verify and troubleshoot

Watch `$PAYARA_HOME/glassfish/domains/domain1/logs/server.log` while you use the example; each agent logs its phases. If answers look canned, the LLM provider is not configured. See [Troubleshooting](../README.md#troubleshooting).
