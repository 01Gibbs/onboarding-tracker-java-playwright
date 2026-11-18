package dev.upskill.e2e.solutions.support;

import java.util.UUID;

/**
 * Unique values so tests never collide with each other or with the seeded data,
 * even when run in parallel or repeatedly without restarting the app.
 */
public final class TestData {

    private TestData() {
    }

    public static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String uniqueEmail(String prefix) {
        return prefix + "." + unique() + "@example.test";
    }

    /** A last name that is easy to search for and obviously test-generated, e.g. "Tester-1a2b3c4d". */
    public static String uniqueLastName() {
        return "Tester-" + unique();
    }
}
