import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ProfilePasswordServlet")
public class ProfilePasswordServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        if (session == null) {
            response.sendRedirect("index.html");
            return;
        }

        String email = (String) session.getAttribute("userEmail");

        String newPassword = request.getParameter("newPassword");
        String confirmPassword = request.getParameter("confirmPassword");

        if (newPassword == null || confirmPassword == null ||
                !newPassword.equals(confirmPassword)) {

            response.sendRedirect("LoadProfileServlet?status=password_mismatch");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            String hashedPassword = PasswordUtil.hashPassword(newPassword);

            String sql = "UPDATE users SET password=? WHERE email=?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, hashedPassword);
            ps.setString(2, email);

            ps.executeUpdate();

            response.sendRedirect("LoadProfileServlet?status=password_updated");

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("LoadProfileServlet?status=password_error");
        }
    }
}
