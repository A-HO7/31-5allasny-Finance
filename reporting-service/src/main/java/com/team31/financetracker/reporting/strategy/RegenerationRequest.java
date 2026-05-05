package com.team31.financetracker.reporting.strategy;

/**
 * Carries the request body data from the controller into the strategy.
 *
 * archivePrevious — true means SnapshotArchiveStrategy will save a copy
 *                   of the report to MongoDB BEFORE regenerating.
 *                   false means StandardRegenerationStrategy regenerates in place.
 *
 * reason          — must not be blank. Validated in the controller before
 *                   the strategy is selected. Stored in reportConfig and
 *                   in the MongoDB audit event.
 */
public class RegenerationRequest {

    private boolean archivePrevious;
    private String reason;

    // Required by Jackson for JSON deserialization from request body
    public RegenerationRequest() {}

    public RegenerationRequest(boolean archivePrevious, String reason) {
        this.archivePrevious = archivePrevious;
        this.reason = reason;
    }

    public boolean isArchivePrevious() { return archivePrevious; }
    public void setArchivePrevious(boolean archivePrevious) {
        this.archivePrevious = archivePrevious;
    }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
