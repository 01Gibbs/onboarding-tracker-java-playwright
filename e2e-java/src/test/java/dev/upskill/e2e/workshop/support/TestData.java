package dev.upskill.e2e.workshop.support;

/** Lesson 05: unique values so tests can run repeatedly and in parallel without colliding. */
public final class TestData {

    private TestData() {
    }

    /** A short random fragment, e.g. "1a2b3c4d". Hint: java.util.UUID */
    public static String unique() {
        throw new UnsupportedOperationException("TODO lesson 05");
    }

    /** e.g. uniqueEmail("grace") -> "grace.1a2b3c4d@example.test" */
    public static String uniqueEmail(String prefix) {
        throw new UnsupportedOperationException("TODO lesson 05");
    }

    /** e.g. "Tester-1a2b3c4d" */
    public static String uniqueLastName() {
        throw new UnsupportedOperationException("TODO lesson 05");
    }
}
