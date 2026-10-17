# Course Content Studio — Jakarta Agentic AI example

A teacher pastes a chapter, picks a subject and clicks **Generate packet**; one
agent writes an introduction, a quiz and a conclusion, another rewrites any one
section from an instruction, and on **Approve & publish** a third turns the
packet into the student-facing lesson, adding learning objectives, so a student
can take the quiz.

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
> model is recommended so the quiz, objectives and grading steps produce
> well-formed JSON.

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
*Newton's Second Law*, paste a few paragraphs and click **Generate packet**.
Under **Refine**, choose **Quiz**, ask for *"a final open question the student
answers in free text"* and click **Refine**. Click **Approve & publish**, then
**View published lesson** to take the quiz as a student.
