package io.ascend.flockr.admin.domain.audit;

public enum AuditLogAction {
    AUDIENCE_CREATED("Audience Created"),
    RULE_ADDED("Rule Added"),
    START_DATE_UPDATED("Start Date for Rule"),
    END_DATE_UPDATED("End Date for Rule"),
    CRON_UPDATED("Cron for Rule"),
    AUDIENCE_VALIDITY_UPDATED("Audience Validity Updated"),
    OWNER_REMOVED("Owner Removed"),
    AUDIENCE_MARKED_VERIFIED("Audience marked as verified"),
    AUDIENCE_MARKED_UNVERIFIED("Audience marked as unverified"),
    OWNER_ADDED("Owner Added"),
    RULE_TERMINATED("Rule Terminated");

    private final String value;

    AuditLogAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}