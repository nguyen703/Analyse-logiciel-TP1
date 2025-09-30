package dossier.test;

import java.util.ArrayList;
import java.util.List;

public class TestRunner {
    private List<SampleTest> testObjects;
    private Helper calculator;
    private static int totalTests = 0;

    public TestRunner() {
        this.testObjects = new ArrayList<>();
        this.calculator = new Helper(100.0);
        totalTests++;
    }

    public void addTestObject(SampleTest test) {
        testObjects.add(test);
    }

    public void runTests() {
        System.out.println("Running " + testObjects.size() + " tests...");

        for (int i = 0; i < testObjects.size(); i++) {
            SampleTest test = testObjects.get(i);
            test.setValue(i * 10);
            test.printInfo();

            // Use helper to calculate something
            calculator.process(test.getValue(), 2);
        }

        System.out.println("Final calculator data: " + calculator.getData());
    }

    public static int getTotalTests() {
        return totalTests;
    }

    public boolean hasTests() {
        return !testObjects.isEmpty();
    }

    public void clearTests() {
        testObjects.clear();
    }
}
