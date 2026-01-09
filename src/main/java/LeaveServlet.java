import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/LeaveServlet")
public class LeaveServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String email = (String) session.getAttribute("userEmail");

        // Combine the two date picker values for the database
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String dateRange = (startDate != null && endDate != null) ? startDate + " to " + endDate : "N/A";

        String leaveType = request.getParameter("leaveType");
        String reason = request.getParameter("reason");

        if (email == null) return;

        try (Connection con = DBConnection.getConnection()) {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO leaves (user_email, leave_type, date_range, reason) VALUES (?, ?, ?, ?)");
            ps.setString(1, email);
            ps.setString(2, leaveType);
            ps.setString(3, dateRange);
            ps.setString(4, reason);
            ps.executeUpdate();
            response.sendRedirect("leavereq.html?status=success");
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("leavereq.html?status=error");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String userEmail = (String) session.getAttribute("userEmail");
        String action = request.getParameter("action");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            // ACTION: User views their specific history
            if ("fetchUserHistory".equals(action)) {
                PreparedStatement ps = con.prepareStatement(
                        "SELECT leave_type, date_range, applied_on, status FROM leaves WHERE user_email=? ORDER BY applied_on DESC");
                ps.setString(1, userEmail);
                ResultSet rs = ps.executeQuery();
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{\"type\":\"").append(rs.getString("leave_type")).append("\",")
                            .append("\"dates\":\"").append(rs.getString("date_range")).append("\",")
                            .append("\"appliedOn\":\"").append(rs.getTimestamp("applied_on")).append("\",")
                            .append("\"status\":\"").append(rs.getString("status")).append("\"}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
            // ACTION: Admin views EVERYONE'S history
            else if ("fetchAllPending".equals(action)) {
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, user_email, leave_type, date_range, reason, status FROM leaves ORDER BY applied_on DESC");
                ResultSet rs = ps.executeQuery();
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    // Escape quotes and remove newlines from reason to prevent JSON errors
                    String safeReason = rs.getString("reason").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
                    json.append("{\"id\":").append(rs.getInt("id"))
                            .append(",\"email\":\"").append(rs.getString("user_email"))
                            .append("\",\"type\":\"").append(rs.getString("leave_type"))
                            .append("\",\"dates\":\"").append(rs.getString("date_range"))
                            .append("\",\"status\":\"").append(rs.getString("status"))
                            .append("\",\"reason\":\"").append(safeReason).append("\"}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
            else if ("updateStatus".equals(action)) {
                String id = request.getParameter("id");
                String status = request.getParameter("status");
                PreparedStatement ps = con.prepareStatement("UPDATE leaves SET status=? WHERE id=?");
                ps.setString(1, status);
                ps.setInt(2, Integer.parseInt(id));
                int res = ps.executeUpdate();
                out.print("{\"success\":" + (res > 0) + "}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }
}
