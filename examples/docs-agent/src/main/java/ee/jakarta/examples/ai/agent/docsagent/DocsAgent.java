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

/**
 * Documentation Agent - Monitors pull requests and automatically generates
 * documentation updates when needed.
 *
 * WORKFLOW:
 * 1. @Trigger - Detects when a pull request is created
 * 2. @Decision - Analyzes the PR to determine if documentation is needed
 *                Returns DocumentationAnalysis (non-null = proceed, null = stop)
 * 3. @Action - Creates a documentation pull request based on the analysis
 * 4. @Action - Reviews the generated documentation
 * 5. @Outcome - Applies the documentation changes
 * 6. @OnException - Handles any errors during the workflow
 *
 * DEMONSTRATES:
 * - Object return from @Decision phase (DocumentationAnalysis)
 * - Parameter injection of decision result into @Action methods
 * - LLM integration for intelligent decision-making
 * - Exception handling with @OnException
 */
@Agent(name = "DocsAgent", description = "Monitors pull requests and automatically generates documentation updates")
public class DocsAgent {

    @Inject
    LargeLanguageModel languageModel;


    /**
     * TRIGGER: Workflow entry point.
     * Invoked when a pull request is detected in the code repository.
     */
    @Trigger
    void detectPullRequest(PullRequest pullRequest) {
        System.out.println("📝 Pull request detected: " + pullRequest.getTitle());
        System.out.println("   Changed files: " + pullRequest.getChangedFiles());
    }

    /**
     * DECISION: Analyzes whether the pull request requires documentation.
     *
     * This method demonstrates the object return pattern:
     * - Returns DocumentationAnalysis (non-null) if documentation is needed
     * - Returns null if no documentation is needed
     *
     * The returned DocumentationAnalysis object will be automatically injected
     * into subsequent @Action methods.
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
            System.out.println("✅ Decision: Documentation is required");

            return DocumentationAnalysis.documentationNeeded(
                "Pull request modifies user-facing API",
                pullRequest.getChangedFiles(),
                List.of("API Changes", "Usage Guide"),
                "high"
            );
        } else {
            // No documentation needed - return null to stop workflow
            System.out.println("⏹️  Decision: No documentation required");
            return null;
        }
    }

    /**
     * ACTION: Generates a documentation pull request.
     *
     * This method demonstrates parameter injection:
     * - PullRequest: the original trigger event
     * - DocumentationAnalysis: the object returned by @Decision phase
     *
     * The analysis object provides context about what documentation is needed.
     */
    @Action
    DocumentationPullRequest produceDocumentationPullRequest(
            PullRequest pullRequest,
            DocumentationAnalysis analysis) {

        System.out.println("📄 Generating documentation PR based on analysis:");
        System.out.println("   Reason: " + analysis.getReason());
        System.out.println("   Priority: " + analysis.getPriority());
        System.out.println("   Topics: " + analysis.getSuggestedTopics());

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

        System.out.println("✅ Documentation PR created: " + docPR.getId());

        return docPR;
    }

    /**
     * ACTION: Reviews the generated documentation pull request.
     *
     * Receives the DocumentationPullRequest from the previous action.
     */
    @Action
    void reviewDocumentationPullRequest(DocumentationPullRequest docPR) {
        System.out.println("🔍 Reviewing documentation PR: " + docPR.getId());
        System.out.println("   Linked to code PR: " + docPR.getLinkedCodePullRequestId());
        System.out.println("   Topics covered: " + docPR.getDocumentedTopics());

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
        System.out.println("🚀 Applying documentation changes from PR: " + docPR.getId());
        System.out.println("   Analysis priority was: " + docPR.getAnalysis().getPriority());

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
        System.err.println("❌ Error during documentation workflow for PR: " + pullRequest.getId());
        System.err.println("   Error: " + error.getMessage());

        // Log error for monitoring
        // Send notification to team
        // Create issue for manual review

        throw error;
    }
}
