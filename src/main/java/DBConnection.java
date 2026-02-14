import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        try {

            String databaseUrl = System.getenv("DATABASE_URL");

            System.out.println("=== DEBUG DATABASE_URL ===");
            System.out.println(databaseUrl);
            System.out.println("=== END DEBUG ===");

            if (databaseUrl == null) {
                throw new RuntimeException("DATABASE_URL is NULL");
            }

            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(databaseUrl);

            System.out.println("CONNECTED SUCCESSFULLY");

            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
