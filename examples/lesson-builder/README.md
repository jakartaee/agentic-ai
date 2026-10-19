# Lesson Builder — Jakarta Agentic AI example

A teacher names a topic, pastes the material and clicks **Generate lesson**; an
agent writes an introduction, a quiz and a conclusion. The teacher edits
anything and clicks **Approve & publish**; the lesson goes live and a second
agent files it in the course catalogue with its topics, curriculum-standard
tags, class-time estimate and a short abstract. The catalogue agent holds the
lesson back when its content does not match its title, or when the catalogue
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

> The `payara.agentic.llm.*` keys are the configuration surface of the Payara
> implementation used to run this example. On another Jakarta Agentic AI
> implementation, configure the provider the way that implementation
> documents. To run fully local instead of Claude, switch to
> `payara.agentic.llm.provider=ollama` / `model=gemma3:12b` — a 12B-class
> model is recommended so the quiz and catalogue steps produce well-formed
> JSON.

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
mvn -pl examples/lesson-builder -am package
asadmin deploy examples/lesson-builder/target/lesson-builder.war
```

Open <http://localhost:8080/lesson-builder/>, pick **Physics**, enter the topic
*Newton's Second Law*, paste a few paragraphs of material and click **Generate
lesson**. Edit anything in the draft and click **Approve & publish**. Switch
to the **Catalogue** tab to see the entry the catalogue agent filed, with its
coverage, curriculum-standard tags, class-time estimate and abstract. Then
publish the same lesson a second time to see the duplicate-detection halt, or
rename it to *Momentum* and publish to see the content-match halt.
