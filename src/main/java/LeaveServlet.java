import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@WebServlet("/LeaveServlet")
public class LeaveServlet extends HttpServlet {
    private static final String BREVO_API_KEY =
    "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";

    private static final String HR_EMAIL =
    "prasanthram@bluegitalllp.com";

    private static final String ADMIN_EMAIL_1 =
    "gprabodhchandra@gmail.com";
    /*"mail2blueeye@gmail.com";*/

    private static final String ADMIN_EMAIL_2 =
    "diptipatra75588@gmail.com";
    /*"harini.blueeye@gmail.com";*/

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
                sendLeaveNotification(
                        email,
                        leaveType,
                        dateRange,
                        reason
                );
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

    private void sendLeaveNotification(
        String employeeEmail,
        String leaveType,
        String dateRange,
        String reason) {

        try {
    
            URL url =
                new URL("https://api.brevo.com/v3/smtp/email");
    
            HttpURLConnection conn =
                (HttpURLConnection) url.openConnection();
    
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
    
            String jsonPayload =
                "{"
                + "\"sender\":{"
                + "\"name\":\"BlueVibes Portal\","
                + "\"email\":\"gprabodhchandra@gmail.com\""
                + "},"
    
                + "\"to\":["
                + "{\"email\":\"" + HR_EMAIL + "\"},"
                + "{\"email\":\"" + ADMIN_EMAIL_1 + "\"},"
                + "{\"email\":\"" + ADMIN_EMAIL_2 + "\"}"
                + "],"
    
                + "\"subject\":\"New Leave Request Submitted\","
    
                + "\"htmlContent\":\""
    
                + "<h3>New Leave Application Received</h3>"
    
                + "<p><b>Employee:</b> "
                + employeeEmail
                + "</p>"
    
                + "<p><b>Leave Type:</b> "
                + leaveType
                + "</p>"
    
                + "<p><b>Date Range:</b> "
                + dateRange
                + "</p>"
    
                + "<p><b>Reason:</b> "
                + reason
                + "</p>"
    
                + "<br>"
    
                + "<p>Please login through Admin Portal and approve/reject the request.</p>"
    
                + "\""
                + "}";
    
            try (OutputStream os = conn.getOutputStream()) {
    
                byte[] input =
                    jsonPayload.getBytes(StandardCharsets.UTF_8);
    
                os.write(input, 0, input.length);
            }
    
            System.out.println(
                "Leave Mail Response Code = "
                + conn.getResponseCode()
            );
    
        }
        catch (Exception ex) {
            ex.printStackTrace();
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
