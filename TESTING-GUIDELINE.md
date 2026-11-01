# Testing Guideline

This document describes the testing practices for Jakarta Agentic AI, including compatibility requirements and TCK usage.

## Technology Compatibility Kit (TCK)

- The TCK module verifies compatibility of implementations with the Jakarta Agentic AI specification.
- All implementations must pass the TCK to be considered compliant.
- A compatibility run is made by an implementation from its own runner project with its own Arquillian adapter, following the TCK Reference Guide in `tck-dist`. This repository builds the TCK and runs only its signature test.

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

This repository has no implementation to deploy to, so, exactly like the Data and Concurrency TCKs, its build compiles the deployed tests into the jar without executing them; the only test it runs is the container-less signature test. The TCK deploys no `LargeLanguageModel` of its own — the implementation's facade is what the LLM assertions test, backed during a run by the echo model the TCK Reference Guide specifies under "Additional Certification Requirements". The plain build is:

```
mvn --projects tck --also-make verify
```

That run is the project's CI path. The deployed tests are validated by the implementation's run of the TCK.

## TCK Distribution

The `tck-dist` module assembles the certifiable TCK: a zip under the Eclipse Foundation Technology Compatibility Kit License that bundles the TCK jar and sources, the signature baseline, the TCK Reference Guide (HTML and PDF) and a starter runner project. It is built by `mvn clean install` and lands in `tck-dist/target/`. Final releases are published to download.eclipse.org rather than Maven Central; the EPL-licensed jar on Maven Central cannot support a compatibility claim.

## Reporting Issues

Please report test failures or compatibility issues through GitHub Issues. A claim that a test does not conform to the specification is a TCK challenge; file it with the `TCK Challenge` issue template. A request to be listed as a compatible implementation uses the `Compatibility Certification Request` template. Both follow the [Jakarta EE TCK Process](https://jakarta.ee/committees/specification/tckprocess).
