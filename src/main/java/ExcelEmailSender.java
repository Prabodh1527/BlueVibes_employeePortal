import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ExcelEmailSender {

    // Brevo API Configuration Credentials
    private static final String BREVO_API_KEY = "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"; 
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@11437826.brevosend.com";

    public static boolean sendExcelEmail(String employeeEmail) {
        try {
            // 1. Query the live PostgreSQL database records for this user
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

                        // Safe replacement of any basic html character breaks
                        taskDesc = taskDesc.replace("<", "&lt;").replace(">", "&gt;");
                        customer = customer.replace("<", "&lt;").replace(">", "&gt;");
                        comments = comments.replace("<", "&lt;").replace(">", "&gt;");

                        tableRowsHtml.append("<tr style='border-bottom: 1px solid #e2e8f0;'>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(taskId).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(taskDesc).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(customer).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0; color: #0284c7;'>").append(status).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(percent).append("%</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(startDate).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(endDate).append("</td>")
                                     .append("<td style='padding: 8px; border: 1px solid #e2e8f0;'>").append(comments).append("</td>")
                                     .append("</tr>");
                    }
                }
            }

            // 2. Build the formal layout email structure
            String htmlContent = "<html><body style='font-family: Arial, sans-serif; background-color: #f1f5f9; padding: 20px;'>"
                + "<div style='max-width: 800px; margin: 0 auto; background: #ffffff; padding: 30px; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1);'>"
                + "<h2 style='color: #0f172a; border-bottom: 2px solid #0284c7; padding-bottom: 10px; margin-top: 0;'>Weekly Status Report Submission</h2>"
                + "<p style='font-size: 14px; color: #334155;'><b>Submitted By employee:</b> " + employeeEmail + "</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin-top: 20px; font-size: 13px; text-align: left;'>"
                + "<thead style='background: #0f172a; color: #ffffff;'>"
                + "<tr>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>ID</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>Task Description</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>Customer</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>Status</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>%</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>Start Date</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>End Date</th>"
                + "<th style='padding: 10px; border: 1px solid #334155;'>Comments</th>"
                + "</tr>"
                + "</thead>"
                + "<tbody>" + (tableRowsHtml.length() == 0 ? "<tr><td colspan='8' style='text-align:center; padding:10px;'>No records found</td></tr>" : tableRowsHtml.toString()) + "</tbody>"
                + "</table>"
                + "<br><p style='font-size: 11px; color: #94a3b8; text-align: center; margin-top: 30px;'>This is an automated operational pipeline delivery from the BlueVibes Employee Portal.</p>"
                + "</div>"
                + "</body></html>";

            // 3. Strict JSON Escaping to ensure Brevo doesn't hit a structural parsing fault
            String escapedHtml = htmlContent.replace("\\", "\\\\")
                                            .replace("\"", "\\\"")
                                            .replace("\n", "\\n")
                                            .replace("\r", "\\r");

            // 4. Initialize Connection to Brevo SMTP REST API
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000); // 10 second timeout limit
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);

            // Constructing strict clean schema structure body 
            String jsonPayload = "{"
                + "\"sender\":{\"email\":\"" + VERIFIED_SENDER_EMAIL + "\",\"name\":\"BlueVibes Portal\"},"
                + "\"to\":["
                    + "{\"email\":\"bharadwaj@bluedigital.co.in\",\"name\":\"Auditor\"},"
                    + "{\"email\":\"" + employeeEmail + "\",\"name\":\"Employee\"}"
                + "],"
                + "\"subject\":\"✨ Weekly Status Report Submission\","
                + "\"htmlContent\":\"" + escapedHtml + "\""
                + "}";

            // 5. Send out Payload Stream Packet
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // 6. Evaluate HTTP Response State Verification
            int responseCode = conn.getResponseCode();
            System.out.println("DEBUG - Brevo Response Code: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                // Fetch internal trace error message from Brevo to see exactly why it rejected it
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("DEBUG - Brevo Failure Details: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
