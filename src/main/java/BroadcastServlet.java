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

@WebServlet("/BroadcastServlet")
public class BroadcastServlet extends HttpServlet {

    // POST: Save new announcement (From Admin: admin_notifications.html)
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String msg = request.getParameter("message");
        String cat = request.getParameter("category");

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                response.setStatus(500);
                out.print("{\"success\": false, \"error\": \"Database Connection Failed\"}");
                return;
            }

            String sql = "INSERT INTO broadcasts (message, category) VALUES (?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, msg);
                ps.setString(2, cat);
                ps.executeUpdate();
                sendBroadcastEmail(msg, cat);
                out.print("{\"success\": true}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(500);
            out.print("{\"success\": false, \"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    // GET: Fetch all announcements (For User: user_notifications.html)
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                out.print("[]");
                return;
            }

            // Fetch last 20 messages, newest first
            String sql = "SELECT message, category, created_at FROM broadcasts ORDER BY created_at DESC LIMIT 20";
            try (PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");

                    json.append("{")
                        .append("\"message\":\"").append(clean(rs.getString("message"))).append("\",")
                        .append("\"category\":\"").append(clean(rs.getString("category"))).append("\",")
                        .append("\"time\":\"").append(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toString() : "").append("\"")
                        .append("}");
                    first = false;
                }
                json.append("]");
                out.print(json.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
            out.print("[]");
        }
    }

    private void sendBroadcastEmail(String message, String category) {

        try {
    
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();
    
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key",
                "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");
    
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            StringBuilder recipients = new StringBuilder();

            try (Connection con = DBConnection.getConnection()) {
            
                String sql =
                    "SELECT email, communication_email FROM users";
            
                try (PreparedStatement ps =
                        con.prepareStatement(sql);
            
                     ResultSet rs = ps.executeQuery()) {
            
                    boolean first = true;
            
                    while (rs.next()) {
            
                        String targetEmail =
                            rs.getString("communication_email");
            
                        if (targetEmail == null ||
                            targetEmail.trim().isEmpty()) {
            
                            targetEmail =
                                rs.getString("email");
                        }
            
                        if (targetEmail == null ||
                            targetEmail.trim().isEmpty()) {
                            continue;
                        }
            
                        if (!first) {
                            recipients.append(",");
                        }
            
                        recipients.append(
                            "{\"email\":\"")
                            .append(targetEmail)
                            .append("\"}");
            
                        first = false;
                    }
                }
            }
    
            String jsonPayload =
                "{"
                + "\"sender\":{"
                + "\"name\":\"BlueVibes Portal\","
                + "\"email\":\"gprabodhchandra@gmail.com\""
                + "},"
    
                + "\"to\":["
                + recipients.toString()
                + "],"
    
                + "\"subject\":\"BlueVibes Notification\","
    
                + "\"htmlContent\":\""
    
                + "<h3>New Notification</h3>"
    
                + "<p><b>Category:</b> "
                + category
                + "</p>"
    
                + "<p><b>Message:</b><br>"
                + message
                + "</p>"
    
                + "<hr>"
    
                + "<p>Please check the portal for more details.</p>"
    
                + "<a href='https://bluevibes-employeeportal.onrender.com'>"
                + "Open BlueVibes Portal"
                + "</a>"
    
                + "\""
                + "}";
    
            try (OutputStream os = conn.getOutputStream()) {
    
                byte[] input =
                    jsonPayload.getBytes(StandardCharsets.UTF_8);
    
                os.write(input, 0, input.length);
            }
    
            System.out.println("MAIL STATUS = "
                    + conn.getResponseCode());
    
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Helper to prevent JSON breaking characters
    private String clean(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
