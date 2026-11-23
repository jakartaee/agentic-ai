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

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records the construction and destruction of bean instances, so a test can check how many
 * instances a scope created, which ones, and which of them were destroyed.
 *
 * <p>A bean calls {@link #postConstruct(Class)} from its {@code @PostConstruct} method and keeps
 * the id it returns, and passes that id to {@link #preDestroy(Class, String)} from its
 * {@code @PreDestroy} method. Records are kept per bean class, so one bean's lifecycle never
 * shows up in another's, and {@link #reset(Class)} clears one class without touching the rest.
 */
@ApplicationScoped
public class LifecycleRecorder {

    private final AtomicInteger sequence = new AtomicInteger();

    private final Map<Class<?>, List<String>> constructed = new ConcurrentHashMap<>();

    private final Map<Class<?>, List<String>> destroyed = new ConcurrentHashMap<>();

    /** Records that an instance of the bean class was constructed and returns the instance's new id. */
    public String postConstruct(Class<?> beanClass) {
        String id = beanClass.getSimpleName() + "#" + sequence.incrementAndGet();
        records(constructed, beanClass).add(id);
        return id;
    }

    /** Records that the instance of the bean class with the given id was destroyed. */
    public void preDestroy(Class<?> beanClass, String id) {
        records(destroyed, beanClass).add(id);
    }

    /** The ids of the constructed instances of the bean class, in the order they were constructed. */
    public List<String> constructed(Class<?> beanClass) {
        return List.copyOf(records(constructed, beanClass));
    }

    /** The ids of the destroyed instances of the bean class, in the order they were destroyed. */
    public List<String> destroyed(Class<?> beanClass) {
        return List.copyOf(records(destroyed, beanClass));
    }

    /** Clears the records of the bean class. */
    public void reset(Class<?> beanClass) {
        constructed.remove(beanClass);
        destroyed.remove(beanClass);
    }

    private static List<String> records(Map<Class<?>, List<String>> records, Class<?> beanClass) {
        return records.computeIfAbsent(beanClass, type -> new CopyOnWriteArrayList<>());
    }
}
