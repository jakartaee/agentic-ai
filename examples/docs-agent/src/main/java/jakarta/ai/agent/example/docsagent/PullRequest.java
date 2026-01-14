package jakarta.ai.agent.example.docsagent;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a pull request in a code repository.
 */
public class PullRequest implements Serializable {

    private String id;
    private String title;
    private String description;
    private String author;
    private List<String> changedFiles;
    private String diff;
    private String targetBranch;
    private String sourceBranch;

    public PullRequest() {
    }

    public PullRequest(String id, String title, String description, String author,
                       List<String> changedFiles, String diff, String targetBranch, String sourceBranch) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.author = author;
        this.changedFiles = changedFiles;
        this.diff = diff;
        this.targetBranch = targetBranch;
        this.sourceBranch = sourceBranch;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public String getDiff() {
        return diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch) {
        this.targetBranch = targetBranch;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch) {
        this.sourceBranch = sourceBranch;
    }

    @Override
    public String toString() {
        return "PullRequest{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", changedFiles=" + changedFiles +
                ", targetBranch='" + targetBranch + '\'' +
                ", sourceBranch='" + sourceBranch + '\'' +
                '}';
    }
}
