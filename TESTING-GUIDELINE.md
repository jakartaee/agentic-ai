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

## Reporting Issues
- Please report any test failures or compatibility issues via GitHub Issues.
