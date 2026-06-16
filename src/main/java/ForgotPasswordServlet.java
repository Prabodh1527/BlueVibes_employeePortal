import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/ForgotPasswordServlet")
public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    // Core Email Configurations
    private static final String BREVO_API_KEY = "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";
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@gmail.com";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputEmail = request.getParameter("email");
        System.out.println("[BLUEVIBES LOG] Incoming password reset request for: " + inputEmail);

        if (inputEmail != null) {
            inputEmail = inputEmail.trim();
        }

        if (inputEmail == null || inputEmail.isEmpty()) {
            System.out.println("[BLUEVIBES LOG] Request parameter 'email' came back empty or null!");
            response.sendRedirect("forgotpassword.html?status=invalid");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        boolean accountFound = false;
        String fullName = "User";
        String targetDeliveryEmail = null;
        int userId = -1;

        try {
            con = DBConnection.getConnection();
            System.out.println("[BLUEVIBES LOG] Database connection established successfully.");
            
            // 🔍 Unified Lookup matching either column
            String sql = "SELECT id, fullname, email, communication_email FROM users WHERE email = ? OR communication_email = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, inputEmail);
            ps.setString(2, inputEmail);
            rs = ps.executeQuery();

            if (rs.next()) {
                accountFound = true;
                userId = rs.getInt("id");
                fullName = rs.getString("fullname");
                
                String primaryEmail = rs.getString("email");
                String commsEmail = rs.getString("communication_email");
                
                System.out.println("[BLUEVIBES LOG] Record matched! User ID: " + userId + ", Name: " + fullName);
                System.out.println("[BLUEVIBES LOG] Database values -> Primary: " + primaryEmail + " | Communication: " + commsEmail);

                // 🌟 EXCLUSIVE ROUTING CRITERIA
                if (commsEmail != null && !commsEmail.trim().isEmpty()) {
                    targetDeliveryEmail = commsEmail.trim();
                    System.out.println("[BLUEVIBES LOG] Routing chosen: Exclusive Communication Email -> " + targetDeliveryEmail);
                } else {
                    targetDeliveryEmail = (primaryEmail != null) ? primaryEmail.trim() : null;
                    System.out.println("[BLUEVIBES LOG] Routing chosen: Fallback Primary Email -> " + targetDeliveryEmail);
                }

                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "User";
                }
            }

            // Close lookup resources early
            if (rs != null) rs.close();
            if (ps != null) ps.close();

            if (accountFound && targetDeliveryEmail != null) {
                // Generate tracking token identifier
                String token = UUID.randomUUID().toString();
                
                // Update user account token
                String tokenSql = "UPDATE users SET reset_token = ? WHERE id = ?";
                try (PreparedStatement tokenPs = con.prepareStatement(tokenSql)) {
                    tokenPs.setString(1, token);
                    tokenPs.setInt(2, userId);
                    tokenPs.executeUpdate();
                }
                System.out.println("[BLUEVIBES LOG] Secure tracking token generated and stored in database.");

                String resetLink = "https://bluevibes-portal.onrender.com/resetpassword.html?token=" + token;
                
                // Dispatch exactly one message to the target email address
                System.out.println("[BLUEVIBES LOG] Triggering API email dispatch to: " + targetDeliveryEmail);
                boolean emailSent = sendResetEmail(targetDeliveryEmail, fullName, resetLink);

                if (emailSent) {
                    System.out.println("[BLUEVIBES LOG] Email successfully accepted by Brevo Gateway!");
                    response.sendRedirect("forgotpassword.html?status=success");
                } else {
                    System.out.println("[BLUEVIBES LOG] Brevo server returned an error code.");
                    response.sendRedirect("forgotpassword.html?status=mail_error");
                }
            } else {
                System.out.println("[BLUEVIBES LOG] Email address typed could not be matched to an active user record.");
                response.sendRedirect("forgotpassword.html?status=not_found");
            }

        } catch (Exception e) {
            System.err.println("[BLUEVIBES CRITICAL ERROR] Exception occurred in doPost processing:");
            e.printStackTrace();
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
            System.out.println("[BLUEVIBES LOG] Database connection safely recycled to pool.");
        }
    }

    private boolean sendResetEmail(String targetEmail, String userName, String resetLink) {
        try {
            URL url = new URL("https://api.brevo.com/v3/smtp/email");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            
            conn.setRequestMethod("POST");
            conn.setRequestProperty("api-key", BREVO_API_KEY.trim());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            // Construct transactional json packet payload string
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                       .append("\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"").append(VERIFIED_SENDER_EMAIL).append("\"},")
                       .append("\"to\":[")
                       .append("{\"email\":\"").append(targetEmail).append("\",\"name\":\"").append(userName).append("\"}")
                       .append("],")
                       .append("\"subject\":\"🔒 BlueVibes Access Recovery Request\",") // 🌟 NEW DISTINCT SUBJECT LINE
                       .append("\"htmlContent\":\"<html><body>")
                       .append("<h3>Hello ").append(userName).append(",</h3>")
                       .append("<p>We received a request to recover your portal credentials. Click the button below to secure your identity:</p>")
                       .append("<p><a href='").append(resetLink).append("' style='background:#0284c7;color:white;padding:10px 20px;text-decoration:none;border-radius:5px;display:inline-block;font-weight:bold;'>Reset Password</a></p>")
                       .append("<p>This security notification copy was sent directly to your designated workspace inbox: <strong>").append(targetEmail).append("</strong></p>")
                       .append("<p>If you didn't request this, you can safely ignore this automated message.</p>")
                       .append("</body></html>\"")
                       .append("}");

            String jsonPayload = jsonBuilder.toString();

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            System.out.println("[BLUEVIBES LOG] Brevo Server HTTP API Code: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("[BLUEVIBES LOG] Brevo Rejection Details: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("[BLUEVIBES LOG] Exception caught within Brevo transmission function: " + e.getMessage());
            return false;
        }
    }
}
