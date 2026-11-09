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
package ee.jakarta.examples.ai.agent.docsagent;

import jakarta.ai.agent.Action;
import jakarta.ai.agent.Agent;
import jakarta.ai.agent.Decision;
import jakarta.ai.agent.LargeLanguageModel;
import jakarta.ai.agent.OnException;
import jakarta.ai.agent.Outcome;
import jakarta.ai.agent.Trigger;
import jakarta.inject.Inject;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Documentation Agent - Monitors pull requests and automatically generates
 * documentation updates when needed.
 *
 * <p>WORKFLOW:</p>
 * <ol>
 *   <li>{@code @Trigger} - Detects when a pull request is created.</li>
 *   <li>{@code @Decision} - Analyzes the PR to determine if documentation is needed.
 *       Returns {@link DocumentationAnalysis} (non-null = proceed, null = stop).</li>
 *   <li>{@code @Action} - Creates a documentation pull request based on the analysis.</li>
 *   <li>{@code @Action} - Reviews the generated documentation.</li>
 *   <li>{@code @Outcome} - Applies the documentation changes.</li>
 *   <li>{@code @OnException} - Handles any errors during the workflow.</li>
 * </ol>
 *
 * <p>DEMONSTRATES:</p>
 * <ul>
 *   <li>Object return from the {@code @Decision} phase ({@link DocumentationAnalysis}).</li>
 *   <li>Parameter injection of the decision result into {@code @Action} methods.</li>
 *   <li>LLM integration for intelligent decision-making.</li>
 *   <li>Exception handling with {@code @OnException}.</li>
 * </ul>
 */
@Agent(name = "DocsAgent", description = "Monitors pull requests and automatically generates documentation updates")
public class DocsAgent {

    private static final Logger LOGGER = Logger.getLogger(DocsAgent.class.getName());

    @Inject
    private LargeLanguageModel languageModel;

    /**
     * TRIGGER: Workflow entry point.
     * Invoked when a pull request is detected in the code repository.
     */
    @Trigger
    void detectPullRequest(PullRequest pullRequest) {
        LOGGER.info("[TRIGGER] pull request " + pullRequest.getTitle()
                + ", changed files " + pullRequest.getChangedFiles());
    }

    /**
     * DECISION: Analyzes whether the pull request requires documentation.
     *
     * <p>This method demonstrates the object return pattern:</p>
     * <ul>
     *   <li>Returns a {@link DocumentationAnalysis} (non-null) if documentation is needed.</li>
     *   <li>Returns {@code null} if no documentation is needed.</li>
     * </ul>
     *
     * <p>The returned {@link DocumentationAnalysis} object will be automatically injected
     * into subsequent {@code @Action} methods.</p>
     */
    @Decision
    DocumentationAnalysis requiresDocumentationPullRequest(PullRequest pullRequest) {
        // Use LLM to analyze the pull request
        String prompt = """
            Analyze this pull request and determine if documentation is needed.
            Pull Request: {}

            Return 'YES' if documentation is needed, 'NO' otherwise.
            """;

        String llmResponse = languageModel.query(prompt, pullRequest);

        // Parse LLM response
        if (llmResponse.toUpperCase().contains("YES")) {
            // Documentation is needed - return analysis object
            LOGGER.info("[DECISION] documentation required");

            return DocumentationAnalysis.documentationNeeded(
                "Pull request modifies user-facing API",
                pullRequest.getChangedFiles(),
                List.of("API Changes", "Usage Guide"),
                "high"
            );
        } else {
            // No documentation needed - return null to stop workflow
            LOGGER.info("[DECISION] no documentation required");
            return null;
        }
    }

    /**
     * ACTION: Generates a documentation pull request.
     *
     * <p>This method demonstrates parameter injection:</p>
     * <ul>
     *   <li>{@link PullRequest} - the original trigger event.</li>
     *   <li>{@link DocumentationAnalysis} - the object returned by the {@code @Decision} phase.</li>
     * </ul>
     *
     * <p>The analysis object provides context about what documentation is needed.</p>
     */
    @Action
    DocumentationPullRequest produceDocumentationPullRequest(
            PullRequest pullRequest,
            DocumentationAnalysis analysis) {

        LOGGER.info("[ACTION] produceDocumentationPullRequest: " + analysis.getReason()
                + ", priority " + analysis.getPriority() + ", topics " + analysis.getSuggestedTopics());

        // Use LLM to generate documentation content
        String prompt = """
            Generate documentation for this pull request: {}
            Use this documentation analysis: {}
            """;

        String docContent = languageModel.query(prompt, pullRequest, analysis);

        // Create documentation pull request
        DocumentationPullRequest docPR = new DocumentationPullRequest();
        docPR.setId("doc-" + pullRequest.getId());
        docPR.setTitle("Documentation for: " + pullRequest.getTitle());
        docPR.setDescription(docContent);
        docPR.setAuthor("DocsAgent");
        docPR.setLinkedCodePullRequestId(pullRequest.getId());
        docPR.setDocumentedTopics(analysis.getSuggestedTopics());
        docPR.setAnalysis(analysis);
        docPR.setTargetBranch("main");
        docPR.setSourceBranch("docs/" + pullRequest.getId());

        LOGGER.info("[ACTION] documentation PR " + docPR.getId() + " created");

        return docPR;
    }

    /**
     * ACTION: Reviews the generated documentation pull request.
     *
     * Receives the DocumentationPullRequest from the previous action.
     */
    @Action
    void reviewDocumentationPullRequest(DocumentationPullRequest docPR) {
        LOGGER.info("[ACTION] reviewDocumentationPullRequest " + docPR.getId()
                + ", linked to " + docPR.getLinkedCodePullRequestId()
                + ", topics " + docPR.getDocumentedTopics());

        // Perform automated review (e.g., spell check, formatting, completeness)
        // This is where additional validation logic would go
    }

    /**
     * OUTCOME: Applies the documentation changes.
     *
     * Final phase that commits and publishes the documentation.
     */
    @Outcome
    void applyDocumentationChange(DocumentationPullRequest docPR) {
        LOGGER.info("[OUTCOME] applying documentation PR " + docPR.getId()
                + ", priority " + docPR.getAnalysis().getPriority());

        // Merge the documentation PR
        // Send notifications
        // Link to original code PR
    }

    /**
     * EXCEPTION HANDLER: Handles errors during the documentation workflow.
     *
     * Every phase needs the result of the one before it, so a failure
     * anywhere is re-thrown to stop the workflow.
     */
    @OnException
    void handleDocumentationPullRequestException(RuntimeException error, PullRequest pullRequest) {
        LOGGER.log(Level.WARNING, "documentation workflow failed for PR " + pullRequest.getId(), error);

        // Send notification to team
        // Create issue for manual review

        throw error;
    }
}
