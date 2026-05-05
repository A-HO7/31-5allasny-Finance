package com.team31.financetracker.account.mongo;

public enum AccountEventActions {
    // M2 new features
    INDEXED,
    DASHBOARD_VIEWED,

    // M1 retrofits
    DETAILS_UPDATED,
    FROZEN,
    UNFROZEN,
    RATED,
    STATEMENT_VERIFIED,

    // CRUD
    ACCOUNT_CREATED,
    ACCOUNT_UPDATED,
    ACCOUNT_DELETED,
    STATEMENT_CREATED,
    STATEMENT_UPDATED,
    STATEMENT_DELETED
}
