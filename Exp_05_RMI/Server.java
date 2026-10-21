import java.rmi.Naming;
public class Server {

    public static void main(String[] args) {
        try {
            CalculatorImpl obj = new CalculatorImpl();
            Naming.rebind("rmi://localhost/Calculator", obj);
            System.out.println("RMI service started...");
        } catch (Exception e) {
           System.out.println(e);
        }
    }
}
