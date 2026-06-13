import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.*;

public class ForgotPasswordServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ForgotPasswordServlet.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String email = request.getParameter("userEmail");
        LOGGER.info("=== FORGOT PASSWORD SERVLET ACTIVATED (DIRECT API MODE) ===");
        LOGGER.info("Target User Email: " + email);
        
        String tempPassword = generateRandomPassword(8);

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            String hashedPassword = PasswordUtil.hashPassword(tempPassword);
            
            PreparedStatement pst = con.prepareStatement("UPDATE users SET password = ? WHERE email = ?");
            pst.setString(1, hashedPassword);
            pst.setString(2, email);
            
            int rowsAffected = pst.executeUpdate();
            
            if (rowsAffected > 0) {
                LOGGER.info("Database updated successfully. Dispatching payload over HTTPS...");
                sendEmailViaAPI(email, tempPassword, "Your Temporary Password");
                response.sendRedirect("forgotpassword.html?status=sent");
            } else {
                LOGGER.warning("Warning: Email Address '" + email + "' not found in Database.");
                response.sendRedirect("forgotpassword.html?status=notfound");
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL FAILURE IN FORGOT PASSWORD SERVLET:", e);
            response.sendRedirect("forgotpassword.html?status=error");
        } finally {
            if (con != null) {
                try { con.close(); } catch (SQLException e) { LOGGER.log(Level.SEVERE, "Error closing connection", e); }
            }
        }
    }

    private void sendEmailViaAPI(String toEmail, String tempPassword, String subject) throws Exception {
        // Direct credentials implementation to safeguard network delivery
        final String apiKey = "xsmtpsib-ec9dbd831b260572b4b49e93550ec3c42100b61313b6c274451f98b55b3ba11f-6nZMgrbU7XsxcyKM";
        final String senderEmail = "gprabodhchandra@gmail.com";

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonPayload = "{"
                + "\"sender\":{\"email\":\"" + senderEmail + "\",\"name\":\"Support Team\"},"
                + "\"to\":[{\"email\":\"" + toEmail + "\"}],"
                + "\"subject\":\"" + subject + "\","
                + "\"textContent\":\"Hello,\\n\\nYour temporary login credentials are:\\nPassword: " + tempPassword + "\\n\\nPlease login and update your credentials immediately.\""
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            LOGGER.info("SUCCESS! Brevo API accepted request. Code: " + responseCode);
        } else {
            LOGGER.severe("Brevo API Rejected Transaction! Response Code: " + responseCode);
            throw new RuntimeException("API Transmission Failure. Code: " + responseCode);
        }
        conn.disconnect();
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
