import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public static Connection getConnection() {
        Connection con = null;
        try {
            // MUST use System.getProperty for Tomcat on Render
            String host = System.getProperty("DB_HOST");
            String db   = System.getProperty("DB_NAME");
            String user = System.getProperty("DB_USER");
            String pass = System.getProperty("DB_PASSWORD");
            String port = System.getProperty("DB_PORT");

            if (host == null || db == null) {
                throw new RuntimeException("Database environment variables NOT loaded into Tomcat");
            }

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

            System.out.println("Connected to live MySQL at " + host);

        } catch (Exception e) {
            System.out.println("DB CONNECTION FAILED");
            e.printStackTrace();
        }
        return con;
    }
}
