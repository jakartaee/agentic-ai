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
package ee.jakarta.tck.ai.agent.framework.llm;

/**
 * The behaviour of the model behind the implementation's
 * {@code LargeLanguageModel} during a TCK run.
 *
 * <p>The TCK deploys no {@code LargeLanguageModel} bean. The facade injected
 * into the test agents is the implementation's own, and the TCK verifies the
 * facade contract — placeholder substitution, JSON Binding conversion,
 * exception mapping and per-workflow conversational state — by observing
 * what comes back from it. That is only possible if the model underneath
 * behaves deterministically, so an implementation under test must be
 * configured so that its facade is backed by a model with exactly this
 * behaviour. How that configuration is done is implementation-specific.</p>
 *
 * <p>In what follows, the <em>rendered prompt</em> is the text the facade
 * sends to the model: for {@code query(String)} and
 * {@code query(String, Class)} it is the prompt as given; for the varargs
 * overloads it is the prompt with each {@code {}} placeholder replaced, in
 * order, by the Jakarta JSON Binding serialization of the corresponding
 * parameter, and a prompt with no placeholder and a single structured-context
 * parameter is left unchanged.</p>
 *
 * <ol>
 * <li>If the rendered prompt contains {@link #FAIL}, the model fails. The
 *     facade must surface that failure as an {@code LLMException}.</li>
 * <li>If the rendered prompt equals {@link #HISTORY}, the response is the
 *     rendered prompts previously sent in the current conversation — the
 *     conversation of the current workflow context — in order, one per line,
 *     joined with {@code "\n"}; the empty string when there are none. History
 *     queries are not themselves recorded in the conversation.</li>
 * <li>Otherwise the response is the rendered prompt, verbatim. A typed
 *     overload therefore returns the echoed text deserialized with JSON
 *     Binding, which is how the TCK drives typed responses. It sends JSON and
 *     expects the object back.</li>
 * </ol>
 */
public final class TckModel {

    /** A rendered prompt containing this token fails at the model. */
    public static final String FAIL = "[tck:fail]";

    /** A rendered prompt equal to this token returns the conversation so far. */
    public static final String HISTORY = "[tck:history]";

    private TckModel() {
    }
}
