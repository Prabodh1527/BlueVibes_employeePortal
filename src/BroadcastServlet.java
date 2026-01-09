import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/BroadcastServlet")
public class BroadcastServlet extends HttpServlet {

    // POST: Save new announcement (From Admin: admin_notifications.html)
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String msg = request.getParameter("message");
        String cat = request.getParameter("category");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO broadcasts (message, category) VALUES (?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, msg);
            ps.setString(2, cat);
            ps.executeUpdate();

            out.print("{\"success\": true}");
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"success\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    // GET: Fetch all announcements (For User: user_notifications.html)
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            // Fetch last 20 messages, newest first
            String sql = "SELECT message, category, created_at FROM broadcasts ORDER BY created_at DESC LIMIT 20";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            StringBuilder json = new StringBuilder("[");
            boolean first = true;
            while (rs.next()) {
                if (!first) json.append(",");

                // Enhanced escaping for JSON safety (quotes and newlines)
                String escapedMsg = rs.getString("message")
                        .replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r");

                json.append("{");
                json.append("\"message\":\"").append(escapedMsg).append("\",");
                json.append("\"category\":\"").append(rs.getString("category")).append("\",");
                json.append("\"time\":\"").append(rs.getTimestamp("created_at").toString()).append("\"");
                json.append("}");
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