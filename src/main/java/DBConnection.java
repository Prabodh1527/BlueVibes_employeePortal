import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    public static Connection getConnection() {
        Connection con = null;
        try {
            String url =
                "jdbc:mysql://shuttle.proxy.rlwy.net:56882/management_system"
              + "?useSSL=true"
              + "&requireSSL=true"
              + "&verifyServerCertificate=false"
              + "&serverTimezone=UTC";

            String user = "root";
            String pass = "OZKiDIwjorSGALvAVhAbgmIKUCeNrVzV";

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

