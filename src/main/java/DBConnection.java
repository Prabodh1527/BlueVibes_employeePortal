import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {

            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String db   = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");

            if (host == null || port == null || db == null || user == null || pass == null) {
                throw new RuntimeException("One or more DB environment variables are missing.");
            }

            String jdbcUrl = "jdbc:postgresql://"
                    + host + ":" + port + "/" + db
                    + "?sslmode=require";

            Class.forName("org.postgresql.Driver");

            return DriverManager.getConnection(jdbcUrl, user, pass);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
