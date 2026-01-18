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
package jakarta.ai.agent.example.docsagent;

import java.util.List;

/**
 * Represents a pull request for documentation changes.
 * This extends PullRequest to add documentation-specific metadata.
 */
public class DocumentationPullRequest extends PullRequest {

    private String linkedCodePullRequestId;
    private List<String> documentedTopics;
    private DocumentationAnalysis analysis;

    public DocumentationPullRequest() {
        super();
    }

    public DocumentationPullRequest(String id, String title, String description, String author,
                                     List<String> changedFiles, String diff,
                                     String targetBranch, String sourceBranch,
                                     String linkedCodePullRequestId,
                                     List<String> documentedTopics,
                                     DocumentationAnalysis analysis) {
        super(id, title, description, author, changedFiles, diff, targetBranch, sourceBranch);
        this.linkedCodePullRequestId = linkedCodePullRequestId;
        this.documentedTopics = documentedTopics;
        this.analysis = analysis;
    }

    public String getLinkedCodePullRequestId() {
        return linkedCodePullRequestId;
    }

    public void setLinkedCodePullRequestId(String linkedCodePullRequestId) {
        this.linkedCodePullRequestId = linkedCodePullRequestId;
    }

    public List<String> getDocumentedTopics() {
        return documentedTopics;
    }

    public void setDocumentedTopics(List<String> documentedTopics) {
        this.documentedTopics = documentedTopics;
    }

    public DocumentationAnalysis getAnalysis() {
        return analysis;
    }

    public void setAnalysis(DocumentationAnalysis analysis) {
        this.analysis = analysis;
    }

    @Override
    public String toString() {
        return "DocumentationPullRequest{" +
                "id='" + getId() + '\'' +
                ", title='" + getTitle() + '\'' +
                ", linkedCodePullRequestId='" + linkedCodePullRequestId + '\'' +
                ", documentedTopics=" + documentedTopics +
                ", analysis=" + analysis +
                '}';
    }
}
