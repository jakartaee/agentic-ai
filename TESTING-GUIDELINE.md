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

## Where the TCK runs

As with other Jakarta EE TCKs, a compatibility run happens in the implementation's own repository, from its own runner project with its own Arquillian adapter. Every test in the TCK jar must run and pass; nothing may be skipped or excluded.

This repository has no implementation, so its build runs the part of the TCK that plain CDI can satisfy on Weld Embedded. Assertions that only a compatible implementation can satisfy — the `@Decision`, `@Action`, `@Outcome` and `@OnException` phases — carry `@RequiresImplementation`, which is a JUnit tag; the `tck` module's Failsafe configuration excludes it, and nothing else does. That is the plain build:

```
mvn --projects tck --also-make verify
```

That run is the project's CI path. It is not a compatibility result.

## TCK Distribution

The `tck-dist` module assembles the certifiable TCK: a zip under the Eclipse Foundation Technology Compatibility Kit License that bundles the TCK jar and sources, the signature baseline, the TCK Reference Guide (HTML and PDF) and a starter runner project. It is built by `mvn clean install` and lands in `tck-dist/target/`. Final releases are published to download.eclipse.org rather than Maven Central; the EPL-licensed jar on Maven Central cannot support a compatibility claim.

## Reporting Issues

Please report test failures or compatibility issues through GitHub Issues. A claim that a test does not conform to the specification is a TCK challenge; file it with the `TCK Challenge` issue template. A request to be listed as a compatible implementation uses the `Compatibility Certification Request` template. Both follow the [Jakarta EE TCK Process](https://jakarta.ee/committees/specification/tckprocess).
