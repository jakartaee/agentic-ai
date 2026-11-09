# Jakarta Agentic AI TCK

The Technology Compatibility Kit for Jakarta Agentic AI. The tests ship in the
published jar; an implementation runs them from its own repository to certify.
This repository only builds the jar — it has no implementation to run them against.

To certify an implementation, read the TCK Reference Guide built by the
[`tck-dist`](../tck-dist/) module: it covers the requirements, running the TCK,
the signature test, and the challenge and certification process.

Build with `mvn clean install`. Regenerate the signature baseline, when an API
change is intended, with `mvn -pl tck verify -Psignature-generation`.

## License

Eclipse Public License v. 2.0 — see [LICENSE](../LICENSE).
