import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/QualificationServlet")
public class QualificationServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String name = request.getParameter("qualName");
                PreparedStatement ps = con.prepareStatement("INSERT INTO master_qualifications (qualification_name) VALUES (?)");
                ps.setString(1, name);
                ps.executeUpdate();
            } else if ("update".equals(action)) {
                // Logic for editing existing record
                int id = Integer.parseInt(request.getParameter("id"));
                String name = request.getParameter("qualName");
                PreparedStatement ps = con.prepareStatement("UPDATE master_qualifications SET qualification_name = ? WHERE id = ?");
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.executeUpdate();
            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM master_qualifications WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // For AJAX calls, redirection isn't strictly necessary but kept for consistency
            if (request.getHeader("X-Requested-With") == null) {
                response.sendRedirect("qualificationmaster.html");
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (request.getHeader("X-Requested-With") == null) {
                response.sendRedirect("qualificationmaster.html?error=true");
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id, qualification_name FROM master_qualifications ORDER BY id ASC");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(rs.getString("qualification_name")).append("\"")
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
