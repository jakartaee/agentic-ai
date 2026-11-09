# Jakarta Agentic AI TCK

The Technology Compatibility Kit for Jakarta Agentic AI.

The tests ship in the
published jar. An implementation runs them from its own repository to certify.
This repository only builds the jar.

To certify an implementation, read the TCK Reference Guide built by the
[`tck-dist`](../tck-dist/) module. It covers the requirements, running the TCK,
the signature test, and the challenge and certification process.

Build with `mvn clean install`. The API signature baseline the signature test
uses is generated from the API during the build and packaged in the jar.

## License

Eclipse Public License v. 2.0 — see [LICENSE](../LICENSE).
