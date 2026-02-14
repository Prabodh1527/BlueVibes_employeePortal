import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {

        try {

            String databaseUrl = System.getenv("DATABASE_URL");

            System.out.println("DATABASE_URL = " + databaseUrl);

            if (databaseUrl == null) {
                throw new RuntimeException("DATABASE_URL is NULL");
            }

            URI dbUri = new URI(databaseUrl);

            String username = dbUri.getUserInfo().split(":")[0];
            String password = dbUri.getUserInfo().split(":")[1];

            String jdbcUrl = "jdbc:postgresql://"
                    + dbUri.getHost()
                    + ":"
                    + dbUri.getPort()
                    + dbUri.getPath()
                    + "?sslmode=require";

            System.out.println("JDBC URL = " + jdbcUrl);

            Class.forName("org.postgresql.Driver");

            Connection con = DriverManager.getConnection(jdbcUrl, username, password);

            System.out.println("Connected Successfully");

            return con;

        } catch (Exception e) {

            System.out.println("DATABASE CONNECTION FAILED");
            e.printStackTrace();   // THIS IS IMPORTANT

            return null;
        }
    }
}
