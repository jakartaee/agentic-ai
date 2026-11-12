# Jakarta Agentic Artificial Intelligence Examples

This module contains example applications and usage patterns for Jakarta Agentic AI. These examples demonstrate how to use the Agentic AI API to build real-world agents on Jakarta EE runtimes.

## Included Examples

### Ask Agent

Located in `ask-agent/`, a compact example: a single `@Agent` that answers a
question, exercising all four phases (`@Trigger`, `@Decision`, `@Action`,
`@Outcome`) over a synchronous REST call. Runs on a small local
Ollama model, so it needs no API key. Start here. See its
[README](ask-agent/README.md).

### Customer Inquiry Intake

Located in `customer-inquiry-intake/`, a deployable web application: a
customer-facing contact form backed by a single-agent pipeline. On submit,
the agent enriches the sender's identity from the message, infers the
sender's company from the business-email domain, classifies the inquiry,
drafts a subject line, and polishes the message in that tone; the customer
reviews the prepared record before confirming. Each phase receives the
earlier results it declares as parameters. See its
[README](customer-inquiry-intake/README.md).

### Fraud Detection Agent

Located in `fraud-detection/`, this example demonstrates:

- Using the `@Agent`, `@Trigger`, `@Decision`, `@Action`, and `@Outcome` annotations
- Integrating a Large Language Model (LLM) for fraud analysis
- Handling workflow context and domain objects
- Marking transactions as suspect and sending notifications

**Purpose:**
Detects potentially fraudulent bank transactions and demonstrates a typical agent workflow with LLM integration and CDI.

### Documentation Agent

Located in `docs-agent/`, this example demonstrates:

- Monitoring pull requests for documentation needs
- Using the `@Decision` annotation to analyze PRs with an LLM
- Generating and reviewing documentation pull requests
- Handling exceptions in agent workflows

**Purpose:**
Automatically generates and applies documentation updates based on code changes, showcasing advanced workflow branching and exception handling.

### Lesson Builder

Located in `lesson-builder/`, a deployable web application with two
agents and the teacher in between. The first writes a lesson's introduction,
quiz and conclusion from a topic and its material. The teacher edits and
publishes the lesson. The publication event triggers the second agent, which
works out what the lesson covers, decides whether the content matches the stated title
and whether the catalog already carries it, then tags curriculum standards,
estimates class time, writes a short abstract and files the entry — or sends
the teacher back to fix a mismatch or merge a duplicate. See its
[README](lesson-builder/README.md).

## Which examples can I run?

| Example | Packaging | Runs on Payara (first implementation)? | LLM backend (default) | Needs |
|---|---|---|---|---|
| `ask-agent` | WAR | Yes, start here | Ollama `gemma3:4b` (local) | Ollama |
| `customer-inquiry-intake` | WAR | Yes | Claude on Vertex AI | GCP credentials, or switch to Ollama / Anthropic |
| `lesson-builder` | WAR | Yes | Claude on Vertex AI | GCP credentials, or switch to Ollama / Anthropic |
| `fraud-detection` | JAR | No, compile-only | n/a | Illustrates the API; has no REST endpoint, event source or config |
| `docs-agent` | JAR | No, compile-only | n/a | Same as `fraud-detection` |

`fraud-detection` and `docs-agent` have no web layer: nothing fires their
`@Trigger` event, so deploying them does nothing. Read them as annotated code
samples. Each deployable example also has its own README.

## Set up the environment (Payara Server Community)

Do this once, whatever example you pick. No prior Payara knowledge is assumed.

### 1. Install the tools

| Tool | Version | Check |
|---|---|---|
| JDK | 21 or newer to run Payara 7 (the examples compile at Java 17) | `java -version` |
| Maven | 3.9+ | `mvn -v` |
| Payara Server Community | **7.2026.8 or newer** (the first release with Jakarta Agentic AI; the latest at the time of writing is 7.2026.10) | see step 2 |
| `curl` | any | `curl --version` |

Payara Server Community is, for now, the **first implementation** of the Jakarta
Agentic AI specification, and the examples are written to run on it. The
specification is still at milestone 1 (`1.0.0-M1`), so the API and Payara's
support for it may change between releases; use the latest Payara Community.
Agentic AI is **already bundled** in Payara Community 7, so there is no separate
runtime module to install. Payara 7 is Jakarta EE 11; the examples are built
against the EE 10 API and run unchanged.

The `payara.agentic.llm.*` configuration keys used below are Payara-specific.
The agent code itself uses only the `jakarta.ai.agent` API, so it should run on
any future implementation. Only the provider configuration would change.

### 2. Download and start Payara

