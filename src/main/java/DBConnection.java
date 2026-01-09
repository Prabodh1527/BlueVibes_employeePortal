import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection() {
        Connection con = null;
        try {
            // Updated URL using MYSQL_PUBLIC_URL from Railway
            // Note: For Java JDBC, we use the jdbc:mysql format
            String url = "jdbc:mysql://switchback.proxy.rlwy.net:55637/railway";
            String user = "root";
            String password = "WFNsuaFPkgNO1DMKHDpuvLAHHIFHeZmn";

            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(url, user, password);

            if(con != null) {
                System.out.println("Successfully connected to the Railway live database!");
            }
        } catch (Exception e) {
            System.out.println("Connection Failed! Error: " + e.getMessage());
            e.printStackTrace();
        }
        return con;
    }
}
