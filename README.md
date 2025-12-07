# Jakarta Agentic Artificial Intelligence
The Jakarta Agentic AI project provides a set of vendor-neutral APIs that make it easy, consistent, and reliable to build, deploy, and run AI agents on Jakarta EE runtimes.

## Background
Artificial Intelligence (AI) agents are one of the most prominent developments in enterprise and cloud native computing in decades. They promise to fundamentally accelerate innovation, automation, and productivity by leveraging AI in virtually every industry – IT, finance, banking, retail, manufacturing, health care, and so many others. Agents operate by leveraging Neural Networks, Machine Learning (ML), Natural Language Processing (NLP), Large Language Models (LLMs), and many other AI technologies to aim to perform specific tasks autonomously with little or no human intervention. They detect events, gather data, generate self-correcting plans, execute actions, process results, and evolve subsequent decisions. Examples include self-driving cars, security monitors, Site Reliability Engineering (SRE) agents, stock monitors, code/application generators, health monitors, customer service agents, manufacturing robots, and many others.

This project aims to create an Agentic AI API for Jakarta EE. We will try to do for developing AI agents what Servlet did for HTTP processing, Jakarta REST did for RESTful web services, or perhaps most appropriately, Jakarta Batch did for batch processing.

## Scope
* Defines common usage patterns and life cycles for AI agents running on Jakarta EE runtimes.
* Provides a very minimal facade to access more foundational AI capabilities, such as LLMs, without attempting to standardize LLMs. Instead, the API provides easy, pluggable, and configurable access to existing LLM APIs such as LangChain4j and Spring AI. This is similar to how Jakarta Persistence provides access to underlying non-standard APIs by unwrapping.
* The API will include a mechanism to define agent workflows. This will be done using a fluent Java API (as opposed to XML). The agent workflow will likely be dynamic at runtime rather than strictly defined and static at deployment time. A pluggability mechanism may be provided for YAML and XML.
* Defines integrations with other key Jakarta EE APIs such as Validation, REST, JSON Binding, Persistence, Data, Transactions, NoSQL, Concurrency, Security, Messaging, and so on.
* The project will aim to utilize Jakarta Config if possible. It may allow implementations to utilize MicroProfile Config.
* Implementations may provide integrations with OpenTelemetry.
* The project makes a reasonable effort to keep the API potentially usable in runtimes such as Quarkus, Micronaut, and Spring Boot, though Jakarta EE compatible runtimes are the clear primary target.

## Version 1.0
The initial version is very intentionally minimal. The release seeks to build early momentum, including broadening awareness, participation, and adoption. Subsequently, we aim to iterate quickly based on evolving industry knowledge on Agentic AI as well as user feedback.

The initial release focuses on key programming models, patterns, life cycles, as well as a lightweight LLM facade. Subsequent releases will likely focus more on a programmatic life cycle management, a workflow API and advanced features.

Specifically 1.0 focuses on:
* @Agent annotation to define an agent and it's basic life-cycle/scope
* @Trigger annotation to process CDI events and start the agent workflow
* @Decision annotation to define simple decision points for an agent
* @Action annotation to define a set of initially simple sequential steps an agent takes
* @Outcome annotation to denote the end of an agent workflow
* A very simple LLM facade 

## API Concepts
The following annotated example demonstrates the key concepts this initial minimal release will aim to build consensus and momentum around.

