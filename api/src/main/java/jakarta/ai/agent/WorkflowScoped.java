package jakarta.ai.agent;

import jakarta.enterprise.context.NormalScope;
import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines a workflow-scoped CDI context.
 * <p>
 * A workflow scope is active during the execution of an agent's workflow,
 * spanning trigger, decision, action, and outcome phases. Beans in this scope
 * are created when the workflow starts and destroyed when it completes.
 * <p>
 * This scope is particularly useful for sharing state across different phases
 * of a workflow execution without requiring explicit parameter passing.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * @Agent
 * @WorkflowScoped
 * public class DocumentationAgent {
 *     // Agent is workflow-scoped
 * }
 *
 * @WorkflowScoped
 * public class AnalysisCache {
 *     // Cache exists for the duration of the workflow
 *     private Map<String, Object> cache = new HashMap<>();
 * }
 * }</pre>
 */
@Target({ TYPE, METHOD, FIELD })
@Retention(RUNTIME)
@Documented
@NormalScope
@Inherited
public @interface WorkflowScoped {

    /**
     * Supports inline instantiation of the WorkflowScoped annotation.
     */
    public final static class Literal extends AnnotationLiteral<WorkflowScoped> implements WorkflowScoped {
        /** Default WorkflowScoped literal */
        public static final Literal INSTANCE = new Literal();

        private static final long serialVersionUID = 1L;

    }

}
