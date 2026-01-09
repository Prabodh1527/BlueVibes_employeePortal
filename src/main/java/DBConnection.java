import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection() {
        Connection con = null;
        try {
            String host = System.getenv("DB_HOST");
            String db   = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");
            String port = System.getenv("DB_PORT");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + db +
                         "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);

            if(con != null) {
                System.out.println("Successfully connected to the live database!");
            }
        } catch (Exception e) {
            System.out.println("Connection Failed! Error: " + e.getMessage());
            e.printStackTrace();
        }
        return con;
    }
}
