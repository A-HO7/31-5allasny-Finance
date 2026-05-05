package com.team31.financetracker.account.mongo;

public class AccountEventActions {
    // M2 new features
    public static final String INDEXED = "INDEXED";
    public static final String DASHBOARD_VIEWED = "DASHBOARD_VIEWED";

    // M1 retrofits
    public static final String DETAILS_UPDATED = "DETAILS_UPDATED";
    public static final String FROZEN = "FROZEN";
    public static final String UNFROZEN = "UNFROZEN";
    public static final String RATED = "RATED";
    public static final String STATEMENT_VERIFIED = "STATEMENT_VERIFIED";

    // CRUD
    public static final String ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String ACCOUNT_UPDATED = "ACCOUNT_UPDATED";
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";
    public static final String STATEMENT_CREATED = "STATEMENT_CREATED";
    public static final String STATEMENT_UPDATED = "STATEMENT_UPDATED";
    public static final String STATEMENT_DELETED = "STATEMENT_DELETED";
}