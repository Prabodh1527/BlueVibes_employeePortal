import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;

        try {
            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String database = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");

            String url = "jdbc:mysql://" + host + ":" + port + "/" + database
                    + "?useSSL=true"
                    + "&requireSSL=true"
                    + "&verifyServerCertificate=false"
                    + "&allowPublicKeyRetrieval=true"
                    + "&serverTimezone=UTC";

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
