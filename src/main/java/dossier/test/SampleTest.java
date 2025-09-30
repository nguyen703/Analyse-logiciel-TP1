package dossier.test;

public class SampleTest {
    private int value;
    private String name;

    public SampleTest() {
        this.value = 0;
        this.name = "default";
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void printInfo() {
        System.out.println("Value: " + value + ", Name: " + name);
    }
}

class Helper {
    private double data;

    public Helper(double data) {
        this.data = data;
    }

    public double getData() {
        return data;
    }

    public void process(int x, int y) {
        data += x * y;
    }
}

