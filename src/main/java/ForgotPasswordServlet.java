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
        LOGGER.info("=== FORGOT PASSWORD SERVLET ACTIVATED (API COMPAT MODE) ===");
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
                LOGGER.info("Database updated successfully. Launching API request pipeline...");
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
        // Safe Environment Fetching to cleanly bypass GitHub filters
        String apiKey = System.getenv("BREVO_API_KEY");
        String senderEmail = System.getenv("BREVO_SENDER_EMAIL");

        if (apiKey == null || senderEmail == null) {
            LOGGER.severe("ERROR: BREVO_API_KEY or BREVO_SENDER_EMAIL is completely missing from Render variables configuration!");
            throw new RuntimeException("Missing credentials config.");
        }

        URL url = new URL("https://api.brevo.com/v3/smtp/email");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("api-key", apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonPayload = "{"
                + "\"sender\":{\"email\":\"" + senderEmail.trim() + "\",\"name\":\"Support Team\"},"
                + "\"to\":[{\"email\":\"" + toEmail.trim() + "\"}],"
                + "\"subject\":\"" + subject + "\","
                + "\"textContent\":\"Hello,\\n\\nYour temporary login credentials are:\\nPassword: " + tempPassword + "\\n\\nPlease login and update your credentials immediately.\""
                + "}";

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode == 201 || responseCode == 200) {
            LOGGER.info("SUCCESS! Brevo Web API accepted payload. Response Status Code: " + responseCode);
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
