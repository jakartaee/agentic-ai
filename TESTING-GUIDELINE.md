# Testing Guideline

This document describes the testing practices for Jakarta Agentic AI, including compatibility requirements and TCK usage.

## Technology Compatibility Kit (TCK)

- The TCK module verifies compatibility of implementations with the Jakarta Agentic AI specification.
- All implementations must pass the TCK to be considered compliant.
- A compatibility run is made by an implementation from its own runner project with its own Arquillian adapter, following the TCK Reference Guide in `tck-dist`. This repository runs the TCK on Weld Embedded, a test dependency of the `tck` module.

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

The implementation under test supplies its Arquillian container configuration and runtime dependencies in its own runner project.

### Plain-CDI baseline

The plain-CDI baseline is a project-development convenience for specification and TCK contributors: it runs as much of the TCK as plain CDI can satisfy. The `baseline` profile of the `tck` module selects it by adding a class path marker, and is active whenever `tck.implementation` is unset, so the plain build is the baseline run:

```
mvn --projects tck --also-make verify
```

In baseline mode, `@RequiresNoImplementation` assertions run and `@RequiresImplementation` assertions are skipped. This is also the project CI path, which additionally runs once with `-Dtck.implementation=none` to prove the implementation-dependent assertions fail, rather than skip, when the marker is absent. Implementors never see the baseline: they consume the published TCK jar from their own runner, where the profile does not exist, and the marker is a class path entry rather than a system property so no `-D` flag can select it by accident.

## TCK Distribution

The `tck-dist` module assembles the certifiable TCK: a zip under the Eclipse Foundation Technology Compatibility Kit License that bundles the TCK jar and sources, the signature baseline, the TCK Reference Guide (HTML and PDF) and a starter runner project. It is built by `mvn clean install` and lands in `tck-dist/target/`. Final releases are published to download.eclipse.org rather than Maven Central; the EPL-licensed jar on Maven Central cannot support a compatibility claim.

## Reporting Issues

Please report test failures or compatibility issues through GitHub Issues. A claim that a test does not conform to the specification is a TCK challenge; file it with the `TCK Challenge` issue template. A request to be listed as a compatible implementation uses the `Compatibility Certification Request` template. Both follow the [Jakarta EE TCK Process](https://jakarta.ee/committees/specification/tckprocess).
