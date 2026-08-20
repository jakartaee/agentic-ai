# Course Content Studio — Jakarta Agentic AI sample

An **education** sample for the Jakarta Agentic AI API, running on **Payara
Server**. A teacher pastes a chapter and picks a subject; an agent generates an
**introduction**, a **quiz** and a **conclusion**; the teacher **refines** any
part by chat and then **approves & publishes**, which triggers a **second agent**
that builds the student-facing lesson.

It is a deliberately "advanced" sample — it exercises features the basic
quickstart does not:

| Feature | Where |
|---|---|
| **Ordered phases** (`@Decision`/`@Action` with `order`) | `CourseContentAgent` — intro → quiz → conclusion |
| **Typed result via JSON-B** | `writeQuiz` binds the model output to a `Quiz` record (parsed defensively, since small models wrap JSON in code fences) |
| **Per-workflow conversational memory** | `writeConclusion` does not re-send the chapter |
| **Human-in-the-loop** (generate vs. refine, per section) | event carries the mode; `refine-section` |
| **Multi-agent chaining via CDI events** | approval fires `LessonApproved` → `PublishAgent` builds the lesson |
| **Resilience** (`@HandleException`) | LLM outage / invalid request handled; unusable quiz JSON falls back to a placeholder |
| **Per-subject specialisation** | `SubjectRubric` prepends a subject rubric (with LaTeX guidance for maths/physics) |
| **Live progress (SSE)** | `ProgressTracker` streams each phase to the browser popup |
| **Polymorphic quiz + LLM grading** | open questions graded by semantic similarity via `POST /api/quiz/grade` (`@Dependent` LLM used directly, no agent) |

## How it works

`CourseResource` fires a `CoursePacketRequest` CDI event. `Event.fire(...)` is
**synchronous**, so the whole workflow (every LLM call) finishes before it
returns; the resource then reads the finished `CoursePacket` back from
`PacketStore`. The agents have **no scope annotation**, so the runtime applies
`@WorkflowScoped` (the spec default) — that is what makes their instance fields
safe as per-workflow state across the ordered phases.

On **approve**, the resource fires a second event, `LessonApproved`, which
triggers `PublishAgent`. The two agents are composed purely through CDI events,
with the approval acting as a human-in-the-loop gate between them. Formulas are
rendered with MathJax in both the studio and the student view.

```
GET  /course/                           the studio UI
GET  /course/student.html               the published lesson, as a student sees it
GET  /course/api/subjects               subject list
POST /course/api/packet/generate        { subject, chapterTitle, chapterBody }
POST /course/api/packet/refine-section  { section: intro|quiz|conclusion|all, instruction }
POST /course/api/packet/approve         approve + trigger PublishAgent
GET  /course/api/packet                 current draft packet
GET  /course/api/lesson                 published (student-facing) lesson
POST /course/api/quiz/grade             grade an open answer via the LLM (similarity)
GET  /course/api/progress/{runId}       live phase progress (Server-Sent Events)
```

## Prerequisites

- JDK 17+
- Maven 3.9+
- A **Payara Server 7** build that includes the `agentic-ai-core` runtime module
  (it provides the `jakarta.ai.agent` API and the LLM backends).
- An LLM backend. **Default: Claude on Google Cloud Vertex AI** — fast and
  reliable for live demos. You need:
  - a GCP project with a Claude model enabled on Vertex AI, and
  - Application Default Credentials on the server (`gcloud auth application-default login`).

## Configure the LLM

Backend selection is server-side, in
`src/main/resources/META-INF/microprofile-config.properties`. The default is
**Vertex** with `claude-sonnet-4-6`:

```properties
payara.agentic.llm.provider=vertex
payara.agentic.llm.model=claude-sonnet-4-6
payara.agentic.llm.max-tokens=8192
```

Set the project, region and auth in the **server environment before**
`restart-domain` (project/region fall back to these env vars when the
properties are absent):

```bash
export ANTHROPIC_VERTEX_PROJECT_ID=your-gcp-project
export CLOUD_ML_REGION=us-east5            # a region where Claude is enabled, or "global"
gcloud auth application-default login       # or: export GOOGLE_ACCESS_TOKEN=$(gcloud auth application-default print-access-token)
```

The file also carries commented blocks for two alternatives — **Anthropic**
direct (an `ANTHROPIC_API_KEY`) and **local Ollama** (free, but slow on a laptop;
the backend times out after 120 s, so a big local model can fail the quiz step).
Uncomment one block, comment the Vertex one, and rebuild.

## Build & deploy

```bash
mvn clean package
asadmin deploy --contextroot /course target/course.war
```

Then open <http://localhost:8080/course/>.

## Try it

1. Pick **Physics**, title `Newton's Second Law`, paste a few paragraphs, click
   **Generate packet**. The popup streams the live phases; in `server.log` you'll
   see `[TRIGGER] → [DECISION] → writeIntro → writeQuiz → writeConclusion → [OUTCOME]`.
   Formulas are rendered with MathJax.
2. In **Refine**, choose **Quiz**, type *"make the questions harder and add a
   final open question for the student to answer in free text"*, click **Refine**
   — only the quiz changes, and the open question comes back with `type: "open"`.
3. Click **Approve & publish** — the popup shows the second agent
   (`PublishAgent`) writing the learning objectives and publishing.
4. Click **View published lesson ↗** to open `student.html`: objectives, intro,
   and an **interactive quiz**. Multiple-choice questions are checked locally;
   the **open question** shows a textarea, and *Check answers* grades it via the
   LLM (**≥ 70 %** correct, **50–69 %** partial, **< 50 %** incorrect) and reveals
   the model answer.

## Notes

- Single-author demo: `PacketStore` / `PublishedLessonStore` keep one latest
  artifact. A multi-user app would key them by author/session.
- The quiz gate (`hasTeachableContent`) requires ≥ 80 characters of chapter text
  in generate mode; short snippets stop the workflow at the decision phase.
- MathJax is **self-hosted** in the WAR (`webapp/vendor/mathjax/tex-svg.js`), so
  formula rendering works fully offline.
- If the configured provider is `none` (no backend), the runtime's no-op LLM
  returns empty content and the packet will be blank — set a real provider.