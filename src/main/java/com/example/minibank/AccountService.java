package com.example.minibank;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class AccountService {

    private final Map<Long, Account> accounts = new HashMap<>();

    public AccountService() {
        // dane startowe
//        accounts.put(1L, new Account(1L, "Alice", 1000));
 //       accounts.put(2L, new Account(2L, "Bob", 500));
    }

    public List<Account> getAllAccounts() {
        return new ArrayList<>(accounts.values());
    }

    public void addAccount(Account account) {
        if (accounts.containsKey(account.getId())) {
            throw new IllegalArgumentException("Account with this ID already exists");
        }
        accounts.put(account.getId(), account);
    }


    public Optional<Account> getAccountById(Long id) {
        return Optional.ofNullable(accounts.get(id));
    }

    public boolean transfer(Long fromId, Long toId, double amount) {
        Optional<Account> from = getAccountById(fromId);
        Optional<Account> to = getAccountById(toId);

        if (from.isPresent() && to.isPresent() && from.get().getBalance() >= amount) {
            from.get().setBalance(from.get().getBalance() - amount);
            to.get().setBalance(to.get().getBalance() + amount);
            return true;
        }
        return false;
    }

    // zwróci wszystkie konta przypisane do ownera

    public List<Account> getAccountsByOwner (String owner) {
        return accounts.values().stream()
                .filter(account -> account.getOwner().equalsIgnoreCase(owner))
                .toList();
    }

    // zwroci wszystkie konta, ktorych saldo jest większe niz podana kwota

    public List<Account> getAccountsWithBalanceGreaterThan (double amount) {
        return accounts.values().stream()
                .filter(account -> account.getBalance() > amount)
                .toList();
    }

    // zwróci sume wszystkich środków we wszystkich kontach

    public double getTotalBalance() {
        return accounts.values().stream()
                .mapToDouble(Account::getBalance)
                .sum();
    }

    // usuwanie konta po id

    public boolean deleteAccount(Long id) {
        if (accounts.containsKey(id)) {
            accounts.remove(id);
            return true;
        }
        return false;
    }

}
