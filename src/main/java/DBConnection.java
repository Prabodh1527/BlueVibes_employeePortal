import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {

            String databaseUrl = System.getenv("DATABASE_URL");

            if (databaseUrl == null || databaseUrl.isEmpty()) {
                System.out.println("DATABASE_URL not set");
                return null;
            }

            // Convert for JDBC
            databaseUrl = databaseUrl.replace("postgres://", "jdbc:postgresql://");

            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(databaseUrl);

            System.out.println("Connected to PostgreSQL");

            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
