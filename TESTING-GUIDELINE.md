# Testing Guideline

This document describes the testing practices for Jakarta Agentic AI, including compatibility requirements and TCK usage.

## Technology Compatibility Kit (TCK)
- The TCK module verifies compatibility of implementations with the Jakarta Agentic AI specification.
- All implementations must pass the TCK to be considered compliant.

## Running Tests
- Build the TCK module with:
  ```
  mvn clean package
  ```
- Add additional tests as needed to cover new features and edge cases.

## Reporting Issues
- Please report any test failures or compatibility issues via GitHub Issues.
