import java.io.*;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/TaskMasterServlet")
public class TaskMasterServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        String action = request.getParameter("action");

        if ("list".equals(action)) {
            try (Connection con = DBConnection.getConnection()) {
                PreparedStatement ps = con.prepareStatement("SELECT id, task_name FROM task_master ORDER BY task_name ASC");
                ResultSet rs = ps.executeQuery();

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append(String.format("{\"id\":%d, \"name\":\"%s\"}",
                            rs.getInt("id"),
                            rs.getString("task_name")));
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String action = request.getParameter("action");

        try (Connection con = DBConnection.getConnection()) {
            if ("add".equals(action)) {
                String taskName = request.getParameter("taskName");
                PreparedStatement ps = con.prepareStatement("INSERT INTO task_master (task_name) VALUES (?)");
                ps.setString(1, taskName);
                ps.executeUpdate();
            } else if ("update".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                String taskName = request.getParameter("taskName");
                PreparedStatement ps = con.prepareStatement("UPDATE task_master SET task_name = ? WHERE id = ?");
                ps.setString(1, taskName);
                ps.setInt(2, id);
                ps.executeUpdate();
            } else if ("delete".equals(action)) {
                int id = Integer.parseInt(request.getParameter("id"));
                PreparedStatement ps = con.prepareStatement("DELETE FROM task_master WHERE id = ?");
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}