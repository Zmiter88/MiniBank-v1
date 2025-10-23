package com.example.minibank;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;


public class AccountIntegrationTest {

    private List<Map<String, Object>> allAccounts;

    @BeforeEach
    public void setup() throws IOException {
        // Wczytanie wszystkich kont z JSON tylko raz przed każdym testem
        String json = Files.readString(Path.of("src/test/resources/accounts.json"));
        JsonPath jsonPath = new JsonPath(json);
        allAccounts = jsonPath.getList("");
    }

    @Test
    public void shouldFilterOnlyActiveAccountsFromJson() throws Exception {
        // 1️⃣ wczytanie pliku JSON jako String
        String json = Files.readString(Path.of("src/test/resources/accounts.json"));

        // 2️⃣ sparsowanie pliku do obiektu JsonPath (REST-assured ma własny parser)
        JsonPath jsonPath = new JsonPath(json);

        // 3️⃣ pobranie listy wszystkich kont jako lista map
        List<Map<String, Object>> allAccounts = jsonPath.getList("");

        // 4️⃣ przefiltrowanie tylko tych, które mają status ACTIVE
        List<Map<String, Object>> activeAccounts = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .toList();

        // 5️⃣ asercje — czy wszystkie mają status ACTIVE
        assertFalse(activeAccounts.isEmpty(), "Lista aktywnych kont nie powinna być pusta");
        assertThat(activeAccounts, everyItem(hasEntry("status", "ACTIVE")));

        // 6️⃣ dla ciekawości wypiszemy właścicieli aktywnych kont
        activeAccounts.forEach(acc ->
                System.out.println("Aktywne konto: " + acc.get("owner") + " [" + acc.get("balance") + "]")
        );
    }

    // sprawdza, że każde z nich ma saldo > 0

    @Test
    public void checkIfEveryAccountHaveBalanceOverThanZero() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/accounts.json"));
        // parsowanie stringa na obiekt JsonPath
        JsonPath jsonPath = new JsonPath(json);
        // pobranie wszystkich kont jako lista map
        List<Map<String, Object>> allAccounts = jsonPath.getList("");
        // filtrowanie po saldzie wiekszym niz 0
        List<Map<String, Object>> accountsOverBalanceZero = allAccounts.stream()
                .filter(acc -> ((Number) acc.get("balance")).doubleValue() > 0)
                .toList();
        assertThat(allAccounts, everyItem(hasEntry(equalTo("balance"), (Object) greaterThan (0.0))));

    }

    // totalBalance sumuje się poprawnie tylko dla ACTIVE

    @Test
    public void shouldCalculateTotalBalanceForActiveAccountsOnly() throws Exception {
        // 1️⃣ Wczytaj plik JSON jako String
        String json = Files.readString(Path.of("src/test/resources/accounts.json"));

        // 2️⃣ Utwórz obiekt JsonPath
        JsonPath jsonPath = new JsonPath(json);

        // 3️⃣ Pobierz listę wszystkich kont
        List<Map<String, Object>> allAccounts = jsonPath.getList("");

        // 4️⃣ Filtrowanie po statusie ACTIVE
        double totalBalance = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                .sum();

        // 5️⃣ Asercja — oczekujemy sumy 19301.25
        assertThat(totalBalance, closeTo(19301.25, 0.001));
    }
