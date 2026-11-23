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
package ee.jakarta.tck.ai.agent.framework;

/**
 * Searches the cause chain of a thrown exception. An implementation may wrap an exception from
 * an agent in exceptions of its own before it reaches the test, so tests look for the exception
 * they expect anywhere in the chain rather than at its root or top.
 */
public final class Causes {

    private Causes() {}

    /**
     * The first exception in the cause chain of {@code thrown}, starting with {@code thrown}
     * itself, that is an instance of any of the given types.
     *
     * @return the exception, or {@code null} if the chain contains none of the types
     */
    @SafeVarargs
    public static Throwable first(Throwable thrown, Class<? extends Throwable>... types) {
        for (Throwable cause = thrown; cause != null; cause = cause.getCause()) {
            for (Class<? extends Throwable> type : types) {
                if (type.isInstance(cause)) {
                    return cause;
                }
            }
        }
        return null;
    }
}
