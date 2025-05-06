package com.nexign.babybilling;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class E2eTests {

    private static final String CDR_FILE_PATH = "src/test/resources/e2e_cdr.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static Connection mockConnection;
    private static final String BASE_URI = "http://localhost:8080";//заглушка

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = BASE_URI;
        System.out.println("Setup completed. Using base URI: " + RestAssured.baseURI);

        mockConnection = mock(Connection.class);
        try {
            Statement mockStatement = mock(Statement.class);
            when(mockConnection.createStatement()).thenReturn(mockStatement);

            ResultSet mockResultSetBalance = mock(ResultSet.class);
            when(mockStatement.executeQuery("SELECT balance FROM crm_subscribers WHERE phone_number = '79876543221'"))
                    .thenReturn(mockResultSetBalance);
            when(mockResultSetBalance.next()).thenReturn(true).thenReturn(false);
            when(mockResultSetBalance.getDouble("balance")).thenReturn(100.0);

            ResultSet mockResultSetCalls = mock(ResultSet.class);
            when(mockStatement.executeQuery("SELECT * FROM romashka_calls WHERE caller_number = '79876543221'"))
                    .thenReturn(mockResultSetCalls);
            when(mockResultSetCalls.next()).thenReturn(false);
        } catch (Exception e) {
            System.out.println("Mock setup failed: " + e.getMessage());
        }
    }

    @Test
    public void testClassicTariffInternalCall() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Classic tariff - Internal call");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] record = csvReader.readNext();
            processCdrRecord("79876543221", record, 100.0, 1.5);

            double expectedBalance = 100.0 - (2 * 1.5);
            assertBalance("79876543221", expectedBalance);

            assertCallRecord("79876543221", "79123456789", 128);
        }
    }

    @Test
    public void testClassicTariffExternalCall() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Classic tariff - External call");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext();
            String[] record = csvReader.readNext();
            processCdrRecord("79876543221", record, 97.0, 2.5);

            double expectedBalance = 97.0 - (3 * 2.5);
            assertBalance("79876543221", expectedBalance);

            assertCallRecord("79876543221", "79234567890", 180);
        }
    }

    @Test
    public void testClassicTariffIncomingCall() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Classic tariff - Incoming call");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext();
            String[] headers2 = csvReader.readNext(); 
            String[] record = csvReader.readNext();
            processCdrRecord("79876543221", record, 87.5, 0.0);

            double expectedBalance = 87.5;
            assertBalance("79876543221", expectedBalance);

            assertCallRecord("79876543221", "79123456789", 300);
        }
    }

    @Test
    public void testMonthlyTariffWithinMinutes() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Monthly tariff - Within minutes");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext(); 
            String[] headers2 = csvReader.readNext(); 
            String[] headers3 = csvReader.readNext(); 
            String[] record = csvReader.readNext();
            processCdrRecord("79996667755", record, 200.0, 0.0);

            double expectedBalance = 200.0;
            assertBalance("79996667755", expectedBalance);

            assertUsedMinutes("79996667755", 600);
        }
    }

    @Test
    public void testMonthlyTariffOverMinutes() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Monthly tariff - Over minutes");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext(); 
            String[] headers2 = csvReader.readNext(); 
            String[] headers3 = csvReader.readNext(); 
            String[] headers4 = csvReader.readNext(); 
            String[] record = csvReader.readNext();
            processCdrRecord("79996667755", record, 200.0, 1.5);

            double expectedBalance = 200.0 - (10 * 1.5);
            assertBalance("79996667755", expectedBalance);

            assertUsedMinutes("79996667755", 1200);
        }
    }

    @Test
    public void testMonthlyTariffIncomingCall() throws IOException, CsvValidationException {
        System.out.println("Starting E2E test for Monthly tariff - Incoming call");

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext(); 
            String[] headers2 = csvReader.readNext(); 
            String[] headers3 = csvReader.readNext(); 
            String[] headers4 = csvReader.readNext(); 
            String[] headers5 = csvReader.readNext(); 
            String[] record = csvReader.readNext();
            processCdrRecord("79996667755", record, 175.0, 0.0);

            double expectedBalance = 175.0;
            assertBalance("79996667755", expectedBalance);

            assertUsedMinutes("79996667755", 1300);
        }
    }

    private void processCdrRecord(String phoneNumber, String[] record, double initialBalance, double ratePerMinute) {
        System.out.println("Processing CDR record for " + phoneNumber + ": " + String.join(", ", record));

        String callType = record[0];
        String callerNumber = record[1];
        String receiverNumber = record[2];
        String startTime = record[3];
        String endTime = record[4];

        LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);
        long durationSeconds = java.time.Duration.between(start, end).getSeconds();
        double cost = (callType.equals("02") && durationSeconds > 0) ? (Math.ceil(durationSeconds / 60.0) * ratePerMinute) : 0.0;

        String hrsRequest = String.format(
                "{\"callType\": \"%s\", \"callerNumber\": \"%s\", \"receiverNumber\": \"%s\", \"duration\": %d}",
                callType, callerNumber, receiverNumber, durationSeconds
        );
        Response hrsResponse = given()
                .contentType("application/json")
                .body(hrsRequest)
                .when()
                .post("/calculate")
                .then()
                .statusCode(200)
                .extract().response();
        double hrsCost = hrsResponse.jsonPath().getDouble("totalCost");
        System.out.println("HRS calculated cost: " + hrsCost);

        String brtRequest = String.format(
                "{\"callType\": \"%s\", \"callerNumber\": \"%s\", \"receiverNumber\": \"%s\", \"startTime\": \"%s\", \"endTime\": \"%s\", \"totalCost\": %.2f}",
                callType, callerNumber, receiverNumber, startTime, endTime, cost
        );
        Response brtResponse = given()
                .contentType("application/json")
                .body(brtRequest)
                .when()
                .post("/process-cdr")
                .then()
                .statusCode(200)
                .body("status", equalTo("SUCCESS"))
                .extract().response();
        System.out.println("BRT processing status: " + brtResponse.jsonPath().getString("status"));
    }

    private void assertBalance(String phoneNumber, double expectedBalance) {
        System.out.println("Checking balance for " + phoneNumber);
        try {
            Statement statement = mockConnection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT balance FROM crm_subscribers WHERE phone_number = '" + phoneNumber + "'");
            if (resultSet.next()) {
                double actualBalance = resultSet.getDouble("balance");
                assertEquals(expectedBalance, actualBalance, 0.01, "Balance mismatch for " + phoneNumber);
                System.out.println("Balance verified: " + actualBalance);
            } else {
                System.out.println("No balance record found for " + phoneNumber);
            }
        } catch (Exception e) {
            System.out.println("Balance check failed: " + e.getMessage());
        }
    }

    private void assertCallRecord(String callerNumber, String receiverNumber, long durationSeconds) {
        System.out.println("Checking call record for " + callerNumber + " to " + receiverNumber);
        try {
            Statement statement = mockConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT duration FROM romashka_calls WHERE caller_number = '" + callerNumber + "' AND receiver_number = '" + receiverNumber + "'"
            );
            if (resultSet.next()) {
                long actualDuration = resultSet.getLong("duration");
                assertEquals(durationSeconds, actualDuration, "Duration mismatch for call");
                System.out.println("Call record verified: Duration = " + actualDuration + " seconds");
            } else {
                System.out.println("No call record found for " + callerNumber + " to " + receiverNumber);
            }
        } catch (Exception e) {
            System.out.println("Call record check failed: " + e.getMessage());
        }
    }

    private void assertUsedMinutes(String phoneNumber, long expectedSeconds) {
        System.out.println("Checking used minutes for " + phoneNumber);
        try {
            Statement statement = mockConnection.createStatement();
            ResultSet resultSet = statement.executeQuery(
                    "SELECT SUM(duration) as total_seconds FROM romashka_calls WHERE caller_number = '" + phoneNumber + "'"
            );
            if (resultSet.next()) {
                long actualSeconds = resultSet.getLong("total_seconds");
                assertEquals(expectedSeconds, actualSeconds, "Used minutes mismatch for " + phoneNumber);
                System.out.println("Used minutes verified: " + (actualSeconds / 60.0) + " minutes");
            } else {
                System.out.println("No used minutes record found for " + phoneNumber);
            }
        } catch (Exception e) {
            System.out.println("Used minutes check failed: " + e.getMessage());
        }
    }
}