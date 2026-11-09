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
package ee.jakarta.tck.ai.agent;

import ee.jakarta.tck.ai.agent.agents.llm.LlmFailureEvent;
import ee.jakarta.tck.ai.agent.agents.llm.LlmFailureHandlingAgent;
import ee.jakarta.tck.ai.agent.agents.llm.LlmProbeAgent;
import ee.jakarta.tck.ai.agent.agents.llm.LlmProbeEvent;
import ee.jakarta.tck.ai.agent.agents.llm.LlmQuerierAgent;
import ee.jakarta.tck.ai.agent.agents.llm.LlmQuerierEvent;
import ee.jakarta.tck.ai.agent.agents.llm.NonSerializableParam;
import ee.jakarta.tck.ai.agent.agents.llm.PersonFixture;
import ee.jakarta.tck.ai.agent.framework.TckModel;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(ArquillianExtension.class)
public class LargeLanguageModelTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "largelanguagemodel.war")
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

    /**
     * null prompt on query(String) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullPromptThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null)));
    }

    /**
     * null prompt on query(String, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullPromptWithParamsThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, "x")));
    }

    /**
     * null prompt on query(String, Class) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullPromptOnTypedOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, PersonFixture.class)));
    }

    /**
     * null resultType on query(String, Class) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullResultTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null)));
    }

    /**
     * null prompt on query(String, Class, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullPromptOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, PersonFixture.class, "x")));
    }

    /**
     * null resultType on query(String, Class, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void nullResultTypeOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null, "x")));
    }

    /**
     * more parameters than {} placeholders throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void arityMoreParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("one {} here", "a", "b")));
    }

    /**
     * fewer parameters than {} placeholders throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void arityFewerParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("two {} and {}", "only")));
    }

    /**
     * more parameters than {} placeholders on the typed varargs overload throws
     * IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void arityMismatchOnTypedVarargsOverloadThrows() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("one {} here", PersonFixture.class, "a", "b")));
    }

    /**
     * a prompt with no placeholder given more than one parameter throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void noPlaceholderRejectsMultipleParams() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("Analyze this", "a", "b")));
    }

    /**
     * a parameter JSON Binding cannot serialize throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void parameterSerializationFailureThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("process {}", new NonSerializableParam())));
    }

    /**
     * a parameter JSON Binding cannot serialize on the typed varargs overload throws
     * IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void parameterSerializationFailureOnTypedOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class,
                probe(llm -> llm.query("process {}", PersonFixture.class, new NonSerializableParam())));
    }

    /**
     * unwrap to a type the facade cannot be unwrapped to throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void unwrapToIncompatibleTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.unwrap(String.class)));
    }

    /**
     * unwrap(Object.class) returns the underlying implementation.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void unwrapToCompatibleTypeReturnsImplementation() {
        assertNotNull(probe(llm -> llm.unwrap(Object.class)));
    }

    // -- rendering: the echoed response is the rendered prompt -----------------

    /**
     * {} placeholders are substituted in declaration order with the JSON Binding form of each
     * parameter; the echo model returns the rendered prompt.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void positionalPlaceholdersSubstitutedInOrderViaJsonB() {
        assertEquals("Hello \"Alice\" and \"Bob\"", probe(llm -> llm.query("Hello {} and {}", "Alice", "Bob")));
    }

    /**
     * look-alike tokens ({name}, { }) are literal prompt text, not placeholders.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void lookAlikeTokensAreNotPlaceholders() {
        assertEquals("{name} and { } stay literal",
                probe(llm -> llm.query("{name} and { } stay literal", new Object[0])));
    }

    /**
     * a prompt with no placeholder accepts one structured-context parameter and is sent unchanged.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void noPlaceholderAllowsSingleStructuredContextParam() {
        assertEquals("Analyze this", probe(llm -> llm.query("Analyze this", new PersonFixture("A", 1))));
    }

    /**
     * a complex parameter is rendered into the prompt as its JSON Binding serialization.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void complexObjectSerializedViaJsonBinding() {
        String rendered = assertInstanceOf(String.class,
                probe(llm -> llm.query("Profile: {}", new PersonFixture("Alice", 30))));
        assertTrue(rendered.startsWith("Profile: "), rendered);
        assertTrue(rendered.contains("\"name\":\"Alice\""), rendered);
        assertTrue(rendered.contains("\"age\":30"), rendered);
    }

    /**
     * a typed response is deserialized from the model's JSON text with JSON Binding.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void typedResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(PersonFixture.class,
                probe(llm -> llm.query("{\"name\":\"Bob\",\"age\":25}", PersonFixture.class)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    /**
     * the typed varargs overload renders the parameters and deserializes the model's JSON text.
     *
     * <p>Specification section {@code llm-integration}.
     */
    @Test
    public void typedVarargsResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(PersonFixture.class,
                probe(llm -> llm.query("{\"name\":{},\"age\":{}}", PersonFixture.class, "Bob", 25)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    // -- failures: LLMException ----------------------------------------------

    /**
     * a model failure surfaces as LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query(TckModel.FAIL)));
    }

    /**
     * a model failure surfaces as LLMException on query(String, Class).
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnTypedOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query(TckModel.FAIL, PersonFixture.class)));
    }

    /**
     * a model failure surfaces as LLMException on query(String, Object...) when the rendered
     * prompt fails.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("{} fails", TckModel.FAIL)));
    }

    /**
     * a model failure surfaces as LLMException on query(String, Class, Object...) when the
     * rendered prompt fails.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnTypedVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("{} fails", PersonFixture.class, TckModel.FAIL)));
    }

    /**
     * a response that cannot be deserialized to the requested type surfaces as LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void typeConversionFailurePropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json", PersonFixture.class)));
    }

    /**
     * a response that cannot be deserialized on the typed varargs overload surfaces as
     * LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void typeConversionFailureOnTypedVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json {}", PersonFixture.class, "x")));
    }

    /**
     * an LLMException thrown from a phase is routed to a matching {@code @OnException} method, and
     * the workflow continues to {@code @Outcome} when the handler returns normally.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void llmExceptionIsHandledByOnException() {
        trace.reset();
        failures.fire(new LlmFailureEvent("x"));
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.ON_EXCEPTION, Phase.OUTCOME), trace.phases());
        assertInstanceOf(LLMException.class, trace.entries().get(2).args()[0]);
    }

    // -- conversational state --------------------------------------------------
    /**
     * each workflow's conversation holds only its own turns. After two sequential workflows, the
     * history the model reports to the second contains only the second's turn.
     *
     * <p>Specification section {@code llm-conversational-state}.
     */
    @Test
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
