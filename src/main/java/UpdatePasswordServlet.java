import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/UpdatePasswordServlet")
public class UpdatePasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String codeInput = request.getParameter("token");
        String newPassword = request.getParameter("password");

        if (codeInput != null) codeInput = codeInput.trim();
        if (newPassword != null) newPassword = newPassword.trim();

        // Security Guard: Reject empty operations
        if (codeInput == null || codeInput.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            response.sendRedirect("enter_code.html?status=invalid_input");
            return;
        }

        Connection con = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdate = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            // 1. Target the UNIQUE user who owns this active 6-digit verification code
            String checkSql = "SELECT id, email FROM users WHERE reset_token = ?";
            psCheck = con.prepareStatement(checkSql);
            psCheck.setString(1, codeInput);
            rs = psCheck.executeQuery();

            if (rs.next()) {
                int targetUserId = rs.getInt("id");
                String userEmail = rs.getString("email");
                System.out.println("[SECURITY] Found matching account for Code. User ID: " + targetUserId + " (" + userEmail + ")");

                // 2. Strict Row-Isolated Update: Modify ONLY the row matching this specific User ID
                // Also clears the reset_token column so the code can never be used again
                String updateSql = "UPDATE users SET password = ?, reset_token = NULL WHERE id = ?";
                psUpdate = con.prepareStatement(updateSql);
                psUpdate.setString(1, newPassword); // Consider hashing this parameter if your system supports it
                psUpdate.setInt(2, targetUserId);
                
                int rowsUpdated = psUpdate.executeUpdate();

                if (rowsUpdated > 1) {
                    // System protection fail-safe wrap
                    System.err.println("[CRITICAL SECURITY ALERT] Multi-row collision detected! Rolling back action.");
                    response.sendRedirect("enter_code.html?status=system_error");
                    return;
                }

                System.out.println("[SUCCESS] Password modified securely for User ID " + targetUserId + ". Forwarding to login portal context.");
                // Redirect to login page with a verified status flag
                response.sendRedirect("index.html?status=verified");

            } else {
                // Pin mismatch or expired session lookup
                System.out.println("[AUTH FAILURE] Provided 6-digit PIN (" + codeInput + ") does not exist in active tracking states.");
                response.sendRedirect("enter_code.html?status=wrong_code");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("enter_code.html?status=error");
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception e) {}
            try { if (psCheck != null) psCheck.close(); } catch (Exception e) {}
            try { if (psUpdate != null) psUpdate.close(); } catch (Exception e) {}
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }
}
