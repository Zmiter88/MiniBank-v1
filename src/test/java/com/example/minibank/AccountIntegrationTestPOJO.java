package com.example.minibank;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.path.json.JsonPath;
import net.minidev.json.JSONUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

public class AccountIntegrationTestPOJO {
    private static List<Account> allAccounts;

    @BeforeAll
    static void setup() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File("src/test/resources/accounts.json");
        allAccounts = objectMapper.readValue(file, new TypeReference<List<Account>>() {});
    }

    // upewnij się, że żadne konto o statusie "ACTIVE" nie ma salda 0

    @Test
    public void balanceOnActiveAccountIsOver0() {
        boolean allActiveAccountsHaveBalanceOver0 = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.getStatus()))
                .allMatch(acc -> acc.getBalance() > 0);
        assertThat(allActiveAccountsHaveBalanceOver0).isTrue();
    }

    // znajdź właściciela konta, który ma najwyższe saldo, i sprawdź, że jego balance jest większe niż 10 000

    @Test
    public void theReachestOwner() {
        Optional<Account> reachestOwner = allAccounts.stream()
                .max(Comparator.comparing(Account::getBalance));
        System.out.println("Najbogatszy wlasciciel to: " + reachestOwner.get().getOwner());
        assertThat(reachestOwner.get().getBalance(), greaterThanOrEqualTo(10000.0));
    }

    // oblicz sumę pól balance dla kont, których currency to "PLN", i sprawdź, że jest większa niż np. 5000.0

    @Test
    public void sumOfPLNAccounts() {
        double sumOfPLNAccounts = allAccounts.stream()
                .filter(acc -> "PLN".equals(acc.getCurrency()))
                .mapToDouble(acc -> acc.getBalance())
                .sum();
        assertThat(sumOfPLNAccounts, greaterThanOrEqualTo(5000.0));
    }

    // logika biznesowa — konto premium nie może być zablokowane

    @Test
    public void premiumAccountNoneBlocked() {
        boolean premiumAccountNoneBlocked = allAccounts.stream()
                .filter(acc -> "PREMIUM".equals(acc.getAccountType()))
                .noneMatch(acc -> "BLOCKED".equals(acc.getStatus()));
        assertThat(premiumAccountNoneBlocked).isTrue();
    }

    // upewnij się, że wszystkie konta w USD mają saldo większe niż 1000

    @Test
    public void checkIfEveryAccountsHaveBalanceOver1000() {
        boolean everyAccountsHaveBalanceOver1000 = allAccounts.stream()
                .filter(acc -> "USD".equals(acc.getCurrency()))
                .allMatch(acc -> acc.getBalance() > 1000.0);
        assertThat(everyAccountsHaveBalanceOver1000).isTrue();

    }

    // policz średnie saldo (balance) wszystkich kont "ACTIVE" i sprawdź, że średnia jest większa niż 3000.

    @Test
    public void averageBalanceOverThan3000() {
        OptionalDouble averageBalanceOverThan3000 = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.getStatus()))
                .mapToDouble(acc -> acc.getBalance())
                .average();
        assertThat(averageBalanceOverThan3000.getAsDouble(), greaterThan(3000.0));
    }

    // znajdź właściciela konta "ACTIVE" z najwcześniejszą datą createdAt i sprawdź, że jego saldo jest większe niż 0.
    @Test
    public void theOldestAccount() {
        Optional<Account> theOldestAccount = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.getStatus()))
                .min(Comparator.comparing(acc -> LocalDateTime.parse(acc.getCreatedAt())));
        System.out.println("Właściciel najstarszego konta to: " + theOldestAccount.get().getOwner());
        assertThat(theOldestAccount.get().getBalance(), greaterThan(0.0));
    }


}
