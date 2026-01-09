import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/GetAdminDataServlet")
public class GetAdminDataServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String adminEmail = (String) session.getAttribute("userEmail");

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT employee_id, fullname, email, phone FROM users WHERE email = ?");
            ps.setString(1, adminEmail);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String json = String.format("{\"employee_id\":\"%s\", \"fullname\":\"%s\", \"email\":\"%s\", \"phone\":\"%s\"}",
                        rs.getString("employee_id"), rs.getString("fullname"), rs.getString("email"),
                        rs.getString("phone") != null ? rs.getString("phone") : "");

                response.setContentType("application/json");
                response.getWriter().write(json);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }
}
