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

        try {
            Connection con = DBConnection.getConnection();

            String sql = "SELECT * FROM management_system_users WHERE email=? AND password=? AND role=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, email);
            ps.setString(2, password);
            ps.setString(3, role);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                HttpSession session = request.getSession();
                session.setAttribute("userEmail", email);
                session.setAttribute("userRole", role);

                // ADDED: Get the real name from DB to display on dashboard
                session.setAttribute("userName", rs.getString("fullname"));

                if ("Admin".equalsIgnoreCase(role)) {
                    response.sendRedirect("adminhome.html");
                } else {
                    // Correctly pointing to the new .jsp dashboard
                    response.sendRedirect("homepage.jsp");
                }
            } else {
                response.sendRedirect("index.html?error=true");
            }
            con.close();
        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("Database Error: " + e.getMessage());
        }
    }
}

