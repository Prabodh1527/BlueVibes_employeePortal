import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class ExcelEmailSender {

    // Brevo Connection Configuration Credentials
    private static final String BREVO_API_KEY = "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"; 
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@11437826.brevosend.com";

    public static boolean sendExcelEmail(String employeeEmail) {
        try {
            // 1. Initialize Document Construction 
            StringBuilder csvBuilder = new StringBuilder();
            
            // Adding standard document headers matching your dashboard columns
            csvBuilder.append("Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

            // 2. Query Live PostgreSQL Data Rows
            String sql = "SELECT * FROM user_weekly_reports WHERE user_email=? ORDER BY created_at DESC";
            
            try (Connection con = DBConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                
                ps.setString(1, employeeEmail);
                
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Extracting columns safely and appending with escape wrapper validation
                        String taskId = rs.getString("task_id") != null ? rs.getString("task_id") : "";
                        String taskDesc = rs.getString("task_description") != null ? rs.getString("task_description") : "";
                        String customer = rs.getString("customer") != null ? rs.getString("customer") : "";
                        String status = rs.getString("status") != null ? rs.getString("status") : "";
                        int percent = rs.getInt("percentage_completed");
                        String startDate = rs.getString("start_date") != null ? rs.getString("start_date") : "";
                        String endDate = rs.getString("end_date") != null ? rs.getString("end_date") : "";
                        String comments = rs.getString("comments") != null ? rs.getString("comments") : "";

                        csvBuilder.append(taskId).append(",")
                                  .append("\"").append(taskDesc.replace("\"", "\"\"")).append("\",")
                                  .append("\"").append(customer.replace("\"", "\"\"")).append("\",")
                                  .append(status).append(",")
                                  .append(percent).append(",")
                                  .append(startDate).append(",")
                                  .append(endDate).append(",")
                                  .append("\"").append(comments.replace("\"", "\"\"")).append("\"\n");
                    }
                }
            }

            // 3. Convert Data Document Payload to Base64
            String base64Content = Base64.getEncoder().encodeToString(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));

            // 4. Setup Outbound HTTP Socket Request Parameters to Brevo API
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Escape-proof clean JSON structural mapping block
            String jsonPayload = "{"
                + "\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"" + VERIFIED_SENDER_EMAIL + "\"},"
                + "\"to\":["
                    + "{\"email\":\"bharadwaj@bluedigital.co.in\",\"name\":\"Auditor\"},"
                    + "{\"email\":\"" + employeeEmail + "\",\"name\":\"Employee\"}"
                + "],"
                + "\"subject\":\" Weekly Status Report Submission\","
                + "\"htmlContent\":\"<html><body>"
                    + "<h3>Hello Auditor,</h3>"
                    + "<p>Please find attached the weekly status report submitted by employee: <b>" + employeeEmail + "</b>.</p>"
                    + "<br><p><i>This is an automated system notification via BlueVibes Portal.</i></p>"
                    + "</body></html>\","
                + "\"attachment\":["
                    + "{"
                        + "\"content\":\"" + base64Content + "\","
                        + "\"name\":\"Weekly_Status_Report.csv\""
                    + "}"
                + "]"
                + "}";

            // 5. Pipe JSON Data Array Stream to Target Socket Endpoints
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 6. Check Server Response Validation Flags
            int responseCode = conn.getResponseCode();
            System.out.println("Brevo System Server Response Code Log: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                // Read exact API server failure trace prints if any execution faults occur
                java.io.InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("Brevo Error Internal Trace Details: " + errorResponse);
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
