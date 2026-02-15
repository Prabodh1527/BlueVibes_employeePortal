import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/LeaveServlet")
public class LeaveServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String email = (session != null) ? (String) session.getAttribute("userEmail") : null;

        if (email == null) {
            response.sendRedirect("index.html");
            return;
        }

        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        String dateRange = (startDate != null && endDate != null) ? startDate + " to " + endDate : "N/A";
        String leaveType = request.getParameter("leaveType");
        String reason = request.getParameter("reason");

        try (Connection con = DBConnection.getConnection()) {
            String sql = "INSERT INTO leaves (user_email, leave_type, date_range, reason) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, email);
                ps.setString(2, leaveType);
                ps.setString(3, dateRange);
                ps.setString(4, reason);
                ps.executeUpdate();
                response.sendRedirect("leavereq.html?status=success");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("leavereq.html?status=error");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        String userEmail = (session != null) ? (String) session.getAttribute("userEmail") : null;
        String action = request.getParameter("action");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        if (userEmail == null) {
            out.print("[]");
            return;
        }

        try (Connection con = DBConnection.getConnection()) {
            // ACTION: User views their specific history
            if ("fetchUserHistory".equals(action)) {
                String sql = "SELECT leave_type, date_range, applied_on, status FROM leaves WHERE user_email=? ORDER BY applied_on DESC";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, userEmail);
                    try (ResultSet rs = ps.executeQuery()) {
                        StringBuilder json = new StringBuilder("[");
                        boolean first = true;
                        while (rs.next()) {
                            if (!first) json.append(",");
                            json.append("{")
                                .append("\"type\":\"").append(clean(rs.getString("leave_type"))).append("\",")
                                .append("\"dates\":\"").append(clean(rs.getString("date_range"))).append("\",")
                                .append("\"appliedOn\":\"").append(rs.getTimestamp("applied_on")).append("\",")
                                .append("\"status\":\"").append(clean(rs.getString("status"))).append("\"")
                                .append("}");
                            first = false;
                        }
                        json.append("]");
                        out.print(json.toString());
                    }
                }
            }
            // ACTION: Admin views EVERYONE'S history
            else if ("fetchAllPending".equals(action)) {
                String sql = "SELECT id, user_email, leave_type, date_range, reason, status FROM leaves ORDER BY applied_on DESC";
                try (PreparedStatement ps = con.prepareStatement(sql);
                     ResultSet rs = ps.executeQuery()) {
                    StringBuilder json = new StringBuilder("[");
                    boolean first = true;
                    while (rs.next()) {
                        if (!first) json.append(",");
                        json.append("{")
                            .append("\"id\":").append(rs.getInt("id")).append(",")
                            .append("\"email\":\"").append(clean(rs.getString("user_email"))).append("\",")
                            .append("\"type\":\"").append(clean(rs.getString("leave_type"))).append("\",")
                            .append("\"dates\":\"").append(clean(rs.getString("date_range"))).append("\",")
                            .append("\"status\":\"").append(clean(rs.getString("status"))).append("\",")
                            .append("\"reason\":\"").append(clean(rs.getString("reason"))).append("\"")
                            .append("}");
                        first = false;
                    }
                    json.append("]");
                    out.print(json.toString());
                }
            }
            else if ("updateStatus".equals(action)) {
                String id = request.getParameter("id");
                String status = request.getParameter("status");
                try (PreparedStatement ps = con.prepareStatement("UPDATE leaves SET status=? WHERE id=?")) {
                    ps.setString(1, status);
                    ps.setInt(2, Integer.parseInt(id));
                    int res = ps.executeUpdate();
                    out.print("{\"success\":" + (res > 0) + "}");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    // Helper to prevent JSON breaking characters
    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", " ")
                    .replace("\r", " ");
    }
}
