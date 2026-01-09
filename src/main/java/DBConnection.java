import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public static Connection getConnection() {
        Connection con = null;
        try {
            String host = System.getenv("DB_HOST");
            String db   = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASSWORD");
            String port = System.getenv("DB_PORT");

            String url =
                "jdbc:mysql://" + host + ":" + port + "/" + db +
                "?useSSL=true" +
                "&requireSSL=true" +
                "&allowPublicKeyRetrieval=true" +
                "&useUnicode=true" +
                "&characterEncoding=utf8" +
                "&autoReconnect=true" +
                "&failOverReadOnly=false" +
                "&maxReconnects=10" +
                "&serverTimezone=UTC";

            Class.forName(DRIVER);
            con = DriverManager.getConnection(url, user, pass);

            if (con != null && !con.isClosed()) {
                System.out.println("Connected to Render Cloud MySQL");
            }

        } catch (Exception e) {
            System.out.println("DB CONNECTION FAILED");
            e.printStackTrace();
        }
        return con;
    }
}
