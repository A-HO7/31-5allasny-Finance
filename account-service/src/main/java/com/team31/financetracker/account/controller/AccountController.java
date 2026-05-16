package com.team31.financetracker.account.controller;

import com.team31.financetracker.account.dto.*;
import com.team31.financetracker.account.model.Account;
import com.team31.financetracker.account.model.AccountStatement;
import com.team31.financetracker.account.model.AccountStatus;
import com.team31.financetracker.account.model.AccountType;
import com.team31.financetracker.account.service.AccountService;
import com.team31.financetracker.account.service.AccountStatementService;
import com.team31.financetracker.contracts.dto.AccountBalanceSummaryDTO;
import com.team31.financetracker.contracts.dto.AccountsExistDTO;
import com.team31.financetracker.contracts.dto.OwnerDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AccountStatementService accountStatementService;

    public AccountController(AccountService accountService, AccountStatementService accountStatementService) {
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

    @GetMapping("/search/full-text")
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

    @GetMapping("/{id}/summary")
    public AccountSummaryDTO getAccountTransactionSummary(
            @PathVariable("id") Long id,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {
        log.info("Incoming request to get account transaction summary for account: {} from {} to {}", id, startDate, endDate);
        return accountService.getAccountTransactionSummary(id, startDate, endDate);
    }

//    @GetMapping("/{id}")
//    public Account getAccountById(@PathVariable Long id) {
//        return accountService.getById(id);
//    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PERSONAL','BUSINESS','ADMIN')")
    public com.team31.financetracker.contracts.dto.AccountDTO getAccount(@PathVariable Long id) {
        return accountService.getAccountById(id);
    }

    @GetMapping("/{id}/owner")
    @PreAuthorize("hasAnyRole('PERSONAL','BUSINESS','ADMIN')")
    public OwnerDTO getAccountOwner(@PathVariable Long id) {
        return accountService.getAccountOwner(id);
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

    @PostMapping("/{accountId}/statements")
    public AccountStatement createStatement(
            @PathVariable Long accountId,
            @RequestBody AccountStatement statement) {
        return accountStatementService.create(accountId, statement);
    }

    @GetMapping("/{accountId}/statements")
    public List<AccountStatement> getAllStatements(@PathVariable Long accountId) {
        return accountStatementService.getByAccountId(accountId);
    }

    @DeleteMapping("/{accountId}/statements/{stmtId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStatement(
            @PathVariable Long accountId,
            @PathVariable Long stmtId) {
        accountStatementService.delete(stmtId);
    }

    @GetMapping("/user/{userId}/balance-summary")
    @PreAuthorize("hasAnyRole('PERSONAL','BUSINESS','ADMIN')")
    public ResponseEntity<AccountBalanceSummaryDTO> getBalanceSummary(@PathVariable Long userId) {
        AccountBalanceSummaryDTO summaryDTO = accountService.getBalanceSummary(userId);
        if (summaryDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(summaryDTO);
    }
    // Add a tiny holder inside AccountController
    private static final class Caller {
        final Long userId;
        final String role;
        Caller(Long userId, String role) { this.userId = userId; this.role = role; }
    }

    /** Extract calling user's id + role, throw 401 if not authenticated. */
    private Caller callerFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        // details may be Long, Integer, or String depending on filter — be permissive
        Object details = auth.getDetails();
        Long uid = null;
        if (details instanceof Long l) {
            uid = l;
        } else if (details instanceof Integer i) {
            uid = i.longValue();
        } else if (details instanceof String s) {
            try { uid = Long.parseLong(s); } catch (NumberFormatException ignored) { /* keep null */ }
        }

        // Role extraction: find any authority and strip ROLE_ prefix
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse(null);
        if (role != null && role.startsWith("ROLE_")) {
            role = role.substring("ROLE_".length());
        }
        return new Caller(uid, role);
    }

    @GetMapping("/exists")
    @PreAuthorize("hasAnyRole('PERSONAL','BUSINESS','ADMIN')")
    public ResponseEntity<AccountsExistDTO> accountsExist(@RequestParam List<Long> ids) {
        boolean allExist = accountService.doAllAccountsExist(ids);
        return ResponseEntity.ok(new AccountsExistDTO(allExist));
    }

}
