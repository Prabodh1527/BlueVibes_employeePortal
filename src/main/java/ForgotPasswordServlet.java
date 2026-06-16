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
            response.sendRedirect("forgotpassword.html?status=invalid");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        boolean accountFound = false;
        String fullName = "User";
        String primaryEmail = null;
        String commsEmail = null;
        String targetDeliveryEmail = null;
        int userId = -1;

        try {
            con = DBConnection.getConnection();
            
            // Look up account matching either column
            String sql = "SELECT id, fullname, email, communication_email FROM users WHERE email = ? OR communication_email = ?";
            ps = con.prepareStatement(sql);
            ps.setString(1, inputEmail);
            ps.setString(2, inputEmail);
            rs = ps.executeQuery();

            if (rs.next()) {
                accountFound = true;
                userId = rs.getInt("id");
                fullName = rs.getString("fullname");
                primaryEmail = rs.getString("email");
                commsEmail = rs.getString("communication_email");
                
                System.out.println("[BLUEVIBES LOG] Match Found -> Primary: " + primaryEmail + " | Comms: " + commsEmail);

                // 🌟 SMART DOMAIN IDENTIFICATION ROUTING CRITERIA
                if (commsEmail != null && !commsEmail.trim().isEmpty()) {
                    String commsLower = commsEmail.toLowerCase().trim();
                    
                    // If the communication address uses an academic/external domain, route to safe Gmail fallback instead
                    if (commsLower.contains(".edu") || commsLower.contains(".ac") || commsLower.contains("student")) {
                        System.out.println("[BLUEVIBES LOG] Academic domain detected. Routing to safe primary Gmail: " + primaryEmail);
                        targetDeliveryEmail = primaryEmail.trim();
                    } else {
                        targetDeliveryEmail = commsEmail.trim();
                        System.out.println("[BLUEVIBES LOG] Routing to workspace address: " + targetDeliveryEmail);
                    }
                } else {
                    targetDeliveryEmail = (primaryEmail != null) ? primaryEmail.trim() : null;
                    System.out.println("[BLUEVIBES LOG] No comms email. Defaulting to primary email: " + targetDeliveryEmail);
                }

                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "User";
                }
            }

            if (rs != null) rs.close();
            if (ps != null) ps.close();

            if (accountFound && targetDeliveryEmail != null) {
                // Generate and update token
                String token = UUID.randomUUID().toString();
                String tokenSql = "UPDATE users SET reset_token = ? WHERE id = ?";
                try (PreparedStatement tokenPs = con.prepareStatement(tokenSql)) {
                    tokenPs.setString(1, token);
                    tokenPs.setInt(2, userId);
                    tokenPs.executeUpdate();
                }

                String resetLink = "https://bluevibes-portal.onrender.com/resetpassword.html?token=" + token;
                
                // Dispatch email safely without risk of throwing thread exceptions
                boolean emailSent = sendResetEmail(targetDeliveryEmail, fullName, resetLink);

                // Final dynamic fallback if the primary target failed anyway
                if (!emailSent && !targetDeliveryEmail.equalsIgnoreCase(primaryEmail) && primaryEmail != null) {
                    System.out.println("[BLUEVIBES LOG] Primary delivery failed. Final fallback attempt to: " + primaryEmail);
                    emailSent = sendResetEmail(primaryEmail.trim(), fullName, resetLink);
                }

                if (emailSent) {
                    response.sendRedirect("forgotpassword.html?status=success");
                } else {
                    response.sendRedirect("forgotpassword.html?status=mail_error");
                }
            } else {
                response.sendRedirect("forgotpassword.html?status=not_found");
            }

        } catch (Exception e) {
            System.err.println("[BLUEVIBES ERROR] Database exception: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            try { if (con != null) con.close(); } catch (Exception e) { e.printStackTrace(); }
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

            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{")
                       .append("\"sender\":{\"name\":\"BlueVibes Portal\",\"email\":\"").append(VERIFIED_SENDER_EMAIL).append("\"},")
                       .append("\"to\":[")
                       .append("{\"email\":\"").append(targetEmail).append("\",\"name\":\"").append(userName).append("\"}")
                       .append("],")
                       .append("\"subject\":\"🔒 BlueVibes Access Recovery Request\",")
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
            System.out.println("[BLUEVIBES LOG] Brevo API Response for [" + targetEmail + "]: " + responseCode);

            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("[BLUEVIBES LOG] Brevo Error Context: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("[BLUEVIBES LOG] Network exception caught for address [" + targetEmail + "]: " + e.getMessage());
            return false;
        }
    }
}
