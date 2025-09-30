package dossier.test;

public class MainTest {
    public static void main(String[] args) {
        System.out.println("=== Java Analyzer Test Program ===");

        // Create test runner
        TestRunner runner = new TestRunner();

        // Create sample test objects
        SampleTest test1 = new SampleTest();
        SampleTest test2 = new SampleTest();
        SampleTest test3 = new SampleTest();

        // Add tests to runner
        runner.addTestObject(test1);
        runner.addTestObject(test2);
        runner.addTestObject(test3);

        // Run the tests
        if (runner.hasTests()) {
            runner.runTests();
        }

        // Create additional helper instances
        Helper mathHelper = new Helper(50.0);
        Helper dataHelper = new Helper(0.0);

        // Perform some calculations
        mathHelper.process(10, 5);
        dataHelper.process(3, 7);

        System.out.println("Math helper result: " + mathHelper.getData());
        System.out.println("Data helper result: " + dataHelper.getData());

        // Display statistics
        System.out.println("Total test runners created: " + TestRunner.getTotalTests());

        // Clean up
        runner.clearTests();

        System.out.println("Test program completed successfully!");
    }
}
