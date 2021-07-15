package org.apereo.cas.github;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * This is {@link CombinedCommitStatus}.
 *
 * @author Misagh Moayyed
 * @since 6.1.0
 */
@Getter
@ToString
public class CombinedCommitStatus {
    public static final String WIP = "WIP";

    private final String state;
    private final List<CommitStatus> statuses;
    private final String sha;

    @JsonCreator
    public CombinedCommitStatus(@JsonProperty("state") final String state,
                                @JsonProperty("status") final List<CommitStatus> statuses,
                                @JsonProperty("sha") final String sha) {
        this.state = state;
        this.statuses = statuses;
        this.sha = sha;
    }

    public boolean isCheckStatusSuccess(final String name) {
        return this.statuses.stream().anyMatch(r -> r.getContext().equalsIgnoreCase(name)
            && r.getState().equalsIgnoreCase("success"));
    }

    public boolean isCheckStatusPending(final String name) {
        return this.statuses.stream().anyMatch(r -> r.getContext().equalsIgnoreCase(name)
            && r.getState().equalsIgnoreCase("pending"));
    }

    public boolean isCheckStatusFailure(final String name) {
        return this.statuses.stream().anyMatch(r -> r.getContext().equalsIgnoreCase(name)
            && r.getState().equalsIgnoreCase("failure"));
    }
}
