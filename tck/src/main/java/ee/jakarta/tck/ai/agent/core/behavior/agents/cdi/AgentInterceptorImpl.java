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
package ee.jakarta.tck.ai.agent.core.behavior.agents.cdi;

// NOTE: NO @Priority here — the interceptor is enabled via the <interceptors>
// element of beans.xml (see CdiIntegrationTests deployment). Enabling it through
// both @Priority and beans.xml would enable it twice (non-portable; Weld may reject).
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder;
import ee.jakarta.tck.ai.agent.framework.trace.ExecutionTraceRecorder.Phase;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@AgentInterceptorBinding
@Interceptor
public class AgentInterceptorImpl {

    @Inject ExecutionTraceRecorder trace;

    @AroundInvoke
    public Object intercept(InvocationContext ctx) throws Exception {
        trace.record(Phase.ACTION, "interceptorBefore", ctx.getMethod().getName());
        Object result = ctx.proceed();
        trace.record(Phase.ACTION, "interceptorAfter", ctx.getMethod().getName());
        return result;
    }
}
