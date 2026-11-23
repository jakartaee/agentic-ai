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

import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.Phase;
import ee.jakarta.tck.ai.agent.framework.ExecutionTraceRecorder.TraceEntry;
import ee.jakarta.tck.ai.agent.framework.TckModel;
import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
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
 * Calls are made from inside a workflow through {@link LlmProbeAgent}, where the
 * facade's full contract applies, except the one verifying that the facade is
 * usable outside a workflow context.
 */
@ExtendWith(ArquillianExtension.class)
public class LargeLanguageModelTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "largelanguagemodel.war")
                .addClasses(
                        LlmProbeAgent.class,
                        LlmProbeEvent.class,
                        LlmQuerierAgent.class,
                        LlmQuerierEvent.class,
                        LlmFailureHandlingAgent.class,
                        LlmFailureEvent.class,
                        PersonFixture.class,
                        NonSerializableParam.class,
                        NonSerializableParam.Serializer.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    Event<LlmProbeEvent> probes;

    @Inject
    Event<LlmQuerierEvent> queries;

    @Inject
    Event<LlmFailureEvent> failures;

    @Inject
    ExecutionTraceRecorder trace;

    @Inject
    LargeLanguageModel llm;

    /** A structured value that round-trips through Jakarta JSON Binding. */
    public record PersonFixture(String name, int age) {}

    /**
     * Carries one call to make against the facade from inside a workflow.
     *
     * @param probe the call; its return value, or the exception it throws, is
     *              recorded as the trigger's trace argument.
     */
    public record LlmProbeEvent(Function<LargeLanguageModel, Object> probe) {}

    /**
     * Applies a probe to the implementation's facade inside a workflow, where the
     * facade's full contract applies, and records the result or the exception for
     * the test to assert on.
     */
    @Agent
    @ApplicationScoped
    public static class LlmProbeAgent {

        @Inject
        LargeLanguageModel llm;

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onProbe(@Observes LlmProbeEvent event) {
            Object result;
            try {
                result = event.probe().apply(llm);
            } catch (RuntimeException e) {
                result = e;
            }
            trace.record(Phase.TRIGGER, "onProbe", result);
        }
    }

    private Object probe(Function<LargeLanguageModel, Object> call) {
        trace.reset();
        probes.fire(new LlmProbeEvent(call));
        return trace.entries().get(0).args()[0];
    }

    // -- validation: IllegalArgumentException before any model call ------------

    /**
     * Null prompt on query(String) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullPromptThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null)));
    }

    /**
     * Null prompt on query(String, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullPromptWithParamsThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, "x")));
    }

    /**
     * Null prompt on query(String, Class) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullPromptOnTypedOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, PersonFixture.class)));
    }

    /**
     * Null resultType on query(String, Class) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullResultTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null)));
    }

    /**
     * Null prompt on query(String, Class, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullPromptOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query(null, PersonFixture.class, "x")));
    }

    /**
     * Null resultType on query(String, Class, Object...) throws IllegalArgumentException.
     *
     * <p>Specification section {@code llm-interface}.
     */
    @Test
    public void nullResultTypeOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("p", (Class<?>) null, "x")));
    }

    /**
     * More parameters than {} placeholders throws IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void arityMoreParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("one {} here", "a", "b")));
    }

    /**
     * Fewer parameters than {} placeholders throws IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void arityFewerParamsThanPlaceholdersThrows() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("two {} and {}", "only")));
    }

    /**
     * More parameters than {} placeholders on the typed varargs overload throws
     * IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void arityMismatchOnTypedVarargsOverloadThrows() {
        assertInstanceOf(
                IllegalArgumentException.class, probe(llm -> llm.query("one {} here", PersonFixture.class, "a", "b")));
    }

    /**
     * A prompt with no placeholder given more than one parameter throws IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void noPlaceholderRejectsMultipleParams() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.query("Analyze this", "a", "b")));
    }

    /** A parameter whose JSON Binding serialization always fails. */
    @JsonbTypeSerializer(NonSerializableParam.Serializer.class)
    public static class NonSerializableParam {

        public static class Serializer implements JsonbSerializer<NonSerializableParam> {
            @Override
            public void serialize(NonSerializableParam obj, JsonGenerator generator, SerializationContext ctx) {
                throw new JsonbException("intentional serialization failure");
            }
        }
    }

    /**
     * A parameter JSON Binding cannot serialize throws IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void parameterSerializationFailureThrowsIllegalArgumentException() {
        assertInstanceOf(
                IllegalArgumentException.class, probe(llm -> llm.query("process {}", new NonSerializableParam())));
    }

    /**
     * A parameter JSON Binding cannot serialize on the typed varargs overload throws
     * IllegalArgumentException.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void parameterSerializationFailureOnTypedOverloadThrowsIllegalArgumentException() {
        assertInstanceOf(
                IllegalArgumentException.class,
                probe(llm -> llm.query("process {}", PersonFixture.class, new NonSerializableParam())));
    }

    /**
     * Unwrapping to a type the facade cannot be unwrapped to throws IllegalArgumentException.
     *
     * <p>Specification section {@code vendor-specific-access}.
     */
    @Test
    public void unwrapToIncompatibleTypeThrowsIllegalArgumentException() {
        assertInstanceOf(IllegalArgumentException.class, probe(llm -> llm.unwrap(String.class)));
    }

    /**
     * {@code unwrap(Object.class)} returns the underlying implementation.
     *
     * <p>Specification section {@code vendor-specific-access}.
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
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void positionalPlaceholdersSubstitutedInOrderViaJsonB() {
        assertEquals("Hello \"Alice\" and \"Bob\"", probe(llm -> llm.query("Hello {} and {}", "Alice", "Bob")));
    }

    /**
     * Look-alike tokens ({name}, { }) are literal prompt text, not placeholders.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void lookAlikeTokensAreNotPlaceholders() {
        assertEquals(
                "{name} and { } stay literal", probe(llm -> llm.query("{name} and { } stay literal", new Object[0])));
    }

    /**
     * A prompt with no placeholder accepts one structured-context parameter and is sent unchanged.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void noPlaceholderAllowsSingleStructuredContextParam() {
        assertEquals("Analyze this", probe(llm -> llm.query("Analyze this", new PersonFixture("A", 1))));
    }

    /**
     * A complex parameter is rendered into the prompt as its JSON Binding serialization.
     *
     * <p>Specification section {@code prompt-construction}.
     */
    @Test
    public void complexObjectSerializedViaJsonBinding() {
        String rendered =
                assertInstanceOf(String.class, probe(llm -> llm.query("Profile: {}", new PersonFixture("Alice", 30))));
        assertTrue(rendered.startsWith("Profile: "), rendered);
        assertTrue(rendered.contains("\"name\":\"Alice\""), rendered);
        assertTrue(rendered.contains("\"age\":30"), rendered);
    }

    /**
     * A typed response is deserialized from the model's JSON text with JSON Binding.
     *
     * <p>Specification section {@code typed-responses}.
     */
    @Test
    public void typedResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(
                PersonFixture.class, probe(llm -> llm.query("{\"name\":\"Bob\",\"age\":25}", PersonFixture.class)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    /**
     * The typed varargs overload renders the parameters and deserializes the model's JSON text.
     *
     * <p>Specification section {@code typed-responses}.
     */
    @Test
    public void typedVarargsResponseDeserializedViaJsonBinding() {
        PersonFixture p = assertInstanceOf(
                PersonFixture.class,
                probe(llm -> llm.query("{\"name\":{},\"age\":{}}", PersonFixture.class, "Bob", 25)));
        assertEquals(new PersonFixture("Bob", 25), p);
    }

    // -- failures: LLMException ------------------------------------------------

    /**
     * A model failure surfaces as LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query(TckModel.FAIL)));
    }

    /**
     * A model failure surfaces as LLMException on query(String, Class).
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnTypedOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query(TckModel.FAIL, PersonFixture.class)));
    }

    /**
     * A model failure surfaces as LLMException on query(String, Object...) when the rendered
     * prompt fails.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("{} fails", TckModel.FAIL)));
    }

    /**
     * A model failure surfaces as LLMException on query(String, Class, Object...) when the
     * rendered prompt fails.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void serviceErrorOnTypedVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("{} fails", PersonFixture.class, TckModel.FAIL)));
    }

    /**
     * A response that cannot be deserialized to the requested type surfaces as LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void typeConversionFailurePropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json", PersonFixture.class)));
    }

    /**
     * A response that cannot be deserialized on the typed varargs overload surfaces as
     * LLMException.
     *
     * <p>Specification section {@code llm-exception}.
     */
    @Test
    public void typeConversionFailureOnTypedVarargsOverloadPropagatesAsLlmException() {
        assertInstanceOf(LLMException.class, probe(llm -> llm.query("not json {}", PersonFixture.class, "x")));
    }

    public record LlmFailureEvent(String payload) {}

    @Agent
    @ApplicationScoped
    public static class LlmFailureHandlingAgent {

        @Inject
        LargeLanguageModel llm;

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes LlmFailureEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
        }

        @Action
        public void act(LlmFailureEvent event) {
            trace.record(Phase.ACTION, "act", event);
            llm.query(TckModel.FAIL);
        }

        @OnException
        public void onLlmFailure(LLMException ex) {
            trace.record(Phase.ON_EXCEPTION, "onLlmFailure", ex);
        }

        @Outcome
        public void finish() {
            trace.record(Phase.OUTCOME, "finish");
        }
    }

    /**
     * An LLMException thrown from a phase is routed to a matching {@code @OnException} method, and
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

    // -- use outside a workflow ------------------------------------------------

    /**
     * The facade can be used outside a workflow context. A call made directly from the test,
     * with no workflow running, returns the rendered prompt.
     *
     * <p>Specification section {@code llm-as-cdi-bean}.
     */
    @Test
    public void facadeIsUsableOutsideWorkflowContext() {
        assertEquals("Hello \"Alice\"", llm.query("Hello {}", "Alice"));
    }

    // -- conversational state --------------------------------------------------

    public record LlmQuerierEvent(String payload) {}

    /**
     * Queries the model in its trigger and reads the conversation back in its
     * action, so a test can see which turns the workflow's conversation holds.
     */
    @Agent
    @ApplicationScoped
    public static class LlmQuerierAgent {

        @Inject
        LargeLanguageModel llm;

        @Inject
        ExecutionTraceRecorder trace;

        @Trigger
        public void onEvent(@Observes LlmQuerierEvent event) {
            trace.record(Phase.TRIGGER, "onEvent", event);
            llm.query("turn for {}", event.payload());
        }

        @Action
        public void execute(LlmQuerierEvent event) {
            trace.record(Phase.ACTION, "execute", event, llm.query(TckModel.HISTORY));
        }
    }

    /**
     * Each workflow's conversation holds only its own turns. After two sequential workflows, the
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
        List<TraceEntry> actions = trace.entries(Phase.ACTION);
        assertEquals("turn for \"workflow-1\"", actions.get(0).args()[1]);
        assertEquals("turn for \"workflow-2\"", actions.get(1).args()[1]);
    }
}
