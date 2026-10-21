import java.sql.*;

public class App {
    public static void main(String[] args) throws Exception {
        Connection con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/college", 
            "root", 
            "AniXDex");
        System.out.println("Connected to database");
        Statement stmt = con.createStatement();
        PreparedStatement ps = con.prepareStatement("INSERT INTO student (id, name, branch) VALUES (?, ?, ?)");
        ps.setInt(1, 5);
        ps.setString(2, "Sanjay");
        ps.setString(3, "CSE");
        int rowsAffected = ps.executeUpdate();
        System.out.println("Rows affected: " + rowsAffected);
        con.close();
    }
}
