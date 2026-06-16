import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.json.JSONArray;
import org.json.JSONObject;

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Pull configuration details safely from environment variables or default to standard parameters
    private static final String BREVO_API_KEY = System.getenv("BREVO_API_KEY") != null ? 
            System.getenv("BREVO_API_KEY") : "xkeysib-your-actual-api-key-string";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SYSTEM_NOTIFICATION_EMAIL = "admin@bluevibes-employeeportal.onrender.com";

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization");
        }
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        applyCorsHeaders(request, response);
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        applyCorsHeaders(request, response);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();

        // Strict Session Validation Check to keep users synced on Render
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }

        String loggedInUser = (String) session.getAttribute("username");
        // Pull email fallback dynamically if preserved inside authentication scopes
        String userEmail = (String) session.getAttribute("email");
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "audit-logs@bluevibes.com"; // Fallback destination routing target
        }

        try {
            // Build the standard payload structure required by Brevo Transactional Email Engine
            JSONObject payload = new JSONObject();
            
            JSONObject sender = new JSONObject();
            sender.put("name", "BlueVibes Portal Engine");
            sender.put("email", SYSTEM_NOTIFICATION_EMAIL);
            payload.put("sender", sender);

            JSONArray toArray = new JSONArray();
            JSONObject recipient = new JSONObject();
            recipient.put("name", loggedInUser);
            recipient.put("email", userEmail);
            toArray.put(recipient);
            payload.put("to", toArray);

            payload.put("subject", "Automated Sync: Weekly Status Report Audit Log");
            
            String htmlContent = "<html><body>"
                    + "<h3>Weekly Status Report Submitted</h3>"
                    + "<p><strong>Employee Username:</strong> " + loggedInUser + "</p>"
                    + "<p>This confirming log states that an audit sheet file was compiled local to the browser and successfully submitted back to the system backend dashboard.</p>"
                    + "<br><p><em>This is an automated operational confirmation notification message.</em></p>"
                    + "</body></html>";
            payload.put("htmlContent", htmlContent);

            // Establish secure connection pipeline to Brevo API Endpoint
            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Write JSON Payload Outbound
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                out.print("{\"success\":true}");
            } else {
                // Read diagnostic context directly from error pipe to debug deployment infrastructure if needed
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                }
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"error\":\"Brevo Engine Rejected Delivery\",\"code\":" + responseCode + ",\"details\":\"" + errorResponse.toString().replace("\"", "\\\"") + "\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"error\":\"Internal Mailing Pipeline Disrupted: " + e.getMessage() + "\"}");
        }
    }
}