// wersja dynamiczna, generyczna

    @Test
    public void shouldCalculateTotalBalanceForActiveAccountsOnly_Dynamic() throws Exception {
        // Wczytanie pliku JSON
        String json = Files.readString(Path.of("src/test/resources/accounts.json"));
        JsonPath jsonPath = new JsonPath(json);

        // Pobranie wszystkich kont
        List<Map<String, Object>> allAccounts = jsonPath.getList("");

        // Filtrowanie kont ACTIVE
        List<Map<String, Object>> activeAccounts = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .toList();

        // Obliczenie sumy balance dla ACTIVE
        double totalBalance = activeAccounts.stream()
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                .sum();

        // Dynamiczne wyliczenie oczekiwanej sumy (dla porównania)
        double expectedTotal = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                .sum();

        // Asercja
        assertThat(totalBalance, closeTo(expectedTotal, 0.001));
    }

    // test dla sumy sald ale tylko w PLN
    @Test
    public void balanceSumOnlyPLNDynamic() {
        List<Map<String, Object>> activeAccounts = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .toList();
        double sumInPLN = activeAccounts.stream()
                .filter(acc -> "PLN".equals(acc.get("curency")))
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                .sum();

        // wyliczenie tej samej wartosci ale metodą petli for

        double sumFromLoop = 0.0;
        for (Map<String, Object> acc : allAccounts) {
            if ("ACTIVE".equals(acc.get("status")) && "PLN".equals(acc.get("curency"))) {
                sumFromLoop += ((Number) acc.get("balance")).doubleValue();
            }
        }
        assertThat(sumFromLoop, equalTo(sumInPLN));
    }

    // 1. Sprawdź, czy każde konto ma wymagane pola
    //Cel: upewnij się, że każde konto w JSON-ie zawiera wszystkie kluczowe pola.

    @Test // wersja streamowa
    public void checkIfEveryAccountHaveRequiredFields() {
        boolean allHaveRequiredFields = allAccounts.stream()
                .allMatch(acc -> acc.keySet().containsAll(
                        List.of("id", "owner", "balance", "currency", "status", "createdAt", "accountType")
                ));

        assertThat(allHaveRequiredFields).isTrue();
    }
    @Test // wersja prostrza
    public void checkIfEveryAccountHaveRequiredFieldsSimple() {
        assertThat(allAccounts, everyItem(hasKey("id")));
        assertThat(allAccounts, everyItem(hasKey("owner")));
        assertThat(allAccounts, everyItem(hasKey("balance")));
        assertThat(allAccounts, everyItem(hasKey("currency")));
        assertThat(allAccounts, everyItem(hasKey("status")));
        assertThat(allAccounts, everyItem(hasKey("createdAt")));
        assertThat(allAccounts, everyItem(hasKey("accountType")));
    }

    //💰 2. Sprawdź, że suma sald wszystkich kont jest większa niż 0
    //Cel: walidacja poprawności danych finansowych (żadne konto nie ma ujemnego salda).

    @Test
    public void totalBalanceShouldBePositive() {
        double totalBalance = allAccounts.stream()
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                        .sum();
                assertThat(totalBalance, greaterThan(0.0));
    }

    //🏦 3. Sprawdź, czy konta „PREMIUM” mają saldo powyżej 5000
    //Cel: logika biznesowa — konto typu PREMIUM powinno mieć wysoki balans.

    @Test
    public void premiumAccountsShouldHaveHighBalance() {
        List<Double> premiumAccounts = allAccounts.stream()
                .filter(acc -> "PREMIUM".equals(acc.get("accountType")))
                .map(acc -> ((Number) acc.get("balance")).doubleValue())
                        .toList();

        assertThat(premiumAccounts, everyItem(greaterThan(5000.0)));
    }

    //🕒 4. Sprawdź, czy konta są utworzone w kolejności chronologicznej (rosnąco po createdAt)
    //Cel: upewnij się, że dane są sortowane poprawnie po dacie utworzenia.

    @Test
    public void accountsShouldBeInChronologicalOrder() {
        List<String> creationDates = allAccounts.stream()
                .map(acc -> acc.get("createdAt").toString())
                .toList();
        List<String> sortedDates = new ArrayList<>(creationDates);
        Collections.sort(sortedDates);
        assertThat(creationDates, equalTo(sortedDates));
    }

    //💸 5. Sprawdź, czy tylko aktywne konta w PLN mają sumę większą niż 10000
    //Cel: test logiki filtrowania i agregacji.

    @Test
    public void totalActivePLNBalanceShouldBeOver10000() {
        List<Map<String, Object>> activeAccountsInPLN = allAccounts.stream()
                .filter(acc -> "ACTIVE".equals(acc.get("status")))
                .filter(acc -> "PLN".equals(acc.get("currency")))
                .toList();
        // wyswietlenie kont
        activeAccountsInPLN.forEach(acc -> System.out.println(acc));
        double totalBalanceActiveAccountsInPLN = activeAccountsInPLN.stream()
                .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
                .sum();
        assertThat(totalBalanceActiveAccountsInPLN, greaterThan(10000.0));
    }

    //🚫 6. Sprawdź, że konta z BLOCKED nie mają dużego salda (>5000)
    //Cel: walidacja reguł bezpieczeństwa / ryzyka.

    @Test
    public void blockedAccountsShouldNotHaveHighBalance() {
        List<Double> blockedAccounts = allAccounts.stream()
                .filter(acc -> "BLOCKED".equals(acc.get("status")))
                .map(acc -> ((Number) acc.get("balance")).doubleValue())
                        .toList();
        assertThat(blockedAccounts, everyItem(lessThanOrEqualTo(5000.0)));
    }

    // sprawdzenie maksymalnej wartości srodkow na kncie

    @Test
    public void maxBalanceInAllAccounts() {
  //      double maxBalance = allAccounts.stream()
    //            .mapToDouble(acc -> ((Number) acc.get("balance")).doubleValue())
   //             .max().orElse(0.0);
        Optional<Map<String, Object>> accountWithMaxBalance = allAccounts.stream()
                        .max(Comparator.comparing( acc -> ((Number) acc.get("balance")).doubleValue()));
        System.out.println("Wartośc konta z najwyższy saldem to: " + accountWithMaxBalance.get().get("balance") + " " + accountWithMaxBalance.get().get("currency") );
        System.out.println("Waściciel z najwyższym saldem to: " + accountWithMaxBalance.get().get("owner"));
    }

    @Test
    public void minBalanceInAllAccounts() {
        Optional<Map<String, Object>> accountWithMinBalance = allAccounts.stream()
                .min(Comparator.comparing(acc -> ((Number) acc.get("balance")).doubleValue()));
        System.out.println("Wartośc konta z najniższym saldem to: " + accountWithMinBalance.get().get("balance"));

    }
    @Test
    public void checkAllFields() {
        // wyswietl saldo konta wlasciela Diana
        allAccounts.stream()
                .filter(acc -> "Diana".equals(acc.get("owner")))
                .map(acc -> acc.get("balance"))
                .forEach(balance -> System.out.println("Saldo Diany: " + balance));

        allAccounts.stream()
                        .filter(acc -> "Diana".equals(acc.get("owner")))
                                .forEach(acc -> System.out.println("Saldo Diany to: " + acc.get("balance")));

        // wyswietlenie waluty w jakiej trzyma hajs Alicja

        allAccounts.stream()
                .filter(acc -> "Alice".equals(acc.get("owner")))
                .map(acc -> acc.get("currency"))
                .forEach(currency -> System.out.println("Waluta w jakiej trzyma hajs Alicja to: " + currency));

        // albo prostrza wersja

        allAccounts.stream()
                .filter(acc -> "Alice".equals(acc.get("owner")))
                .forEach(acc -> System.out.println("Waluta w jakiej trzyma hajs Alicja to: " + acc.get("currency")));

        // przypisanie do zmiennej

        Map<String, Object> bobAccount = allAccounts.stream()
                .filter(acc -> "Bob".equals(acc.get("owner")))
                .findFirst().orElse(null);
        System.out.println("Id Boba to: " + bobAccount.get("id") + " z kolei jego saldo wynosi: " + bobAccount.get("balance") + " w walucie: "
                + bobAccount.get("currency") + " a jego konto ma status: " + bobAccount.get("status"));

    }


}




