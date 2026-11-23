package carrental.e2e;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pure Java SE lightweight assertion library (zero external test dependencies).
 * Enforces strict verification of contracts, boundaries, and failure invariants.
 */
public final class Assertions {

    private static final AtomicInteger assertionCount = new AtomicInteger(0);

    private Assertions() {
        // Utility class, do not instantiate
    }

    @FunctionalInterface
    public interface Executable {
        void execute() throws Throwable;
    }

    public static int getAssertionCount() {
        return assertionCount.get();
    }

    public static void resetAssertionCount() {
        assertionCount.set(0);
    }

    private static void recordAssertion() {
        assertionCount.incrementAndGet();
    }

    public static void assertTrue(boolean condition, String message) {
        recordAssertion();
        if (!condition) {
            fail(message != null ? message : "Expected condition to be TRUE, but was FALSE.");
        }
    }

    public static void assertTrue(boolean condition) {
        assertTrue(condition, null);
    }

    public static void assertFalse(boolean condition, String message) {
        recordAssertion();
        if (condition) {
            fail(message != null ? message : "Expected condition to be FALSE, but was TRUE.");
        }
    }

    public static void assertFalse(boolean condition) {
        assertFalse(condition, null);
    }

    public static void assertEquals(Object expected, Object actual, String message) {
        recordAssertion();
        if (!Objects.equals(expected, actual)) {
            String msg = (message != null ? message + " - " : "") +
                    "Expected: [" + expected + "], but got: [" + actual + "]";
            fail(msg);
        }
    }

    public static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(int expected, int actual, String message) {
        recordAssertion();
        if (expected != actual) {
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + ">, but got: <" + actual + ">";
            fail(msg);
        }
    }

    public static void assertEquals(int expected, int actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(long expected, long actual, String message) {
        recordAssertion();
        if (expected != actual) {
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + "L>, but got: <" + actual + "L>";
            fail(msg);
        }
    }

    public static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, null);
    }

    public static void assertEquals(double expected, double actual, double delta, String message) {
        recordAssertion();
        if (Double.isNaN(expected) || Double.isNaN(actual)) {
            if (Double.isNaN(expected) && Double.isNaN(actual)) {
                return;
            }
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + "> (delta=" + delta + "), but got: <" + actual + ">";
            fail(msg);
            return;
        }
        if (Double.isInfinite(expected) || Double.isInfinite(actual)) {
            if (expected == actual) {
                return;
            }
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + "> (delta=" + delta + "), but got: <" + actual + ">";
            fail(msg);
            return;
        }
        if (Math.abs(expected - actual) > delta) {
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + "> (delta=" + delta + "), but got: <" + actual + ">";
            fail(msg);
        }
    }

    public static void assertEquals(double expected, double actual, double delta) {
        assertEquals(expected, actual, delta, null);
    }

    public static void assertEquals(boolean expected, boolean actual, String message) {
        recordAssertion();
        if (expected != actual) {
            String msg = (message != null ? message + " - " : "") +
                    "Expected: <" + expected + ">, but got: <" + actual + ">";
            fail(msg);
        }
    }

    public static void assertNotNull(Object actual, String message) {
        recordAssertion();
        if (actual == null) {
            fail(message != null ? message : "Expected non-null reference, but was NULL.");
        }
    }

    public static void assertNotNull(Object actual) {
        assertNotNull(actual, null);
    }

    public static void assertNull(Object actual, String message) {
        recordAssertion();
        if (actual != null) {
            fail(message != null ? message : "Expected null reference, but got: [" + actual + "]");
        }
    }

    public static void assertNull(Object actual) {
        assertNull(actual, null);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable, String message) {
        recordAssertion();
        if (expectedType == null) {
            throw new IllegalArgumentException("expectedType class must not be null");
        }
        if (executable == null) {
            throw new IllegalArgumentException("executable must not be null");
        }
        try {
            executable.execute();
        } catch (Throwable actualThrown) {
            if (expectedType.isInstance(actualThrown)) {
                return (T) actualThrown;
            } else {
                String msg = (message != null ? message + " - " : "") +
                        "Unexpected exception type thrown. Expected: " + expectedType.getName() +
                        ", but caught: " + actualThrown.getClass().getName() + " (" + actualThrown.getMessage() + ")";
                throw new AssertionError(msg, actualThrown);
            }
        }
        String msg = (message != null ? message + " - " : "") +
                "Expected " + expectedType.getName() + " to be thrown, but nothing was thrown.";
        throw new AssertionError(msg);
    }

    public static <T extends Throwable> T assertThrows(Class<T> expectedType, Executable executable) {
        return assertThrows(expectedType, executable, null);
    }

    public static void fail(String message) {
        throw new AssertionError(message != null ? message : "Assertion failed.");
    }

    public static void fail(String message, Throwable cause) {
        throw new AssertionError(message != null ? message : "Assertion failed.", cause);
    }
}
