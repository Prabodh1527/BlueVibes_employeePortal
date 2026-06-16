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
    
    // Brevo Gateway Connection Constants
    private static final String BREVO_API_KEY = "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";
    private static final String VERIFIED_SENDER_EMAIL = "gprabodhchandra@gmail.com";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String inputEmail = request.getParameter("email");
        
        if (inputEmail != null) {
            inputEmail = inputEmail.trim().toLowerCase();
        }

        if (inputEmail == null || inputEmail.isEmpty()) {
            response.sendRedirect("forgotpassword.html?status=invalid");
            return;
        }

        // 🛡️ DOMAIN FILTER: Only allow approved workspace extensions or standard Gmail
        boolean isAllowedDomain = inputEmail.endsWith("@bluevibes.com") || 
                                  inputEmail.endsWith("@bluegitalllp.com") || 
                                  inputEmail.endsWith("@gmail.com");

        if (!isAllowedDomain) {
            System.out.println("[FILTER BLOCKED] Prevented dispatch to prohibited domain: " + inputEmail);
            response.sendRedirect("forgotpassword.html?status=domain_blocked");
            return;
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        boolean accountFound = false;
        boolean commsColumnExists = true;
        
        String fullName = "User";
        String primaryEmail = null;
        String commsEmail = null;
        String targetDeliveryEmail = null;
        int userId = -1;

        try {
            con = DBConnection.getConnection();
            
            // Defensively evaluate if 'communication_email' table column exists
            try {
                String testSql = "SELECT communication_email FROM users LIMIT 1";
                try (PreparedStatement testPs = con.prepareStatement(testSql)) {
                    testPs.executeQuery();
                }
            } catch (Exception e) {
                commsColumnExists = false;
            }
            
            if (commsColumnExists) {
                String sql = "SELECT id, fullname, email, communication_email FROM users WHERE email = ? OR communication_email = ?";
                ps = con.prepareStatement(sql);
                ps.setString(1, inputEmail);
                ps.setString(2, inputEmail);
            } else {
                String sql = "SELECT id, fullname, email FROM users WHERE email = ?";
                ps = con.prepareStatement(sql);
                ps.setString(1, inputEmail);
            }
            
            rs = ps.executeQuery();

            if (rs.next()) {
                accountFound = true;
                userId = rs.getInt("id");
                fullName = rs.getString("fullname");
                primaryEmail = rs.getString("email");
                
                if (commsColumnExists) {
                    commsEmail = rs.getString("communication_email");
                }
                
                // Determine destination address based on whitelist rules
                if (commsEmail != null && !commsEmail.trim().isEmpty()) {
                    String commsLower = commsEmail.toLowerCase().trim();
                    if (commsLower.endsWith("@bluevibes.com") || commsLower.endsWith("@bluegitalllp.com") || commsLower.endsWith("@gmail.com")) {
                        targetDeliveryEmail = commsEmail.trim();
                    } else {
                        targetDeliveryEmail = (primaryEmail != null) ? primaryEmail.trim() : null;
                    }
                } else {
                    targetDeliveryEmail = (primaryEmail != null) ? primaryEmail.trim() : null;
                }

                if (fullName == null || fullName.trim().isEmpty()) {
                    fullName = "User";
                }
            }

            if (rs != null) rs.close();
            if (ps != null) ps.close();

            if (accountFound && targetDeliveryEmail != null) {
                String token = UUID.randomUUID().toString();
                
                // Secure transaction persistence block
                try {
                    String tokenSql = "UPDATE users SET reset_token = ? WHERE id = ?";
                    try (PreparedStatement tokenPs = con.prepareStatement(tokenSql)) {
                        tokenPs.setString(1, token);
                        tokenPs.setInt(2, userId);
                        tokenPs.executeUpdate();
                    }
                } catch (Exception sqlEx) {
                    System.err.println("[DB WARNING] Problem saving token. Ensure 'reset_token' is live: " + sqlEx.getMessage());
                }

                String resetLink = "https://bluevibes-portal.onrender.com/resetpassword.html?token=" + token;
                
                // Fire Outbound API Payload via Brevo Engine
                boolean emailSent = sendResetEmail(targetDeliveryEmail, fullName, resetLink);

                if (emailSent) {
                    response.sendRedirect("forgotpassword.html?status=success");
                } else {
                    response.sendRedirect("forgotpassword.html?status=mail_error");
                }
            } else {
                response.sendRedirect("forgotpassword.html?status=not_found");
            }

        } catch (Exception e) {
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
                       .append("<p>This automated security copy was sent directly to your workspace inbox: <strong>").append(targetEmail).append("</strong></p>")
                       .append("</body></html>\"")
                       .append("}");

            String jsonPayload = jsonBuilder.toString();

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 201 || responseCode == 200) {
                return true;
            } else {
                try (InputStream errorStream = conn.getErrorStream()) {
                    if (errorStream != null) {
                        String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("[BREVO ERROR CODE " + responseCode + "] System Context: " + errorResponse);
                    }
                }
                return false;
            }

        } catch (Exception e) {
            System.err.println("[SMTP PIPE ERROR] " + e.getMessage());
            return false;
        }
    }
}
