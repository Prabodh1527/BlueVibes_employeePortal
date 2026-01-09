import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/DesignationServlet")
public class DesignationServlet extends HttpServlet {

    // Handles ADD, DELETE, and UPDATE via POST
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                // FIXED: Changed parameter to "newName" to match the frontend fetch request
                String name = request.getParameter("newName");
                if (name != null && !name.trim().isEmpty()) {
                    PreparedStatement ps = con.prepareStatement("INSERT INTO designation_master (designation_name) VALUES (?)");
                    ps.setString(1, name.trim());
                    ps.executeUpdate();
                }

            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM designation_master WHERE designation_id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();

            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                String newName = request.getParameter("newName");
                if (newName != null && !newName.trim().isEmpty()) {
                    PreparedStatement ps = con.prepareStatement("UPDATE designation_master SET designation_name = ? WHERE designation_id = ?");
                    ps.setString(1, newName.trim());
                    ps.setInt(2, id);
                    ps.executeUpdate();
                }
            }

            // Since we are using fetch() on the frontend, this redirect isn't strictly necessary,
            // but it acts as a safe fallback.
            response.setStatus(HttpServletResponse.SC_OK);

        } catch (Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Database Error");
        }
    }

    // Fetches the list for the table in JSON format
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement("SELECT designation_id, designation_name FROM designation_master ORDER BY designation_id ASC");
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");
                json.append("{")
                        .append("\"id\":").append(rs.getInt("designation_id")).append(",")
                        .append("\"name\":\"").append(rs.getString("designation_name").replace("\"", "\\\"")).append("\"")
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
