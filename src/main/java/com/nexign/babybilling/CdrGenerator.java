package src.main.java.com.nexign.babybilling;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class CdrGenerator {

    private static final String OUTPUT_FILE = "src/test/resources/test_cdr_generated.csv";
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final Random random = new Random();

    public static void generateCdrFiles() {
        try (CSVWriter writer = new CSVWriter(new FileWriter(OUTPUT_FILE))) {
            String[] headers = {"callType", "callerNumber", "receiverNumber", "startTime", "endTime"};
            writer.writeNext(headers);

            System.out.println("Generating 10 positive CDR records...");
            for (int i = 0; i < 10; i++) {
                String[] cdr = generatePositiveCdr();
                writer.writeNext(cdr);
                System.out.println("Positive CDR #" + (i + 1) + ": " + String.join(", ", cdr));
            }

            System.out.println("Generating negative CDR records...");

            for (int i = 0; i < 5; i++) {
                String[] cdr = generateInvalidDateCdr();
                writer.writeNext(cdr);
                System.out.println("Negative CDR (Invalid Date) #" + (i + 1) + ": " + String.join(", ", cdr));
            }

            for (int i = 0; i < 5; i++) {
                String[] cdr = generateInvalidOperatorCdr();
                writer.writeNext(cdr);
                System.out.println("Negative CDR (Invalid Operator) #" + (i + 1) + ": " + String.join(", ", cdr));
            }

            for (int i = 0; i < 5; i++) {
                String[] cdr = generateInvalidNumberCdr();
                writer.writeNext(cdr);
                System.out.println("Negative CDR (Invalid Number) #" + (i + 1) + ": " + String.join(", ", cdr));
            }

            for (int i = 0; i < 5; i++) {
                String[] cdr = generateEmptyFieldsCdr();
                writer.writeNext(cdr);
                System.out.println("Negative CDR (Empty Fields) #" + (i + 1) + ": " + String.join(", ", cdr));
            }

            System.out.println("CDR generation completed. File saved to: " + OUTPUT_FILE);
        } catch (IOException e) {
            System.err.println("Error writing CDR file: " + e.getMessage());
        }
    }

    private static String[] generatePositiveCdr() {
        String callType = random.nextBoolean() ? "01" : "02";
        String callerNumber = "7" + String.format("%010d", random.nextInt(1000000000));
        String receiverNumber = "7" + String.format("%010d", random.nextInt(1000000000));
        LocalDateTime startTime = LocalDateTime.of(2025, 2, random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60));
        LocalDateTime endTime = startTime.plusMinutes(random.nextInt(5) + 1);
        return new String[]{
                callType,
                callerNumber,
                receiverNumber,
                startTime.format(DATE_TIME_FORMATTER),
                endTime.format(DATE_TIME_FORMATTER)
        };
    }

    private static String[] generateInvalidDateCdr() {
        String callType = random.nextBoolean() ? "01" : "02";
        String callerNumber = "7" + String.format("%010d", random.nextInt(1000000000));
        String receiverNumber = "7" + String.format("%010d", random.nextInt(1000000000));
        LocalDateTime endTime = LocalDateTime.of(2025, 2, random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60));
        LocalDateTime startTime = endTime.plusMinutes(random.nextInt(5) + 1);
        return new String[]{
                callType,
                callerNumber,
                receiverNumber,
                startTime.format(DATE_TIME_FORMATTER),
                endTime.format(DATE_TIME_FORMATTER)
        };
    }

    private static String[] generateInvalidOperatorCdr() {
        String callType = "02";
        String callerNumber = "7" + String.format("%010d", random.nextInt(1000000000));
        String receiverNumber = "operator" + random.nextInt(1000);
        LocalDateTime startTime = LocalDateTime.of(2025, 2, random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60));
        LocalDateTime endTime = startTime.plusMinutes(random.nextInt(5) + 1);
        return new String[]{
                callType,
                callerNumber,
                receiverNumber,
                startTime.format(DATE_TIME_FORMATTER),
                endTime.format(DATE_TIME_FORMATTER)
        };
    }

    private static String[] generateInvalidNumberCdr() {
        String callType = random.nextBoolean() ? "01" : "02";
        String callerNumber = "7" + String.format("%08d", random.nextInt(10000));
        String receiverNumber = "7abc" + random.nextInt(1000);
        LocalDateTime startTime = LocalDateTime.of(2025, 2, random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60));
        LocalDateTime endTime = startTime.plusMinutes(random.nextInt(5) + 1);
        return new String[]{
                callType,
                callerNumber,
                receiverNumber,
                startTime.format(DATE_TIME_FORMATTER),
                endTime.format(DATE_TIME_FORMATTER)
        };
    }

    private static String[] generateEmptyFieldsCdr() {
        String callType = "";
        String callerNumber = "";
        String receiverNumber = "";
        LocalDateTime startTime = LocalDateTime.of(2025, 2, random.nextInt(28) + 1, random.nextInt(24), random.nextInt(60));
        LocalDateTime endTime = startTime.plusMinutes(random.nextInt(5) + 1);
        return new String[]{
                callType,
                callerNumber,
                receiverNumber,
                startTime.format(DATE_TIME_FORMATTER),
                endTime.format(DATE_TIME_FORMATTER)
        };
    }

    public static void main(String[] args) {
        generateCdrFiles();
    }
}