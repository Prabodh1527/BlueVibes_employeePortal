import java.io.IOException;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/UpdateAdminProfileServlet")
public class UpdateAdminProfileServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("email");
        String name = request.getParameter("fullname");
        String phone = request.getParameter("phone");

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.sendRedirect("adminprofile.html?status=error");
                return;
            }

            String sql = "UPDATE users SET fullname = ?, phone = ? WHERE email = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, email);
                
                int rowsUpdated = ps.executeUpdate();
                if (rowsUpdated > 0) {
                    response.sendRedirect("adminprofile.html?status=success");
                } else {
                    response.sendRedirect("adminprofile.html?status=notfound");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("adminprofile.html?status=error");
        }
    }
}
