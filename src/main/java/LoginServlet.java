import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/LoginServlet")
public class LoginServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String role = request.getParameter("role");

        // Use try-with-resources to ensure connection is ALWAYS closed
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.getWriter().println("Database Error: Connection could not be established.");
                return;
            }

            String sql = "SELECT fullname, role FROM users WHERE email=? AND password=? AND role=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, password);
                ps.setString(3, role);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        HttpSession session = request.getSession();
                        
                        // Set attributes exactly as expected by other Servlets
                        session.setAttribute("userEmail", email);
                        
                        // We take the role directly from the DB to be safe
                        String dbRole = rs.getString("role");
                        session.setAttribute("userRole", dbRole);
                        
                        // Store the real name for the dashboard
                        String fullName = rs.getString("fullname");
                        session.setAttribute("userName", (fullName != null) ? fullName : "User");

                        // Redirect based on the DB role
                        if ("Admin".equalsIgnoreCase(dbRole)) {
                            response.sendRedirect("adminhome.html");
                        } else {
                            response.sendRedirect("homepage.jsp");
                        }
                    } else {
                        // Invalid credentials
                        response.sendRedirect("index.html?error=true");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Database Error: " + e.getMessage());
        }
    }
}
