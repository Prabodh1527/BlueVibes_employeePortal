import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        try {

            String databaseUrl = System.getenv("DATABASE_URL");

            if (databaseUrl == null) {
                throw new RuntimeException("DATABASE_URL is NULL");
            }

            URI uri = new URI(databaseUrl);

            String username = uri.getUserInfo().split(":")[0];
            String password = uri.getUserInfo().split(":")[1];

            String jdbcUrl = "jdbc:postgresql://"
                    + uri.getHost()
                    + ":"
                    + uri.getPort()
                    + uri.getPath()
                    + "?sslmode=require";

            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(jdbcUrl, username, password);

            System.out.println("PostgreSQL Connected");

            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
