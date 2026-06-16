import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String BREVO_API_KEY = "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";
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@gmail.com";
    private static final String AUDITOR_EMAIL = "prasanthram@bluegitalllp.com";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        
        // Session Guard
        if (session == null || session.getAttribute("email") == null) {
            response.sendRedirect("index.html?status=session_expired");
            return;
        }

        String primaryEmail = (String) session.getAttribute("email");
        String targetDeliveryEmail = primaryEmail; // Default fallback
        String exporterName = "Employee"; 

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();
            
            // Query both the fallback email and the preferred communication_email column
            String sql = "SELECT fullname, email, communication_email FROM users WHERE email = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, primaryEmail);
            rs = ps.executeQuery();

            if (rs.next()) {
                exporterName = rs.getString("fullname");
                String commsEmail = rs.getString("communication_email");
                
                // If communication_email is present and not empty, route the mail there
                if (commsEmail != null && !commsEmail.trim().isEmpty()) {
                    targetDeliveryEmail = commsEmail.trim();
                }
            }
            
            rs.close();
            ps.close();

            // Dispatch the report notice to the computed target address and the auditor
            boolean mailSent = sendDualExportEmail(targetDeliveryEmail, exporterName);

            if (mailSent) {
                response.sendRedirect("weekly_status.html?export=success");
            } else {
                response.sendRedirect("weekly_status.html?export=mail_error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            response.sendRedirect("weekly_status.html?export=error");
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) {}
        }
    }

    private boolean sendDualExportEmail(String targetEmail, String exporterName) {
        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY.trim());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Constructing JSON routing block containing the dual destinations
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                       .append("\"sender\":{\"name\":\"BlueVibes Audit Engine\",\"email\":\"").append(VERIFIED_SENDER_EMAIL).append("\"},")
                       .append("\"to\":[")
                       // Destination 1: The Auditor
                       .append("{\"email\":\"").append(AUDITOR_EMAIL).append("\",\"name\":\"Prasanth Ram\"},")
                       // Destination 2: The User's checked Communication Email
                       .append("{\"email\":\"").append(targetEmail.trim()).append("\",\"name\":\"").append(exporterName).append("\"}")
                       .append("],")
                       .append("\"subject\":\"📊 Weekly Status Report: Data Export Activity Log\",")
                       .append("\"htmlContent\":\"<html><body style='font-family: Arial, sans-serif; line-height: 1.6;'>")
                       .append("<h2 style='color: #0284c7;'>Data Export Activity Notification</h2>")
                       .append("<p>An authorized export action was executed inside the weekly status sheet reporting layout tracking block.</p>")
                       .append("<div style='background: #f1f5f9; padding: 15px; border-radius: 8px; border-left: 4px solid #0284c7;'>")
                       .append("<strong>Action Initiator:</strong> ").append(exporterName).append("<br>")
                       .append("<strong>Delivery Destination:</strong> ").append(targetEmail).append("<br>")
                       .append("<strong>Activity Status:</strong> Export Log Dispatched Successfully")
                       .append division("</div>")
                       .append("<p style='font-size: 12px; color: #64748b; margin-top: 20px;'>This message is a compliance tracking carbon copy delivered simultaneously to management and the destination inbox.</p>")
                       .append("</body></html>\"")
                       .append("}");

            String jsonPayload = jsonBuilder.toString();

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            return (conn.getResponseCode() == 201 || conn.getResponseCode() == 200);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
