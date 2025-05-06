package com.nexign.babybilling;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class ApiTests {

    private static final String BASE_URI = "http://localhost:8080"; //заглушка
    private static String authToken;

    @BeforeAll
    public static void setUp() {
        RestAssured.baseURI = BASE_URI;
        System.out.println("Setup completed. Using base URI: " + RestAssured.baseURI);

        Response loginResponse = given()
                .contentType("application/json")
                .body("{\"phone_number\": \"79991234567\", \"pin\": \"1234\"}")
                .when()
                .post("/login")
                .then()
                .extract().response();

        authToken = loginResponse.jsonPath().getString("token");
        System.out.println("Auth token received: " + authToken);
    }

    //CRM
    @Test
    public void testCrmLoginSuccess() {
        System.out.println("Starting test: CRM Login Success");

        Response response = given()
                .contentType("application/json")
                .body("{\"phone_number\": \"79991234567\", \"pin\": \"1234\"}")
                .when()
                .post("/login")
                .then()
                .extract().response();

        response.then()
                .statusCode(200)
                .body("token", equalTo("jwt_token"));

        System.out.println("CRM Login Success test passed. Response: " + response.asString());
    }

    @Test
    public void testCrmLoginInvalidPin() {
        System.out.println("Starting test: CRM Login Invalid PIN");

        Response response = given()
                .contentType("application/json")
                .body("{\"phone_number\": \"79991234567\", \"pin\": \"9999\"}")
                .when()
                .post("/login")
                .then()
                .extract().response();

        response.then()
                .statusCode(401)
                .body("error", equalTo("Неверные данные"));

        System.out.println("CRM Login Invalid PIN test passed. Response: " + response.asString());
    }

    @Test
    public void testCrmSubscriberRegistration() {
        System.out.println("Starting test: CRM Subscriber Registration");

        Response response = given()
                .contentType("application/json")
                .body("{\"phone_number\": \"79991234567\", \"full_name\": \"Ivan Ivanov\", \"pin\": \"1234\"}")
                .when()
                .post("/subscribers")
                .then()
                .extract().response();

        response.then()
                .statusCode(201)
                .body("message", equalTo("Subscriber created"));

        System.out.println("CRM Subscriber Registration test passed. Response: " + response.asString());
    }

    @Test
    public void testCrmSubscriberBalanceUpdate() {
        System.out.println("Starting test: CRM Subscriber Balance Update");

        Response response = given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + authToken)
                .body("{\"amount\": 50.0}")
                .when()
                .post("/subscribers/79991234567/balance")
                .then()
                .extract().response();

        response.then()
                .statusCode(200)
                .body("message", equalTo("Balance updated"));

        System.out.println("CRM Subscriber Balance Update test passed. Response: " + response.asString());
    }

    //HRS
    @Test
    public void testHrsCalculateIncomingCall() {
        System.out.println("Starting test: HRS Calculate Incoming Call");

        Response response = given()
                .contentType("application/json")
                .body("{\"callType\": \"01\", \"duration\": 120, \"callerNumber\": \"79991234567\"}")
                .when()
                .post("/calculate")
                .then()
                .extract().response();

        response.then()
                .statusCode(200)
                .body("totalCost", equalTo(0.0f));

        System.out.println("HRS Calculate Incoming Call test passed. Response: " + response.asString());
    }

    @Test
    public void testHrsCalculateOutgoingCall() {
        System.out.println("Starting test: HRS Calculate Outgoing Call");

        Response response = given()
                .contentType("application/json")
                .body("{\"callType\": \"02\", \"duration\": 120, \"callerNumber\": \"79991234567\"}")
                .when()
                .post("/calculate")
                .then()
                .extract().response();

        response.then()
                .statusCode(200)
                .body("totalCost", equalTo(3.0f));

        System.out.println("HRS Calculate Outgoing Call test passed. Response: " + response.asString());
    }

    //BRT
    @Test
    public void testBrtBillSuccess() {
        System.out.println("Starting test: BRT Bill Success");

        Response response = given()
                .contentType("application/json")
                .body("{\"callerNumber\": \"79991234567\", \"totalCost\": 3.0}")
                .when()
                .post("/bill")
                .then()
                .extract().response();

        response.then()
                .statusCode(200)
                .body("success", equalTo(true));

        System.out.println("BRT Bill Success test passed. Response: " + response.asString());
    }

    @Test
    public void testBrtBillInsufficientBalance() {
        System.out.println("Starting test: BRT Bill Insufficient Balance");

        Response response = given()
                .contentType("application/json")
                .body("{\"callerNumber\": \"79991234567\", \"totalCost\": 1000.0}")
                .when()
                .post("/bill")
                .then()
                .extract().response();

        response.then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("error", equalTo("Insufficient balance"));

        System.out.println("BRT Bill Insufficient Balance test passed. Response: " + response.asString());
    }
}