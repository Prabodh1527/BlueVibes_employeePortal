import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DBConnection {

    private static boolean initialized = false;

    public static Connection getConnection() {
        try {
            Class.forName("org.h2.Driver");

            Connection con = DriverManager.getConnection(
                    "jdbc:h2:mem:bluevibes;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
            );

            if (!initialized) {
                initializeDatabase(con);
                initialized = true;
            }

            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void initializeDatabase(Connection con) throws Exception {

        Statement st = con.createStatement();

        st.execute("""
            CREATE TABLE users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                fullname VARCHAR(100),
                email VARCHAR(100) UNIQUE,
                password VARCHAR(100),
                role VARCHAR(20)
            );
        """);

        st.execute("""
            INSERT INTO users (fullname, email, password, role)
            VALUES ('Admin User', 'admin@bluegital.com', '111', 'Admin');
        """);

        st.close();
    }
}
