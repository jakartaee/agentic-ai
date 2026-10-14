# Testing Guideline

This document describes the testing practices for Jakarta Agentic AI, including compatibility requirements and TCK usage.

## Technology Compatibility Kit (TCK)

- The TCK module verifies compatibility of implementations with the Jakarta Agentic AI specification.
- All implementations must pass the TCK to be considered compliant.
- A compatibility run requires an Arquillian container profile, such as `-Pweld-embedded` for the embedded CDI container used by this project. `mvn verify` without a container profile excludes the `deployed` group and is an API-only run, not a compatibility result.

## Running Tests

Build and run the TCK with the required upstream modules:

```
mvn --projects tck --also-make verify
```

For a full clean build of the TCK artifacts:

```
mvn --projects tck --also-make clean install
```

Add tests as needed to cover new features and edge cases.

## TCK Run Modes

The TCK contains two kinds of assertions:

- `@RequiresImplementation` assertions exercise behavior that only a compatible Jakarta Agentic AI implementation can dispatch, including `@Decision`, `@Action`, `@Outcome`, and `@OnException` phases.
- `@RequiresNoImplementation` assertions verify the limited plain-CDI baseline in which only ordinary CDI behavior, such as an observed `@Trigger`, is available.

### Implementation mode is the default

The primary purpose of the TCK is to certify implementations. A container run enables `@RequiresImplementation` assertions and disables `@RequiresNoImplementation` baseline assertions. No implementation-presence flag or implementation profile is required.

A container run without a compatible implementation is expected to fail its implementation-dependent assertions. This fail-closed default prevents an omitted flag from silently skipping the compatibility suite and reporting false compliance.

The implementation under test supplies its Arquillian container configuration and runtime dependencies. For this repository that container is `-Pweld-embedded`.

### Explicit plain-CDI baseline

The plain-CDI baseline is a project-development convenience for specification and TCK contributors. Select it only with `-Pbaseline`, together with the embedded container:

```
mvn --projects tck --also-make verify -Pweld-embedded,baseline
```

The `baseline` profile is what selects this mode. In baseline mode, `@RequiresNoImplementation` assertions run and `@RequiresImplementation` assertions are skipped. This is also the project CI path.

Passing `-Djakarta.ai.agent.tck.baseline=true`, `-Dtck.baseline=true`, or `-Djakarta.ai.agent.tck.internal.baseline-profile-active=true` does not select baseline mode. The profile adds a classpath marker rather than a system property. Other Maven `-D` settings are still passed through to the test JVM.

## Reporting Issues

Please report test failures or compatibility issues through GitHub Issues.
