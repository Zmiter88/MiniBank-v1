package com.example.minibank;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AccountControllerRestAssuredTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        // REST-assured będzie wysyłać żądania na właściwy port
        RestAssured.port = port;
    }


    @Test
    public void testGetSingleAccount() {
        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("owner", equalTo("Alice"))
                .body("balance", equalTo(1000.0F));
    }

    // test dla konta, które nie istnieje
    @Test
    public void notExistingAccount() {
        given()
                .log().all()
                .when()
                .get("/accounts/99")
                .then()
                .log().body()
                .statusCode(404)
                .body(equalTo("Account with ID 99 not found"));
    }

    // Endpoint: /accounts
    //Cel: sprawdzić, czy lista kont ma poprawną liczbę elementów i czy pola kont są zgodne z tym, co jest w AccountService

    @Test
    public void testGetAllAccounts() {
        given()
                .log().all()
                .when()
                .get("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body("size()", equalTo(2))
                .body("[0].id", equalTo(1))
                .body("[0].owner", equalTo("Alice"))
                .body("[0].balance", equalTo(1000.0F))
                .body("[1].id", equalTo(2))
                .body("[1].owner", equalTo("Bob"))
                .body("[1].balance", equalTo(500.0F));
    }

    // Endpoint: /accounts
    //Cel: dodać nowe konto przez POST i sprawdzić: Status odpowiedzi (np. 200 lub 201)
    //Czy nowe konto faktycznie pojawia się w GET /accounts

    @Test
    public void addNewAccount() {
        String requestBody = """
                {
                "id": 3,
                "owner": "Charlie",
                "balance": 2000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Account added"));

        // GET → sprawdź, że konto faktycznie istnieje

        given()
                .log().all()
                .when()
                .get("/accounts/3")
                .then()
                .log().body()
                .statusCode(200)
                .body("id", equalTo(3))
                .body("owner", equalTo("Charlie"))
                .body("balance", equalTo(2000.0F));
    }

    // sprawdzić, że filtr działa poprawnie i zwraca tylko konta danego właściciela.
    //Dodatkowe wyzwanie: spróbuj dla właściciela, który nie ma kont i sprawdź, czy zwraca pustą listę.

    @Test
    public void getAccountByOwner() {
        given()
                .log().all()
                .when()
                .get("/accounts/owner/Alice")
                .then()
                .log().body()
                .statusCode(200)
                // każdy element listy powinien mieć owner = "Alice"
                .body("owner", everyItem(equalTo("Alice")));
    }

    @Test
    public void getAccountByNotExistingOwner() {
        given()
                .log().all()
                .when()
                .get("/accounts/owner/Adam")
                .then()
                .log().body()
                .statusCode(200)
                .body("size()", equalTo(0));
    }

    // Endpoint: /accounts/balance/greater/{amount}
    //Cel: sprawdzić, że zwracane są tylko konta z saldem większym niż podana wartość.
    //Wskazówka: np. amount=700 powinno zwrócić tylko konto Alice (1000).

    @Test
    public void getAccountBalanceGreaterThan700() {
        var response =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/balance/greater/700")
                        .then()
                        .log().body()
                        .statusCode(200);
        response.body("size()", equalTo(1));
        response.body("owner", hasItem("Alice"));
    }

    @Test
    public void getAccountBalanceGreaterThan200() {
        var response =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/balance/greater/200")
                        .then()
                        .log().body()
                        .statusCode(200);
        response.body("size()", equalTo(2));
        response.body("owner", hasItems("Alice", "Bob"));
    }

    // ten sam tets co wyzej ale dynamiczny, ze nie wpiusje z palca imion, tylko tets sam wyskzuje ownerów z listy na podstawie logiki serwisu

    @Autowired
    private AccountService accountService;

    @Test
    public void dynamicCheckAccountsBalanceGreaterThan() {
        double amount = 200;
        List<Account> expectedAccouts = accountService.getAccountsWithBalanceGreaterThan(amount);
        List<String> expectedOwners = expectedAccouts.stream().map(Account::getOwner).toList();

        var response =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/balance/greater/" + amount)
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract();
        List<String> actualOwners = response.jsonPath().getList("owner");
        assertThat(actualOwners, contains(expectedOwners.toArray()));
    }

    // Endpoint: /accounts/totalBalance
    //Cel: sprawdzić, że zwraca poprawną sumę wszystkich kont.
    //Podpowiedź: w Twoim przypadku Alice 1000 + Bob 500 → wynik = 1500

    @Test
    public void getAccountTotalBalance() {

        given()
                .log().all()
                .when()
                .get("/accounts/totalBalance")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo(1500.0));
    }

    @Test
    public void dynamicCheckAccountsTotalBalance() {

        double expectedAmount = accountService.getTotalBalance();

        var response =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/totalBalance")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .as(Double.class);
        assertThat(response, equalTo(expectedAmount));
    }

    // POST transferu między kontami
    //Endpoint: /accounts/transfer
    //Cel: przetestować transfer środków między dwoma kontami:
    //Sprawdź status odpowiedzi i komunikat ("Transfer successful" lub "Transfer failed")
    //Sprawdź, czy saldo obu kont zostało poprawnie zmienione.
    //Dodatkowe wyzwanie: spróbuj transfer większej kwoty niż saldo konta źródłowego → test powinien zwrócić "Transfer failed" i saldo nie powinno się zmienić.

    @Test
    public void testTransferValid() {
        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 500
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer successful"));

        // sprawdzenie sald po transferze
        float fromBalance =
                given()
                        .when()
                        .get("/accounts/1")
                        .then()
                        .extract()
                        .path("balance");

        float toBalance =
                given()
                        .when()
                        .get("/accounts/2")
                        .then()
                        .extract()
                        .path("balance");

        assertThat(fromBalance, equalTo(500.0f));
        assertThat(toBalance, equalTo(1000.0f));
    }

    @Test
    public void testTransferInValid() {
        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 5000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer failed"));

        // sprawdzenie sald po transferze
        float fromBalance =
                given()
                        .when()
                        .get("/accounts/1")
                        .then()
                        .extract()
                        .path("balance");

        float toBalance =
                given()
                        .when()
                        .get("/accounts/2")
                        .then()
                        .extract()
                        .path("balance");

        assertThat(fromBalance, equalTo(1000.0f));
        assertThat(toBalance, equalTo(500.0f));
    }

    @Test
    public void checkThatIs2Accounts() {

        given()
                .log().all()
                .when()
                .get("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body("size()", equalTo(2));
    }

    @Test
    public void checkThatAccountsId2Has100AndAlice() {

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("id", equalTo(1))
                .body("owner", equalTo("Alice"))
                .body("balance", equalTo(1000.0f));
    }

    @Test
    public void checkIfFilterByOwnerWorks() {

        given()
                .log().all()
                .when()
                .get("/accounts/owner/Alice")
                .then()
                .log().body()
                .statusCode(200)
                .body("owner", hasItem("Alice"))
                .body("size()", equalTo(1));
    }

    @Test
    public void getAccountOwnerWithBalanceOver600() {

        given()
                .log().all()
                .when()
                .get("/accounts/balance/greater/600")
                .then()
                .log().body()
                .statusCode(200)
                .body("owner", hasItem("Alice"));
    }

    @Test
    public void dynamicGetAccontOwnerWithBalanceOver600() {
        double amount = 600.0;

        List<Account> expectedAccouts = accountService.getAccountsWithBalanceGreaterThan(amount);
        List<String> expectedOwners = expectedAccouts.stream().map(Account::getOwner).toList();

        var response =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/balance/greater/600")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract();
        List<String> actualOwners = response.jsonPath().getList("owner");
        assertThat(actualOwners, contains(expectedOwners.toArray()));
    }

    // Dodaj nowe konto i sprawdź, że pojawia się w GET /accounts
//Endpoint: POST /accounts z body:


    @Test
    public void addNewAccountAndCheckIfExist() {

        String requestBody = """
                {
                "id": 3,
                "owner": "Charlie",
                "balance": 2000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Account added"));

        // sprawdzenie ze lista zawiera Charlie

        given()
                .log().all()
                .when()
                .get("/accounts/3")
                .then()
                .log().body()
                .statusCode(200)
                .body("id", equalTo(3))
                .body("owner", equalTo("Charlie"));

        // sprawdzenie ze lista ma 3 elementy

        given()
                .log().all()
                .when()
                .get("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body("size()", equalTo(3));

    }

    // 6️⃣ Przetestuj transfer między kontami

    //Endpoint: POST /accounts/transfer
    //Body:
    //{
    //  "fromId": 1,
    //  "toId": 2,
    //  "amount": 500
    //}

    //Sprawdź status "Transfer successful" i salda kont po transferze (użyj GET /accounts/{id}).

    @Test
    public void testTransferMethod() {

        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 500
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer successful"));

        // sprawdzenie salda kont po transferze

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(500f));

        given()
                .log().all()
                .when()
                .get("/accounts/2")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(1000f));
    }


    //7️⃣ Przetestuj nieudany transfer

    //Endpoint: POST /accounts/transfer

    //Body:

    //{
    //  "fromId": 1,
    //  "toId": 2,
    //  "amount": 5000
    //}

    //Sprawdź status "Transfer failed" i że salda kont nie zmieniły się.

    @Test
    public void testTransferMethodWithFailed() {

        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 5000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer failed"));

        // sprawdzenie salda kont po transferze

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(1000f));

        given()
                .log().all()
                .when()
                .get("/accounts/2")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(500f));
    }

    // wersja tego co wyzje ale dynamiczna

    @Test
    public void dynamicTestTransferMethodWithFailed() {

        double amount = 5000;
        // sprawdzenie sald kont przed transferem


        var response =   // response trzyma cała odpowiedź

                given()
                        .log().all()
                        .when()
                        .get("/accounts/1")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .response();
        double beforeFrom = response
                .jsonPath()
                .getDouble("balance");  // służy do wyciągnięcia konkretnej wartości z tej odpowiedzi (balance).

        System.out.println("co pokaze wyjscie: " + response);
        System.out.println("co pokaze wyjscie2: " + beforeFrom);
        System.out.println("co pokaze wyjscie3: " + response.asString());
        System.out.println("co pokaze wyjscie4: " + response.prettyPrint());


        double beforeTo =

                given()
                        .log().all()
                        .when()
                        .get("/accounts/2")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getDouble("balance");

        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 5000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer failed"));

        double afterFrom =

                given()
                        .log().all()
                        .when()
                        .get("/accounts/1")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getDouble("balance");


        double afterTo =

                given()
                        .log().all()
                        .when()
                        .get("/accounts/2")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .jsonPath()
                        .getDouble("balance");

        assertThat(beforeFrom, equalTo(afterFrom));
        assertThat(beforeTo, equalTo(afterTo));
    }

        // Zadanie 1 — Sprawdzenie sumy sald po dodaniu nowego konta
        //Cel: po dodaniu nowego konta, sprawdź, że /accounts/totalBalance wzrosło dokładnie o jego wartość.
        //Wskazówki:
        //Pobierz aktualną sumę przed dodaniem (GET /accounts/totalBalance).
        //Dodaj nowe konto przez POST /accounts.
        //Pobierz sumę ponownie.
        //Sprawdź, że różnica = saldo nowego konta.


        @Test
        public void checkTotalBalanceAfterAddingNewAccount() {
        double before =
            given()
                    .log().all()
                    .when()
                    .get("/accounts/totalBalance")
                    .then()
                    .log().body()
                    .statusCode(200)
                    .extract()
                    .as(Double.class);

        // dodaje nowe konto

            String requestBody = """
                {
                "id": 10,
                "owner": "Kaczan",
                "balance": 300
                }
                """;

            given()
                    .log().all()
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .when()
                    .post("/accounts")
                    .then()
                    .log().body()
                    .statusCode(200)
                    .body(equalTo("Account added"));

            // sprawdzenie sumy sald, po dodaniu nowego konta

            double after =
                    given()
                            .log().all()
                            .when()
                            .get("/accounts/totalBalance")
                            .then()
                            .log().body()
                            .statusCode(200)
                            .extract()
                            .as(Double.class);

            assertThat(after - before, equalTo(300.0));
        }

        // Zadanie 2 — Test błędu: dodanie konta o istniejącym ID
    //Cel: zweryfikuj, że serwis zwraca błąd, jeśli spróbujesz dodać konto z ID, które już istnieje.
    //Wskazówki:
    //POST /accounts z body { "id": 1, "owner": "X", "balance": 999 }
    //Oczekiwany status: 500 (lub 400, zależnie od implementacji)
    //Możesz też pobrać wiadomość błędu przez .extract().asString() i sprawdzić, że zawiera "already exists".


