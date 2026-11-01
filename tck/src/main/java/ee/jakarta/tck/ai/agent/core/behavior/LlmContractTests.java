/*****************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/
package ee.jakarta.tck.ai.agent.core.behavior;

import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmFailureEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmFailureHandlingAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmProbeAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmProbeEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmQuerierAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmQuerierEvent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.NonSerializableParam;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.PersonFixture;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.llm.TckModel;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@link LargeLanguageModel} facade contract, verified against the
 * implementation's facade backed by the {@link TckModel} echo behaviour.
 * Every call is made from inside a workflow through {@link LlmProbeAgent},
 * because the facade and its conversational state are defined only there.
 */
@Deployed
public class LlmContractTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "llmcontract.war")
                .addClasses(LlmProbeAgent.class, LlmProbeEvent.class,
                            LlmQuerierAgent.class, LlmQuerierEvent.class,
                            LlmFailureHandlingAgent.class, LlmFailureEvent.class,
                            PersonFixture.class, NonSerializableParam.class, NonSerializableParam.Serializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject Event<LlmProbeEvent>   probes;
    @Inject Event<LlmQuerierEvent> queries;
    @Inject Event<LlmFailureEvent> failures;
    @Inject ExecutionTraceRecorder trace;

    private Object probe(Function<LargeLanguageModel, Object> call) {
        trace.reset();
        probes.fire(new LlmProbeEvent(call));
        return trace.entries().get(0).args()[0];
    }

    // -- validation: IllegalArgumentException before any model call ----------

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on query(String) throws IllegalArgumentException")
    public void nullPromptThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null)));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on query(String, Object...) throws IllegalArgumentException")
    public void nullPromptWithParamsThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, "x")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null resultType on query(String, Class) throws IllegalArgumentException")
    public void nullResultTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null)));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on query(String, Class, Object...) throws IllegalArgumentException")
    public void nullPromptOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, PersonFixture.class, "x")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null resultType on query(String, Class, Object...) throws IllegalArgumentException")
    public void nullResultTypeOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null, "x")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "more parameters than {} placeholders throws IllegalArgumentException")
    public void arityMoreParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("one {} here", "a", "b")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "fewer parameters than {} placeholders throws IllegalArgumentException")
    public void arityFewerParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("two {} and {}", "only")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "more parameters than {} placeholders on the typed varargs overload throws IllegalArgumentException")
    public void arityMismatchOnTypedVarargsOverloadThrows() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("one {} here", PersonFixture.class, "a", "b")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "a prompt with no placeholder given more than one parameter throws IllegalArgumentException")
    public void noPlaceholderRejectsMultipleParams() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("Analyze this", "a", "b")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "a parameter JSON Binding cannot serialize throws IllegalArgumentException")
    public void parameterSerializationFailureThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("process {}", new NonSerializableParam())));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "a parameter JSON Binding cannot serialize on the typed varargs overload throws IllegalArgumentException")
    public void parameterSerializationFailureOnTypedOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("process {}", PersonFixture.class, new NonSerializableParam())));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "llm-integration",
               strategy = "unwrap to a type the facade cannot be unwrapped to throws IllegalArgumentException")
    public void unwrapToIncompatibleTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.unwrap(String.class)));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "llm-integration",
               strategy = "unwrap(Object.class) returns the underlying implementation")
    public void unwrapToCompatibleTypeReturnsImplementation() {
        assertNotNull(probe(llm -> llm.unwrap(Object.class)));
    }

    // -- rendering: the echoed response is the rendered prompt -----------------

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "llm-integration",
               strategy = "{} placeholders are substituted in declaration order with the JSON Binding form of each parameter; "
                       + "the echo model returns the rendered prompt")
    public void positionalPlaceholdersSubstitutedInOrderViaJsonB() {
        assertEquals("Hello \"Alice\" and \"Bob\"", probe(llm -> llm.query("Hello {} and {}", "Alice", "Bob")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "llm-integration",
               strategy = "look-alike tokens ({name}, { }) are literal prompt text, not placeholders")
    public void lookAlikeTokensAreNotPlaceholders() {
        assertEquals("{name} and { } stay literal",
                probe(llm -> llm.query("{name} and { } stay literal", new Object[0])));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "a prompt with no placeholder accepts one structured-context parameter and is sent unchanged")
    public void noPlaceholderAllowsSingleStructuredContextParam() {
        assertEquals("Analyze this", probe(llm -> llm.query("Analyze this", new PersonFixture("A", 1))));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-003",
               section = "llm-integration",
               strategy = "a complex parameter is rendered into the prompt as its JSON Binding serialization")
    public void complexObjectSerializedViaJsonBinding() {
        String rendered = assertInstanceOf(String.class,
                probe(llm -> llm.query("Profile: {}", new PersonFixture("Alice", 30))));
        assertTrue(rendered.startsWith("Profile: "), rendered);
        assertTrue(rendered.contains("\"name\":\"Alice\""), rendered);
        assertTrue(rendered.contains("\"age\":30"), rendered);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-004",
               section = "llm-integration",
               strategy = "a typed response is deserialized from the model's JSON text with JSON Binding")
    public void typedResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(PersonFixture.class,
                probe(llm -> llm.query("{\"name\":\"Bob\",\"age\":25}", PersonFixture.class)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-004",
               section = "llm-integration",
               strategy = "the typed varargs overload renders the parameters and deserializes the model's JSON text")
    public void typedVarargsResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(PersonFixture.class,
                probe(llm -> llm.query("{\"name\":{},\"age\":{}}", PersonFixture.class, "Bob", 25)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    // -- failures: LLMException ----------------------------------------------

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "a model failure surfaces as LLMException")
    public void serviceErrorPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query(TckModel.FAIL)));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "a response that cannot be deserialized to the requested type surfaces as LLMException")
    public void typeConversionFailurePropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json", PersonFixture.class)));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "a response that cannot be deserialized on the typed varargs overload surfaces as LLMException")
    public void typeConversionFailureOnTypedVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json {}", PersonFixture.class, "x")));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-008",
               section = "llm-exception",
               strategy = "an LLMException thrown from a phase is routed to a matching @OnException method, "
                       + "and the workflow continues to @Outcome when the handler returns normally")
    public void llmExceptionIsHandledByOnException() {
        trace.reset();
        failures.fire(new LlmFailureEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.HANDLE_EXCEPTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(LLMException.class, trace.entries().get(2).args()[0]);
    }

    // -- conversational state --------------------------------------------------
    @Assertion(id = "AGENTICAI-LLM-BHV-007",
               section = "llm-conversational-state",
               strategy = "each workflow's conversation holds only its own turns: after two sequential workflows, "
                       + "the history the model reports to the second contains only the second's turn")
    public void conversationalStateIsolatedBetweenWorkflows() {
        trace.reset();
        queries.fire(new LlmQuerierEvent("workflow-1"));
        queries.fire(new LlmQuerierEvent("workflow-2"));

        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.TRIGGER, Phase.ACTION), trace.phases());
        List<TraceEntry> actions = trace.entries().stream().filter(e -> e.phase() == Phase.ACTION).toList();
        assertEquals("turn for \"workflow-1\"", actions.get(0).args()[1]);
        assertEquals("turn for \"workflow-2\"", actions.get(1).args()[1]);
    }
}
