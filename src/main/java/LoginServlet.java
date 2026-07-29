import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

            // ADDED: Fetch 'status' alongside other fields
            String sql = "SELECT password, fullname, role, password_changed, status FROM users WHERE email=?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);

                try (ResultSet rs = ps.executeQuery()) {
                    // Check if the user even exists with that email
                    if (rs.next()) {
                        
                        String storedPasswordFromDb = rs.getString("password");
                        String dbRole = rs.getString("role");
                        String status = rs.getString("status"); // Retrieve status column

                        // Verify password and role first
                        if (PasswordUtil.verifyPassword(password, storedPasswordFromDb) && dbRole.equalsIgnoreCase(role)) {
                            
                            // CHECK ACCOUNT STATUS (case-insensitive check for "active")
                            if (status == null || !"active".equalsIgnoreCase(status.trim())) {
                                String suspendedMessage = "Your account is " + (status != null ? status : "inactive") + 
                                                         ". Kindly contact the administrator.";
                                
                                // Redirect back to login with encoded error message
                                response.sendRedirect("index.html?error=" + URLEncoder.encode(suspendedMessage, StandardCharsets.UTF_8.toString()));
                                return;
                            }

                            // If active, proceed with session creation
                            HttpSession session = request.getSession();
                            
                            session.setAttribute("userEmail", email);
                            session.setAttribute("userRole", dbRole);
                            
                            String fullName = rs.getString("fullname");
                            session.setAttribute("userName", (fullName != null) ? fullName : "User");
                            
                            boolean passwordChanged = rs.getBoolean("password_changed");
                            session.setAttribute("passwordChanged", passwordChanged);
                            
                            if (!"Admin".equalsIgnoreCase(dbRole) && !passwordChanged) {
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
                            // Password failed verification or role mismatch
                            response.sendRedirect("index.html?error=invalid");
                        }
                    } else {
                        // User email not found
                        response.sendRedirect("index.html?error=invalid");
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
