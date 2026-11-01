# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

- TCK: the API signature check now runs as a JUnit test (`SignatureTests`) driving the Jakarta TCK signature engine against the shipped baseline, so implementors run it in their normal TCK execution; the plugin-only `strictcheck` and the hand-written reflection checks it replaces are gone.
- TCK: tests use JUnit assertions instead of AssertJ, which was not part of the deployed archive on a real container.
- New `tck-dist` module assembling the EFTL-licensed TCK distribution zip: TCK jar and sources, signature baseline, TCK Reference Guide (HTML and PDF) and a starter runner project.
- GitHub issue templates for TCK challenges and compatibility certification requests.
- TCK: compatibility runs happen in the implementation's repository and must execute every test with nothing skipped; the trigger-only plain-CDI checks moved out of the jar into this repository's `src/test`, `@RequiresImplementation` is now a plain JUnit tag that only this repository's build excludes, and the baseline marker, profile, conditions and `tck.implementation` property are gone.
- TCK trimmed to what the Data and Concurrency TCKs carry: the 70 reflection-based structural tests are gone (the strict signature baseline already asserts everything they did), as are the stub's unused per-workflow keying, `ExecutionTraceRecorder.assertOrder`, the per-test INFO logging and the framework unit tests that only re-tested what a TCK run proves.
- TCK build simplified to its two audiences: Weld Embedded is a plain test dependency, the `weld-embedded` profile and `deployed` group exclusion are gone, the `baseline` marker is added straight from `src/baseline-marker`, and `@SkipWhenBaseline` is replaced by class-level `@RequiresImplementation`.

## [1.0.0-M1] - 2026-07-29
First community milestone of Jakarta Agentic AI. Published as a non-final, draft-status API to Maven Central for early feedback. Not an EFSP-reviewed release.

- Initial `jakarta.ai.agent` API surface: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@HandleException`, `@WorkflowScoped`, `LargeLanguageModel`, `Result`, `LLMException`.
- Specification draft covering programming model, lifecycle, CDI integration, and LLM facade.
