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

import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmQuerierAgent;
import ee.jakarta.tck.ai.agent.core.behavior.agents.llm.LlmQuerierEvent;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import ee.jakarta.tck.ai.agent.framework.junit.anno.Deployed;
import ee.jakarta.tck.ai.agent.framework.junit.anno.RequiresImplementation;
import ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.ai.agent.LLMException;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.TestInstance;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Deployed
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class LlmContractTests {

    @Deployment
    public static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "llmcontract.war")
                .addClasses(LlmQuerierAgent.class, LlmQuerierEvent.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject LargeLanguageModel     llm;
    @Inject LargeLanguageModelStub stub;
    @Inject Event<LlmQuerierEvent> events;
    @Inject ExecutionTraceRecorder trace;

    public static record PersonFixture(String name, int age) {}

    @JsonbTypeSerializer(NonSerializableParam.Serializer.class)
    public static class NonSerializableParam {
        public static class Serializer implements JsonbSerializer<NonSerializableParam> {
            @Override
            public void serialize(NonSerializableParam obj, JsonGenerator generator, SerializationContext ctx) {
                throw new JsonbException("intentional serialization failure");
            }
        }
    }

    // -------------------------------------------------------------------------
    // A. Portable contract tests — assert against LargeLanguageModel interface
    // -------------------------------------------------------------------------

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on single-arg overload must throw IllegalArgumentException immediately")
    public void nullPromptThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.query(null));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on varargs overload must throw IllegalArgumentException immediately")
    public void nullPromptWithParamsThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.query(null, "x"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null resultType on typed overload must throw IllegalArgumentException immediately")
    public void nullResultTypeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.query("p", (Class<?>) null));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "more parameters than placeholders must throw IllegalArgumentException")
    public void arityMoreParamsThanPlaceholdersThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThrows(IllegalArgumentException.class, () -> llm.query("one {} here", "a", "b"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "fewer parameters than placeholders must throw IllegalArgumentException")
    public void arityFewerParamsThanPlaceholdersThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThrows(IllegalArgumentException.class, () -> llm.query("two {} and {}", "only"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "prompt with no placeholder receiving multiple params must throw IllegalArgumentException")
    public void noPlaceholderRejectsMultipleParams() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThrows(IllegalArgumentException.class, () -> llm.query("Analyze this", "a", "b"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "parameter that cannot be converted must throw IllegalArgumentException")
    public void parameterSerializationFailureThrowsIllegalArgumentException() {
        stub.reset();
        assertThrows(IllegalArgumentException.class, () -> llm.query("process {}", new NonSerializableParam()));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "parameter that cannot be converted on the typed varargs overload must throw IllegalArgumentException")
    public void parameterSerializationFailureOnTypedOverloadThrowsIllegalArgumentException() {
        stub.reset();
        assertThrows(IllegalArgumentException.class, () -> llm.query("process {}", PersonFixture.class, new NonSerializableParam()));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null prompt on the typed varargs overload must throw IllegalArgumentException immediately")
    public void nullPromptOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.query(null, PersonFixture.class, "x"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "null resultType on the typed varargs overload must throw IllegalArgumentException immediately")
    public void nullResultTypeOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.query("p", (Class<?>) null, "x"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "more parameters than placeholders on the typed varargs overload must throw IllegalArgumentException")
    public void arityMismatchOnTypedVarargsOverloadThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThrows(IllegalArgumentException.class, () -> llm.query("one {} here", PersonFixture.class, "a", "b"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "llm-integration",
               strategy = "unwrap to an incompatible type must throw IllegalArgumentException")
    public void unwrapToIncompatibleTypeThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> llm.unwrap(String.class));
    }

    // -------------------------------------------------------------------------
    // B. Reference-stub tests — scripted; validate impl + document the contract
    // -------------------------------------------------------------------------

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "llm-integration",
               strategy = "positional {} placeholders are substituted in declaration order via JSON-B")
    public void positionalPlaceholdersSubstitutedInOrderViaJsonB() {
        stub.reset();
        stub.enqueueResponse("ok");
        llm.query("Hello {} and {}", "Alice", "Bob");
        assertEquals("Hello \"Alice\" and \"Bob\"", stub.lastCall().effectivePrompt());
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "llm-integration",
               strategy = "look-alike tokens ({name}, { }) are not treated as positional placeholders")
    public void lookAlikeTokensAreNotPlaceholders() {
        stub.reset();
        stub.enqueueResponse("ok");
        // Explicit empty varargs array forces the varargs overload (3) so the substitution
        // implementation actually runs; the single-arg overload bypasses placeholder scanning entirely.
        assertDoesNotThrow(() -> llm.query("{name} and { } stay literal", new Object[0]));
        assertEquals("{name} and { } stay literal", stub.lastCall().effectivePrompt());
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "llm-integration",
               strategy = "prompt with no placeholder allows exactly one structured context parameter")
    public void noPlaceholderAllowsSingleStructuredContextParam() {
        stub.reset();
        stub.enqueueResponse("ok");
        PersonFixture context = new PersonFixture("A", 1);
        assertDoesNotThrow(() -> llm.query("Analyze this", context));
        // Prompt is unchanged (no placeholder to substitute) but the structured param is recorded.
        assertEquals("Analyze this", stub.lastCall().effectivePrompt());
        assertEquals(1, stub.lastCall().params().length);
        assertSame(context, stub.lastCall().params()[0]);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-003",
               section = "llm-integration",
               strategy = "complex object parameters are serialized into the prompt via Jakarta JSON Binding")
    public void complexObjectSerializedViaJsonBinding() {
        stub.reset();
        stub.enqueueResponse("ok");
        llm.query("Profile: {}", new PersonFixture("Alice", 30));
        assertTrue(stub.lastCall().effectivePrompt().contains("\"name\":\"Alice\""));
                assertTrue(stub.lastCall().effectivePrompt().contains("\"age\":30"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-004",
               section = "llm-integration",
               strategy = "typed response is deserialized from JSON string via Jakarta JSON Binding")
    public void typedResponseDeserializedViaJsonBinding() {
        stub.reset();
        stub.enqueueResponse("{\"name\":\"Bob\",\"age\":25}");
        PersonFixture p = llm.query("give person", PersonFixture.class);
        assertEquals("Bob", p.name());
        assertEquals(25, p.age());
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "LLM service error propagates as LLMException")
    public void serviceErrorPropagatesAsLlmException() {
        stub.reset();
        stub.failWith(new LLMException("service down"));
        assertThrows(LLMException.class, () -> llm.query("p"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "response type conversion failure propagates as LLMException")
    public void typeConversionFailurePropagatesAsLlmException() {
        stub.reset();
        stub.enqueueResponse(Integer.valueOf(42));
        assertThrows(LLMException.class, () -> llm.query("p", PersonFixture.class));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "llm-exception",
               strategy = "response type conversion failure on the typed varargs overload propagates as LLMException")
    public void typeConversionFailureOnTypedVarargsOverloadPropagatesAsLlmException() {
        stub.reset();
        stub.enqueueResponse(Integer.valueOf(42));
        assertThrows(LLMException.class, () -> llm.query("p", PersonFixture.class, "irrelevant"));
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "llm-integration",
               strategy = "unwrap to the concrete stub type returns the same instance")
    public void unwrapToCompatibleTypeReturnsImplementation() {
        LargeLanguageModelStub unwrapped = llm.unwrap(LargeLanguageModelStub.class);
        assertNotNull(unwrapped);
                assertInstanceOf(LargeLanguageModelStub.class, unwrapped);
    }

    // -------------------------------------------------------------------------
    // C. Disabled — requires compatible implementation / spec-open
    // -------------------------------------------------------------------------

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-LLM-BHV-007",
               section = "cdi-integration",
               strategy = "conversational state does not leak between sequential workflow executions "
                        + "(sequential firing is a v1 proxy for concurrency)")
    public void conversationalStateIsolatedBetweenWorkflows() {
        stub.reset();
        trace.reset();
        stub.enqueueResponse("first");
        stub.enqueueResponse("second");
        events.fire(new LlmQuerierEvent("workflow-1"));
        events.fire(new LlmQuerierEvent("workflow-2"));

        // Structural: each workflow ran its @Trigger + @Action and produced exactly one LLM call.
        // LlmQuerierAgent has both @Trigger and @Action; the compatible implementation dispatches both phases per workflow.
        assertEquals(List.of(Phase.TRIGGER, Phase.ACTION, Phase.TRIGGER, Phase.ACTION), trace.phases());
        assertEquals(2, stub.recordedCalls().size());
        assertEquals("turn for \"workflow-1\"", stub.recordedCalls().get(0).effectivePrompt());
        assertEquals("turn for \"workflow-2\"", stub.recordedCalls().get(1).effectivePrompt());

        // Isolation guarantee (BHV-007 proper): each event must materialize its own
        // @WorkflowScoped LLM context so the second call cannot observe the first
        // workflow's conversational state. Verifiable only once the compatible implementation exposes
        // per-workflow conversation history — this test currently asserts only the
        // structural precondition.
    }
}