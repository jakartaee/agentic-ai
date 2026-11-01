# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

- TCK: the API signature check now runs as a JUnit test (`SignatureTests`) driving the Jakarta TCK signature engine against the shipped baseline, so implementors run it in their normal TCK execution; the plugin-only `strictcheck` and the hand-written reflection checks it replaces are gone.
- TCK: tests use JUnit assertions instead of AssertJ, which was not part of the deployed archive on a real container.
- New `tck-dist` module assembling the EFTL-licensed TCK distribution zip: TCK jar and sources, signature baseline, TCK Reference Guide (HTML and PDF) and a starter runner project.
- GitHub issue templates for TCK challenges and compatibility certification requests.

## [1.0.0-M1] - 2026-07-29
First community milestone of Jakarta Agentic AI. Published as a non-final, draft-status API to Maven Central for early feedback. Not an EFSP-reviewed release.

- Initial `jakarta.ai.agent` API surface: `@Agent`, `@Trigger`, `@Decision`, `@Action`, `@Outcome`, `@HandleException`, `@WorkflowScoped`, `LargeLanguageModel`, `Result`, `LLMException`.
- Specification draft covering programming model, lifecycle, CDI integration, and LLM facade.
