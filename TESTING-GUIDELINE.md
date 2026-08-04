# Testing Guideline

This document describes the testing practices for Jakarta Agentic AI, including compatibility requirements and TCK usage.

## Technology Compatibility Kit (TCK)
- The TCK module verifies compatibility of implementations with the Jakarta Agentic AI specification.
- All implementations must pass the TCK to be considered compliant.

## Running Tests
- Build and run the TCK with required upstream modules:
  ```
  mvn --projects tck --also-make verify
  ```
- If you need a full clean build of the TCK artifacts as well:
  ```
  mvn --projects tck --also-make clean install
  ```
- Add additional tests as needed to cover new features and edge cases.

## Signaling Implementation Presence
The TCK contains two kinds of assertions:

- Assertions tagged with `@RequiresImplementation` exercise behavior that
  only a compatible Jakarta Agentic AI implementation can dispatch
  (`@Decision`, `@Action`, `@Outcome`, `@HandleException`).
- Assertions tagged with `@RequiresNoImplementation` verify the plain-CDI
  baseline (`@Trigger` observation only) and must be skipped when a
  compatible implementation is present.

The TCK detects implementation presence via a system property.
Implementations running the TCK must set:

```
-Djakarta.ai.agent.tck.implementation.present=true
```

on the Surefire / Failsafe `argLine` (or globally on the test JVM). Leaving
the property unset — the default — signals a plain-CDI run and skips the
`@RequiresImplementation` assertions.

## Declaring Expected Run Mode
An optional second property declares the expected run mode so a misconfigured
invocation fails fast instead of silently skipping the behavioral suite:

```
-Djakarta.ai.agent.tck.mode=implementation
```

or

```
-Djakarta.ai.agent.tck.mode=baseline
```

Rules:

- `mode=implementation` requires `-Djakarta.ai.agent.tck.implementation.present=true`.
  If the presence flag is missing or false, the run fails with a clear error.
- `mode=baseline` requires the presence flag to be unset / false. If the
  presence flag is true, the run fails.
- When `jakarta.ai.agent.tck.mode` is unset, behavior is unchanged (default
  baseline: `@RequiresImplementation` assertions are skipped).
- Values are case-insensitive; unknown values fail fast.

### Implementation-mode invocation
Vendors running against a compatible implementation should set both properties:

```
mvn -pl tck verify -Pweld-embedded \
  -Djakarta.ai.agent.tck.implementation.present=true \
  -Djakarta.ai.agent.tck.mode=implementation
```

A plain-CDI baseline run needs neither property (or may set
`-Djakarta.ai.agent.tck.mode=baseline` explicitly).

## Reporting Issues
- Please report any test failures or compatibility issues via GitHub Issues.
