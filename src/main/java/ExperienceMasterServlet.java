import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/ExperienceMasterServlet")
public class ExperienceMasterServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String name = request.getParameter("companyName");
                PreparedStatement ps = con.prepareStatement("INSERT INTO master_organizations (organization_name) VALUES (?)");
                ps.setString(1, name);
                ps.executeUpdate();

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                String name = request.getParameter("companyName");
                PreparedStatement ps = con.prepareStatement("UPDATE master_organizations SET organization_name = ? WHERE id = ?");
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.executeUpdate();

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM master_organizations WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
            }
            // Fallback: If not an AJAX request, redirect
            if (request.getHeader("X-Requested-With") == null) {
                response.sendRedirect("experiencemaster.html");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (request.getHeader("X-Requested-With") == null) {
                response.sendRedirect("experiencemaster.html?error=true");
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id, organization_name FROM master_organizations ORDER BY id ASC");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(rs.getString("organization_name")).append("\"")
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
