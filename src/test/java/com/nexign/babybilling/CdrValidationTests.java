package com.nexign.babybilling;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CdrValidationTests {

    private static final String CDR_FILE_PATH = "src/test/resources/week2_cdr.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @BeforeAll
    public static void setUp() {
        // Установка базового URL (заглушка, заменить на реальный адрес BRT)
        RestAssured.baseURI = "http://localhost:8080";
        System.out.println("Setup completed. Using base URI: " + RestAssured.baseURI);
    }

    @Test
    public void testCdrValidationFullBatch() throws IOException, CsvValidationException {
        System.out.println("Starting CDR validation test for all records in " + CDR_FILE_PATH);

        try (CSVReader csvReader = new CSVReader(new FileReader(CDR_FILE_PATH))) {
            String[] headers = csvReader.readNext(); // Пропускаем заголовки, если они есть (в данном случае их нет)
            String[] record;
            int recordNumber = 0;

            while ((record = csvReader.readNext()) != null) {
                recordNumber++;
                System.out.println("Processing record #" + recordNumber + ": " + String.join(", ", record));

                String callType = record[0];
                String callerNumber = record[1];
                String receiverNumber = record[2];
                String startTime = record[3];
                String endTime = record[4];

                // Формирование JSON-запроса для отправки в BRT
                String requestBody = String.format(
                        "{\"callType\": \"%s\", \"callerNumber\": \"%s\", \"receiverNumber\": \"%s\", " +
                                "\"startTime\": \"%s\", \"endTime\": \"%s\"}",
                        callType, callerNumber, receiverNumber, startTime, endTime
                );

                // Эмуляция отправки данных коммутатором через API
                Response response = given()
                        .contentType("application/json")
                        .body(requestBody)
                        .when()
                        .post("/process-cdr") // Предполагаемый эндпоинт для валидации CDR
                        .then()
                        .extract().response();

                // Вывод ответа для отладки
                System.out.println("Response for record #" + recordNumber + ": " + response.asString());

                // Валидация ответа BRT
                validateCdrResponse(recordNumber, callType, callerNumber, receiverNumber, startTime, endTime, response);
            }

            System.out.println("CDR validation test completed for all " + recordNumber + " records.");
        }
    }

    private void validateCdrResponse(int recordNumber, String callType, String callerNumber, String receiverNumber,
                                     String startTime, String endTime, Response response) {
        System.out.println("Validating response for record #" + recordNumber);

        try {
            // Парсинг дат для проверки
            LocalDateTime start = LocalDateTime.parse(startTime, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTime, DATE_TIME_FORMATTER);

            // Правила валидации
            boolean isCallerNumberValid = callerNumber.matches("\\d{11}"); // Номер должен быть 11 цифр
            boolean isReceiverNumberValid = receiverNumber.matches("\\d{11}"); // Номер должен быть 11 цифр
            boolean isTimeValid = !end.isBefore(start); // endTime должен быть больше startTime
            boolean isDateFormatValid = true; // Предполагаем, что формат валиден, если парсинг прошёл

            if (isCallerNumberValid && isReceiverNumberValid && isTimeValid && isDateFormatValid) {
                // Позитивный сценарий: запись валидна
                response.then()
                        .statusCode(200)
                        .body("status", equalTo("SUCCESS"))
                        .body("message", equalTo("CDR record processed successfully"));
                System.out.println("Record #" + recordNumber + " validated successfully.");
            } else {
                // Негативный сценарий: запись невалидна
                response.then()
                        .statusCode(400)
                        .body("status", equalTo("FAILED"));

                if (!isCallerNumberValid) {
                    response.then().body("error", equalTo("Invalid caller number format"));
                    System.out.println("Record #" + recordNumber + " failed: Invalid caller number (" + callerNumber + ").");
                } else if (!isReceiverNumberValid) {
                    response.then().body("error", equalTo("Invalid receiver number format"));
                    System.out.println("Record #" + recordNumber + " failed: Invalid receiver number (" + receiverNumber + ").");
                } else if (!isTimeValid) {
                    response.then().body("error", equalTo("end_time is before start_time"));
                    System.out.println("Record #" + recordNumber + " failed: end_time < start_time.");
                } else if (!isDateFormatValid) {
                    response.then().body("error", equalTo("Invalid date format"));
                    System.out.println("Record #" + recordNumber + " failed: Invalid date format.");
                }
            }
        } catch (Exception e) {
            // Обработка ошибки парсинга даты
            response.then()
                    .statusCode(400)
                    .body("status", equalTo("FAILED"))
                    .body("error", equalTo("Invalid date format"));
            System.out.println("Record #" + recordNumber + " failed: Invalid date format due to exception: " + e.getMessage());
        }
    }
}