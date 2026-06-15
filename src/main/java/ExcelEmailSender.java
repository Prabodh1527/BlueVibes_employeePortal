import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ExcelEmailSender {

    // Your Brevo API key
    private static final String BREVO_API_KEY = "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";
    
    // Your verified Brevo sender email address
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@gmail.com"; 

    public static void sendReportWithAttachment(byte[] fileBytes, String fileName, String userEmail) throws Exception {
        
        // 1. Convert the binary Excel file array into a Base64 text string for JSON transfer
        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        // 2. Target email routing destination (Auditor)
        String auditorEmail = "prasanthrambharadwaj@gmail.com";

        // 3. Construct the raw JSON payload manually to keep dependencies clean
        String jsonPayload = "{"
            + "\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"" + VERIFIED_SENDER_EMAIL + "\"},"
            + "\"to\":["
            + "  {\"email\":\"" + auditorEmail + "\",\"name\":\"Auditor\"},"
            + "  {\"email\":\"" + userEmail + "\",\"name\":\"Employee\"}"
            + "],"
            + "\"subject\":\"✨ Weekly Status Report Submission\","
            + "\"htmlContent\":\"<html><body><h3>Hello,</h3><p>Please find attached the weekly status report submitted by <b>" + userEmail + "</b>.</p></body></html>\","
            + "\"attachments\":["
            + "  {"
            + "    \"content\":\"" + base64Content + "\","
            + "    \"name\":\"" + fileName + "\""
            + "  }"
            + "]"
            + "}";

        // 4. Open an HTTP connection to Brevo's REST API endpoint v3
        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", BREVO_API_KEY);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        // 5. Stream the payload data channel down to the server
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // 6. Check if the server accepted it successfully (HTTP 201 Created or 200 OK)
        int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
            System.out.println("✅ Email dispatched perfectly via Brevo API!");
        } else {
            // Read failure token trace for debugging if things break
            try (java.util.Scanner s = new java.util.Scanner(conn.getErrorStream())) {
                String responseBody = s.useDelimiter("\\A").hasNext() ? s.next() : "";
                throw new RuntimeException("Brevo API error (Status " + responseCode + "): " + responseBody);
            }
        }
    }
}
