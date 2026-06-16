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

@WebServlet("/ExportReportServlet")
public class ExportReportServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private static final String BREVO_API_KEY = System.getenv("BREVO_API_KEY") != null ? 
            System.getenv("BREVO_API_KEY") : "your_real_brevo_api_key_here";
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String SYSTEM_NOTIFICATION_EMAIL = "admin@bluevibes-employeeportal.onrender.com";

    private void applyCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "POST, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept");
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

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"error\":\"session_expired\"}");
            return;
        }

        String loggedInUser = (String) session.getAttribute("username");
        String userEmail = (String) session.getAttribute("email");
        if (userEmail == null || userEmail.trim().isEmpty()) {
            userEmail = "audit-logs@bluevibes.com"; 
        }

        String excelDataBase64 = request.getParameter("excelData");
        if (excelDataBase64 == null || excelDataBase64.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\":false,\"error\":\"Excel spreadsheet input stream parameter payload is empty.\"}");
            return;
        }

        try {
            String htmlContent = "<html><body>"
                    + "<h2>Weekly Status Report Submitted Successfully</h2>"
                    + "<p><strong>Employee Profile:</strong> " + loggedInUser + "</p>"
                    + "<p>Please view your compiled worksheet attachment below sent securely from the server backend dashboard terminal entry log.</p>"
                    + "<br><p><em>This is an automated operational notification email confirmation message.</em></p>"
                    + "</body></html>";

            String jsonPayload = "{"
                    + "\"sender\":{\"name\":\"BlueVibes Portal Engine\",\"email\":\"" + SYSTEM_NOTIFICATION_EMAIL + "\"},"
                    + "\"to\":[{\"name\":\"" + loggedInUser + "\",\"email\":\"" + userEmail + "\"}],"
                    + "\"subject\":\"Weekly Status Report Audit Attachment: " + loggedInUser + "\","
                    + "\"htmlContent\":\"" + htmlContent.replace("\"", "\\\"") + "\","
                    + "\"attachment\":["
                    + "{"
                    + "\"content\":\"" + excelDataBase64 + "\","
                    + "\"name\":\"Weekly_Status_Report.xlsx\""
                    + "}"
                    + "]"
                    + "}";

            URL url = new URL(BREVO_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                out.print("{\"success\":true}");
            } else {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        errorResponse.append(line.trim());
                    }
                }
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                out.print("{\"success\":false,\"error\":\"Brevo API Delivery Pipeline Rejection\",\"code\":" + responseCode + "}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\":false,\"error\":\"Internal System Mailing Engine Exception Error: " + e.getMessage() + "\"}");
        }
    }
}
