package com.team31.financetracker.account.controller;
import com.team31.financetracker.account.dto.*;

import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.team31.financetracker.account.service.AccountStatementService;
import org.springframework.security.core.Authentication;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;
    private final AccountStatementService accountStatementService;

    public AccountController (AccountService accountService, AccountStatementService accountStatementService){
        this.accountService = accountService;
        this.accountStatementService = accountStatementService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> searchAccounts(
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) Double minBalance,
            @RequestParam(required = false) Double maxBalance) {
        return accountService.searchByStatusAndBalanceRange(status, minBalance, maxBalance);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> getAllAccounts() {
        return accountService.getAll();
    }

    @GetMapping("/search/full-text") // Resulting path: /api/accounts/search/full-text
    @PreAuthorize("hasAnyRole('PERSONAL', 'BUSINESS', 'ADMIN')")
    public List<AccountDTO> fullTextSearch(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Double minBalance,
            @RequestParam(required = false) Double maxBalance,
            @RequestParam(required = false) Double minRating,
            @RequestParam(required = false) Double maxRating) {

        return accountService.fullTextSearch(query, type, status, currency, minBalance, maxBalance, minRating, maxRating);
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<AccountPerformanceDashboardDTO> getDashboard(
            @PathVariable Long id,
            HttpServletRequest request) {

        // Extract uid and role from JWT via SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long requestingUserId = (Long) auth.getDetails();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");

        return ResponseEntity.ok(accountService.getDashboard(id, requestingUserId, role));
    }


    @GetMapping("/email")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> getByUserEmail(@RequestParam String email) {
        return accountService.getByUserEmail(email);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> getAccountByUserId(@PathVariable Long userId) {
        return accountService.getByUserId(userId);
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> getAccountsByType(@PathVariable AccountType type) {
        return accountService.getByType(type);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public List<Account> getAccountsByStatus(@PathVariable AccountStatus status) {
        return accountService.getByStatus(status);
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public AccountSummaryDTO getSummaryByQueryParams(@RequestParam MultiValueMap<String, String> queryParams) {
        Long account = findLongParam(queryParams, "accountId", "id", "account_id");
        if (account == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account id is required");
        }
        return buildAccountSummary(account, queryParams);
    }

    @GetMapping("/{id}/summary/{rangeStart}/{rangeEnd}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public AccountSummaryDTO getSummaryWithPathRange(
            @PathVariable Long id,
            @PathVariable String rangeStart,
            @PathVariable String rangeEnd) {
        LocalDateTime start = parseFlexibleDateTime(rangeStart);
        LocalDateTime end = parseFlexibleDateTime(rangeEnd);
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        return accountService.getSummary(id, start, end);
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public AccountSummaryDTO getSummary(
            @PathVariable Long id,
            @RequestParam MultiValueMap<String, String> queryParams) {
        return buildAccountSummary(id, queryParams);
    }

    private AccountSummaryDTO buildAccountSummary(Long id, MultiValueMap<String, String> queryParams) {
        String rangeStart = findDateParam(queryParams,
                "startDate", "start", "from", "fromDate", "begin", "beginDate",
                "periodStart", "dateFrom", "rangeStart", "lower", "minDate");
        String rangeEnd = findDateParam(queryParams,
                "endDate", "end", "to", "toDate", "finish", "finishDate",
                "periodEnd", "dateTo", "rangeEnd", "upper", "maxDate");

        LocalDateTime start;
        LocalDateTime end;

        if (rangeStart != null && rangeEnd != null) {
            start = parseFlexibleDateTime(rangeStart);
            end = parseFlexibleDateTime(rangeEnd);
        } else if (rangeStart != null) {
            start = parseFlexibleDateTime(rangeStart);
            end = LocalDateTime.now();
        } else if (rangeEnd != null) {
            start = LocalDateTime.of(2000, 1, 1, 0, 0);
            end = parseFlexibleDateTime(rangeEnd);
        } else {
            // No dates provided — default to all time
            start = LocalDateTime.of(2000, 1, 1, 0, 0);
            end = LocalDateTime.now();
        }

        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }
        return accountService.getSummary(id, start, end);
    }

    private static Long findLongParam(MultiValueMap<String, String> queryParams, String... acceptedNames) {
        if (queryParams == null || queryParams.isEmpty()) {
            return null;
        }
        for (String want : acceptedNames) {
            String w = want.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
                if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(w)) {
                    List<String> vals = e.getValue();
                    if (vals != null && !vals.isEmpty() && vals.get(0) != null && !vals.get(0).isBlank()) {
                        try {
                            return Long.parseLong(vals.get(0).trim());
                        } catch (NumberFormatException ignored) {
                            // try next matching key
                        }
                    }
                }
            }
        }
        return null;
    }

    private static String findDateParam(MultiValueMap<String, String> queryParams, String... acceptedNames) {
        if (queryParams == null || queryParams.isEmpty()) {
            return null;
        }
        for (String want : acceptedNames) {
            String w = want.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
                if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(w)) {
                    List<String> vals = e.getValue();
                    if (vals != null && !vals.isEmpty()) {
                        String v = vals.get(0);
                        if (v != null && !v.isBlank()) {
                            return v.trim();
                        }
                    }
                }
            }
        }
        return null;
    }

    private static LocalDateTime parseFlexibleDateTime(String raw) {
        String s = raw.trim();
        if (s.matches("^\\d{10,13}$")) {
            long ms = Long.parseLong(s);
            if (s.length() <= 10) {
                ms *= 1000L;
            }
            return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDateTime();
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (DateTimeParseException ignored2) {
                try {
                    return LocalDate.parse(s).atStartOfDay();
                } catch (DateTimeParseException e3) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date: " + raw);
                }
            }
        }
    }

    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @PostMapping
    public Account createAccount(@RequestBody Account account) {
        return accountService.create(account);
    }

    @PutMapping("/{id}")
    public Account updateAccount(@PathVariable Long id, @RequestBody Account account) {
        return accountService.update(id, account);
    }

    @PutMapping("/{id}/details")
    public Account updateAccountDetails(
            @PathVariable Long id,
            @RequestBody Map<String, Object> details
    ) {
        return accountService.updateAccountDetails(id, details);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.delete(id);
    }

    @PutMapping("/{id}/status")
    public int updateStatus(@PathVariable Long id, @RequestParam AccountStatus status) {
        return accountService.updateStatusById(id, status);
    }
            @GetMapping("/statements/expired")
    public List<AccountStatementAlertDTO> getAccountsWithExpiredStatements() {
        return accountService.getAccountsWithExpiredStatements();
    }
    @GetMapping("/reports/top-balance")
    public List<TopAccountDTO> getTopBalanceAccounts(
            @RequestParam(required = false) Integer limit) {
        int effective = (limit != null && limit > 0) ? limit : 10;
        return accountService.getTopBalanceAccounts(effective);
    }

    @PostMapping("/{id}/rate")
    public ResponseEntity<Void> rateAccount(@PathVariable Long id, @RequestBody RateAccountRequest body) {
        if (body == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        accountService.rateAccountAfterStatementReview(id, body.getStatementId(), body.getRating());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/index")
    public ResponseEntity<Void> indexAccount(@PathVariable Long id) {
        accountService.indexAccountById(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/freeze")
    public ResponseEntity<Void> freezeAccount(@PathVariable Long id, @RequestBody(required = false) FreezeAccountRequest body) {
        AccountStatus status = (body != null && body.getStatus() != null)
                ? body.getStatus()
                : AccountStatus.FROZEN;
        accountService.freezeAccount(id, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/details/search")
    public List<Account> searchByDetail(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) AccountStatus status
    ) {
        return accountService.searchByDetail(key, value, status);
    }

    @PutMapping("/{accountId}/statements/{statementId}/verify")
    public Account verifyStatement(
            @PathVariable Long accountId,
            @PathVariable Long statementId,
            @RequestBody RequestDTO request
    ) {
        return accountService.verifyStatement(accountId, statementId, request.getVerifiedBy());
    }

    // [TC_S2_11] Create Statement
    @PostMapping("/{accountId}/statements")
    public AccountStatement createStatement(
            @PathVariable Long accountId,
            @RequestBody AccountStatement statement) {
        //AccountStatement a;
        return accountStatementService.create(accountId, statement);
    }
//
//    // [TC_S2_12] List Statements
//    @GetMapping("/{accountId}/statements")
//    public List<AccountStatement> getAllStatements() {
//        return accountStatementService.getAll();
//    }

    @GetMapping("/{accountId}/statements")
    public List<AccountStatement> getAllStatements(@PathVariable Long accountId) {
        return accountStatementService.getByAccountId(accountId);
    }

    // [TC_S2_13] Delete Statement
    @DeleteMapping("/{accountId}/statements/{stmtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStatement(
            @PathVariable Long accountId,
            @PathVariable Long stmtId) {
        accountStatementService.delete(stmtId);
    }}
