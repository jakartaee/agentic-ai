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
package ee.jakarta.tck.ai.agent.framework.workflow;

/**
 * Associates the current thread with a workflow identifier for TCK fixtures.
 *
 * <p>Fixtures set a workflow id around a unit of work; the reference
 * {@link ee.jakarta.tck.ai.agent.framework.stub.LargeLanguageModelStub} reads
 * {@link #current()} so recorded calls and conversation history stay
 * isolated per workflow even when the stub is {@code @ApplicationScoped}.</p>
 */
public final class WorkflowContext {

    /**
     * Workflow id used when none has been set on the current thread.
     */
    public static final String DEFAULT = "default";

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private WorkflowContext() {
    }

    /**
     * Binds {@code workflowId} to the current thread.
     */
    public static void set(String workflowId) {
        CURRENT.set(workflowId);
    }

    /**
     * @return the workflow id bound to this thread, or {@link #DEFAULT} when unset
     */
    public static String current() {
        String id = CURRENT.get();
        return id != null ? id : DEFAULT;
    }

    /**
     * Clears the workflow id bound to this thread.
     */
    public static void clear() {
        CURRENT.remove();
    }

    /**
     * Runs {@code action} with {@code workflowId} bound, restoring the previous
     * binding (or clearing) afterward.
     */
    public static void run(String workflowId, Runnable action) {
        String previous = CURRENT.get();
        set(workflowId);
        try {
            action.run();
        } finally {
            if (previous != null) {
                set(previous);
            } else {
                clear();
            }
        }
    }
}
