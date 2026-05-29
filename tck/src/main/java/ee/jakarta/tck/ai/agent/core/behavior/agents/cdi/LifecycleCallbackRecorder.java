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

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class LifecycleCallbackRecorder {

    private final AtomicInteger postConstructCount = new AtomicInteger(0);
    private final AtomicInteger preDestroyCount    = new AtomicInteger(0);
    private final List<String>  instanceIds        = Collections.synchronizedList(new ArrayList<>());

    public void recordPostConstruct(String instanceId) {
        postConstructCount.incrementAndGet();
        instanceIds.add(instanceId);
    }

    public void recordPreDestroy() { preDestroyCount.incrementAndGet(); }

    public int          getPostConstructCount() { return postConstructCount.get(); }
    public int          getPreDestroyCount()    { return preDestroyCount.get(); }
    public List<String> getInstanceIds()        { return List.copyOf(instanceIds); }

    public void reset() {
        postConstructCount.set(0);
        preDestroyCount.set(0);
        instanceIds.clear();
    }
}
