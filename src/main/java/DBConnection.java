import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;

        try {
            String databaseUrl = System.getenv("DATABASE_URL");

            if (databaseUrl == null || databaseUrl.isEmpty()) {
                System.out.println("DATABASE_URL is not set!");
                return null;
            }

            // Convert postgresql:// to jdbc:postgresql://
            databaseUrl = databaseUrl.replace("postgresql://", "jdbc:postgresql://");

            Class.forName("org.postgresql.Driver");
            con = DriverManager.getConnection(databaseUrl);

            System.out.println("PostgreSQL CONNECTED SUCCESSFULLY");

        } catch (Exception e) {
            System.out.println("DB CONNECTION FAILED");
            e.printStackTrace();
        }

        return con;
    }
}
