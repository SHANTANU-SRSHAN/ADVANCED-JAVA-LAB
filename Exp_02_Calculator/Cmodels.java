package Calculator;

public class Cmodels {
    public double add(double a, double b) {
        return a + b;
    }

    public double subtract(double a, double b) {
        return a - b;
    }

    public double multiply(double a, double b) {
        return a * b;
    }

    public double divide(double a, double b) {
        if (b == 0) {
            return 0;
        }
        return a / b;
    }

    public double percentage(double a, double b) {
        return (a + b) / 100;
    }
}
