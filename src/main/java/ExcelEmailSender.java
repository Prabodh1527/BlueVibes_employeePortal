import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ExcelEmailSender {

    // Brevo API Connection Parameters
    private static final String BREVO_API_KEY = "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"; 
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@11437826.brevosend.com";

    public static boolean sendExcelEmail(String employeeEmail) {
        try {
            // 1. Build a clean HTML Table layout of the status report for the Auditor
            StringBuilder tableRowsHtml = new StringBuilder();

            String sql = "SELECT * FROM user_weekly_reports WHERE user_email=? ORDER BY created_at DESC";
            
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                
                ps.setString(1, employeeEmail);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String taskId = rs.getString("task_id") != null ? rs.getString("task_id") : "";
                        String taskDesc = rs.getString("task_description") != null ? rs.getString("task_description") : "";
                        String customer = rs.getString("customer") != null ? rs.getString("customer") : "";
                        String status = rs.getString("status") != null ? rs.getString("status") : "";
                        int percent = rs.getInt("percentage_completed");
                        String startDate = rs.getString("start_date") != null ? rs.getString("start_date") : "";
                        String endDate = rs.getString("end_date") != null ? rs.getString("end_date") : "";
                        String comments = rs.getString("comments") != null ? rs.getString("comments") : "";

                        // Append structured HTML formatting for crisp document tracking
                        tableRowsHtml.append("<tr style='border-bottom: 1px solid #e2e8f0;'>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #1e293b;'>").append(taskId).append("</td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #1e293b;'>").append(taskDesc).append("</td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #1e293b;'>").append(customer).append("</td>")
                                     .append("<td style='padding: 8px; font-size: 13px;'><span style='padding: 2px 6px; background: #e0f2fe; color: #0369a1; border-radius: 4px; font-size: 12px;'>").append(status).append("</span></td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #1e293b;'>").append(percent).append("%</td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #64748b;'>").append(startDate).append("</td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #64748b;'>").append(endDate).append("</td>")
                                     .append("<td style='padding: 8px; font-size: 13px; color: #1e293b;'>").append(comments).append("</td>")
                                     .append("</tr>");
                    }
                }
            }

            // 2. Prepare full HTML layout container to serve as your formal digital delivery document
            String cleanHtmlDocument = "<html><body style='font-family: Arial, sans-serif; margin: 0; padding: 20px; color: #1e293b;'>"
                + "<div style='max-width: 800px; margin: auto; border: 1px solid #e2e8f0; padding: 24px; border-radius: 8px;'>"
                + "<h2 style='color: #0f172a; margin-bottom: 5px; border-bottom: 2px solid #0284c7; padding-bottom: 10px;'>BlueVibes Weekly Status Report</h2>"
                + "<p style='font-size: 14px;'><b>Submitted By:</b> " + employeeEmail + "</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin-top: 20px; text-align: left;'>"
                + "<thead><tr style='background: #0f172a; color: white;'>"
                + "<th style='padding: 10px; font-size: 12px;'>ID</th>"
                + "<th style='padding: 10px; font-size: 12px;'>Description</th>"
                + "<th style='padding: 10px; font-size: 12px;'>Customer</th>"
                + "<th style='padding: 10px; font-size: 12px;'>Status</th>"
                + "<th style='padding: 10px; font-size: 12px;'>%</th>"
                + "<th style='padding: 10px; font-size: 12px;'>Start</th>"
                + "<th style='padding: 10px; font-size: 12px;'>End</th>"
                + "<th style='padding: 10px; font-size: 12px;'>Comments</th>"
                + "</tr></thead>"
                + "<tbody>" + tableRowsHtml.toString() + "</tbody>"
                + "</table>"
                + "<br><hr style='border: 0; border-top: 1px solid #e2e8f0;'>"
                + "<p style='font-size: 11px; color: #94a3b8; text-align: center;'>Automated tracking transmission processed securely by BlueVibes Portal Engine.</p>"
                + "</div></body></html>";

            // 3. Escape JSON quotes and backslashes carefully to avoid malformed JSON strings
            String escapedHtmlDocument = cleanHtmlDocument.replace("\\", "\\\\").replace("\"", "\\\"");

            // 4. Configure Outbound Http Connection Socket
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Payload body with integrated clean HTML report layout
            String jsonPayload = "{"
                + "\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"" + VERIFIED_SENDER_EMAIL + "\"},"
                + "\"to\":["
                    + "{\"email\":\"bharadwaj@bluedigital.co.in\",\"name\":\"Auditor\"},"
                    + "{\"email\":\"" + employeeEmail + "\",\"name\":\"Employee\"}"
                + "],"
                + "\"subject\":\"Weekly Status Report Submission\","
                + "\"htmlContent\":\"" + escapedHtmlDocument + "\""
                + "}";

            // 5. Write data packet out to connection
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 6. Read Validation Response code status flags from remote terminal endpoint
            int responseCode = conn.getResponseCode();
            System.out.println("Brevo Endpoint Response Status Code Log: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("Brevo Error Payloads Log Breakdown: " + errorResponse);
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
