package com.nexign.babybilling;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.Test;
import src.main.java.com.nexign.babybilling.CdrGenerator;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CdrGeneratorTest {

    private static final String TEST_FILE = "src/test/resources/test_cdr_generated.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Test
    public void testCdrGeneration() throws IOException {
        System.out.println("Starting test for CDR generation...");

        CdrGenerator.generateCdrFiles();

        List<String[]> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(TEST_FILE))) {
            String[] headers = csvReader.readNext();
            String[] record;
            while ((record = csvReader.readNext()) != null) {
                records.add(record);
                System.out.println("Read record: " + String.join(", ", record));
            }
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }

        assertEquals(35, records.size(), "Expected 10 positive + 5*5 negative = 35 records");
        System.out.println("Total records verified: " + records.size());

        for (int i = 0; i < 10; i++) {
            String[] cdr = records.get(i);
            assertEquals(5, cdr.length, "Each record should have 5 fields");
            assertTrue(cdr[1].matches("7\\d{10}"), "Caller number should be 11 digits starting with 7");
            assertTrue(cdr[2].matches("7\\d{10}"), "Receiver number should be 11 digits starting with 7");
            assertTrue(cdr[0].matches("01|02"), "Call type should be 01 or 02");
            validateDateOrder(cdr[3], cdr[4]);
        }
        System.out.println("Positive records validated successfully.");

        int negativeIndex = 10;
        for (int i = 0; i < 5; i++) {
            String[] cdr = records.get(negativeIndex + i);
            validateInvalidDate(cdr[3], cdr[4]);
        }
        System.out.println("Invalid date records validated.");

        negativeIndex += 5;
        for (int i = 0; i < 5; i++) {
            String[] cdr = records.get(negativeIndex + i);
            assertTrue(!cdr[2].matches("\\d{11}"), "Receiver number should not be valid");
        }
        System.out.println("Invalid operator records validated.");

        negativeIndex += 5;
        for (int i = 0; i < 5; i++) {
            String[] cdr = records.get(negativeIndex + i);
            assertTrue(!cdr[1].matches("\\d{11}") || !cdr[2].matches("\\d{11}"), "Numbers should be invalid");
        }
        System.out.println("Invalid number records validated.");

        negativeIndex += 5;
        for (int i = 0; i < 5; i++) {
            String[] cdr = records.get(negativeIndex + i);
            assertTrue(cdr[0].isEmpty() || cdr[1].isEmpty() || cdr[2].isEmpty(), "Some fields should be empty");
        }
        System.out.println("Empty fields records validated.");
    }

    private void validateDateOrder(String startTimeStr, String endTimeStr) {
        LocalDateTime start = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
        LocalDateTime end = LocalDateTime.parse(endTimeStr, DATE_TIME_FORMATTER);
        assertTrue(!end.isBefore(start), "endTime should be after startTime");
    }

    private void validateInvalidDate(String startTimeStr, String endTimeStr) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTimeStr, DATE_TIME_FORMATTER);
            LocalDateTime end = LocalDateTime.parse(endTimeStr, DATE_TIME_FORMATTER);
            assertTrue(end.isBefore(start), "endTime should be before startTime for invalid date test");
        } catch (Exception e) {
            System.out.println("Invalid date format detected as expected: " + e.getMessage());
        }
    }
}