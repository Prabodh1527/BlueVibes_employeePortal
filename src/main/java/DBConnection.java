import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        try {

            String databaseUrl = System.getenv("DATABASE_URL");

            if (databaseUrl == null) {
                System.out.println("DATABASE_URL is null");
                return null;
            }

            URI dbUri = new URI(databaseUrl);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];
            String jdbcUrl = "jdbc:postgresql://" +
                    dbUri.getHost() +
                    ":" +
                    dbUri.getPort() +
                    dbUri.getPath();

            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(jdbcUrl, username, password);

            System.out.println("PostgreSQL Connected Successfully");

            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
