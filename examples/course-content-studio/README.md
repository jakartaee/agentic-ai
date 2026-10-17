# Course Content Studio — Jakarta Agentic AI example

A teacher pastes a chapter, picks a subject and clicks **Generate lesson**; an
agent writes an introduction, a quiz and a conclusion. The teacher edits
anything and clicks **Approve & publish**; the lesson goes live and a second
agent schedules it on the course syllabus — in the first week after everything
it assumes has been taught, or not at all until that has been.

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
> model is recommended so the quiz and syllabus steps produce well-formed
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
mvn -pl examples/course-content-studio -am package
asadmin deploy examples/course-content-studio/target/course-content-studio.war
```

Open <http://localhost:8080/course-content-studio/>, pick **Physics**, title it
*Newton's Second Law*, paste a few paragraphs and click **Generate lesson**.
Edit anything in the draft and click **Approve & publish**. Then do the same
for a chapter on *Newton's First Law*, and once more for the second law.
