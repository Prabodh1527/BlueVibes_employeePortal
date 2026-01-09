import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/UpdatePasswordServlet")
public class UpdatePasswordServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // 1. Get the current user's email from the session
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("userEmail");

        // Check if the request came from Admin Profile or Employee Profile
        String source = request.getParameter("source");

        if (email == null) {
            response.sendRedirect("index.html");
            return;
        }

        // 2. Capture passwords
        String newPass = request.getParameter("newPassword");
        String confirmPass = request.getParameter("confirmPassword");

        // Determine where to send the user back to
        String redirectTarget = (source != null && source.equals("admin")) ? "adminprofile.html" : "LoadProfileServlet";

        // 3. Logic to ensure passwords match
        if (newPass == null || !newPass.equals(confirmPass)) {
            response.setContentType("text/html");
            response.getWriter().println("<script>alert('Passwords do not match!'); window.location='" + redirectTarget + "';</script>");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // 4. Execute UPDATE
            String sql = "UPDATE users SET password = ? WHERE email = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newPass);
            ps.setString(2, email);

            int result = ps.executeUpdate();

            response.setContentType("text/html");
            if (result > 0) {
                response.getWriter().println("<script>alert('Password updated successfully!'); window.location='" + redirectTarget + "';</script>");
            } else {
                response.getWriter().println("<script>alert('Error: Could not update database.'); window.location='" + redirectTarget + "';</script>");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Database Error: " + e.getMessage());
        }
    }
}