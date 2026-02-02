import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;
        try {
            String url = "jdbc:mysql://sql12.freesqldatabase.com:3306/sql12813830?useSSL=false&serverTimezone=UTC";
            String user = "sql12813830";
            String pass = "7aU9wdUTF5";

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, pass);

            System.out.println("DB CONNECTED SUCCESSFULLY");

        } catch (Exception e) {
            System.out.println("DB CONNECTION FAILED");
            e.printStackTrace();
        }
        return con;
    }
}
