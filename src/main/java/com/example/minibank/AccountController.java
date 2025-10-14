package com.example.minibank;

import com.example.minibank.exception.AccountNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountService.getAllAccounts();
    }

    // GET /accounts/{id}
    @GetMapping("/{id}")
    public Account getAccountById(@PathVariable Long id) {
        return accountService.getAccountById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account with ID " + id + " not found"));
    }

    // GET /accounts/owner/{owner}
    @GetMapping("/owner/{owner}")
    public List<Account> getAccountsByOwner(@PathVariable String owner) {
        return accountService.getAccountsByOwner(owner);
    }

// GET /accounts/balance/greater/{amount}
    @GetMapping("balance/greater/{amount}")
    public List<Account> getAccountsWithBalanceGreaterThan(@PathVariable double amount) {
        return accountService.getAccountsWithBalanceGreaterThan(amount);
    }

    // GET /accounts/totalBalance
    @GetMapping("/totalBalance")
    public double getTotalBalance() {
        return accountService.getTotalBalance();
    }

    @PostMapping
    public String addAccount(@RequestBody Account account) {
        accountService.addAccount(account);
        return "Account added";
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request) {
        boolean success = accountService.transfer(request.getFromId(), request.getToId(), request.getAmount());
        return success ? "Transfer successful" : "Transfer failed";
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(@PathVariable Long id) {
        boolean removed = accountService.deleteAccount(id);
        return removed ? "Account deleted" : "Account not found";
    }
}
