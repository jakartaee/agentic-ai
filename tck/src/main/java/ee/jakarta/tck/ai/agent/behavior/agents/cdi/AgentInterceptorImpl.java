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
package ee.jakarta.tck.ai.agent.behavior.agents.cdi;

// No @Priority: the interceptor is enabled through the <interceptors> element
// of the beans.xml in the CdiIntegrationTests deployment.
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
