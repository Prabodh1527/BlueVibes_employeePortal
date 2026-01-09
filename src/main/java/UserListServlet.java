import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/UserListServlet")
public class UserListServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            // Selects specific columns required for the User Management table
            String sql = "SELECT employee_id, fullname, email, department, designation FROM users WHERE role = 'User' ORDER BY employee_id ASC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"empid\":\"").append(rs.getString("employee_id")).append("\",")
                        .append("\"name\":\"").append(rs.getString("fullname")).append("\",")
                        .append("\"email\":\"").append(rs.getString("email")).append("\",")
                        .append("\"dept\":\"").append(rs.getString("department")).append("\",")
                        .append("\"desig\":\"").append(rs.getString("designation")).append("\"")
                        .append("}");
                first = false;
            }
            json.append("]");
            out.print(json.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }
}
