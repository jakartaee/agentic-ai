# Jakarta Agentic AI TCK ${project.version}

This is the Technology Compatibility Kit (TCK) for Jakarta Agentic AI, distributed under the
Eclipse Foundation Technology Compatibility Kit License (see `LICENSE`). Only a run of this
distribution can support a claim of compatibility; the EPL-licensed copy of the TCK jar on
Maven Central carries no such standing.

Contents:

- `docs/` — the TCK Reference Guide (HTML and PDF). Start here. It covers requirements,
  installation, how to run the TCK, the signature test, how to file challenges and
  certification requests, and the compatibility rules.
- `artifacts/` — the TCK jar, its sources jar, the parent pom it needs, the API signature
  file (`jakarta.ai.agent.sig_*`) for reference, and `artifact-install.sh`, which installs
  the jar and pom into a local Maven repository.
- `starter/` — a minimal Maven runner project to copy and configure for your implementation.
- `LICENSE` — the EFTL terms.
- `README.md` — this file.

Quick start:

```
cd artifacts && ./artifact-install.sh && cd ../starter
# edit pom.xml and src/test/resources/arquillian.xml for your runtime
mvn clean verify
```