```java
/*
 * Simple agent for bank fraud detection.
 * Doesn't actually block a transaction but marks it suspect and sends notifications.
 */
// Infers agent type and name by default.
// Default scope is agent workflow, but agents can have application scope.
// Just a CDI bean and ideally @Agent is a CDI stereotype.
@Agent
public class FraudDetectionAgent {

    // Injects default LLM in the implementation, but can be configured to inject specific ones.
    @Inject private LargeLanguageModel model;
    @Inject private EntityManager entityManager;

    // Initiates the agent workflow. For this initial release, the workflow can only be triggered by
    // CDI events.
    // In the future, there could be many other types of triggers such as Jakarta Messaging or
    // direct invocation from a programmatic life cycle API.
    @Trigger
    // Return type can be void or a domain object stored in the workflow and accessible in
    // the context.
    // Parameters are automatically added to the workflow context.
    private void processTransaction(@Valid BankTransaction transaction) {
        // Simple check to see if this is a type of transaction that makes sense to check for
        // fraud detection.
        // Could add a bit more data, likely looked up from a database, and return an enhanced
        // version of the transaction or return another domain object entirely. 
    }

    // Can return boolean or a built-in result Record type. In this initial release, workflows
    // will automatically end with a negative result.
    // In subsequent releases, more robust decision flows should be possible, either with
    // annotations/EL and/or the programmatic workflow API.
    @Decision
    private Result checkFraud (BankTransaction transaction) {
        /*
         * One of the value propositions of the LLM facade is automatic type conversion in Java,
         * both for parameters and return types.
         *
         * If nothing is specified, it's all strings.
         * Probably only JSON and string are supported initially for conversion.
         * Queries can be parameterized similar to Jakarta Persistence.
         */
        String output = model.query(
            "Is this a fraudulent transaction? If so, how serious is it?", transaction);

        boolean fraud = isFraud(output); // Does some simple custom text parsing.
        Fraud details = null;

        if (fraud) {
            details = getFraudDetails(output); // Does some simple custom text parsing,
                                               // possibly involving database queries.
        }
 
        return new Result (fraud, details);
    }

    // Only one action here, but there could be multiple actions and/or decisions in sequence.
    // In the initial version, it's just one linear flow.
    // In subsequent releases, the workflow API can define complex flows, including
    // pre-conditions for actions defined via annotation/EL.
    @Action
    // Notice that we are automatically injecting domain objects from the workflow context.
    private void handleFraud (Fraud fraud, BankTransaction transaction) {
        /*
         * IMPORTANT FUNDAMENTAL CONCEPT:
         * This is an example of hard-coded logic, which would still be possible if desired.
         *
         * The power of a programmatic/structured workflow, instead, is that this could change
         * entirely at runtime, driven by further LLM queries.
         * Even for simple, static workflows, the API helps developers think through how agents
         * operate fundamentally - introducing a common vocabulary/patterns.
         *
         * Dynamically altered workflows could possibly be serialized into persistent storage.
         */
        if (fraud.isSerious()) {
            alertBankSecurity(fraud);
        }

        Customer customer = getCustomer(transaction);
        alertCustomer(fraud, transaction, customer);
    }

    // In this initial release, outcomes are essentially the same as actions, but specifically
    // mark the end of the workflow.
    // In subsequent releases, outcomes can do more powerful things such as pass a domain
    // object to a subsequent workflow or agent.
    // This is probably also where it best makes sense to dynamically alter a workflow using
    // a programmatic API.
    @Outcome
    private void processTransaction(BankTransaction transaction) {
        // Mark a transaction suspect, probably in the database.
    }
}
```

## Target Platform
* Jakarta EE 10 or higher
* Java SE 17 or higher

## Standalone Specification
The project will not initially seek inclusion into the Jakarta EE platform or any profile. Rather, the project will seek to provide a usable standalone API under the Jakarta EE umbrella that vendors may choose to adopt. In the future, it may make sense to define a Jakarta EE profile for AI in general to which this project could be added. Such a profile could conceivably also include separate specifications to attempt to standardize other important AI concepts, such as LLMs and model augmentation/context servers.

## Community
The project aims for the broadest industry consensus possible by engaging as many relevant subject matter experts and API consumers as possible, from within the Java/Jakarta EE ecosystem as well as externally.

Our mailing list is [agentic-ai-dev@eclipse.org](https://accounts.eclipse.org/mailing-list/agentic-ai-dev). You are also welcome to join the agentic-ai channel on the [Jakarta EE Development Slack](https://eclipsefoundationhq.slack.com/join/shared_invite/zt-crh7mheq-3on2tophEuvQTUGidEAWlg?u=eaf9e1f06f194eadc66788a85&id=98ae69e304&join=Join#/shared-invite/email).

## Frequently Asked Questions
### Why do I need this?
You don't necessarily. Whether you want to use this API is entirely up to you.

It is certainly possible to write script-like agent code that invokes LLMs and other AI capabilties to achieve autonomous or semi-autonomous business functionality. The problem is that for any reasonably complex agent, you'll probably end up with spaghetti code that's hard to maintain, especially for someone that isn't the original developer of the code. APIs like this one will provide your code long-term, predictable, and maintenable structure that fits the general patterns for AI agents. It may also help you think through your agent implementation, especially as a Jakarta EE developer.

The power of this API shines when you need to write agents that need to adapt their behaviour at runtime. You can do that using this API by keeping the fundamental building blocks of your domain logic simple and changing the workflow of those building blocks dynamically - possibly in response to LLM interactions.

### Why here?
The project aims to do for AI agent developers what Jakarta REST did for REST service developers, for example. Therefore, making it an official Jakarta specification makes perfect sense. The goal is also to ensure that agents implemented using the API work well with other Jakarta EE technologies and runtimes, such as CDI, etc.

### Is this an LLM API like Spring AI and LangChain4j?
This is not an LLM API. It is an API that will help you write better AI agents using Jakarta EE. In your agent code you very likely will be using LLMs. For that reason, we provide a very simple LLM facade. Implementations will likely use Spring AI and LangChain4j under the hood of that facade. The facade also let's you easily access Spring AI, LangChain4j, etc directly when you need it.
