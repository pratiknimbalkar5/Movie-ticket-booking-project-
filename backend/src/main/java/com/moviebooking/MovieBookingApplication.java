package com.moviebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Entry point of the Movie Ticket Booking System.
 */
@SpringBootApplication
public class MovieBookingApplication {

    static {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");
        System.setProperty("https.protocols", "TLSv1.2,TLSv1.3");
        loadDotenvIfPresent();
        SpringApplication.run(MovieBookingApplication.class, args);
    }

    /**
     * Automatically loads root or current directory .env file into Java System Properties
     * without requiring any third-party dotenv dependencies.
     */
    private static void loadDotenvIfPresent() {
        Path[] candidatePaths = new Path[]{
                Paths.get(".env"),
                Paths.get("../.env"),
                Paths.get("../../.env")
        };

        for (Path envPath : candidatePaths) {
            if (Files.isRegularFile(envPath)) {
                try (BufferedReader reader = Files.newBufferedReader(envPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();

                            // Strip optional wrapping quotes
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                if (value.length() >= 2) {
                                    value = value.substring(1, value.length() - 1);
                                }
                            }

                            // Only set if not already defined in OS environment or system properties
                            if (System.getenv(key) == null && System.getProperty(key) == null && !value.isBlank()) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    break;
                } catch (Exception ignored) {
                }
            }
        }
    }
}
