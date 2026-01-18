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

import java.io.Serializable;
import java.util.List;

/**
 * Result of analyzing a pull request to determine if documentation is needed.
 *
 * This object is returned by the @Decision phase and provides detailed information
 * about what documentation changes are required. When non-null, it indicates that
 * the workflow should proceed to create documentation.
 */
public class DocumentationAnalysis implements Serializable {

    private final boolean requiresDocumentation;
    private final String reason;
    private final List<String> affectedFiles;
    private final List<String> suggestedTopics;
    private final String priority; // "high", "medium", "low"

    public DocumentationAnalysis(boolean requiresDocumentation,
                                  String reason,
                                  List<String> affectedFiles,
                                  List<String> suggestedTopics,
                                  String priority) {
        this.requiresDocumentation = requiresDocumentation;
        this.reason = reason;
        this.affectedFiles = affectedFiles;
        this.suggestedTopics = suggestedTopics;
        this.priority = priority;
    }

    /**
     * Factory method to create an analysis indicating no documentation is needed.
     * Returns null to signal the workflow should stop.
     */
    public static DocumentationAnalysis noDocumentationNeeded() {
        return null;
    }

    /**
     * Factory method to create an analysis indicating documentation is needed.
     */
    public static DocumentationAnalysis documentationNeeded(String reason,
                                                             List<String> affectedFiles,
                                                             List<String> suggestedTopics,
                                                             String priority) {
        return new DocumentationAnalysis(true, reason, affectedFiles, suggestedTopics, priority);
    }

    public boolean requiresDocumentation() {
        return requiresDocumentation;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getAffectedFiles() {
        return affectedFiles;
    }

    public List<String> getSuggestedTopics() {
        return suggestedTopics;
    }

    public String getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return "DocumentationAnalysis{" +
                "requiresDocumentation=" + requiresDocumentation +
                ", reason='" + reason + '\'' +
                ", affectedFiles=" + affectedFiles +
                ", suggestedTopics=" + suggestedTopics +
                ", priority='" + priority + '\'' +
                '}';
    }
}
