import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;

public class ExcelEmailSender {

    // Your working, verified Brevo API Key
    private static final String BREVO_API_KEY = "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"; 
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@11437826.brevosend.com";

    public static boolean sendExcelEmail(String employeeEmail) {
        try {
            // 1. Fetch live database records and build the CSV content string
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("Task ID,Task Description,Customer,Status,% Completed,Start Date,End Date,Comments\n");

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

                        // Clean data inputs to prevent parsing breaks inside the JSON payload string
                        taskDesc = sanitizeForJson(taskDesc);
                        customer = sanitizeForJson(customer);
                        comments = sanitizeForJson(comments);

                        csvBuilder.append(taskId).append(",")
                                  .append("\"").append(taskDesc).append("\",")
                                  .append("\"").append(customer).append("\",")
                                  .append(status).append(",")
                                  .append(percent).append(",")
                                  .append(startDate).append(",")
                                  .append(endDate).append(",")
                                  .append("\"").append(comments).append("\"\n");
                    }
                }
            }

            // 2. Convert CSV data string to standard Base64 encoding
            String base64Content = Base64.getEncoder().encodeToString(csvBuilder.toString().getBytes(StandardCharsets.UTF_8));

            // 3. Configure HTTP properties for Brevo REST API Endpoint connection
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY.trim());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // 4. Construct JSON Payload — explicitly delivering to both the Auditor AND the exporting Employee
            String jsonPayload = "{"
                + "\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"" + VERIFIED_SENDER_EMAIL + "\"},"
                + "\"to\":["
                    + "{\"email\":\"bharadwaj@bluedigital.co.in\",\"name\":\"Auditor\"},"
                    + "{\"email\":\"" + employeeEmail.trim() + "\",\"name\":\"Employee\"}"
                + "],"
                + "\"subject\":\"✨ Weekly Status Report Submission\","
                + "\"htmlContent\":\"<html><body><h3>Hello,</h3><p>Please find attached the copy of your weekly status report spreadsheet file submitted via the BlueVibes Employee Portal.</p></body></html>\","
                + "\"attachment\":["
                    + "{"
                        + "\"content\":\"" + base64Content + "\","
                        + "\"name\":\"Weekly_Status_Report.csv\""
                    + "}"
                + "]"
                + "}";

            // 5. Pipe payload downstream to Brevo servers
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            // 6. Read HTTP response code
            int responseCode = conn.getResponseCode();
            System.out.println("Brevo Engine Connection Response Code: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("Brevo Engine Rejection Message: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper method to keep JSON format structured correctly
    private static String sanitizeForJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "'")
                    .replace("\n", " ")
                    .replace("\r", " ")
                    .replace("\t", " ");
    }
}