Get the zip from the
[Azul Payara Community downloads](https://www.azul.com/downloads/azul-payara-community-edition/)
or from Maven Central
(`fish.payara.distributions:payara:<version>`, `.zip`), then:

```bash
unzip payara-<version>.zip
export PAYARA_HOME="$PWD/payara7"
export PATH="$PAYARA_HOME/bin:$PATH"     # puts `asadmin` on the PATH
asadmin start-domain                     # starts domain1: HTTP 8080, admin console 4848
asadmin list-applications                # "Nothing to list." means the server is up
```

`asadmin` is not installed globally; every `asadmin` command in these READMEs
assumes the `PATH` line above. Stop the server with `asadmin stop-domain`.

> **Set LLM credentials before `start-domain`.** The server inherits the
> environment of the shell that starts it. Exporting a variable afterwards
> has no effect until you run `asadmin restart-domain` from that shell.

### 3. Build the examples

Build from the **repository root**, not from inside `examples/`. The examples
depend on `jakarta.agentic-ai-api:1.0.0-SNAPSHOT`, which is not on Maven
Central, so `-am` (also-make) builds it first:

```bash
mvn -pl examples/ask-agent -am clean package
```

Use `clean`: stale `target/` directories can leave old WARs behind. For
example `course-content-studio.war`, `quickstart.war` and `tutorial-generator.war`
may linger from older builds; `mvn clean` does not remove them because those
directories are no longer modules. Delete the directories, or deploy only the
WAR you just built.

To build every example (and the API they need), run
`mvn clean package -DskipTests` from the root.

### 4. Pick an LLM backend

Agents never call a provider directly. They inject `LargeLanguageModel`, and
Payara picks the backend from MicroProfile Config keys prefixed
`payara.agentic.llm.`. Each example ships defaults in
`src/main/resources/META-INF/microprofile-config.properties`.

| `provider` | Needs | Extra keys |
|---|---|---|
| `ollama` | A local Ollama (<https://ollama.com>), no key, no cost | `model`, `ollama.base-url` (default `http://localhost:11434`) |
| `anthropic` (alias `claude`) | An Anthropic API key | `anthropic.api-key` or env `ANTHROPIC_API_KEY` |
| `vertex` | Claude on Google Vertex AI and `gcloud auth application-default login` | `vertex.project-id` or env `ANTHROPIC_VERTEX_PROJECT_ID`; `vertex.region` or env `CLOUD_ML_REGION` (default `global`) |
| `none` / unset | Nothing | Returns a fixed placeholder response |

**An unset or misspelled provider does not fail the deployment.** It silently
falls back to `none`, so you get placeholder answers instead of an error. If the
output looks canned, check the provider key first.

**Ollama setup**

```bash
ollama pull gemma3:4b        # ask-agent
ollama pull gemma3:12b       # intake / lesson-builder when run locally
curl -s http://localhost:11434/api/tags   # confirms Ollama is answering
```

The 4B model is enough for `ask-agent`. The other two examples need
well-formed JSON from the model, so use a 12B-class model for them.

**Changing the backend without editing the WAR.** Config sources can be
overridden at the server. Environment variables map by upper-casing and
replacing `.` and `-` with `_`, so set them before `start-domain`:

```bash
export PAYARA_AGENTIC_LLM_PROVIDER=ollama
export PAYARA_AGENTIC_LLM_MODEL=gemma3:12b
asadmin start-domain
```

### 5. Deploy, verify and read the logs

Run from the repository root:

```bash
asadmin deploy --force examples/ask-agent/target/ask-agent.war   # --force redeploys
asadmin list-applications                                        # ask-agent should be listed
tail -f "$PAYARA_HOME/glassfish/domains/domain1/logs/server.log"
```

Redeploy with `--force` after every rebuild. `undeploy` takes the
application name, not the path: `asadmin undeploy ask-agent`.

Each agent logs its phases as they run (`[TRIGGER]`, `[DECISION]`,
`[ACTION]`, `[OUTCOME]`). Watching `server.log` while you call the endpoint is the
quickest way to see how the specification's workflow executes.

## Running the tests

These examples have no unit or integration tests of their own, so there is no
`mvn test` step. Treat each endpoint as a smoke test:

| Example | Check | Expected |
|---|---|---|
| `ask-agent` | `curl -s localhost:8080/ask-agent/api/ask -H 'Content-Type: application/json' -d '{"question":"What is Jakarta EE in one sentence?"}'` | `200` with `{"question":...,"answer":...}`; the log shows all four phases |
| `ask-agent` | same call with `{"question":""}` | `400` `{"message":"A question is required."}` |
| `ask-agent` | repeat the first question | Same answer. `@Decision` returns `false` for a question already answered, so the LLM is not called again |
| `ask-agent` | stop Ollama, ask a new question | `503` `{"message":"The LLM backend is unavailable..."}` |
| `customer-inquiry-intake` | open <http://localhost:8080/customer-inquiry-intake/> and follow its README | A prepared record with name, email, company, category, subject and polished message |
| `lesson-builder` | open <http://localhost:8080/lesson-builder/> and follow its README | A generated lesson draft; after publishing, a **Catalog** entry, or a halt for a duplicate or title mismatch |

Local models can take tens of seconds per phase, especially on first use while
the model loads. The first request is often much slower than the rest.

The specification's own conformance suite lives in the `tck` module. See
[TESTING-GUIDELINE.md](../TESTING-GUIDELINE.md).

## Troubleshooting

| Symptom | Likely cause and fix |
|---|---|
| `asadmin: command not found` | `$PAYARA_HOME/bin` is not on the `PATH`; see step 2 |
| `Could not resolve ... jakarta.agentic-ai-api:1.0.0-SNAPSHOT` | You built from inside `examples/` or a module directory. Build from the root with `-am` |
| Deploy succeeds but answers look fixed or canned | Provider is `none`: the key is missing or misspelled. Check the properties file, and any `PAYARA_AGENTIC_LLM_*` variables you exported |
| `503` / LLM unavailable | Ollama is not running, the model is not pulled, or `ollama.base-url` is wrong |
| `IllegalStateException` at deploy naming a key | The provider needs a credential (for example `anthropic.api-key`). The message names the exact key or variable |
| Vertex `401`/`403`, or no project | `gcloud auth application-default login` was not run, or `ANTHROPIC_VERTEX_PROJECT_ID` / `CLOUD_ML_REGION` were exported *after* the domain started. Restart the domain from that shell |
| Intake or lesson-builder steps return malformed or empty results | The model is too small to produce reliable JSON. Use a 12B-class model or Claude |
| Port 8080 or 4848 already in use | Another process holds it. Stop it, or run `asadmin create-domain` on other ports |
| Changes not visible after a rebuild | Redeploy with `asadmin deploy --force`, and open the page with a hard refresh |
