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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
               section = "LLM Interface, Input Integrity",
               strategy = "null prompt on single-arg overload must throw IllegalArgumentException immediately")
    public void nullPromptThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.query(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Input Integrity",
               strategy = "null prompt on varargs overload must throw IllegalArgumentException immediately")
    public void nullPromptWithParamsThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.query(null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Input Integrity",
               strategy = "null resultType on typed overload must throw IllegalArgumentException immediately")
    public void nullResultTypeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.query("p", (Class<?>) null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "more parameters than placeholders must throw IllegalArgumentException")
    public void arityMoreParamsThanPlaceholdersThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThatThrownBy(() -> llm.query("one {} here", "a", "b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "fewer parameters than placeholders must throw IllegalArgumentException")
    public void arityFewerParamsThanPlaceholdersThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThatThrownBy(() -> llm.query("two {} and {}", "only"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "prompt with no placeholder receiving multiple params must throw IllegalArgumentException")
    public void noPlaceholderRejectsMultipleParams() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThatThrownBy(() -> llm.query("Analyze this", "a", "b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "parameter that cannot be converted must throw IllegalArgumentException")
    public void parameterSerializationFailureThrowsIllegalArgumentException() {
        stub.reset();
        assertThatThrownBy(() -> llm.query("process {}", new NonSerializableParam()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "parameter that cannot be converted on the typed varargs overload must throw IllegalArgumentException")
    public void parameterSerializationFailureOnTypedOverloadThrowsIllegalArgumentException() {
        stub.reset();
        assertThatThrownBy(() -> llm.query("process {}", PersonFixture.class, new NonSerializableParam()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Input Integrity",
               strategy = "null prompt on the typed varargs overload must throw IllegalArgumentException immediately")
    public void nullPromptOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.query(null, PersonFixture.class, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Input Integrity",
               strategy = "null resultType on the typed varargs overload must throw IllegalArgumentException immediately")
    public void nullResultTypeOnTypedVarargsOverloadThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.query("p", (Class<?>) null, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "more parameters than placeholders on the typed varargs overload must throw IllegalArgumentException")
    public void arityMismatchOnTypedVarargsOverloadThrows() {
        stub.reset();
        stub.enqueueResponse("ok");
        assertThatThrownBy(() -> llm.query("one {} here", PersonFixture.class, "a", "b"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "LLM Interface, Provider Abstraction",
               strategy = "unwrap to an incompatible type must throw IllegalArgumentException")
    public void unwrapToIncompatibleTypeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> llm.unwrap(String.class))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // -------------------------------------------------------------------------
    // B. Reference-stub tests — scripted; validate impl + document the contract
    // -------------------------------------------------------------------------

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "API Overview, LLM Interface",
               strategy = "positional {} placeholders are substituted in declaration order via JSON-B")
    public void positionalPlaceholdersSubstitutedInOrderViaJsonB() {
        stub.reset();
        stub.enqueueResponse("ok");
        llm.query("Hello {} and {}", "Alice", "Bob");
        assertThat(stub.lastCall().effectivePrompt()).isEqualTo("Hello \"Alice\" and \"Bob\"");
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-001",
               section = "API Overview, LLM Interface",
               strategy = "look-alike tokens ({name}, { }) are not treated as positional placeholders")
    public void lookAlikeTokensAreNotPlaceholders() {
        stub.reset();
        stub.enqueueResponse("ok");
        // Explicit empty varargs array forces the varargs overload (3) so the substitution
        // implementation actually runs; the single-arg overload bypasses placeholder scanning entirely.
        assertThatCode(() -> llm.query("{name} and { } stay literal", new Object[0]))
                .doesNotThrowAnyException();
        assertThat(stub.lastCall().effectivePrompt()).isEqualTo("{name} and { } stay literal");
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-002",
               section = "LLM Interface, Positional Parameters",
               strategy = "prompt with no placeholder allows exactly one structured context parameter")
    public void noPlaceholderAllowsSingleStructuredContextParam() {
        stub.reset();
        stub.enqueueResponse("ok");
        PersonFixture context = new PersonFixture("A", 1);
        assertThatCode(() -> llm.query("Analyze this", context))
                .doesNotThrowAnyException();
        // Prompt is unchanged (no placeholder to substitute) but the structured param is recorded.
        assertThat(stub.lastCall().effectivePrompt()).isEqualTo("Analyze this");
        assertThat(stub.lastCall().params()).hasSize(1);
        assertThat(stub.lastCall().params()[0]).isSameAs(context);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-003",
               section = "Architecture, JSON-B",
               strategy = "complex object parameters are serialized into the prompt via Jakarta JSON Binding")
    public void complexObjectSerializedViaJsonBinding() {
        stub.reset();
        stub.enqueueResponse("ok");
        llm.query("Profile: {}", new PersonFixture("Alice", 30));
        assertThat(stub.lastCall().effectivePrompt())
                .contains("\"name\":\"Alice\"")
                .contains("\"age\":30");
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-004",
               section = "Architecture, JSON-B",
               strategy = "typed response is deserialized from JSON string via Jakarta JSON Binding")
    public void typedResponseDeserializedViaJsonBinding() {
        stub.reset();
        stub.enqueueResponse("{\"name\":\"Bob\",\"age\":25}");
        PersonFixture p = llm.query("give person", PersonFixture.class);
        assertThat(p.name()).isEqualTo("Bob");
        assertThat(p.age()).isEqualTo(25);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "LLM Interface, Error Semantics",
               strategy = "LLM service error propagates as LLMException")
    public void serviceErrorPropagatesAsLlmException() {
        stub.reset();
        stub.failWith(new LLMException("service down"));
        assertThatThrownBy(() -> llm.query("p"))
                .isInstanceOf(LLMException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "LLM Interface, Error Semantics",
               strategy = "response type conversion failure propagates as LLMException")
    public void typeConversionFailurePropagatesAsLlmException() {
        stub.reset();
        stub.enqueueResponse(Integer.valueOf(42));
        assertThatThrownBy(() -> llm.query("p", PersonFixture.class))
                .isInstanceOf(LLMException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-005",
               section = "LLM Interface, Error Semantics",
               strategy = "response type conversion failure on the typed varargs overload propagates as LLMException")
    public void typeConversionFailureOnTypedVarargsOverloadPropagatesAsLlmException() {
        stub.reset();
        stub.enqueueResponse(Integer.valueOf(42));
        assertThatThrownBy(() -> llm.query("p", PersonFixture.class, "irrelevant"))
                .isInstanceOf(LLMException.class);
    }

    @Assertion(id = "AGENTICAI-LLM-BHV-006",
               section = "LLM Interface, Provider Abstraction",
               strategy = "unwrap to the concrete stub type returns the same instance")
    public void unwrapToCompatibleTypeReturnsImplementation() {
        LargeLanguageModelStub unwrapped = llm.unwrap(LargeLanguageModelStub.class);
        assertThat(unwrapped).isNotNull().isInstanceOf(LargeLanguageModelStub.class);
    }

    // -------------------------------------------------------------------------
    // C. Disabled — requires compatible implementation / spec-open
    // -------------------------------------------------------------------------

    @RequiresImplementation
    @Assertion(id = "AGENTICAI-LLM-BHV-007",
               section = "Architecture, Concurrency",
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
        assertThat(trace.phases()).containsExactly(Phase.TRIGGER, Phase.ACTION, Phase.TRIGGER, Phase.ACTION);
        assertThat(stub.recordedCalls()).hasSize(2);
        assertThat(stub.recordedCalls().get(0).effectivePrompt()).isEqualTo("turn for \"workflow-1\"");
        assertThat(stub.recordedCalls().get(1).effectivePrompt()).isEqualTo("turn for \"workflow-2\"");

        // Isolation guarantee (BHV-007 proper): each event must materialize its own
        // @WorkflowScoped LLM context so the second call cannot observe the first
        // workflow's conversational state. Verifiable only once the compatible implementation exposes
        // per-workflow conversation history — this test currently asserts only the
        // structural precondition.
    }
}