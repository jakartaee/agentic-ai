# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

- Examples: the REST resources caught `ObserverException` around `Event.fire()` and inspected its cause, but CDI wraps only checked exceptions that way; `ConstraintViolationException` and `LLMException` are unchecked and arrive unwrapped, so the handlers never ran. They now catch the two exceptions directly.
The TCK was reworked to the shape of the other modern Jakarta EE TCKs (Data, Concurrency):

- TCK coverage: assertions no longer require `ObserverException` wrapping that CDI does not perform for unchecked exceptions, orchestration agents use explicit `order` instead of relying on source-declaration order the specification does not guarantee, and a CDI client-proxy check that tested CDI rather than this specification is removed. New assertions cover general `@Observes` methods on `@ApplicationScoped` agents, validation on trigger and decision parameters, triggers declared without `@Observes`, handlers with additional state parameters, `LLMException` routed to `@OnException`, context destruction after a decision stop, a constraint violation on an `@OnException` parameter propagating instead of re-entering handling, `@Priority` mixed with unordered methods as a deployment error, and a null prompt on `query(String, Class)`.

- New `tck-dist` module assembling the EFTL-licensed TCK distribution zip required for any compatibility claim: the TCK jar and sources, the signature baseline, the TCK Reference Guide (HTML and PDF) and a starter runner project. GitHub issue templates for TCK challenges and compatibility certification requests.
- Compatibility runs happen in the implementation's repository and must execute every test in the jar with nothing skipped. The specification repository builds the jar and runs only the container-less signature test; the former in-repo Weld run, its run modes, profiles, tags, execution conditions, class path marker and `tck.implementation` property are gone.
- The API signature check is a JUnit test in the jar (`SignatureTests`) driving the Jakarta TCK signature engine against the shipped baseline, strict in both directions, so implementors run it in their normal execution; the plugin-only check it replaces is gone.
- The TCK deploys no `LargeLanguageModel`: the implementation's own facade is injected and verified. A certification requirement (`TckModel`, guide section "Additional Certification Requirements") defines the echo behaviour the model behind that facade must have during a run, which makes placeholder substitution, JSON Binding conversion, `LLMException` mapping and per-workflow conversational state testable against the implementation. The TCK-owned stub that previously stood in for the facade is removed.
- Tests that did not exercise an implementation are removed: the reflection-based structural tests (the signature baseline already asserts everything they did), the trigger-only checks that only held without an implementation, and assertions that reflected over fixtures. Tests use JUnit assertions instead of AssertJ, which was never part of a deployed archive. The framework is reduced to the `@Assertion` and tag annotations, the Arquillian extension, the trace recorder, `TckModel` and the signature test.

## [1.0.0-M1] - 2026-07-29
First community milestone of Jakarta Agentic AI. Published as a non-final, draft-status API to Maven Central for early feedback. Not an EFSP-reviewed release.

- Initial `jakarta.ai.agent` API surface: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@HandleException`, `@WorkflowScoped`, `LargeLanguageModel`, `Result`, `LLMException`.
- Specification draft covering programming model, lifecycle, CDI integration, and LLM facade.
