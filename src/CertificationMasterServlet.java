import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/CertificationMasterServlet")
public class CertificationMasterServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String name = request.getParameter("certName");
                PreparedStatement ps = con.prepareStatement("INSERT INTO master_certifications (certification_name) VALUES (?)");
                ps.setString(1, name);
                ps.executeUpdate();

            } else if ("update".equals(action)) {
                // NEW: Handle the update logic from the edit form
                int id = Integer.parseInt(request.getParameter("id"));
                String name = request.getParameter("certName");
                PreparedStatement ps = con.prepareStatement("UPDATE master_certifications SET certification_name = ? WHERE id = ?");
                ps.setString(1, name);
                ps.setInt(2, id);
                ps.executeUpdate();

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM master_certifications WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // Note: Since you are using fetch() in the frontend, this redirect
            // usually won't be seen, but it's good as a fallback.
            response.sendRedirect("certificationmaster.html");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("certificationmaster.html?error=true");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT id, certification_name FROM master_certifications ORDER BY id ASC");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("id")).append(",")
                        .append("\"name\":\"").append(rs.getString("certification_name")).append("\"")
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