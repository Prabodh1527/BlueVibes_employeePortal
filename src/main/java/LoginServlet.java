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

            // FIXED SQL: Select the password hash alongside other fields using just the email
            String sql = "SELECT password, fullname, role, password_changed FROM users WHERE email=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    // Check if the user even exists with that email
                    if (rs.next()) {
                        
                        // 1. Grab the stored password string (could be plain-text or a Bcrypt hash)
                        String storedPasswordFromDb = rs.getString("password");
                        String dbRole = rs.getString("role");

                        // 2. FIXED: Use our smart PasswordUtil to evaluate if it matches
                        // This handles both old plain text entries and your new secure hashes!
                        if (PasswordUtil.verifyPassword(password, storedPasswordFromDb) && dbRole.equalsIgnoreCase(role)) {
                            
                            HttpSession session = request.getSession();
                            
                            // Set attributes exactly as expected by other Servlets
                            session.setAttribute("userEmail", email);
                            session.setAttribute("userRole", dbRole);
                            
                            // Store the real name for the dashboard
                            String fullName = rs.getString("fullname");
                            session.setAttribute("userName", (fullName != null) ? fullName : "User");
                            boolean passwordChanged = rs.getBoolean("password_changed");
                            session.setAttribute("passwordChanged", passwordChanged);
                            if (!passwordChanged) {
                                response.sendRedirect("LoadProfileServlet?forcePasswordChange=true");
                                return;
                            }

                            // Redirect based on the DB role
                            if ("Admin".equalsIgnoreCase(dbRole)) {
                                response.sendRedirect("adminhome.html");
                            } else {
                                response.sendRedirect("homepage.jsp");
                            }
                        } else {
                            // Password failed verification or the role didn't match up
                            response.sendRedirect("index.html?error=true");
                        }
                    } else {
                        // User email not found in the records
                        response.sendRedirect("index.html?error=true");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            response.getWriter().println("Database Error: " + e.getMessage());
        }
    }
}