@Test
public void addAccountWithExistingId() {

    String requestBody = """
            {
            "id": 1,
            "owner": "X",
            "balance": 999
            }
            """;

    var response =
    given()
            .log().all()
            .header("Content-Type", "application/json")
            .body(requestBody)
            .when()
            .post("/accounts")
            .then()
            .log().body()
            .statusCode(400)
            .extract()
            .asString();
    assertThat(response, containsString("already exists"));
}

// Zadanie 4 — Test integralności po serii transferów
//Cel: wykonaj kilka transferów między kontami (np. 3 różne przelewy) i sprawdź, że:
//Suma wszystkich środków (/accounts/totalBalance) pozostała taka sama.
//Salda kont zmieniły się poprawnie.
//Wskazówki:
//Możesz przechowywać saldo przed transferami w mapie (Map<Long, Double>).
//Po wszystkich przelewach porównaj sum(beforeBalances) == sum(afterBalances).

    @Test
    public void checkTransfersMethods() {

        // sprawdzenie sumy środków przed transferami
        double totalBefore =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/totalBalance")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .as(Double.class);

        // wykonanie 3 transferów
        // pierwszy przelew i sprawdzenie czy czy sold kont sie zgadza

        String requestBody = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 800
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer successful"));

        // sprawdzenie salda kont po transferze

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(200f));

        given()
                .log().all()
                .when()
                .get("/accounts/2")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(1300f));

        // drugi przelew i sprawdzenie czy czy sold kont sie zgadza

        String requestBody2 = """
                {
                "fromId": 2,
                "toId": 1,
                "amount": 1000
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody2)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer successful"));

        // sprawdzenie salda kont po transferze

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(1200f));

        given()
                .log().all()
                .when()
                .get("/accounts/2")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(300f));

        // trzeci transfer i sprawdzenie sold

        String requestBody3 = """
                {
                "fromId": 1,
                "toId": 2,
                "amount": 500
                }
                """;

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(requestBody3)
                .when()
                .post("/accounts/transfer")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Transfer successful"));

        // sprawdzenie salda kont po transferze

        given()
                .log().all()
                .when()
                .get("/accounts/1")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(700f));

        given()
                .log().all()
                .when()
                .get("/accounts/2")
                .then()
                .log().body()
                .statusCode(200)
                .body("balance", equalTo(800f));

        // sprawdzenie sumy sald po przelewach

        double totalAfter =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/totalBalance")
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .as(Double.class);

        assertThat(totalAfter, equalTo(totalBefore));
    }

    // Zadanie 6 — Serializacja i deserializacja obiektów
    //Cel:
    //Zamiast wysyłać body jako String, użyj bezpośrednio obiektu Account w metodzie .body(accountObject).
    //Utwórz new Account(5L, "Diana", 3000) w teście.
    //Wyślij go przez POST /accounts.
    //Pobierz /accounts/5 i zdeserializuj go do klasy Account.
    //Porównaj obiekty (np. assertThat(fetchedAccount.getOwner(), equalTo(account.getOwner()))).

    @Test
    public void deserializacjaObiektow() {

        Account newAccount = new Account(5L, "Diana", 3000.0);
        // dodanie nowego konta

        given()
                .log().all()
                .header("Content-Type", "application/json")
                .body(newAccount)
                .when()
                .post("/accounts")
                .then()
                .log().body()
                .statusCode(200)
                .body(equalTo("Account added"));

        // pobranie tego konta
        Account fetchedAccount =
                given()
                        .log().all()
                        .when()
                        .get("/accounts/" + newAccount.getId())
                        .then()
                        .log().body()
                        .statusCode(200)
                        .extract()
                        .as(Account.class);
        assertThat(fetchedAccount.getId(), equalTo(newAccount.getId()));
        assertThat(fetchedAccount.getOwner(), equalTo(newAccount.getOwner()));
        assertThat(fetchedAccount.getBalance(), equalTo(newAccount.getBalance()));
    }



    }





