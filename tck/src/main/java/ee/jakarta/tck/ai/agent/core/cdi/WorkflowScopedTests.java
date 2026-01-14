/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package ee.jakarta.tck.ai.agent.core.cdi;

import ee.jakarta.tck.ai.agent.framework.junit.anno.Assertion;
import jakarta.ai.agent.WorkflowScoped;
import jakarta.enterprise.context.NormalScope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TCK tests for the {@link WorkflowScoped} CDI scope annotation.
 *
 * <p>These tests verify that the @WorkflowScoped annotation conforms to the
 * Jakarta Agentic AI 1.0 specification requirements for custom CDI scopes.
 */
public class WorkflowScopedTests {

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-001",
               strategy = "Verify @WorkflowScoped annotation exists in the jakarta.ai.agent package")
    public void testWorkflowScopedAnnotationExists() {
        assertNotNull(WorkflowScoped.class,
                "@WorkflowScoped annotation must exist in jakarta.ai.agent package");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-002",
               strategy = "Verify @WorkflowScoped annotation has RUNTIME retention policy")
    public void testWorkflowScopedAnnotationRetention() {
        Retention retention = WorkflowScoped.class.getAnnotation(Retention.class);
        assertNotNull(retention,
                "@WorkflowScoped must have @Retention annotation");
        assertEquals(RetentionPolicy.RUNTIME, retention.value(),
                "@WorkflowScoped must have RUNTIME retention policy");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-003",
               strategy = "Verify @WorkflowScoped annotation targets TYPE, METHOD, and FIELD elements")
    public void testWorkflowScopedAnnotationTarget() {
        Target target = WorkflowScoped.class.getAnnotation(Target.class);
        assertNotNull(target,
                "@WorkflowScoped must have @Target annotation");

        ElementType[] targets = target.value();
        assertTrue(Arrays.asList(targets).contains(ElementType.TYPE),
                "@WorkflowScoped must target TYPE elements");
        assertTrue(Arrays.asList(targets).contains(ElementType.METHOD),
                "@WorkflowScoped must target METHOD elements");
        assertTrue(Arrays.asList(targets).contains(ElementType.FIELD),
                "@WorkflowScoped must target FIELD elements");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-004",
               strategy = "Verify @WorkflowScoped is annotated with @NormalScope")
    public void testWorkflowScopedIsNormalScope() {
        NormalScope normalScope = WorkflowScoped.class.getAnnotation(NormalScope.class);
        assertNotNull(normalScope,
                "@WorkflowScoped must be annotated with @NormalScope");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-005",
               strategy = "Verify @WorkflowScoped is annotated with @Documented")
    public void testWorkflowScopedIsDocumented() {
        Documented documented = WorkflowScoped.class.getAnnotation(Documented.class);
        assertNotNull(documented,
                "@WorkflowScoped must be annotated with @Documented");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-006",
               strategy = "Verify @WorkflowScoped is annotated with @Inherited")
    public void testWorkflowScopedIsInherited() {
        Inherited inherited = WorkflowScoped.class.getAnnotation(Inherited.class);
        assertNotNull(inherited,
                "@WorkflowScoped must be annotated with @Inherited");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-007",
               strategy = "Verify @WorkflowScoped annotation is an annotation type")
    public void testWorkflowScopedIsAnnotationType() {
        assertTrue(WorkflowScoped.class.isAnnotation(),
                "@WorkflowScoped must be an annotation type");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-008",
               strategy = "Verify @WorkflowScoped.Literal class exists and is accessible")
    public void testWorkflowScopedLiteralExists() {
        assertNotNull(WorkflowScoped.Literal.class,
                "WorkflowScoped.Literal class must exist");
        assertNotNull(WorkflowScoped.Literal.INSTANCE,
                "WorkflowScoped.Literal.INSTANCE must exist");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-009",
               strategy = "Verify @WorkflowScoped.Literal implements WorkflowScoped")
    public void testWorkflowScopedLiteralImplementsAnnotation() {
        assertTrue(WorkflowScoped.class.isAssignableFrom(WorkflowScoped.Literal.class),
                "WorkflowScoped.Literal must implement WorkflowScoped");
    }

    @Assertion(id = "AGENTICAI-WORKFLOWSCOPED-010",
               strategy = "Verify @WorkflowScoped can be applied to a class")
    public void testWorkflowScopedCanBeAppliedToClass() {
        @WorkflowScoped
        class TestBean {}

        WorkflowScoped annotation = TestBean.class.getAnnotation(WorkflowScoped.class);
        assertNotNull(annotation,
                "@WorkflowScoped annotation must be retrievable from annotated class");
    }
}
