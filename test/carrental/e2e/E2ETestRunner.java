package carrental.e2e;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Main E2E Acceptance Test Runner for Group 10 Car Rental System.
 * Executes Tiers 1-4, tracks total assertions, reports detailed metrics,
 * and enforces strict exit codes (0 for success, 1 for failure).
 */
public class E2ETestRunner {

    public static class TestResult {
        public final String suiteName;
        public final String testName;
        public final boolean passed;
        public final long durationMs;
        public final Throwable failure;

        public TestResult(String suiteName, String testName, boolean passed, long durationMs, Throwable failure) {
            this.suiteName = suiteName;
            this.testName = testName;
            this.passed = passed;
            this.durationMs = durationMs;
            this.failure = failure;
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("           GROUP 10 CAR RENTAL SYSTEM - PURE JAVA SE E2E TEST RUNNER            ");
        System.out.println("          Testing Track: Tiers 1-4 Acceptance & Specification Verification       ");
        System.out.println("================================================================================");

        long globalStartTime = System.currentTimeMillis();
        Assertions.resetAssertionCount();

        List<TestResult> allResults = new ArrayList<>();
        boolean allPassed = true;

        Class<?>[] testSuites = new Class<?>[]{
                Tier1FeatureTests.class,
                Tier2BoundaryTests.class,
                Tier3CombinationTests.class,
                Tier4ScenarioTests.class
        };

        for (Class<?> suiteClass : testSuites) {
            String suiteName = suiteClass.getSimpleName();
            System.out.println();
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println(">> Running Test Suite: " + suiteName);
            System.out.println("--------------------------------------------------------------------------------");

            Object suiteInstance;
            try {
                suiteInstance = suiteClass.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to instantiate suite " + suiteName + ": " + e.getMessage());
                allPassed = false;
                continue;
            }

            Method[] methods = suiteClass.getDeclaredMethods();
            List<Method> testMethods = new ArrayList<>();
            for (Method m : methods) {
                if (m.getName().startsWith("test") && Modifier.isPublic(m.getModifiers()) && m.getParameterCount() == 0) {
                    testMethods.add(m);
                }
            }

            // Sort methods alphabetically for deterministic execution order
            testMethods.sort((m1, m2) -> m1.getName().compareTo(m2.getName()));

            int suitePassed = 0;
            int suiteFailed = 0;
            long suiteStartTime = System.currentTimeMillis();

            for (Method testMethod : testMethods) {
                String methodName = testMethod.getName();
                long start = System.currentTimeMillis();
                try {
                    testMethod.invoke(suiteInstance);
                    long duration = System.currentTimeMillis() - start;
                    suitePassed++;
                    allResults.add(new TestResult(suiteName, methodName, true, duration, null));
                    System.out.printf("  [PASS] %-48s (%d ms)%n", methodName, duration);
                } catch (Throwable t) {
                    long duration = System.currentTimeMillis() - start;
                    suiteFailed++;
                    allPassed = false;
                    Throwable actualCause = t.getCause() != null ? t.getCause() : t;
                    allResults.add(new TestResult(suiteName, methodName, false, duration, actualCause));
                    System.out.printf("  [FAIL] %-48s (%d ms)%n", methodName, duration);
                    System.err.println("         Reason: " + actualCause.getMessage());
                }
            }

            long suiteDuration = System.currentTimeMillis() - suiteStartTime;
            System.out.printf(">> Suite %s Finished: %d Passed, %d Failed (Duration: %d ms)%n",
                    suiteName, suitePassed, suiteFailed, suiteDuration);
        }

        long globalDuration = System.currentTimeMillis() - globalStartTime;
        int totalTests = allResults.size();
        int totalPassed = 0;
        int totalFailed = 0;
        List<TestResult> failedTests = new ArrayList<>();

        for (TestResult r : allResults) {
            if (r.passed) {
                totalPassed++;
            } else {
                totalFailed++;
                failedTests.add(r);
            }
        }

        System.out.println();
        System.out.println("================================================================================");
        System.out.println("                              E2E TEST RUN SUMMARY                              ");
        System.out.println("================================================================================");
        System.out.printf("Total Test Suites Executed : %d%n", testSuites.length);
        System.out.printf("Total Test Methods Run     : %d%n", totalTests);
        System.out.printf("Total Assertions Evaluated : %d%n", Assertions.getAssertionCount());
        System.out.printf("Total Passed               : %d%n", totalPassed);
        System.out.printf("Total Failed               : %d%n", totalFailed);
        System.out.printf("Total Elapsed Time         : %d ms (%.2f s)%n", globalDuration, globalDuration / 1000.0);
        System.out.println("--------------------------------------------------------------------------------");

        if (totalFailed > 0) {
            System.err.println("FAILED TESTS DETAILS:");
            for (TestResult f : failedTests) {
                System.err.printf("  - [%s] %s%n", f.suiteName, f.testName);
                if (f.failure != null) {
                    System.err.printf("    Error: %s%n", f.failure.getMessage());
                    f.failure.printStackTrace(System.err);
                }
            }
            System.out.println("================================================================================");
            System.out.println("[RESULT: FAILED] Some tests failed. Please review errors above.");
            System.out.println("================================================================================");
            System.exit(1);
        } else {
            System.out.println("[RESULT: SUCCESS] 100% of all E2E Acceptance Tests PASSED!");
            System.out.println("System verified compliant with Course Syllabus Units 1-5 & Project Specifications.");
            System.out.println("================================================================================");
            System.exit(0);
        }
    }
}
